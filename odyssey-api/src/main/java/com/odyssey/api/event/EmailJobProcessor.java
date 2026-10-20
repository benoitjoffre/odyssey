package com.odyssey.api.event;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class EmailJobProcessor {

    private static final Logger logger = LoggerFactory.getLogger(EmailJobProcessor.class);
    private final EmailJobRepository repository;
    private final EmailJobQueue queue;
    private final EmailService emailService;
    private final ObjectMapper mapper;
    private final boolean enabled;

    public EmailJobProcessor(EmailJobRepository repository, EmailJobQueue queue, EmailService emailService,
        ObjectMapper mapper, @Value("${app.email.enabled:false}") boolean enabled) {
        this.repository = repository;
        this.queue = queue;
        this.emailService = emailService;
        this.mapper = mapper;
        this.enabled = enabled;
        if (!enabled) {
            logger.warn("Email delivery is disabled; durable jobs remain pending. Set EMAIL_ENABLED=true to send.");
        }
    }

    @Scheduled(fixedDelayString = "${app.email.poll-delay-ms:5000}")
    public void processPending() {
        if (!enabled) {
            return;
        }
        Instant now = Instant.now();
        Instant cutoff = now.minus(Duration.ofMinutes(10));
        int recovered = repository.recover(cutoff, now);
        int failed = repository.failExpired(cutoff);
        if (recovered > 0 || failed > 0) {
            logger.error("EMAIL_QUEUE_LEASE_EXPIRED: recovered={}, permanently_failed={}", recovered, failed);
        }
        for (Long id : repository.findReady(now, PageRequest.of(0, 100))) {
            process(id);
        }
    }

    private void process(Long id) {
        String token = UUID.randomUUID().toString();
        if (repository.claim(id, Instant.now(), token) != 1) {
            return;
        }
        EmailJob job = repository.findById(id).orElseThrow();
        try {
            if ("DELIVERY".equals(job.getType())) {
                // Resend keys expire after 24h; do not blindly resend an uncertain delivery beyond that window.
                if (job.getFirstAttemptAt().isBefore(Instant.now().minus(Duration.ofHours(23)))) {
                    repository.fail(id, token, EmailJob.Status.FAILED, Instant.now(),
                        "Idempotency safety window expired; verify delivery with Resend before manual retry");
                    logger.error("EMAIL_DELIVERY_REQUIRES_REVIEW: job={}", id);
                    return;
                }
                String providerId = emailService.sendEmail(
                    mapper.readValue(job.getPayload(), EmailMessage.class), job.getDeduplicationKey());
                if (repository.complete(id, token, Instant.now(), providerId) != 1) {
                    throw new IllegalStateException("Email delivery lease lost for job " + id);
                }
                logger.info("Email accepted by provider: job={}, providerId={}", id, providerId);
            } else {
                queue.prepare(job);
            }
        } catch (Exception exception) {
            boolean exhausted = job.getAttempts() >= 8;
            long delaySeconds = Math.min(3600, 30L << Math.min(job.getAttempts() - 1, 7));
            repository.fail(id, token, exhausted ? EmailJob.Status.FAILED : EmailJob.Status.PENDING,
                Instant.now().plusSeconds(delaySeconds), exception.getClass().getSimpleName());
            // Avoid provider response bodies and recipient/content data in logs.
            if (exhausted) {
                logger.error("EMAIL_JOB_FAILED: job={}, type={}, attempts={}, error={}",
                    id, job.getType(), job.getAttempts(), exception.getClass().getSimpleName());
            } else {
                logger.warn("Email job retry scheduled: job={}, type={}, attempt={}, delaySeconds={}, error={}",
                    id, job.getType(), job.getAttempts(), delaySeconds, exception.getClass().getSimpleName());
            }
        }
    }
}

package com.odyssey.api.event;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

class EmailJobProcessorTest {

    private final EmailJobRepository repository = mock(EmailJobRepository.class);
    private final EmailJobQueue queue = mock(EmailJobQueue.class);
    private final EmailService sender = mock(EmailService.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private EmailJobProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new EmailJobProcessor(repository, queue, sender, mapper, true);
    }

    @Test
    void disabledDeliveryLeavesAllJobsUntouched() {
        new EmailJobProcessor(repository, queue, sender, mapper, false).processPending();
        verifyNoInteractions(repository, queue, sender);
    }

    @Test
    void persistsProviderAcceptanceWithStableIdempotencyKey() {
        EmailJob job = delivery(1);
        arrange(job);
        when(sender.sendEmail(any(), eq("stable-key"))).thenReturn("resend-123");
        when(repository.complete(eq(1L), anyString(), any(), eq("resend-123"))).thenReturn(1);
        processor.processPending();
        verify(sender).sendEmail(any(EmailMessage.class), eq("stable-key"));
        verify(repository).complete(eq(1L), anyString(), any(), eq("resend-123"));
        verify(repository, never()).fail(any(), anyString(), any(), any(), anyString());
    }

    @Test
    void failureIsRetriedWithBackoffRatherThanCompleted() {
        arrange(delivery(1));
        when(sender.sendEmail(any(), anyString())).thenThrow(new EmailDeliveryException("Provider unavailable", null));
        Instant before = Instant.now();
        processor.processPending();
        verify(repository).fail(eq(1L), anyString(), eq(EmailJob.Status.PENDING),
            argThat(next -> next.isAfter(before.plusSeconds(29))), eq("EmailDeliveryException"));
        verify(repository, never()).complete(any(), anyString(), any(), any());
    }

    @Test
    void finalFailureRequiresOperatorAttention() {
        arrange(delivery(8));
        when(sender.sendEmail(any(), anyString())).thenThrow(new IllegalStateException("Unavailable"));
        processor.processPending();
        verify(repository).fail(eq(1L), anyString(), eq(EmailJob.Status.FAILED), any(), anyString());
    }

    @Test
    void neverAutomaticallyResendsOutsideProviderIdempotencyWindow() {
        EmailJob job = delivery(2);
        ReflectionTestUtils.setField(job, "firstAttemptAt", Instant.now().minus(Duration.ofHours(24)));
        arrange(job);
        processor.processPending();
        verifyNoInteractions(sender);
        verify(repository).fail(eq(1L), anyString(), eq(EmailJob.Status.FAILED), any(), contains("safety window"));
    }

    @Test
    void losingClaimDoesNotSend() {
        when(repository.findReady(any(), any())).thenReturn(List.of(1L));
        processor.processPending();
        verifyNoInteractions(sender, queue);
        verify(repository, never()).findById(any());
    }

    @Test
    void preparationFailureIsRetriedIndependently() {
        EmailJob job = new EmailJob("event", "AGENT:BOOKING_REQUESTED", "{}");
        ReflectionTestUtils.setField(job, "id", 1L);
        ReflectionTestUtils.setField(job, "attempts", 1);
        arrange(job);
        doThrow(new IllegalStateException("No available agent")).when(queue).prepare(job);
        processor.processPending();
        verify(repository).fail(eq(1L), anyString(), eq(EmailJob.Status.PENDING), any(), anyString());
        verifyNoInteractions(sender);
    }

    private EmailJob delivery(int attempts) {
        EmailMessage message = new EmailMessage("a@example.com", "Bienvenue", "<html></html>", "Bonjour");
        EmailJob job = new EmailJob("stable-key", "DELIVERY", mapper.writeValueAsString(message));
        ReflectionTestUtils.setField(job, "id", 1L);
        ReflectionTestUtils.setField(job, "attempts", attempts);
        ReflectionTestUtils.setField(job, "firstAttemptAt", Instant.now());
        return job;
    }

    private void arrange(EmailJob job) {
        when(repository.findReady(any(), any())).thenReturn(List.of(1L));
        when(repository.claim(eq(1L), any(), anyString())).thenReturn(1);
        when(repository.findById(1L)).thenReturn(Optional.of(job));
    }
}

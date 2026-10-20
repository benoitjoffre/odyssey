package com.odyssey.api.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.resend.Resend;
import com.resend.core.net.RequestOptions;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResendAdapterTest {

    @Test
    void sendsMultipartMessageWithReplyToAndIdempotencyKey() throws Exception {
        var adapter = new ResendAdapter("test-key", "Odyssey <mail@example.com>",
            "help@example.com", true, "https://example.com");
        Resend resend = mock(Resend.class);
        Emails emails = mock(Emails.class);
        when(resend.emails()).thenReturn(emails);
        when(emails.send(any(CreateEmailOptions.class), any(RequestOptions.class)))
            .thenReturn(new CreateEmailResponse("email-123"));
        ReflectionTestUtils.setField(adapter, "resend", resend);

        assertEquals("email-123", adapter.sendEmail(
            new EmailMessage("a@example.com", "Hello", "<p>Hello</p>", "Hello"), "job-123"));

        var options = ArgumentCaptor.forClass(CreateEmailOptions.class);
        var request = ArgumentCaptor.forClass(RequestOptions.class);
        verify(emails).send(options.capture(), request.capture());
        assertEquals(List.of("help@example.com"), options.getValue().getReplyTo());
        assertEquals("Hello", options.getValue().getText());
        assertEquals("<p>Hello</p>", options.getValue().getHtml());
        assertEquals("job-123", request.getValue().getIdempotencyKey());
    }

    @Test
    void enablingDeliveryRequiresCredentialsAndPublicHttpsFrontend() {
        assertThrows(IllegalStateException.class,
            () -> new ResendAdapter("", "Odyssey <mail@example.com>", "", true, "https://example.com"));
        assertThrows(IllegalStateException.class,
            () -> new ResendAdapter("test", "Odyssey <mail@example.com>", "", true, "http://localhost:5173"));
        assertThrows(IllegalStateException.class,
            () -> new ResendAdapter("test", "", "", true, "https://example.com"));
    }

    @Test
    void missingApiKeyNeverReturnsSuccess() {
        ResendAdapter adapter = new ResendAdapter("", "Odyssey <mail@example.com>", "", false, "http://localhost:5173");
        assertThrows(IllegalStateException.class,
            () -> adapter.sendEmail(new EmailMessage("a@example.com", "Hello", "<p>Hello</p>", "Hello"), "test-key"));
    }

    @Test
    void disabledFlagBlocksDeliveryEvenWhenAnApiKeyExists() {
        var adapter = new ResendAdapter("test-key", "Odyssey <mail@example.com>", "", false, "https://example.com");
        assertThrows(IllegalStateException.class,
            () -> adapter.sendEmail(new EmailMessage("a@example.com", "Hello", "<p>Hello</p>", "Hello"), "test-key"));
    }
}

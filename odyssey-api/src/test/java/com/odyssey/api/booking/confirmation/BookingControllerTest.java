package com.odyssey.api.booking.confirmation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;

class BookingControllerTest {

    private BookingService bookingService;
    private BookingController controller;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        bookingService = mock(BookingService.class);
        controller = new BookingController(bookingService);
        jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("auth0|agent-a");
    }

    @Test
    void createBookingForwardsAuthenticatedAgentSubject() throws Exception {
        controller.createBooking(42L, jwt);

        verify(bookingService).createBooking(42L, "auth0|agent-a");
        assertAgentOnly("createBooking", Long.class, Jwt.class);
    }

    @Test
    void confirmBookingForwardsAuthenticatedAgentSubject() throws Exception {
        controller.confirmBooking(500L, jwt);

        verify(bookingService).confirmBooking(500L, "auth0|agent-a");
        assertAgentOnly("confirmBooking", Long.class, Jwt.class);
    }

    @Test
    void updateProviderDetailsForwardsAuthenticatedAgentSubject() throws Exception {
        UpdateProviderDetailsRequest request = new UpdateProviderDetailsRequest(
            "PNR-123",
            "https://provider.example.com/pay/PNR-123",
            ProviderPaymentStatus.PAYMENT_REQUIRED
        );

        controller.updateProviderDetails(500L, jwt, request);

        verify(bookingService).updateProviderDetails(
            500L,
            "auth0|agent-a",
            request.providerReference(),
            request.providerPaymentUrl(),
            request.providerPaymentStatus()
        );
        assertAgentOnly(
            "updateProviderDetails",
            Long.class,
            Jwt.class,
            UpdateProviderDetailsRequest.class
        );
    }

    private void assertAgentOnly(String methodName, Class<?>... parameterTypes)
        throws Exception {
        Method method = BookingController.class.getMethod(methodName, parameterTypes);
        assertEquals(
            "hasRole('AGENT')",
            method.getAnnotation(PreAuthorize.class).value()
        );
    }
}
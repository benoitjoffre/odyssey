package com.odyssey.api.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;

import com.odyssey.api.quote.QuoteService;

class BookingRequestControllerTest {

    private QuoteService quoteService;
    private BookingRequestService bookingRequestService;
    private BookingRequestController controller;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        quoteService = mock(QuoteService.class);
        bookingRequestService = mock(BookingRequestService.class);
        controller = new BookingRequestController(
            bookingRequestService,
            quoteService
        );
        jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("auth0|agent-a");
    }

    @Test
    void getQuotesForwardsAuthenticatedAgentSubject() throws Exception {
        controller.getQuotes(10L, jwt);

        verify(quoteService).getQuotesByBookingRequest(10L, "auth0|agent-a");

        Method method = BookingRequestController.class.getMethod(
            "getQuotes",
            Long.class,
            Jwt.class
        );
        assertEquals(
            "hasRole('AGENT')",
            method.getAnnotation(PreAuthorize.class).value()
        );
    }

    @Test
    void claimForwardsAuthenticatedAgentSubject() throws Exception {
        controller.claimBookingRequest(10L, jwt);

        verify(bookingRequestService).claimBookingRequest(10L, "auth0|agent-a");

        Method method = BookingRequestController.class.getMethod(
            "claimBookingRequest",
            Long.class,
            Jwt.class
        );
        assertEquals(
            "hasRole('AGENT')",
            method.getAnnotation(PreAuthorize.class).value()
        );
    }
}
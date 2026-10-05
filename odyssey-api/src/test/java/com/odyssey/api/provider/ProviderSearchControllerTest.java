package com.odyssey.api.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;

class ProviderSearchControllerTest {

    private ProviderSearchService providerSearchService;
    private ProviderSearchController controller;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        providerSearchService = mock(ProviderSearchService.class);
        controller = new ProviderSearchController(providerSearchService);
        jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("auth0|agent-a");
    }

    @Test
    void searchOffersForwardsAuthenticatedSubjectAndIsAgentOnly()
        throws Exception {
        controller.searchOffers(10L, jwt);

        verify(providerSearchService).search(10L, "auth0|agent-a");

        Method method = ProviderSearchController.class.getMethod(
            "searchOffers",
            Long.class,
            Jwt.class
        );
        assertEquals(
            "hasRole('AGENT')",
            method.getAnnotation(PreAuthorize.class).value()
        );
    }
}

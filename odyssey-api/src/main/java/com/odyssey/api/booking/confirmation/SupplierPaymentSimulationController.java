package com.odyssey.api.booking.confirmation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings/{bookingId}/supplier-payment-simulation")
@Profile("(dev | test) & !prod & !production")
@ConditionalOnProperty(name = "app.supplier-payment-simulation.enabled", havingValue = "true")
@PreAuthorize("hasRole('TRAVELER')")
public class SupplierPaymentSimulationController {

    private final SupplierPaymentSimulationService service;

    public SupplierPaymentSimulationController(SupplierPaymentSimulationService service) {
        this.service = service;
    }

    @GetMapping
    public SupplierPaymentSimulationResponse getPayment(
        @PathVariable Long bookingId, @AuthenticationPrincipal Jwt jwt
    ) {
        return service.getPayment(bookingId, jwt.getSubject());
    }

    @PostMapping
    public SupplierPaymentSimulationResponse simulatePayment(
        @PathVariable Long bookingId, @AuthenticationPrincipal Jwt jwt
    ) {
        return service.simulatePayment(bookingId, jwt.getSubject());
    }
}

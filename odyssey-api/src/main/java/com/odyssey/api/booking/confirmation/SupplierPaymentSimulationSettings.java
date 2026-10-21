package com.odyssey.api.booking.confirmation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class SupplierPaymentSimulationSettings {

    private final boolean enabled;

    public SupplierPaymentSimulationSettings(
        Environment environment,
        @Value("${app.supplier-payment-simulation.enabled:false}") boolean enabled
    ) {
        this.enabled = enabled && environment.acceptsProfiles(
            Profiles.of("(dev | test) & !prod & !production")
        );
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String paymentUrl(Booking booking) {
        if (enabled && booking.getProviderPaymentStatus() == ProviderPaymentStatus.PAYMENT_REQUIRED) {
            return "/traveler/supplier-payment-simulation/" + booking.getId();
        }
        return booking.getProviderPaymentUrl();
    }
}

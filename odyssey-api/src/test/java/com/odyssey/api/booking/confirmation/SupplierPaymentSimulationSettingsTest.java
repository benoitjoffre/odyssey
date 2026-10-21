package com.odyssey.api.booking.confirmation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class SupplierPaymentSimulationSettingsTest {

    @ParameterizedTest
    @CsvSource({
        "dev,true,true", "test,true,true", "dev,false,false", "test,false,false",
        "prod,true,false", "production,true,false", "default,true,false"
    })
    void requiresExplicitFlagAndDevelopmentOrTestProfile(String profile, boolean flag, boolean expected) {
        var environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        assertEquals(expected, new SupplierPaymentSimulationSettings(environment, flag).isEnabled());
    }

    @Test
    void productionProfileOverridesDevelopmentAndTest() {
        for (String productionProfile : new String[]{"prod", "production"}) {
            var environment = new MockEnvironment();
            environment.setActiveProfiles("dev", "test", productionProfile);
            var settings = new SupplierPaymentSimulationSettings(environment, true);
            Booking booking = new Booking();
            booking.setProviderPaymentStatus(ProviderPaymentStatus.PAYMENT_REQUIRED);
            booking.setProviderPaymentUrl("https://supplier.example/payment");
            assertFalse(settings.isEnabled());
            assertEquals("https://supplier.example/payment", settings.paymentUrl(booking));
        }
    }

    @Test
    void onlyOverridesPaymentRequiredLinksWithoutMutatingStoredUrl() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        var settings = new SupplierPaymentSimulationSettings(environment, true);
        Booking booking = new Booking();
        org.springframework.test.util.ReflectionTestUtils.setField(booking, "id", 42L);
        booking.setProviderPaymentUrl("https://supplier.example/payment");
        booking.setProviderPaymentStatus(ProviderPaymentStatus.PAYMENT_REQUIRED);
        assertEquals("/traveler/supplier-payment-simulation/42", settings.paymentUrl(booking));
        assertEquals("https://supplier.example/payment", booking.getProviderPaymentUrl());
        booking.setProviderPaymentStatus(ProviderPaymentStatus.PAID_TO_PROVIDER);
        assertEquals("https://supplier.example/payment", settings.paymentUrl(booking));
    }
}

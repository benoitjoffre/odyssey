package com.odyssey.api.booking.confirmation;

public record UpdateProviderDetailsRequest(
    String providerReference,
    String providerPaymentUrl,
    ProviderPaymentStatus providerPaymentStatus
) {}

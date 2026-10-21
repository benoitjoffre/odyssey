package com.odyssey.api.booking.confirmation;

import com.odyssey.api.need.NeedType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SupplierPaymentSimulationResponse(
    Long bookingId,
    Long tripId,
    String tripTitle,
    LocalDate tripStartDate,
    LocalDate tripEndDate,
    String travelerFirstName,
    String travelerLastName,
    String travelerEmail,
    String provider,
    String providerReference,
    String description,
    NeedType serviceType,
    String notes,
    String origin,
    String destination,
    String city,
    Integer travelers,
    Integer rooms,
    BigDecimal amount,
    String currency,
    ProviderPaymentStatus providerPaymentStatus
) {}

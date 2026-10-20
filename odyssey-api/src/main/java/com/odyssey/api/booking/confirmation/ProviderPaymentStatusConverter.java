package com.odyssey.api.booking.confirmation;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores {@link ProviderPaymentStatus} by name. Rows written before the
 * removal of the former {@code UNKNOWN} status are read back as
 * {@link ProviderPaymentStatus#NOT_REQUIRED_YET}, which had the same meaning.
 */
@Converter
public class ProviderPaymentStatusConverter implements AttributeConverter<ProviderPaymentStatus, String> {

    private static final String LEGACY_UNKNOWN = "UNKNOWN";

    @Override
    public String convertToDatabaseColumn(ProviderPaymentStatus status) {
        return status == null ? null : status.name();
    }

    @Override
    public ProviderPaymentStatus convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        if (LEGACY_UNKNOWN.equals(value)) {
            return ProviderPaymentStatus.NOT_REQUIRED_YET;
        }
        return ProviderPaymentStatus.valueOf(value);
    }
}

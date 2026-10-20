package com.odyssey.api.booking.confirmation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ProviderPaymentStatusConverterTest {

    private final ProviderPaymentStatusConverter converter = new ProviderPaymentStatusConverter();

    @Test
    void legacyUnknownIsReadAsNotDeterminedYet() {
        assertEquals(ProviderPaymentStatus.NOT_REQUIRED_YET, converter.convertToEntityAttribute("UNKNOWN"));
    }

    @Test
    void currentStatusesRoundTrip() {
        for (ProviderPaymentStatus status : ProviderPaymentStatus.values()) {
            assertEquals(status, converter.convertToEntityAttribute(converter.convertToDatabaseColumn(status)));
        }
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToDatabaseColumn(null));
    }
}

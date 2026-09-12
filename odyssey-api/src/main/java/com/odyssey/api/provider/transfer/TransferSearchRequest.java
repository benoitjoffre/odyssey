package com.odyssey.api.provider.transfer;

import java.time.LocalDate;

public record TransferSearchRequest(
        String pickupLocation,
        String dropoffLocation,
        LocalDate date,
        int travelers
) {
}
package com.odyssey.api.trip;

import com.odyssey.api.booking.BookingRequestStatus;
import com.odyssey.api.booking.confirmation.BookingStatus;
import com.odyssey.api.need.NeedStatus;
import com.odyssey.api.need.NeedType;

public record TripNeedResponse(
    Long id,
    NeedType type,
    NeedStatus status,
    String notes,
    BookingRequestStatus bookingRequestStatus,
    BookingStatus bookingStatus,
    String providerConfirmationId
) {}
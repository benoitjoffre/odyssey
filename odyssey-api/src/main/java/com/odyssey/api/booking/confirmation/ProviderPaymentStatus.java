package com.odyssey.api.booking.confirmation;

/**
 * Status of the Traveler's direct payment to the Provider for a
 * {@link Booking}. Entirely independent from {@link Booking#getStatus()}
 * and from the Odyssey assistance {@code Payment}: Odyssey never
 * collects or verifies this payment, it only lets the Agent record what
 * they know about it (manually, for V1).
 */
public enum ProviderPaymentStatus {

    /** The Agent has not yet determined whether/when the Provider must be paid. */
    NOT_REQUIRED_YET,

    /** The Traveler must pay the Provider directly (see {@code providerPaymentUrl}). */
    PAYMENT_REQUIRED,

    /** The Agent has manually recorded that the Provider has been paid. */
    PAID_TO_PROVIDER,

    /** Status could not be determined. */
    UNKNOWN
}

export type BookingStatus = "PENDING" | "CONFIRMED";

export type ProviderPaymentStatus = "NOT_REQUIRED_YET" | "PAYMENT_REQUIRED" | "PAID_TO_PROVIDER" | "UNKNOWN";

export interface Booking {
  id: number;
  quoteId: number;
  bookingRequestId: number;
  status: BookingStatus;
  providerConfirmationId: string | null;
  providerReference: string | null;
  providerPaymentUrl: string | null;
  providerPaymentStatus: ProviderPaymentStatus;
  createdAt: string;
  confirmedAt: string | null;
}

export interface UpdateBookingProviderDetailsRequest {
  providerReference: string;
  providerPaymentUrl: string;
  providerPaymentStatus: ProviderPaymentStatus;
}

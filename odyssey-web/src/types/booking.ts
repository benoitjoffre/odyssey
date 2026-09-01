export type BookingStatus = "PENDING" | "CONFIRMED";

export interface Booking {
  id: number;
  quoteId: number;
  bookingRequestId: number;
  status: BookingStatus;
  providerConfirmationId: string | null;
  createdAt: string;
  confirmedAt: string | null;
}

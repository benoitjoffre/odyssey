export type TravelerQuoteStatus = "SENT" | "ACCEPTED" | "REJECTED" | "EXPIRED";

export interface TravelerQuote {
  id: number;
  bookingRequestId: number;
  price: number;
  currency: string;
  description: string;
  status: TravelerQuoteStatus;
  createdAt: string;
  expiresAt: string | null;
}

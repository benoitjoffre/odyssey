import type { PaymentStatus } from "./payment";

export type TravelerQuoteStatus = "SENT" | "ACCEPTED" | "REJECTED" | "EXPIRED";

export interface TravelerQuote {
  id: number;
  bookingRequestId: number;
  totalAmount: number;
  currency: string;
  description: string;
  status: TravelerQuoteStatus;
  createdAt: string;
  expiresAt: string | null;
  paymentStatus: PaymentStatus | null;
}

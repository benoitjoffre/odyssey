import type { PaymentStatus } from "./payment";
import type { ProviderPaymentStatus } from "./booking";

export type TravelerQuoteStatus = "SENT" | "ACCEPTED" | "REJECTED" | "EXPIRED";

export interface TravelerQuote {
  id: number;
  bookingRequestId: number;
  providerPrice: number;
  assistanceFee: number;
  totalAmount: number;
  currency: string;
  description: string;
  status: TravelerQuoteStatus;
  createdAt: string;
  expiresAt: string | null;
  paymentStatus: PaymentStatus | null;
  providerPaymentUrl: string | null;
  providerPaymentStatus: ProviderPaymentStatus;
}

import type { ProviderPaymentStatus } from "./booking";
import type { PaymentStatus } from "./payment";

export interface CreateQuoteRequest {
  provider: string;
  externalOfferId: string;
  providerPrice: number;
  assistanceFee: number;
  currency: string;
  description: string;
  expiresAt: string | null;
}

export interface QuoteResponse {
  id: number;
  bookingRequestId: number;
  provider: string;
  externalOfferId: string;
  providerPrice: number;
  assistanceFee: number;
  totalAmount: number;
  currency: string;
  description: string;
  status: string;
  createdAt: string;
  expiresAt: string | null;
}

export interface AgentQuoteResponse extends QuoteResponse {
  paymentStatus: PaymentStatus | null;
  providerPaymentUrl: string | null;
  providerPaymentStatus: ProviderPaymentStatus;
}

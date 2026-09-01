export interface CreateQuoteRequest {
  provider: string;
  externalOfferId: string;
  providerPrice: number;
  sellingPrice: number;
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
  sellingPrice: number;
  currency: string;
  description: string;
  status: string;
  createdAt: string;
  expiresAt: string | null;
}

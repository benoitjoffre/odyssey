import { apiFetch } from "./client";
import type { CreateQuoteRequest, QuoteResponse } from "../types/quote";

export function createQuote(bookingRequestId: number, agentId: number, request: CreateQuoteRequest): Promise<QuoteResponse> {
  return apiFetch<QuoteResponse>(`/api/booking-requests/${bookingRequestId}/quotes?agentId=${agentId}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });
}

export function sendQuote(bookingRequestId: number, quoteId: number, agentId: number): Promise<QuoteResponse> {
  return apiFetch<QuoteResponse>(`/api/booking-requests/${bookingRequestId}/quotes/${quoteId}/send?agentId=${agentId}`, {
    method: "POST",
  });
}

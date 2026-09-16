import { apiFetch } from "./client";
import type { AgentQuoteResponse, CreateQuoteRequest, QuoteResponse } from "../types/quote";

export function getAgentBookingRequestQuotes(bookingRequestId: number, signal?: AbortSignal): Promise<AgentQuoteResponse[]> {
  return apiFetch<AgentQuoteResponse[]>(`/api/booking-requests/${bookingRequestId}/quotes`, { signal });
}

export function createQuote(bookingRequestId: number, request: CreateQuoteRequest): Promise<QuoteResponse> {
  return apiFetch<QuoteResponse>(`/api/booking-requests/${bookingRequestId}/quotes`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });
}

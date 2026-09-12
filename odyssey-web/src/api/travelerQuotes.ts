import { apiFetch } from "./client";
import type { TravelerQuote } from "../types/travelerQuote";

export function getTravelerQuotes(travelerIdOrSignal?: number | AbortSignal, signal?: AbortSignal): Promise<TravelerQuote[]> {
  if (typeof travelerIdOrSignal === "number") {
    return apiFetch<TravelerQuote[]>(`/api/travelers/${travelerIdOrSignal}/quotes`, { signal });
  }

  return apiFetch<TravelerQuote[]>("/api/travelers/me/quotes", { signal: travelerIdOrSignal ?? signal });
}

export function getBookingRequestQuotes(bookingRequestId: number, signal?: AbortSignal): Promise<TravelerQuote[]> {
  return apiFetch<TravelerQuote[]>(`/api/booking-requests/${bookingRequestId}/quotes`, { signal });
}

export function acceptTravelerQuote(quoteId: number, travelerId?: number): Promise<TravelerQuote> {
  const url = travelerId == null ? `/api/travelers/me/quotes/${quoteId}/accept` : `/api/travelers/${travelerId}/quotes/${quoteId}/accept`;
  return apiFetch<TravelerQuote>(url, { method: "POST" });
}

export function rejectTravelerQuote(quoteId: number, travelerId?: number): Promise<TravelerQuote> {
  const url = travelerId == null ? `/api/travelers/me/quotes/${quoteId}/reject` : `/api/travelers/${travelerId}/quotes/${quoteId}/reject`;
  return apiFetch<TravelerQuote>(url, { method: "POST" });
}

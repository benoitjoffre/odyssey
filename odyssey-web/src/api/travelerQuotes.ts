import { apiFetch } from "./client";
import type { TravelerQuote } from "../types/travelerQuote";

export function getTravelerQuotes(signal?: AbortSignal): Promise<TravelerQuote[]> {
  return apiFetch<TravelerQuote[]>("/api/travelers/me/quotes", { signal });
}

export function acceptTravelerQuote(quoteId: number): Promise<TravelerQuote> {
  return apiFetch<TravelerQuote>(`/api/travelers/me/quotes/${quoteId}/accept`, { method: "POST" });
}

export function rejectTravelerQuote(quoteId: number): Promise<TravelerQuote> {
  return apiFetch<TravelerQuote>(`/api/travelers/me/quotes/${quoteId}/reject`, { method: "POST" });
}

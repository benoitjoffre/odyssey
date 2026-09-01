import { apiFetch } from "./client";
import type { TravelerQuote } from "../types/travelerQuote";

export function getTravelerQuotes(travelerId: number, signal?: AbortSignal): Promise<TravelerQuote[]> {
  return apiFetch<TravelerQuote[]>(`/api/travelers/${travelerId}/quotes`, { signal });
}

export function acceptTravelerQuote(travelerId: number, quoteId: number): Promise<TravelerQuote> {
  return apiFetch<TravelerQuote>(`/api/travelers/${travelerId}/quotes/${quoteId}/accept`, {
    method: "POST",
  });
}

export function rejectTravelerQuote(travelerId: number, quoteId: number): Promise<TravelerQuote> {
  return apiFetch<TravelerQuote>(`/api/travelers/${travelerId}/quotes/${quoteId}/reject`, {
    method: "POST",
  });
}

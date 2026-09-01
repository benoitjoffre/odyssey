import { apiFetch } from "./client";
import type { BookingRequest } from "../types/bookingRequest";
import type { ProviderOffer } from "../types/providerOffer";

export function getBookingRequest(id: number, signal?: AbortSignal): Promise<BookingRequest> {
  return apiFetch<BookingRequest>(`/api/booking-requests/${id}`, { signal });
}

export function claimBookingRequest(id: number, agentId: number): Promise<BookingRequest> {
  return apiFetch<BookingRequest>(`/api/booking-requests/${id}/claim?agentId=${agentId}`, {
    method: "POST",
  });
}

export function searchBookingRequestOffers(id: number): Promise<ProviderOffer[]> {
  return apiFetch<ProviderOffer[]>(`/api/booking-requests/${id}/offers/search`, {
    method: "POST",
  });
}

import { apiFetch } from "./client";
import type { Booking, UpdateBookingProviderDetailsRequest } from "../types/booking";

export function createBooking(quoteId: number, agentId: number): Promise<Booking> {
  return apiFetch<Booking>(`/api/bookings?quoteId=${quoteId}&agentId=${agentId}`, {
    method: "POST",
  });
}

export function confirmBooking(bookingId: number, agentId: number): Promise<Booking> {
  return apiFetch<Booking>(`/api/bookings/${bookingId}/confirm?agentId=${agentId}`, {
    method: "POST",
  });
}

export function updateBookingProviderDetails(bookingId: number, agentId: number, request: UpdateBookingProviderDetailsRequest): Promise<Booking> {
  return apiFetch<Booking>(`/api/bookings/${bookingId}/provider-details?agentId=${agentId}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });
}

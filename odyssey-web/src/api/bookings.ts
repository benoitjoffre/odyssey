import { apiFetch } from "./client";
import type { Booking } from "../types/booking";

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

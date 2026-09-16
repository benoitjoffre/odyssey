import { apiFetch } from "./client";
import type { Booking, UpdateBookingProviderDetailsRequest } from "../types/booking";

export function createBooking(quoteId: number): Promise<Booking> {
  return apiFetch<Booking>(`/api/bookings?quoteId=${quoteId}`, {
    method: "POST",
  });
}

export function confirmBooking(bookingId: number): Promise<Booking> {
  return apiFetch<Booking>(`/api/bookings/${bookingId}/confirm`, {
    method: "POST",
  });
}

export function updateBookingProviderDetails(bookingId: number, request: UpdateBookingProviderDetailsRequest): Promise<Booking> {
  return apiFetch<Booking>(`/api/bookings/${bookingId}/provider-details`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });
}

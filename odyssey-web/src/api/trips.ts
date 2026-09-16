import { apiFetch } from "./client";
import type { CreateTripRequest, Trip, TripDetail } from "../types/trip";

export interface SendTripQuotesResponse {
  tripId: number;
  sentQuoteIds: number[];
}

export function getTravelerTrips(signal?: AbortSignal): Promise<Trip[]> {
  return apiFetch<Trip[]>(`/api/trips`, { signal });
}

export function getTripDetail(tripId: number, signal?: AbortSignal): Promise<TripDetail> {
  return apiFetch<TripDetail>(`/api/trips/${tripId}/detail`, { signal });
}

export function createTrip(request: CreateTripRequest): Promise<Trip> {
  return apiFetch<Trip>("/api/trips", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

export function updateTripAssistanceFee(tripId: number, assistanceFee: number): Promise<Trip> {
  return apiFetch<Trip>(`/api/trips/${tripId}/assistance-fee`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ assistanceFee }),
  });
}

export function sendTripQuotes(tripId: number): Promise<SendTripQuotesResponse> {
  return apiFetch<SendTripQuotesResponse>(`/api/trips/${tripId}/quotes/send`, {
    method: "POST",
  });
}

export function deleteTrip(tripId: number): Promise<void> {
  return apiFetch<void>(`/api/trips/${tripId}`, { method: "DELETE" });
}

import { apiFetch } from "./client";
import type { CreateTripRequest, Trip, TripDetail } from "../types/trip";

export function getTravelerTrips(travelerId: number, signal?: AbortSignal): Promise<Trip[]> {
  return apiFetch<Trip[]>(`/api/travelers/${travelerId}/trips`, { signal });
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

export function deleteTrip(tripId: number): Promise<void> {
  return apiFetch<void>(`/api/trips/${tripId}`, { method: "DELETE" });
}

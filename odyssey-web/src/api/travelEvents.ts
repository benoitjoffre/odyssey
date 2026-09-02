import { apiFetch } from "./client";
import type { CreateTravelEventRequest, TravelEvent } from "../types/travelEvent";

export function getTravelEvents(signal?: AbortSignal): Promise<TravelEvent[]> {
  return apiFetch<TravelEvent[]>("/api/travel-events", { signal });
}

export function getTravelEvent(eventId: number, signal?: AbortSignal): Promise<TravelEvent> {
  return apiFetch<TravelEvent>(`/api/travel-events/${eventId}`, { signal });
}

export function createTravelEvent(request: CreateTravelEventRequest): Promise<TravelEvent> {
  return apiFetch<TravelEvent>("/api/travel-events", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

import { apiFetch } from "./client";
import type { TravelEvent } from "../types/travelEvent";

export function getTravelEvents(signal?: AbortSignal): Promise<TravelEvent[]> {
  return apiFetch<TravelEvent[]>("/api/travel-events", { signal });
}

export function getTravelEvent(eventId: number, signal?: AbortSignal): Promise<TravelEvent> {
  return apiFetch<TravelEvent>(`/api/travel-events/${eventId}`, { signal });
}

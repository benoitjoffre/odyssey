import { apiFetch } from "./client";
import type { CreateDestinationRequest, Destination } from "../types/destination";

export function getDestinations(signal?: AbortSignal): Promise<Destination[]> {
  return apiFetch<Destination[]>("/api/destinations", { signal });
}

export function createDestination(request: CreateDestinationRequest): Promise<Destination> {
  return apiFetch<Destination>("/api/destinations", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

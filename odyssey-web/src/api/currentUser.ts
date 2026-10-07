import { apiFetch } from "./client";
import type { CurrentUser } from "../types/currentUser";

export interface UpdateCurrentTravelerProfileRequest {
  firstName: string;
  lastName: string;
  phoneNumber: string;
  whatsappNumber: string | null;
  preferredLanguage: string;
}

export function getCurrentUser(signal?: AbortSignal): Promise<CurrentUser> {
  return apiFetch<CurrentUser>(`/api/me`, { signal });
}

export function updateCurrentTravelerProfile(request: UpdateCurrentTravelerProfileRequest): Promise<CurrentUser> {
  return apiFetch<CurrentUser>(`/api/me`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

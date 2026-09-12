import { apiFetch } from "./client";
import type { CurrentUser } from "../types/currentUser";

export function getCurrentUser(signal?: AbortSignal): Promise<CurrentUser> {
  return apiFetch<CurrentUser>(`/api/me`, { signal });
}

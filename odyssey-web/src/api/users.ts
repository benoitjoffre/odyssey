import { apiFetch } from "./client";

export type ProfileType = "TRAVELER" | "AGENT";

export interface PlatformUser {
  id: number;
  profileType: ProfileType;
  firstName: string | null;
  lastName: string | null;
  email: string;
  phoneNumber: string | null;
  whatsappNumber: string | null;
  preferredLanguage: string | null;
  onboardingCompleted: boolean | null;
  agentStatus: "AVAILABLE" | "BUSY" | "OFFLINE" | null;
}

export interface UserPage {
  items: PlatformUser[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export function getUsers(profileType: ProfileType, search: string, page: number, signal?: AbortSignal) {
  const params = new URLSearchParams({ profileType, search, page: String(page), size: "20" });
  return apiFetch<UserPage>(`/api/users?${params}`, { signal });
}

import { apiFetch } from "./client";

export interface UpdateTravelerOnboardingRequest {
  firstName: string;
  lastName: string;
  phoneNumber: string;
  whatsappNumber?: string;
  preferredLanguage: string;
}

export function updateTravelerOnboarding(request: UpdateTravelerOnboardingRequest): Promise<void> {
  return apiFetch<void>("/api/travelers/me/onboarding", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

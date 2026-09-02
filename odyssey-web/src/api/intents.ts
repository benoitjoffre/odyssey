import { apiFetch } from "./client";
import type { CreateIntentRequest, IntentResponse, ScoredExperienceResponse } from "../types/intent";

export function createIntent(request: CreateIntentRequest): Promise<IntentResponse> {
  return apiFetch<IntentResponse>("/api/intents", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

export function getIntentRecommendations(intentId: number): Promise<ScoredExperienceResponse[]> {
  return apiFetch<ScoredExperienceResponse[]>(`/api/intents/${intentId}/recommendations`);
}

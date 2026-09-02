export type ExperienceCategory = "DANCE" | "CULTURE" | "NATURE" | "ADVENTURE" | "FOOD" | "BEACH" | "ROAD_TRIP" | "SURF";

export type IntentStatus = "DRAFT" | "ACTIVE" | "MATCHED" | "CLOSED";

export interface IntentResponse {
  id: number;
  title: string;
  description: string;
  status: IntentStatus;
  category: ExperienceCategory | null;
  travelerId: number;
}

export interface ScoredExperienceResponse {
  id: number;
  title: string;
  description: string;
  category: ExperienceCategory;
  destination: string;
  durationDays: number;
  score: number;
}

export interface CreateIntentRequest {
  title: string;
  description: string;
  travelerId: number;
}

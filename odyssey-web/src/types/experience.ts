import type { ExperienceCategory } from "./intent";
import type { Destination } from "./destination";

export interface Experience {
  id: number;
  title: string;
  description: string;
  destination: Destination;
  category: ExperienceCategory;
  durationDays: number;
}

export interface CreateExperienceRequest {
  title: string;
  description: string;
  destinationId: number;
  category: ExperienceCategory;
  durationDays: number;
}

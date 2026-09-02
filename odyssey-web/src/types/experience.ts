import type { ExperienceCategory } from "./intent";

export interface Experience {
  id: number;
  title: string;
  description: string;
  destination: string;
  category: ExperienceCategory;
  durationDays: number;
}

export interface CreateExperienceRequest {
  title: string;
  description: string;
  destination: string;
  category: ExperienceCategory;
  durationDays: number;
}

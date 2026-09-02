import { apiFetch } from "./client";
import type { CreateExperienceRequest, Experience } from "../types/experience";

export function getExperiences(signal?: AbortSignal): Promise<Experience[]> {
  return apiFetch<Experience[]>("/api/experiences", { signal });
}

export function createExperience(request: CreateExperienceRequest): Promise<Experience> {
  return apiFetch<Experience>("/api/experiences", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

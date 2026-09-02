import { apiFetch } from "./client";
import type { CreateNeedRequest, Need } from "../types/need";

export function createNeed(request: CreateNeedRequest): Promise<Need> {
  return apiFetch<Need>("/api/needs", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

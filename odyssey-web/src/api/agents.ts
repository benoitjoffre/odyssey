import { apiFetch } from "./client";
import type { AgentNotification } from "../types/agent";

export function getAgentNotifications(signal?: AbortSignal): Promise<AgentNotification[]> {
  return apiFetch<AgentNotification[]>(`/api/agents/me/notifications`, {
    signal,
  });
}

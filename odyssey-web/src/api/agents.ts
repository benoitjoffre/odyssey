import { apiFetch } from "./client";
import type { AgentNotification } from "../types/agent";

export function getAgentNotifications(agentId: number, signal?: AbortSignal): Promise<AgentNotification[]> {
  return apiFetch<AgentNotification[]>(`/api/agents/${agentId}/notifications`, {
    signal,
  });
}

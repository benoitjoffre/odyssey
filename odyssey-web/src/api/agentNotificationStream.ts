import { API_BASE_URL } from "./client";
import type { AgentNotification } from "../types/agent";

export function openAgentNotificationStream(agentId: number, onNotification: (notification: AgentNotification) => void): EventSource {
  const eventSource = new EventSource(`${API_BASE_URL}/api/agents/${agentId}/notifications/stream`);

  eventSource.addEventListener("notification", (event) => {
    try {
      onNotification(JSON.parse(event.data) as AgentNotification);
    } catch {
      if (import.meta.env.DEV) {
        console.warn("Invalid SSE notification payload");
      }
    }
  });

  eventSource.onerror = () => {
    if (import.meta.env.DEV) {
      console.warn("SSE connection interrupted");
    }
  };

  return eventSource;
}

import { API_BASE_URL, getApiAccessToken } from "./client";

import type { AgentNotification } from "../types/agent";
export async function openAgentNotificationStream(onNotification: (notification: AgentNotification) => void, signal?: AbortSignal): Promise<void> {
  const token = await getApiAccessToken();

  const response = await fetch(`${API_BASE_URL}/api/agents/me/notifications/stream`, {
    headers: {
      Accept: "text/event-stream",
      Authorization: `Bearer ${token}`,
    },
    signal,
  });

  if (!response.ok) {
    throw new Error(`SSE connection failed (${response.status})`);
  }

  if (!response.body) {
    throw new Error("SSE response has no body");
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();

  let buffer = "";
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const events = buffer.split("\n\n");
    buffer = events.pop() ?? "";

    for (const event of events) {
      const lines = event.split("\n");
      for (const line of lines) {
        if (line.startsWith("data:")) {
          try {
            const data = line.slice(5).trimStart();
            const notification = JSON.parse(data) as AgentNotification;
            onNotification(notification);
          } catch {
            if (import.meta.env.DEV) {
              console.warn("Invalid SSE notification payload");
            }
          }
        }
      }
    }
  }
}

import { API_BASE_URL, getApiAccessToken } from "./client";

import type { AgentNotification } from "../types/agent";

const RECONNECT_DELAYS_MS = [1_000, 2_000, 5_000] as const;

function waitForReconnect(delayMs: number, signal?: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) {
      reject(signal.reason ?? new DOMException("Aborted", "AbortError"));
      return;
    }

    const handleAbort = () => {
      window.clearTimeout(timeoutId);
      reject(signal?.reason ?? new DOMException("Aborted", "AbortError"));
    };
    const timeoutId = window.setTimeout(() => {
      signal?.removeEventListener("abort", handleAbort);
      resolve();
    }, delayMs);
    signal?.addEventListener("abort", handleAbort, { once: true });
  });
}

async function connectAgentNotificationStream(onNotification: (notification: AgentNotification) => void, signal?: AbortSignal): Promise<void> {
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

  if (import.meta.env.DEV) console.info("Agent notification stream connected");

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

export async function openAgentNotificationStream(onNotification: (notification: AgentNotification) => void, signal?: AbortSignal): Promise<void> {
  let attempt = 0;

  while (!signal?.aborted) {
    try {
      await connectAgentNotificationStream(onNotification, signal);
      attempt = 0;
    } catch (error) {
      if (signal?.aborted || (error instanceof DOMException && error.name === "AbortError")) throw error;
      if (import.meta.env.DEV) console.warn("Agent notification stream interrupted; reconnecting", error);
    }

    const delay = RECONNECT_DELAYS_MS[Math.min(attempt, RECONNECT_DELAYS_MS.length - 1)];
    attempt += 1;
    await waitForReconnect(delay, signal);
  }
}

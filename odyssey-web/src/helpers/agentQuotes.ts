import type { AgentQuoteResponse } from "../types/quote";

const currentStatuses = new Set(["DRAFT", "SENT", "ACCEPTED"]);

export function getCurrentAgentQuote(quotes?: AgentQuoteResponse[]): AgentQuoteResponse | null {
  return [...(quotes ?? [])].filter((quote) => currentStatuses.has(quote.status)).sort((first, second) => second.id - first.id)[0] ?? null;
}

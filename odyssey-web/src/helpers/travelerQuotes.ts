import type { TravelerQuote } from "../types/travelerQuote";

export function getLatestAcceptedQuote(quotes: TravelerQuote[] | undefined): TravelerQuote | null {
  return quotes?.filter((quote) => quote.status === "ACCEPTED").sort((first, second) => second.id - first.id)[0] ?? null;
}

/**
 * Returns the most recent Quote for a BookingRequest, regardless of its status
 * (SENT, ACCEPTED, REJECTED or EXPIRED). Used to determine the traveler-facing
 * state of a Need, which must reflect a pending or past decision, not only an
 * accepted one.
 */
export function getLatestQuote(quotes: TravelerQuote[] | undefined): TravelerQuote | null {
  return [...(quotes ?? [])].sort((first, second) => second.id - first.id)[0] ?? null;
}

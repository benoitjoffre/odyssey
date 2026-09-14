import type { TravelerQuote } from "../types/travelerQuote";

export function getLatestAcceptedQuote(quotes: TravelerQuote[] | undefined): TravelerQuote | null {
  return quotes?.filter((quote) => quote.status === "ACCEPTED").sort((first, second) => second.id - first.id)[0] ?? null;
}

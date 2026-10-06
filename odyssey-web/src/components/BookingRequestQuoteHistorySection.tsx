import type { AgentQuoteResponse } from "../types/quote";

function formatPrice(price: number, currency: string) {
  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency,
    maximumFractionDigits: 2,
  }).format(price);
}

export interface BookingRequestQuoteHistorySectionProps {
  quotes: AgentQuoteResponse[];
}

export function BookingRequestQuoteHistorySection({ quotes }: BookingRequestQuoteHistorySectionProps) {
  if (quotes.length === 0) {
    return null;
  }

  return (
    <section className="proposal-history-section" aria-labelledby="proposal-history-title">
      <div className="section-heading">
        <div>
          <h2 id="proposal-history-title">Historique des propositions</h2>
          <p>La proposition la plus récente est affichée en premier.</p>
        </div>
      </div>
      <div className="proposal-history-list">
        {quotes.map((quote, index) => {
          const isCurrent = index === 0;
          const statusText =
            quote.status === "SENT"
              ? "À examiner"
              : quote.status === "ACCEPTED"
                ? "Acceptée"
                : quote.status === "REJECTED"
                  ? "Refusée"
                  : quote.status === "EXPIRED"
                    ? "Expirée"
                    : quote.status;
          const statusClass = quote.status === "SENT" ? "sent" : quote.status.toLowerCase();

          return (
            <div key={quote.id} className={`proposal-history-item proposal-history-item--${isCurrent ? "current" : "previous"}`}>
              <span className="proposal-history-marker" aria-hidden="true" />
              <div className="proposal-history-card">
                <div className="proposal-history-header">
                  <span className="proposal-history-label">
                    {isCurrent ? "PROPOSITION ACTUELLE" : index === 1 ? "PROPOSITION PRÉCÉDENTE" : "ANCIENNE PROPOSITION"}
                  </span>
                  <span className={`proposal-history-status proposal-history-status--${statusClass}`}>{statusText}</span>
                </div>
                <strong>{quote.provider}</strong>
                <p>{quote.description}</p>
                <div className="proposal-history-meta">
                  <span>{formatPrice(quote.providerPrice, quote.currency)}</span>
                  {quote.createdAt && (
                    <span>
                      Proposée le {new Date(quote.createdAt).toLocaleDateString("fr-FR", { day: "numeric", month: "short", year: "numeric" })}
                    </span>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}

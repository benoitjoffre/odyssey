import { useEffect, useState } from "react";
import { CalendarDays, Check, Clock3, CreditCard, FileText, Inbox, LoaderCircle, RefreshCw, X } from "lucide-react";
import { acceptTravelerQuote, getTravelerQuotes, rejectTravelerQuote } from "../../api/travelerQuotes";
import type { TravelerQuote, TravelerQuoteStatus } from "../../types/travelerQuote";

const statusLabels: Record<TravelerQuoteStatus, string> = {
  SENT: "Proposition reçue",
  ACCEPTED: "Acceptée",
  REJECTED: "Refusée",
  EXPIRED: "Expirée",
};

type QuoteAction = "accept" | "reject";

function formatPrice(price: number, currency: string) {
  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency,
    maximumFractionDigits: 2,
  }).format(price);
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("fr-FR", {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(new Date(value));
}

export function TravelerQuotesPage() {
  const [quotes, setQuotes] = useState<TravelerQuote[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [requestVersion, setRequestVersion] = useState(0);
  const [pendingActions, setPendingActions] = useState<Record<number, QuoteAction>>({});
  const [actionErrors, setActionErrors] = useState<Record<number, string>>({});

  useEffect(() => {
    const controller = new AbortController();

    async function loadQuotes() {
      setLoading(true);
      setError(null);

      try {
        setQuotes(await getTravelerQuotes(controller.signal));
      } catch (requestError: unknown) {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        setError("Impossible de charger vos propositions pour le moment.");
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void loadQuotes();
    return () => controller.abort();
  }, [requestVersion]);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const paymentResult = params.get("payment");
    if (paymentResult !== "success" && paymentResult !== "cancelled") return;

    const paymentTripId = sessionStorage.getItem("odyssey-payment-trip-id");
    if (!paymentTripId) return;
    window.location.replace(`/traveler/trips/${paymentTripId}?payment=${paymentResult}`);
  }, []);

  async function handleQuoteAction(quote: TravelerQuote, action: QuoteAction) {
    if (pendingActions[quote.id]) return;

    setPendingActions((current) => ({ ...current, [quote.id]: action }));
    setActionErrors((current) => {
      const next = { ...current };
      delete next[quote.id];
      return next;
    });

    try {
      const updatedQuote = action === "accept" ? await acceptTravelerQuote(quote.id) : await rejectTravelerQuote(quote.id);
      setQuotes((current) => current.map((item) => (item.id === updatedQuote.id ? updatedQuote : item)));
    } catch {
      setActionErrors((current) => ({
        ...current,
        [quote.id]: "Cette proposition n’a pas pu être mise à jour. Elle a peut-être déjà été traitée.",
      }));
    } finally {
      setPendingActions((current) => {
        const next = { ...current };
        delete next[quote.id];
        return next;
      });
    }
  }

  return (
    <div className="traveler-page">
      <section className="traveler-page-heading">
        <span className="eyebrow">Votre voyage, préparé avec soin</span>
        <h1>Mes propositions</h1>
        <p>Retrouvez ici les propositions préparées pour votre voyage.</p>
      </section>

      {loading && (
        <div className="state-panel" role="status">
          <span className="spinner" aria-hidden="true" />
          <strong>Chargement de vos propositions…</strong>
          <p>Nous récupérons les dernières propositions de votre conseiller.</p>
        </div>
      )}

      {!loading && error && (
        <div className="state-panel error-panel" role="alert">
          <RefreshCw size={24} aria-hidden="true" />
          <strong>{error}</strong>
          <button type="button" className="secondary-button" onClick={() => setRequestVersion((version) => version + 1)}>
            <RefreshCw size={16} /> Réessayer
          </button>
        </div>
      )}

      {!loading && !error && quotes.length === 0 && (
        <div className="state-panel">
          <Inbox size={28} aria-hidden="true" />
          <strong>Aucune proposition pour le moment</strong>
          <p>Les propositions préparées par votre conseiller apparaîtront ici.</p>
        </div>
      )}

      {!loading && !error && quotes.length > 0 && (
        <section className="traveler-quotes-grid" aria-label="Propositions reçues">
          {quotes.map((quote) => {
            const pendingAction = pendingActions[quote.id];
            const isPending = Boolean(pendingAction);

            return (
              <article className="traveler-quote-card" key={quote.id}>
                <div className="traveler-quote-topline">
                  <span className={`traveler-status status-${quote.status.toLowerCase()}`}>{statusLabels[quote.status]}</span>
                  <span className="traveler-request-number">
                    <FileText size={14} /> Demande #{quote.bookingRequestId}
                  </span>
                </div>
                <div className="traveler-quote-main">
                  <h2>{quote.description}</h2>
                </div>
                <div className="traveler-quote-breakdown">
                  <div className="traveler-quote-line">
                    <span>Prix fournisseur</span>
                    <strong>{formatPrice(quote.providerPrice, quote.currency)}</strong>
                    <span className="traveler-quote-hint">À régler directement au fournisseur</span>
                  </div>
                </div>
                <div className="traveler-quote-dates">
                  <span>
                    <CalendarDays size={16} />
                    <span>
                      Reçue le <strong>{formatDate(quote.createdAt)}</strong>
                    </span>
                  </span>
                  {quote.expiresAt && (
                    <span>
                      <Clock3 size={16} />
                      <span>
                        Expire le <strong>{formatDate(quote.expiresAt)}</strong>
                      </span>
                    </span>
                  )}
                </div>
                {quote.status === "SENT" && (
                  <div className="traveler-quote-actions">
                    {actionErrors[quote.id] && (
                      <p className="traveler-action-error" role="alert">
                        {actionErrors[quote.id]}
                      </p>
                    )}
                    <button type="button" className="primary-button" disabled={isPending} onClick={() => void handleQuoteAction(quote, "accept")}>
                      {pendingAction === "accept" ? <LoaderCircle className="rotating" size={18} /> : <Check size={18} />}
                      {pendingAction === "accept" ? "Acceptation..." : "Accepter la proposition"}
                    </button>
                    <button
                      type="button"
                      className="traveler-reject-button"
                      disabled={isPending}
                      onClick={() => void handleQuoteAction(quote, "reject")}
                    >
                      {pendingAction === "reject" ? <LoaderCircle className="rotating" size={18} /> : <X size={18} />}
                      {pendingAction === "reject" ? "Refus..." : "Refuser"}
                    </button>
                  </div>
                )}
                {quote.status === "ACCEPTED" && (
                  <>
                    <p className="traveler-quote-outcome accepted">
                      <Check size={18} /> Proposition acceptée
                    </p>
                    {quote.providerPaymentStatus === "PAYMENT_REQUIRED" && quote.providerPaymentUrl && (
                      <div className="traveler-quote-actions">
                        <p className="traveler-provider-payment-hint">
                          {formatPrice(quote.providerPrice, quote.currency)} à régler directement au fournisseur
                        </p>
                        <a className="primary-button" href={quote.providerPaymentUrl} target="_blank" rel="noopener noreferrer">
                          <CreditCard size={18} /> Payer auprès du fournisseur
                        </a>
                      </div>
                    )}
                    {quote.providerPaymentStatus === "PAID_TO_PROVIDER" && (
                      <p className="traveler-quote-outcome accepted">
                        <Check size={18} /> Prestation fournisseur payée
                      </p>
                    )}
                  </>
                )}
                {quote.status === "REJECTED" && (
                  <p className="traveler-quote-outcome rejected">
                    <X size={18} /> Proposition refusée
                  </p>
                )}
                {quote.status === "EXPIRED" && (
                  <p className="traveler-quote-outcome expired">
                    <Clock3 size={18} /> Proposition expirée
                  </p>
                )}
              </article>
            );
          })}
        </section>
      )}
    </div>
  );
}

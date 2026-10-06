import { useEffect, useState } from "react";
import { ArrowLeft, Check, FileCheck2, LoaderCircle, RefreshCw } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { getBookingByQuote } from "../api/bookings";
import { claimBookingRequest, getBookingRequest } from "../api/bookingRequests";
import { getAgentBookingRequestQuotes } from "../api/quotes";
import type { Booking } from "../types/booking";
import type { BookingRequest, BookingRequestStatus } from "../types/bookingRequest";
import type { AgentQuoteResponse, QuoteResponse } from "../types/quote";
import { BookingRequestBookingWorkflowSection } from "../components/BookingRequestBookingWorkflowSection";
import { BookingRequestOfferWorkflowSection } from "../components/BookingRequestOfferWorkflowSection";
import { BookingRequestOverviewSection } from "../components/BookingRequestOverviewSection";
import { BookingRequestQuoteHistorySection } from "../components/BookingRequestQuoteHistorySection";
import { getCurrentAgentQuote } from "../helpers/agentQuotes";

const statusLabels: Record<BookingRequestStatus, string> = {
  REQUESTED: "Demandée",
  IN_PROGRESS: "En cours",
  COMPLETED: "Terminée",
  CANCELLED: "Annulée",
};

const quoteStatusLabels: Record<string, string> = {
  DRAFT: "Brouillon",
  SENT: "Envoyé",
  ACCEPTED: "Accepté",
  REJECTED: "Refusé",
  EXPIRED: "Expiré",
};

function formatPrice(price: number, currency: string) {
  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency,
    maximumFractionDigits: 2,
  }).format(price);
}

function toAgentQuoteResponse(quote: QuoteResponse): AgentQuoteResponse {
  return {
    ...quote,
    paymentStatus: null,
    providerPaymentUrl: null,
    providerPaymentStatus: "NOT_REQUIRED_YET",
  };
}

export function BookingRequestPage() {
  const { id } = useParams<{ id: string }>();
  const bookingRequestId = Number(id);
  const hasInvalidId = !Number.isInteger(bookingRequestId) || bookingRequestId <= 0;
  const [bookingRequest, setBookingRequest] = useState<BookingRequest | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadVersion, setReloadVersion] = useState(0);
  const [claiming, setClaiming] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [agentQuotes, setAgentQuotes] = useState<AgentQuoteResponse[]>([]);
  const [booking, setBooking] = useState<Booking | null>(null);

  const acceptedQuote = agentQuotes.find((quote) => quote.status === "ACCEPTED") ?? null;

  useEffect(() => {
    if (hasInvalidId) return;

    const controller = new AbortController();

    async function loadBookingRequest() {
      setLoading(true);
      setError(null);

      try {
        const request = await getBookingRequest(bookingRequestId, controller.signal);
        setBookingRequest(request);

        if (request.assignedAgentId !== null) {
          try {
            const quotes = await getAgentBookingRequestQuotes(request.id, controller.signal);

            const sortedQuotes = quotes.sort((first, second) => second.id - first.id);

            setAgentQuotes(sortedQuotes);

            const acceptedQuote = sortedQuotes.find((quote) => quote.status === "ACCEPTED");

            if (acceptedQuote) {
              const existingBooking = await getBookingByQuote(acceptedQuote.id);
              setBooking(existingBooking);
            } else {
              setBooking(null);
            }
          } catch (quotesError: unknown) {
            if (quotesError instanceof DOMException && quotesError.name === "AbortError") throw quotesError;
            setAgentQuotes([]);
          }
        } else {
          setAgentQuotes([]);
        }
      } catch (requestError: unknown) {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        setError("Impossible de charger cette demande pour le moment.");
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void loadBookingRequest();

    return () => controller.abort();
  }, [bookingRequestId, hasInvalidId, reloadVersion]);

  async function handleClaim() {
    setClaiming(true);
    setActionError(null);

    try {
      await claimBookingRequest(bookingRequestId);
      setReloadVersion((version) => version + 1);
    } catch {
      setActionError("La prise en charge a échoué. Veuillez réessayer.");
    } finally {
      setClaiming(false);
    }
  }

  const handleQuoteCreated = (quote: QuoteResponse) => {
    setAgentQuotes((currentQuotes) => [toAgentQuoteResponse(quote), ...currentQuotes.filter((currentQuote) => currentQuote.id !== quote.id)]);
  };

  const handleBookingChanged = (updatedBooking: Booking) => {
    setBooking(updatedBooking);
  };

  if (hasInvalidId) {
    return (
      <div className="page-stack">
        <Link className="back-link" to="/agent">
          <ArrowLeft size={17} />
          Retour au dashboard
        </Link>
        <div className="state-panel error-panel" role="alert">
          <strong>L’identifiant de la demande est invalide.</strong>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="state-panel" role="status">
        <span className="spinner" aria-hidden="true" />
        <strong>Chargement de la demande…</strong>
        <p>Nous récupérons les informations du voyage.</p>
      </div>
    );
  }

  if (error || !bookingRequest) {
    return (
      <div className="page-stack">
        <Link className="back-link" to="/agent">
          <ArrowLeft size={17} />
          Retour au dashboard
        </Link>
        <div className="state-panel error-panel" role="alert">
          <RefreshCw size={24} aria-hidden="true" />
          <strong>{error ?? "Demande introuvable."}</strong>
          <button type="button" className="secondary-button" onClick={() => setReloadVersion((version) => version + 1)}>
            <RefreshCw size={16} />
            Réessayer
          </button>
        </div>
      </div>
    );
  }

  const canClaim = bookingRequest.status === "REQUESTED" && bookingRequest.assignedAgentId === null;
  const canSearchOffers = bookingRequest.status === "IN_PROGRESS" && bookingRequest.assignedAgentId !== null;
  const sortedQuotes = [...agentQuotes].sort((first, second) => second.id - first.id);
  const currentQuote = getCurrentAgentQuote(sortedQuotes) ?? sortedQuotes[0] ?? null;
  const displayRequestStatus = currentQuote?.status ?? bookingRequest.status;
  const displayRequestStatusLabel = currentQuote
    ? (quoteStatusLabels[currentQuote.status] ?? currentQuote.status)
    : statusLabels[bookingRequest.status];

  return (
    <div className="page-stack booking-request-page">
      <Link className="back-link" to="/agent/booking-requests">
        <ArrowLeft size={17} />
        Retour aux demandes
      </Link>
      <section className="page-heading compact">
        <div>
          <span className="eyebrow">Détail de la demande</span>
          <div className="title-with-status">
            <h1>Demande #{bookingRequest.id}</h1>
            <span className={`request-status status-${displayRequestStatus.toLowerCase()}`}>{displayRequestStatusLabel}</span>
          </div>
          {bookingRequest.notes && <p>{bookingRequest.notes}</p>}
        </div>
      </section>

      <BookingRequestOverviewSection bookingRequest={bookingRequest} />

      <section className="request-actions" aria-label="Actions sur la demande">
        {canClaim && (
          <button type="button" className="primary-button" onClick={handleClaim} disabled={claiming}>
            {claiming ? <LoaderCircle className="rotating" size={18} /> : <Check size={18} />}
            {claiming ? "Prise en charge…" : "Prendre en charge"}
          </button>
        )}

        {bookingRequest.assignedAgentId !== null && (
          <p className="assignment-message">
            <Check size={17} />
            Demande prise en charge par l’agent #{bookingRequest.assignedAgentId}
          </p>
        )}

        {actionError && (
          <p className="action-error" role="alert">
            {actionError}
          </p>
        )}
      </section>

      <BookingRequestOfferWorkflowSection
        bookingRequestId={bookingRequest.id}
        canSearchOffers={canSearchOffers}
        currentQuote={currentQuote}
        onQuoteCreated={handleQuoteCreated}
      />

      <BookingRequestQuoteHistorySection quotes={sortedQuotes} />

      {agentQuotes.length > 0 && (
        <section className="agent-quotes-section" aria-labelledby="agent-quotes-title">
          <div className="section-heading">
            <div>
              <h2 id="agent-quotes-title">Propositions du dossier</h2>
              <p>
                {agentQuotes.length} proposition{agentQuotes.length > 1 ? "s" : ""}
              </p>
            </div>
          </div>
          <div className="agent-quotes-list">
            {agentQuotes.map((quote) => (
              <article className="quote-success-card" key={quote.id}>
                <div className="quote-success-heading">
                  <span className="quote-success-icon">
                    <FileCheck2 size={20} />
                  </span>
                  <div>
                    <span className="eyebrow">Proposition #{quote.id}</span>
                    <h3>{quote.provider}</h3>
                  </div>
                  <span className="quote-status">{quote.status}</span>
                </div>
                <div className="quote-result-grid">
                  <div>
                    <span>Prix fournisseur</span>
                    <strong>{formatPrice(quote.providerPrice, quote.currency)}</strong>
                  </div>
                  <div className="quote-description">
                    <span>Description</span>
                    <strong>{quote.description}</strong>
                  </div>
                </div>
                {quote.status === "SENT" && (
                  <p className="quote-sent-confirmation">
                    <Check size={18} /> Proposition envoyée au client
                  </p>
                )}
              </article>
            ))}
          </div>
        </section>
      )}

      <BookingRequestBookingWorkflowSection acceptedQuote={acceptedQuote} booking={booking} onBookingChanged={handleBookingChanged} />
    </div>
  );
}

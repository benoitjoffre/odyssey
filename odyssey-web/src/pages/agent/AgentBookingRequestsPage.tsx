import { useEffect, useRef, useState } from "react";
import { ArrowRight, BedDouble, Bus, CalendarDays, Car, Inbox, LoaderCircle, Plane, RefreshCw, Route, Send } from "lucide-react";
import { Link } from "react-router-dom";
import { openAgentNotificationStream } from "../../api/agentNotificationStream";
import { getBookingRequests } from "../../api/bookingRequests";
import { getAgentBookingRequestQuotes } from "../../api/quotes";
import { sendTripQuotes, updateTripAssistanceFee } from "../../api/trips";
import { TripFinancialSummary } from "../../components/TripFinancialSummary";
import { getCurrentAgentQuote } from "../../helpers/agentQuotes";
import type { BookingRequest, BookingRequestStatus, NeedType } from "../../types/bookingRequest";
import type { AgentQuoteResponse } from "../../types/quote";
import { createBooking, getBookingByQuote } from "../../api/bookings";
import type { Booking, BookingStatus } from "../../types/booking";
import { AgentBookingModal } from "../../components/AgentBookingModal";

const needPresentation: Record<NeedType, { label: string; icon: typeof Plane }> = {
  FLIGHT: { label: "Vol", icon: Plane },
  ACCOMMODATION: { label: "Hébergement", icon: BedDouble },
  TRANSFER: { label: "Transfert", icon: Route },
  CAR: { label: "Voiture", icon: Car },
  BUS: { label: "Bus", icon: Bus },
};

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

const bookingStatusLabels: Record<BookingStatus, string> = {
  PENDING: "Réservation à finaliser",
  CONFIRMED: "Réservation confirmée",
  FAILED: "Réservation échouée",
  CANCELLED: "Réservation annulée",
};

interface TripRequestGroup {
  tripId: number;
  title: string;
  startDate: string;
  endDate: string;
  travelerName: string;
  travelerEmail: string;
  assistanceFee?: number;
  requests: BookingRequest[];
}

function groupRequestsByTrip(requests: BookingRequest[]) {
  const requestsById = new Map(requests.map((request) => [request.id, request]));
  const groups = new Map<number, TripRequestGroup>();

  [...requestsById.values()]
    .sort((first, second) => second.id - first.id)
    .forEach((request) => {
      const existing = groups.get(request.trip.id);
      if (existing) {
        existing.requests.push(request);
        return;
      }

      groups.set(request.trip.id, {
        tripId: request.trip.id,
        title: request.trip.title,
        startDate: request.trip.startDate,
        endDate: request.trip.endDate,
        travelerName: request.traveler.firstName,
        travelerEmail: request.traveler.email,
        assistanceFee: request.trip.assistanceFee,
        requests: [request],
      });
    });

  return [...groups.values()];
}

function formatTripDates(startDate: string, endDate: string) {
  const dateFormatter = new Intl.DateTimeFormat("fr-FR", { day: "numeric", month: "short" });
  const yearFormatter = new Intl.DateTimeFormat("fr-FR", { year: "numeric" });
  const start = new Date(`${startDate}T00:00:00`);
  const end = new Date(`${endDate}T00:00:00`);
  return `${dateFormatter.format(start)} → ${dateFormatter.format(end)} ${yearFormatter.format(end)}`;
}

function getRequestDetail(request: BookingRequest) {
  if (request.need.type === "FLIGHT" && request.need.flightCriteria) {
    return `${request.need.flightCriteria.origin} → ${request.need.flightCriteria.destination}`;
  }
  if (request.need.type === "ACCOMMODATION" && request.need.accommodationCriteria) {
    const { city, rooms } = request.need.accommodationCriteria;
    return `${city} · ${rooms} ${rooms > 1 ? "chambres" : "chambre"}`;
  }
  if (request.need.type === "TRANSFER" && request.need.transferCriteria) {
    return `${request.need.transferCriteria.pickupLocation} → ${request.need.transferCriteria.dropoffLocation}`;
  }
  return request.notes ?? request.need.notes ?? "Aucune précision ajoutée";
}

function formatPrice(price: number, currency: string) {
  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency,
    maximumFractionDigits: 2,
  }).format(price);
}

export function AgentBookingRequestsPage() {
  const [requests, setRequests] = useState<BookingRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadVersion, setReloadVersion] = useState(0);
  const [quotesByRequestId, setQuotesByRequestId] = useState<Record<number, AgentQuoteResponse[]>>({});
  const [tripFeeOverrides, setTripFeeOverrides] = useState<Record<number, number>>({});
  const [sendingTripId, setSendingTripId] = useState<number | null>(null);
  const [sendErrors, setSendErrors] = useState<Record<number, string>>({});
  const latestRequestId = useRef(0);
  const [creatingBookingQuoteId, setCreatingBookingQuoteId] = useState<number | null>(null);
  const [bookingErrors, setBookingErrors] = useState<Record<number, string>>({});
  const [bookingsByQuoteId, setBookingsByQuoteId] = useState<Record<number, Booking | null>>({});
  const [editingBookingId, setEditingBookingId] = useState<number | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    let disposed = false;

    async function loadRequests(showLoading = false) {
      const requestId = ++latestRequestId.current;
      if (showLoading) setLoading(true);
      setError(null);

      try {
        const data = await getBookingRequests(controller.signal);
        const quoteEntries = await Promise.all(
          data.map(async (request) => {
            if (request.assignedAgentId === null) return [request.id, []] as const;
            try {
              return [request.id, await getAgentBookingRequestQuotes(request.id, controller.signal)] as const;
            } catch (quoteError) {
              if (quoteError instanceof DOMException && quoteError.name === "AbortError") throw quoteError;
              return [request.id, []] as const;
            }
          }),
        );
        const bookingEntries = await Promise.all(
          quoteEntries
            .flatMap(([, quotes]) => quotes)
            .filter((quote) => quote.status === "ACCEPTED")
            .map(async (quote) => {
              const booking = await getBookingByQuote(quote.id);
              return [quote.id, booking] as const;
            }),
        );
        if (!disposed && requestId === latestRequestId.current) {
          const requestsById = new Map(data.map((request) => [request.id, request]));
          setRequests([...requestsById.values()]);
          setQuotesByRequestId(Object.fromEntries(quoteEntries));
          setBookingsByQuoteId(Object.fromEntries(bookingEntries));
        }
      } catch (requestError) {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        if (!disposed && requestId === latestRequestId.current) {
          setError("Impossible de charger les demandes pour le moment.");
        }
      } finally {
        if (!disposed && showLoading && requestId === latestRequestId.current) setLoading(false);
      }
    }

    void openAgentNotificationStream(() => {
      void loadRequests();
    }, controller.signal).catch((error) => {
      if (error instanceof DOMException && error.name === "AbortError") {
        return;
      }

      if (import.meta.env.DEV) {
        console.warn("SSE connection interrupted", error);
      }
    });
    void loadRequests(true);

    return () => {
      disposed = true;
      controller.abort();
    };
  }, [reloadVersion]);

  const tripGroups = groupRequestsByTrip(requests);

  async function handleUpdateTripAssistanceFee(tripId: number, assistanceFee: number) {
    const updatedTrip = await updateTripAssistanceFee(tripId, assistanceFee);
    setTripFeeOverrides((current) => ({ ...current, [tripId]: updatedTrip.assistanceFee }));
  }

  async function handleSendTripQuotes(tripId: number) {
    if (sendingTripId !== null) return;
    setSendingTripId(tripId);
    setSendErrors((current) => ({ ...current, [tripId]: "" }));

    try {
      await sendTripQuotes(tripId);
      setReloadVersion((version) => version + 1);
    } catch {
      setSendErrors((current) => ({
        ...current,
        [tripId]: "Les offres n’ont pas pu être envoyées au voyageur.",
      }));
    } finally {
      setSendingTripId(null);
    }
  }

  const handleCreateBooking = async (quoteId: number) => {
    if (creatingBookingQuoteId !== null) return;
    setCreatingBookingQuoteId(quoteId);
    setBookingErrors((current) => ({ ...current, [quoteId]: "" }));

    try {
      await createBooking(quoteId);
      setReloadVersion((version) => version + 1);
    } catch {
      setBookingErrors((current) => ({
        ...current,
        [quoteId]: "La réservation n’a pas pu être créée.",
      }));
    } finally {
      setCreatingBookingQuoteId(null);
    }
  };

  const handleBookingSaved = (updatedBooking: Booking) => {
    setBookingsByQuoteId((current) => ({
      ...current,
      [updatedBooking.quoteId]: updatedBooking,
    }));
  };

  const editingBooking = Object.values(bookingsByQuoteId).find((booking) => booking?.id === editingBookingId) ?? null;

  return (
    <div className="page-stack agent-requests-page">
      <section className="page-heading">
        <div>
          <span className="eyebrow">Suivi opérationnel</span>
          <h1>Demandes par voyage</h1>
          <p>Traitez chaque besoin séparément tout en gardant le contexte du voyage.</p>
        </div>
        {!loading && !error && (
          <div className="summary-stat" role="status" aria-label={`${requests.length} demandes dans ${tripGroups.length} voyages`}>
            <span>{requests.length}</span>
            <p>
              {requests.length > 1 ? "Demandes" : "Demande"} · {tripGroups.length} {tripGroups.length > 1 ? "voyages" : "voyage"}
            </p>
          </div>
        )}
      </section>

      {loading && (
        <div className="state-panel" role="status">
          <span className="spinner" aria-hidden="true" />
          <strong>Chargement des demandes…</strong>
          <p>Nous regroupons les besoins par voyage.</p>
        </div>
      )}

      {!loading && error && (
        <div className="state-panel error-panel" role="alert">
          <RefreshCw size={24} aria-hidden="true" />
          <strong>Impossible de charger les demandes.</strong>
          <button type="button" className="secondary-button" onClick={() => setReloadVersion((version) => version + 1)}>
            <RefreshCw size={16} />
            Réessayer
          </button>
        </div>
      )}

      {!loading && !error && tripGroups.length === 0 && (
        <div className="state-panel">
          <Inbox size={28} aria-hidden="true" />
          <strong>Aucune demande à traiter.</strong>
          <p>Les nouveaux besoins des voyageurs apparaîtront ici automatiquement.</p>
        </div>
      )}

      {!loading && !error && tripGroups.length > 0 && (
        <div className="agent-trip-request-list">
          {tripGroups.map((group) => {
            const currentQuotes = group.requests
              .map((request) => getCurrentAgentQuote(quotesByRequestId[request.id]))
              .filter((quote): quote is AgentQuoteResponse => quote !== null);
            const assistanceFee = tripFeeOverrides[group.tripId] ?? group.assistanceFee;
            const hasDraftQuote = currentQuotes.some((quote) => quote.status === "DRAFT");
            const paymentStatus = currentQuotes.find((quote) => quote.paymentStatus !== null)?.paymentStatus ?? null;

            return (
              <section className="agent-trip-request-group" key={group.tripId} aria-labelledby={`trip-${group.tripId}-title`}>
                <header className="agent-trip-request-heading">
                  <div>
                    <span className="agent-trip-reference">Voyage #{group.tripId}</span>
                    <h2 id={`trip-${group.tripId}-title`}>{group.title}</h2>
                    <p>
                      {group.travelerName} · {group.travelerEmail}
                    </p>
                  </div>
                  <div className="agent-trip-request-meta">
                    <span>
                      <CalendarDays size={15} aria-hidden="true" />
                      {formatTripDates(group.startDate, group.endDate)}
                    </span>
                    <strong>
                      {group.requests.length} {group.requests.length > 1 ? "demandes" : "demande"}
                    </strong>
                  </div>
                </header>

                <ul className="agent-trip-requests">
                  {group.requests.map((request) => {
                    const presentation = needPresentation[request.need.type];
                    const NeedIcon = presentation.icon;
                    const currentQuote = getCurrentAgentQuote(quotesByRequestId[request.id]);
                    const currentBooking = currentQuote ? (bookingsByQuoteId[currentQuote.id] ?? null) : null;
                    const canCreateBooking = currentQuote?.status === "ACCEPTED" && paymentStatus === "PAID" && currentBooking === null;
                    return (
                      <li key={request.id}>
                        <span className={`agent-request-type-icon ${request.need.type.toLowerCase()}`} aria-hidden="true">
                          <NeedIcon size={19} />
                        </span>
                        <div className="agent-request-copy">
                          <span>{presentation.label}</span>
                          <strong>Demande #{request.id}</strong>
                          <p>{getRequestDetail(request)}</p>
                        </div>
                        <div className="agent-request-price">
                          <span>Prix de l’offre</span>
                          <strong>{currentQuote ? formatPrice(currentQuote.providerPrice, currentQuote.currency) : "Offre à définir"}</strong>
                        </div>
                        <span className={`request-status status-${(currentQuote?.status ?? request.status).toLowerCase()}`}>
                          {currentQuote ? (quoteStatusLabels[currentQuote.status] ?? currentQuote.status) : statusLabels[request.status]}
                        </span>
                        <div className="agent-request-actions">
                          <span>
                            {canCreateBooking && (
                              <button
                                type="button"
                                className="primary-button"
                                aria-label="Créer la réservation"
                                disabled={creatingBookingQuoteId !== null}
                                onClick={() => handleCreateBooking(currentQuote!.id)}
                              >
                                {creatingBookingQuoteId === currentQuote!.id ? "Création…" : "Créer la réservation"}
                              </button>
                            )}
                            {currentBooking?.status === "PENDING" && (
                              <button type="button" className="primary-button" onClick={() => setEditingBookingId(currentBooking.id)}>
                                Finaliser la réservation
                              </button>
                            )}
                            {currentBooking?.status === "CONFIRMED" && (
                              <span className={`request-status booking-status-${currentBooking.status.toLowerCase()}`}>
                                {bookingStatusLabels[currentBooking.status]}
                              </span>
                            )}
                            {canCreateBooking && bookingErrors[currentQuote!.id] && (
                              <p className="action-error" role="alert">
                                {bookingErrors[currentQuote!.id]}
                              </p>
                            )}
                          </span>
                          <Link className="agent-request-link" to={`/agent/booking-requests/${request.id}`}>
                            Voir la demande
                            <ArrowRight size={16} aria-hidden="true" />
                          </Link>
                        </div>
                      </li>
                    );
                  })}
                </ul>
                <TripFinancialSummary
                  assistanceFee={assistanceFee}
                  providerOffers={currentQuotes}
                  editable
                  onSaveAssistanceFee={(value) => handleUpdateTripAssistanceFee(group.tripId, value)}
                  paymentStatus={paymentStatus}
                  showPaymentStatus
                />
                <div className="trip-quote-send-actions">
                  {sendErrors[group.tripId] && (
                    <p className="action-error" role="alert">
                      {sendErrors[group.tripId]}
                    </p>
                  )}
                  <button
                    type="button"
                    className="primary-button"
                    aria-label="Envoyer les offres au voyageur"
                    disabled={!hasDraftQuote || sendingTripId !== null}
                    onClick={() => void handleSendTripQuotes(group.tripId)}
                  >
                    {sendingTripId === group.tripId ? <LoaderCircle className="rotating" size={18} /> : <Send size={18} />}
                    {sendingTripId === group.tripId ? "Envoi…" : "Envoyer au voyageur"}
                  </button>
                </div>
              </section>
            );
          })}
        </div>
      )}
      {editingBooking && <AgentBookingModal booking={editingBooking} onClose={() => setEditingBookingId(null)} onSaved={handleBookingSaved} />}
    </div>
  );
}

import { useEffect, useRef, useState } from "react";
import { ArrowRight, BedDouble, Bus, CalendarDays, Car, Inbox, Plane, RefreshCw, Route } from "lucide-react";
import { Link } from "react-router-dom";
import { openAgentNotificationStream } from "../../api/agentNotificationStream";
import { getBookingRequests } from "../../api/bookingRequests";
import { getBookingRequestQuotes } from "../../api/travelerQuotes";
import { updateTripAssistanceFee } from "../../api/trips";
import { TripFinancialSummary } from "../../components/TripFinancialSummary";
import { getLatestAcceptedQuote } from "../../helpers/travelerQuotes";
import type { BookingRequest, BookingRequestStatus, NeedType } from "../../types/bookingRequest";
import type { TravelerQuote } from "../../types/travelerQuote";

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
  const [quotesByRequestId, setQuotesByRequestId] = useState<Record<number, TravelerQuote[]>>({});
  const [tripFeeOverrides, setTripFeeOverrides] = useState<Record<number, number>>({});
  const latestRequestId = useRef(0);

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
          data.map(async (request) => [request.id, await getBookingRequestQuotes(request.id, controller.signal)] as const),
        );
        if (!disposed && requestId === latestRequestId.current) {
          const requestsById = new Map(data.map((request) => [request.id, request]));
          setRequests([...requestsById.values()]);
          setQuotesByRequestId(Object.fromEntries(quoteEntries));
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
            const acceptedQuotes = group.requests
              .map((request) => getLatestAcceptedQuote(quotesByRequestId[request.id]))
              .filter((quote): quote is TravelerQuote => quote !== null);
            const assistanceFee = tripFeeOverrides[group.tripId] ?? group.assistanceFee;

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
                    const acceptedQuote = getLatestAcceptedQuote(quotesByRequestId[request.id]);
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
                          <strong>{acceptedQuote ? formatPrice(acceptedQuote.providerPrice, acceptedQuote.currency) : "Offre à définir"}</strong>
                        </div>
                        <span className={`request-status status-${request.status.toLowerCase()}`}>{statusLabels[request.status]}</span>
                        <Link className="agent-request-link" to={`/agent/booking-requests/${request.id}`}>
                          Voir la demande
                          <ArrowRight size={16} aria-hidden="true" />
                        </Link>
                      </li>
                    );
                  })}
                </ul>
                <TripFinancialSummary
                  assistanceFee={assistanceFee}
                  providerOffers={acceptedQuotes}
                  editable
                  onSaveAssistanceFee={(value) => handleUpdateTripAssistanceFee(group.tripId, value)}
                />
              </section>
            );
          })}
        </div>
      )}
    </div>
  );
}

import { useEffect, useMemo, useState } from "react";
import { AlertCircle, ArrowRight, CalendarDays, CheckCircle2, Inbox, ListChecks, MapPin, Plane, Plus, RefreshCw } from "lucide-react";
import { Link } from "react-router-dom";
import { getTravelerQuotes } from "../../api/travelerQuotes";
import { getTravelEvent } from "../../api/travelEvents";
import { getTripDetail, getTravelerTrips } from "../../api/trips";
import { deriveTravelerNeedState, type TravelerNeedUxState } from "../../helpers/travelerNeedState";
import {
  computeDaysUntil,
  formatDateBadge,
  formatDateRange,
  getTravelEventImage,
  parseLocalDate,
  startOfToday,
  tripStatusLabels,
} from "../../helpers/travelerTripPresentation";
import { getLatestQuote } from "../../helpers/travelerQuotes";
import { deriveTravelerTripProgress, type TravelerNeedStateEntry } from "../../helpers/travelerTripState";
import { Badge } from "../../components/ui/Badge";
import type { TravelEvent } from "../../types/travelEvent";
import type { Trip } from "../../types/trip";

const userActionNeedStates: TravelerNeedUxState[] = [
  "TO_ORGANIZE",
  "PROPOSAL_READY",
  "SUPPLIER_PAYMENT_REQUIRED",
  "BOOKING_CONFIRMED_SUPPLIER_PAYMENT_REQUIRED",
];

function getNextTrip(trips: Trip[]) {
  const today = startOfToday();
  return (
    trips
      .filter((trip) => parseLocalDate(trip.startDate) >= today)
      .sort((first, second) => parseLocalDate(first.startDate).getTime() - parseLocalDate(second.startDate).getTime())[0] ?? null
  );
}

interface NextTripMetrics {
  progressPercent: number;
  progressLabel: string;
  totalNeeds: number;
  bookedNeeds: number;
  pendingActions: number;
}

interface OtherTripCardData {
  metrics: NextTripMetrics;
  location: string | null;
  description: string | null;
  imageUrl: string;
}

type OtherTripsTab = "upcoming" | "past";
type OtherTripsSort = "recent" | "oldest";

function deriveNextTripMetrics(needStates: TravelerNeedStateEntry[]): NextTripMetrics {
  const activeNeeds = needStates.filter((entry) => entry.state !== "CANCELLED");
  const progress = deriveTravelerTripProgress(needStates);
  const progressPercent = progress ? Math.round((progress.finalized / progress.total) * 100) : 0;

  const bookedNeeds = activeNeeds.filter(
    (entry) => entry.state === "BOOKING_CONFIRMED" || entry.state === "BOOKING_CONFIRMED_SUPPLIER_PAYMENT_REQUIRED",
  ).length;
  const pendingActions = activeNeeds.filter((entry) => userActionNeedStates.includes(entry.state)).length;

  return {
    progressPercent,
    progressLabel: progress ? `${progressPercent} %` : "Préparation à démarrer",
    totalNeeds: activeNeeds.length,
    bookedNeeds,
    pendingActions,
  };
}

function getTripStatusBadgeVariant(status: Trip["status"]) {
  switch (status) {
    case "CONFIRMED":
      return "success";
    case "CANCELLED":
      return "danger";
    case "DRAFT":
      return "warning";
    default:
      return "neutral";
  }
}

function deriveNeedStates(
  detail: Awaited<ReturnType<typeof getTripDetail>>,
  quotesByBookingRequestId: Map<number, Awaited<ReturnType<typeof getTravelerQuotes>>>,
) {
  return detail.needs.map((need) => ({
    needId: need.id,
    state: deriveTravelerNeedState(need, need.bookingRequestId ? getLatestQuote(quotesByBookingRequestId.get(need.bookingRequestId)) : null),
  }));
}

function formatDisplayedTripsCount(count: number) {
  return `${count} voyage${count > 1 ? "s" : ""}`;
}

function buildOperationalSummary(metrics: NextTripMetrics) {
  if (metrics.totalNeeds === 0) {
    return "Préparation à démarrer";
  }

  const parts = [`${metrics.totalNeeds} besoin${metrics.totalNeeds > 1 ? "s" : ""}`];
  if (metrics.bookedNeeds > 0) {
    parts.push(`${metrics.bookedNeeds} réservé${metrics.bookedNeeds > 1 ? "s" : ""}`);
  }
  if (metrics.pendingActions > 0) {
    parts.push(`${metrics.pendingActions} action${metrics.pendingActions > 1 ? "s" : ""} à faire`);
  }

  return parts.join(" • ");
}

export function TravelerTripsPage() {
  const [trips, setTrips] = useState<Trip[]>([]);
  const [nextTripEvent, setNextTripEvent] = useState<TravelEvent | null>(null);
  const [nextTripMetrics, setNextTripMetrics] = useState<NextTripMetrics | null>(null);
  const [nextTripLoading, setNextTripLoading] = useState(false);
  const [otherTripCards, setOtherTripCards] = useState<Map<number, OtherTripCardData>>(new Map());
  const [otherTripsLoading, setOtherTripsLoading] = useState(false);
  const [activeOtherTripsTab, setActiveOtherTripsTab] = useState<OtherTripsTab>("upcoming");
  const [otherTripsSort, setOtherTripsSort] = useState<OtherTripsSort>("recent");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [requestVersion, setRequestVersion] = useState(0);

  const nextTrip = useMemo(() => getNextTrip(trips), [trips]);
  const shouldShowNextTripCta = (nextTripMetrics?.pendingActions ?? 0) > 0;
  const nextTripCtaLabel = shouldShowNextTripCta ? "Continuer la préparation" : "Voir mon voyage";
  const otherTrips = useMemo(() => {
    if (!nextTrip) return trips;
    return trips.filter((trip) => trip.id !== nextTrip.id);
  }, [nextTrip, trips]);

  useEffect(() => {
    const controller = new AbortController();

    async function loadTrips() {
      setLoading(true);
      setError(null);

      try {
        setTrips(await getTravelerTrips(controller.signal));
      } catch (requestError: unknown) {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        setError("Impossible de charger vos voyages pour le moment.");
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void loadTrips();
    return () => controller.abort();
  }, [requestVersion]);

  useEffect(() => {
    if (!nextTrip) {
      return;
    }

    const controller = new AbortController();

    async function loadNextTripData() {
      setNextTripLoading(true);

      try {
        const [detail, travelerQuotes, event] = await Promise.all([
          getTripDetail(nextTrip.id, controller.signal),
          getTravelerQuotes(controller.signal),
          nextTrip.travelEventId ? getTravelEvent(nextTrip.travelEventId, controller.signal) : Promise.resolve(null),
        ]);

        const quotesByBookingRequestId = travelerQuotes.reduce<Map<number, typeof travelerQuotes>>((quotesByRequest, quote) => {
          const requestQuotes = quotesByRequest.get(quote.bookingRequestId) ?? [];
          requestQuotes.push(quote);
          quotesByRequest.set(quote.bookingRequestId, requestQuotes);
          return quotesByRequest;
        }, new Map());

        const needStates: TravelerNeedStateEntry[] = detail.needs.map((need) => ({
          needId: need.id,
          state: deriveTravelerNeedState(need, need.bookingRequestId ? getLatestQuote(quotesByBookingRequestId.get(need.bookingRequestId)) : null),
        }));

        setNextTripEvent(event);
        setNextTripMetrics(deriveNextTripMetrics(needStates));
      } catch (requestError: unknown) {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        setNextTripEvent(null);
        setNextTripMetrics(null);
      } finally {
        if (!controller.signal.aborted) setNextTripLoading(false);
      }
    }

    void loadNextTripData();
    return () => controller.abort();
  }, [nextTrip]);

  useEffect(() => {
    if (otherTrips.length === 0) {
      return;
    }

    const controller = new AbortController();

    async function loadOtherTripsData() {
      setOtherTripsLoading(true);

      try {
        const travelerQuotes = await getTravelerQuotes(controller.signal);
        const quotesByBookingRequestId = travelerQuotes.reduce<Map<number, typeof travelerQuotes>>((quotesByRequest, quote) => {
          const requestQuotes = quotesByRequest.get(quote.bookingRequestId) ?? [];
          requestQuotes.push(quote);
          quotesByRequest.set(quote.bookingRequestId, requestQuotes);
          return quotesByRequest;
        }, new Map());

        const tripCards = await Promise.all(
          otherTrips.map(async (trip) => {
            const [detail, event] = await Promise.all([
              getTripDetail(trip.id, controller.signal),
              trip.travelEventId ? getTravelEvent(trip.travelEventId, controller.signal) : Promise.resolve(null),
            ]);

            const needStates = deriveNeedStates(detail, quotesByBookingRequestId);
            const metrics = deriveNextTripMetrics(needStates);

            return [
              trip.id,
              {
                metrics,
                location: event?.location?.trim() ? event.location : null,
                description: event?.description?.trim() ? event.description : null,
                imageUrl: getTravelEventImage(event),
              },
            ] as const;
          }),
        );

        if (!controller.signal.aborted) {
          setOtherTripCards(new Map(tripCards));
        }
      } catch (requestError: unknown) {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        setOtherTripCards(new Map());
      } finally {
        if (!controller.signal.aborted) setOtherTripsLoading(false);
      }
    }

    void loadOtherTripsData();
    return () => controller.abort();
  }, [otherTrips]);

  const daysUntilNextTrip = nextTrip ? computeDaysUntil(nextTrip.startDate) : null;
  const tripStatusLabel = nextTrip ? tripStatusLabels[nextTrip.status] : null;
  const nextTripDateRange = nextTrip ? formatDateRange(nextTrip.startDate, nextTrip.endDate) : null;
  const nextTripLocation = nextTripEvent?.location?.trim() ? nextTripEvent.location : null;
  const nextTripImageUrl = getTravelEventImage(nextTripEvent);
  const countdownLabel =
    daysUntilNextTrip === null
      ? null
      : daysUntilNextTrip <= 0
        ? "Aujourd'hui"
        : daysUntilNextTrip === 1
          ? "Demain"
          : `Dans ${daysUntilNextTrip} jours`;

  const today = useMemo(() => startOfToday(), []);
  const upcomingOtherTrips = useMemo(() => otherTrips.filter((trip) => parseLocalDate(trip.endDate) >= today), [otherTrips, today]);
  const pastOtherTrips = useMemo(() => otherTrips.filter((trip) => parseLocalDate(trip.endDate) < today), [otherTrips, today]);
  const selectedOtherTrips = activeOtherTripsTab === "upcoming" ? upcomingOtherTrips : pastOtherTrips;

  const displayedOtherTrips = useMemo(() => {
    const list = [...selectedOtherTrips];

    list.sort((first, second) => {
      if (activeOtherTripsTab === "upcoming") {
        const firstTime = parseLocalDate(first.startDate).getTime();
        const secondTime = parseLocalDate(second.startDate).getTime();
        return otherTripsSort === "recent" ? firstTime - secondTime : secondTime - firstTime;
      }

      const firstTime = parseLocalDate(first.endDate).getTime();
      const secondTime = parseLocalDate(second.endDate).getTime();
      return otherTripsSort === "recent" ? secondTime - firstTime : firstTime - secondTime;
    });

    return list;
  }, [activeOtherTripsTab, otherTripsSort, selectedOtherTrips]);

  return (
    <div className="traveler-page traveler-trips-page">
      <section className="traveler-trips-heading">
        <div className="traveler-trips-heading__copy">
          <span className="eyebrow traveler-trips-heading__eyebrow">VOTRE CARNET DE VOYAGE</span>
          <h1 className="traveler-trips-heading__title">Mes voyages</h1>
          <p className="traveler-trips-heading__description">Retrouvez vos voyages et suivez leur préparation.</p>
        </div>
        <Link className="primary-button traveler-trips-heading__create" to="/traveler/trips/new">
          <Plus size={18} />
          <span>Nouveau voyage</span>
        </Link>
      </section>

      <section className="traveler-trips-next-section" aria-label="Vos voyages">
        {loading && (
          <div className="state-panel" role="status">
            <span className="spinner" aria-hidden="true" />
            <strong>Chargement de vos voyages…</strong>
            <p>Nous préparons votre carnet de voyage.</p>
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

        {!loading && !error && trips.length === 0 && (
          <div className="state-panel">
            <Inbox size={28} aria-hidden="true" />
            <strong>Aucun voyage pour le moment</strong>
            <p>Vos prochains voyages apparaîtront ici.</p>
            <Link className="primary-button" to="/traveler/trips/new">
              <Plus size={17} /> Organiser mon voyage
            </Link>
          </div>
        )}

        {!loading && !error && trips.length > 0 && (
          <>
            {nextTrip ? (
              <article className="next-trip-card" aria-labelledby="next-trip-title">
                <div className="next-trip-card__media">
                  <img src={nextTripImageUrl} alt={`Illustration du voyage ${nextTrip.title || `#${nextTrip.id}`}`} loading="lazy" />
                  <span className="next-trip-card__badge">
                    <Plane size={14} /> Prochain voyage
                  </span>
                </div>

                <div className="next-trip-card__content">
                  <div className="next-trip-card__topline">
                    <Badge variant={getTripStatusBadgeVariant(nextTrip.status)} className="trip-status">
                      {tripStatusLabel}
                    </Badge>
                    {countdownLabel && (
                      <span className="next-trip-card__countdown" aria-label={countdownLabel}>
                        <span className="next-trip-card__countdown-label">
                          <CalendarDays size={16} /> {countdownLabel}
                        </span>
                        <span className="next-trip-card__countdown-subtitle">avant le départ</span>
                      </span>
                    )}
                  </div>

                  <h2 id="next-trip-title" className="next-trip-card__title">
                    {nextTrip.title || `Voyage #${nextTrip.id}`}
                  </h2>

                  <div className="next-trip-card__meta">
                    {nextTripLocation && (
                      <p>
                        <MapPin size={16} /> {nextTripLocation}
                      </p>
                    )}
                    {nextTripDateRange && (
                      <p>
                        <CalendarDays size={16} /> {nextTripDateRange}
                      </p>
                    )}
                  </div>

                  <div className="next-trip-card__progress-block">
                    <div className="next-trip-card__progress-heading">
                      <span>Préparation du voyage</span>
                      <span>{nextTripMetrics?.progressLabel ?? "Préparation à démarrer"}</span>
                    </div>
                    <div
                      className="next-trip-card__progress-track"
                      role="progressbar"
                      aria-label="Préparation du voyage"
                      aria-valuemin={0}
                      aria-valuemax={100}
                      aria-valuenow={nextTripMetrics?.progressPercent ?? 0}
                    >
                      <span className="next-trip-card__progress-fill" style={{ width: `${nextTripMetrics?.progressPercent ?? 0}%` }} />
                    </div>
                  </div>

                  <div className="next-trip-card__footer">
                    <div className="next-trip-card__indicators" aria-label="Indicateurs de préparation">
                      <div className="next-trip-card__indicator">
                        <span className="next-trip-card__indicator-icon is-info">
                          <ListChecks size={15} />
                        </span>
                        <div>
                          <strong>
                            {nextTripMetrics?.totalNeeds ?? 0} besoin{(nextTripMetrics?.totalNeeds ?? 0) > 1 ? "s" : ""}
                          </strong>
                          <span>à organiser</span>
                        </div>
                      </div>

                      <div className="next-trip-card__indicator">
                        <span className="next-trip-card__indicator-icon is-success">
                          <CheckCircle2 size={15} />
                        </span>
                        <div>
                          <strong>
                            {nextTripMetrics?.bookedNeeds ?? 0} réservé{(nextTripMetrics?.bookedNeeds ?? 0) > 1 ? "s" : ""}
                          </strong>
                          <span>déjà confirmés</span>
                        </div>
                      </div>

                      <div className="next-trip-card__indicator">
                        <span className="next-trip-card__indicator-icon is-warning">
                          <AlertCircle size={15} />
                        </span>
                        <div>
                          <strong>
                            {nextTripMetrics?.pendingActions ?? 0} action{(nextTripMetrics?.pendingActions ?? 0) > 1 ? "s" : ""} à faire
                          </strong>
                          <span>en attente</span>
                        </div>
                      </div>
                    </div>

                    <Link className="next-trip-card__cta" to={`/traveler/trips/${nextTrip.id}`}>
                      {nextTripCtaLabel} <ArrowRight size={16} />
                    </Link>
                  </div>

                  {nextTripLoading && <p className="next-trip-card__loading">Mise à jour des indicateurs…</p>}
                </div>
              </article>
            ) : (
              <article className="next-trip-empty" aria-label="Aucun prochain voyage">
                <h2>Votre prochaine aventure commence ici.</h2>
                <p>Créez un voyage pour commencer à organiser votre séjour.</p>
                <Link className="primary-button" to="/traveler/trips/new">
                  <Plus size={17} /> Nouveau voyage
                </Link>
              </article>
            )}

            <section className="traveler-trips-other" aria-label="Mes autres voyages">
              <h2 className="traveler-trips-other__title">Mes autres voyages</h2>

              <div className="traveler-trips-other__toolbar" role="group" aria-label="Filtres des autres voyages">
                <div className="traveler-trips-other__tabs" role="tablist" aria-label="Filtrer les voyages">
                  <button
                    type="button"
                    role="tab"
                    aria-selected={activeOtherTripsTab === "upcoming"}
                    className={`trips-tab${activeOtherTripsTab === "upcoming" ? " is-active" : ""}`}
                    onClick={() => setActiveOtherTripsTab("upcoming")}
                  >
                    À venir ({upcomingOtherTrips.length})
                  </button>
                  <button
                    type="button"
                    role="tab"
                    aria-selected={activeOtherTripsTab === "past"}
                    className={`trips-tab${activeOtherTripsTab === "past" ? " is-active" : ""}`}
                    onClick={() => setActiveOtherTripsTab("past")}
                  >
                    Passés ({pastOtherTrips.length})
                  </button>
                </div>

                <div className="traveler-trips-other__controls">
                  <label className="trips-sort-select" htmlFor="other-trips-sort">
                    <span className="sr-only">Trier les voyages</span>
                    <select
                      id="other-trips-sort"
                      value={otherTripsSort}
                      onChange={(event) => setOtherTripsSort(event.target.value as OtherTripsSort)}
                    >
                      <option value="recent">Plus récent</option>
                      <option value="oldest">Plus ancien</option>
                    </select>
                  </label>
                  <span className="trips-count" aria-live="polite">
                    {formatDisplayedTripsCount(displayedOtherTrips.length)}
                  </span>
                </div>
              </div>

              {displayedOtherTrips.length > 0 ? (
                <div className="other-trips-grid">
                  {displayedOtherTrips.map((trip) => {
                    const cardData = otherTripCards.get(trip.id);
                    const dateBadge = formatDateBadge(trip.startDate, trip.endDate);
                    const statusLabel = tripStatusLabels[trip.status];

                    return (
                      <Link
                        className="other-trip-card"
                        to={`/traveler/trips/${trip.id}`}
                        key={trip.id}
                        aria-label={`Consulter le voyage ${trip.title || `#${trip.id}`}`}
                      >
                        <div className="other-trip-card__media">
                          <img
                            src={cardData?.imageUrl ?? getTravelEventImage(null)}
                            alt={`Illustration du voyage ${trip.title || `#${trip.id}`}`}
                            loading="lazy"
                          />
                          <Badge variant={getTripStatusBadgeVariant(trip.status)} className="trip-status other-trip-card__status">
                            {statusLabel}
                          </Badge>
                          <span className="other-trip-card__date-pill">
                            <CalendarDays size={14} /> {dateBadge}
                          </span>
                        </div>

                        <div className="other-trip-card__body">
                          <h3>{trip.title || `Voyage #${trip.id}`}</h3>

                          {cardData?.location && (
                            <p className="other-trip-card__location">
                              <MapPin size={15} /> {cardData.location}
                            </p>
                          )}

                          {cardData?.description && <p className="other-trip-card__description">{cardData.description}</p>}

                          <div className="other-trip-card__footer">
                            <p>{buildOperationalSummary(cardData?.metrics ?? deriveNextTripMetrics([]))}</p>
                            <span className="other-trip-card__cta" aria-hidden="true">
                              <ArrowRight size={16} />
                            </span>
                          </div>
                        </div>
                      </Link>
                    );
                  })}
                </div>
              ) : activeOtherTripsTab === "upcoming" ? (
                <div className="other-trips-empty" role="status">
                  <p>Vous n'avez pas d'autre voyage à venir.</p>
                  <span>Commencez à préparer votre prochaine aventure.</span>
                  <Link className="primary-button" to="/traveler/trips/new">
                    <Plus size={17} /> Nouveau voyage
                  </Link>
                </div>
              ) : (
                <p className="other-trips-empty-inline">Aucun voyage passé pour le moment.</p>
              )}

              {otherTripsLoading && <p className="other-trips-loading">Mise à jour des données de voyage…</p>}
            </section>
          </>
        )}
      </section>
    </div>
  );
}

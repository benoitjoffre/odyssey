import { useEffect, useMemo, useRef, useState } from "react";
import { AlertCircle, Bus, CalendarDays, Car, Check, ChevronRight, Hotel, Inbox, LoaderCircle, Plane, RefreshCw, Route } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { acceptTravelerQuote, getTravelerQuotes, rejectTravelerQuote } from "../../api/travelerQuotes";
import { getTripDetail, getTravelerTrips } from "../../api/trips";
import { formatDateRange } from "../../helpers/travelerTripPresentation";
import type { TravelerQuote, TravelerQuoteStatus } from "../../types/travelerQuote";
import type { Trip, TripNeed, TripNeedType } from "../../types/trip";

const statusLabels: Record<TravelerQuoteStatus, string> = {
  SENT: "À examiner",
  ACCEPTED: "Acceptée",
  REJECTED: "Refusée",
  EXPIRED: "Expirée",
};

const needTypeLabels: Record<TripNeedType, string> = {
  FLIGHT: "Vol",
  ACCOMMODATION: "Hébergement",
  TRANSFER: "Transfert",
  CAR: "Voiture",
  BUS: "Bus",
};

const needTypeEyebrows: Record<TripNeedType, string> = {
  FLIGHT: "VOL",
  ACCOMMODATION: "HÉBERGEMENT",
  TRANSFER: "TRANSFERT",
  CAR: "VOITURE",
  BUS: "BUS",
};

const needTypeIcons: Record<TripNeedType, typeof Plane> = {
  FLIGHT: Plane,
  ACCOMMODATION: Hotel,
  TRANSFER: Route,
  CAR: Car,
  BUS: Bus,
};

type TravelerQuoteFilter = "all" | "review" | "accepted";
type TravelerQuoteSort = "recent" | "oldest";

interface QuoteNeedContext {
  trip: Trip;
  need: TripNeed;
}

interface GroupedTripQuotes {
  trip: Trip;
  quotes: TravelerQuote[];
  latestCreatedAt: string;
}

function formatPrice(price: number, currency: string) {
  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency,
    maximumFractionDigits: 2,
  }).format(price);
}

function getQuoteSecondaryDetail(need: TripNeed) {
  if (need.type === "TRANSFER" && need.transferCriteria) {
    return `${need.transferCriteria.pickupLocation} → ${need.transferCriteria.dropoffLocation}`;
  }
  const notes = need.notes?.trim();
  return notes && notes.length > 0 ? notes : null;
}

export function TravelerQuotesPage() {
  const navigate = useNavigate();
  const [quotes, setQuotes] = useState<TravelerQuote[]>([]);
  const [needContextByBookingRequestId, setNeedContextByBookingRequestId] = useState<Map<number, QuoteNeedContext>>(new Map());
  const [activeFilter, setActiveFilter] = useState<TravelerQuoteFilter>("all");
  const [sortOrder, setSortOrder] = useState<TravelerQuoteSort>("recent");
  const [pendingQuoteActions, setPendingQuoteActions] = useState<Record<number, "accept" | "reject">>({});
  const [quoteActionErrors, setQuoteActionErrors] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [requestVersion, setRequestVersion] = useState(0);
  const pendingQuoteActionRef = useRef<Set<number>>(new Set());

  async function handleQuoteAction(quote: TravelerQuote, action: "accept" | "reject") {
    if (pendingQuoteActionRef.current.has(quote.id)) return;

    pendingQuoteActionRef.current.add(quote.id);
    setPendingQuoteActions((current) => ({ ...current, [quote.id]: action }));
    setQuoteActionErrors((current) => {
      const next = { ...current };
      delete next[quote.id];
      return next;
    });

    try {
      const updatedQuote = action === "accept" ? await acceptTravelerQuote(quote.id) : await rejectTravelerQuote(quote.id);

      setQuotes((current) => current.map((item) => (item.id === updatedQuote.id ? updatedQuote : item)));
    } catch {
      setQuoteActionErrors((current) => ({
        ...current,
        [quote.id]: "Cette proposition n’a pas pu être mise à jour. Elle a peut-être déjà été traitée.",
      }));
    } finally {
      pendingQuoteActionRef.current.delete(quote.id);
      setPendingQuoteActions((current) => {
        const next = { ...current };
        delete next[quote.id];
        return next;
      });
    }
  }

  useEffect(() => {
    const controller = new AbortController();

    async function loadQuotes() {
      setLoading(true);
      setError(null);

      try {
        const [travelerQuotes, travelerTrips] = await Promise.all([getTravelerQuotes(controller.signal), getTravelerTrips(controller.signal)]);

        const tripDetails = await Promise.all(travelerTrips.map((trip) => getTripDetail(trip.id, controller.signal)));
        const needContextEntries = tripDetails.flatMap((detail) =>
          detail.needs
            .filter((need) => need.bookingRequestId !== null)
            .map((need) => [need.bookingRequestId as number, { trip: detail, need }] as const),
        );

        setQuotes(travelerQuotes);
        setNeedContextByBookingRequestId(new Map(needContextEntries));
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

  const counts = useMemo(() => {
    const review = quotes.filter((quote) => quote.status === "SENT").length;
    const accepted = quotes.filter((quote) => quote.status === "ACCEPTED").length;
    return { all: quotes.length, review, accepted };
  }, [quotes]);

  const filteredQuotes = useMemo(() => {
    if (activeFilter === "review") return quotes.filter((quote) => quote.status === "SENT");
    if (activeFilter === "accepted") return quotes.filter((quote) => quote.status === "ACCEPTED");
    return quotes;
  }, [activeFilter, quotes]);

  const sortedQuotes = useMemo(() => {
    return [...filteredQuotes].sort((first, second) => {
      const firstTime = new Date(first.createdAt).getTime();
      const secondTime = new Date(second.createdAt).getTime();
      return sortOrder === "recent" ? secondTime - firstTime : firstTime - secondTime;
    });
  }, [filteredQuotes, sortOrder]);

  const groupedTripQuotes = useMemo(() => {
    const groups = new Map<number, GroupedTripQuotes>();

    sortedQuotes.forEach((quote) => {
      const context = needContextByBookingRequestId.get(quote.bookingRequestId);
      if (!context) return;

      const existing = groups.get(context.trip.id);
      if (!existing) {
        groups.set(context.trip.id, {
          trip: context.trip,
          quotes: [quote],
          latestCreatedAt: quote.createdAt,
        });
        return;
      }

      existing.quotes.push(quote);
      if (new Date(quote.createdAt).getTime() > new Date(existing.latestCreatedAt).getTime()) {
        existing.latestCreatedAt = quote.createdAt;
      }
    });

    const grouped = [...groups.values()];
    grouped.sort((first, second) => {
      const firstTime = new Date(first.latestCreatedAt).getTime();
      const secondTime = new Date(second.latestCreatedAt).getTime();
      return sortOrder === "recent" ? secondTime - firstTime : firstTime - secondTime;
    });
    return grouped;
  }, [needContextByBookingRequestId, sortedQuotes, sortOrder]);

  const orphanQuotesCount = useMemo(
    () => sortedQuotes.filter((quote) => !needContextByBookingRequestId.has(quote.bookingRequestId)).length,
    [needContextByBookingRequestId, sortedQuotes],
  );

  const emptyFilterMessage =
    activeFilter === "review"
      ? { title: "Aucune proposition à examiner.", body: "Vous êtes à jour." }
      : activeFilter === "accepted"
        ? { title: "Aucune proposition acceptée pour le moment.", body: "Les propositions acceptées apparaîtront ici." }
        : { title: "Aucune proposition pour le moment.", body: "Les propositions préparées par votre conseiller apparaîtront ici." };

  return (
    <div className="traveler-page">
      <section className="traveler-page-heading traveler-proposals-heading">
        <span className="eyebrow">Votre voyage, préparé avec soin</span>
        <h1>Mes propositions</h1>
        <p>Retrouvez ici les propositions préparées pour vos voyages.</p>
      </section>

      {!loading && !error && (
        <section className="traveler-proposals-toolbar" aria-label="Filtres et tri des propositions">
          <div className="traveler-proposals-filters" role="tablist" aria-label="Filtrer les propositions">
            <button
              type="button"
              className={`traveler-proposals-filter ${activeFilter === "all" ? "is-active" : ""}`}
              onClick={() => setActiveFilter("all")}
              aria-pressed={activeFilter === "all"}
            >
              Toutes ({counts.all})
            </button>
            <button
              type="button"
              className={`traveler-proposals-filter ${activeFilter === "review" ? "is-active" : ""}`}
              onClick={() => setActiveFilter("review")}
              aria-pressed={activeFilter === "review"}
            >
              À examiner ({counts.review})
            </button>
            <button
              type="button"
              className={`traveler-proposals-filter ${activeFilter === "accepted" ? "is-active" : ""}`}
              onClick={() => setActiveFilter("accepted")}
              aria-pressed={activeFilter === "accepted"}
            >
              Acceptées ({counts.accepted})
            </button>
          </div>

          <label className="traveler-proposals-sort" htmlFor="traveler-proposals-sort">
            <span className="sr-only">Trier les propositions</span>
            <select
              id="traveler-proposals-sort"
              value={sortOrder}
              onChange={(event) => setSortOrder(event.target.value as TravelerQuoteSort)}
              aria-label="Trier les propositions"
            >
              <option value="recent">Plus récentes</option>
              <option value="oldest">Plus anciennes</option>
            </select>
          </label>
        </section>
      )}

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

      {!loading && !error && groupedTripQuotes.length === 0 && (
        <div className="state-panel">
          <Inbox size={28} aria-hidden="true" />
          <strong>{emptyFilterMessage.title}</strong>
          <p>{emptyFilterMessage.body}</p>
        </div>
      )}

      {!loading && !error && groupedTripQuotes.length > 0 && (
        <section className="traveler-proposal-groups" aria-label="Propositions par voyage">
          {groupedTripQuotes.map((group) => (
            <article className="traveler-proposal-group" key={group.trip.id}>
              <header className="traveler-proposal-group__header">
                <div className="traveler-proposal-group__header-main">
                  <h2>{group.trip.title}</h2>
                  <p>
                    <span>
                      <CalendarDays size={14} aria-hidden="true" /> {formatDateRange(group.trip.startDate, group.trip.endDate)}
                    </span>
                    <span aria-hidden="true">|</span>
                    <span>
                      {group.quotes.length} proposition{group.quotes.length > 1 ? "s" : ""}
                    </span>
                  </p>
                </div>

                <Link className="traveler-proposal-group__trip-link" to={`/traveler/trips/${group.trip.id}`}>
                  Voir le voyage <ChevronRight size={16} aria-hidden="true" />
                </Link>
              </header>

              <div className="traveler-proposal-list" role="list">
                {group.quotes.map((quote) => {
                  const context = needContextByBookingRequestId.get(quote.bookingRequestId);
                  if (!context) return null;

                  const need = context.need;
                  const needType = need.type;
                  const Icon = needTypeIcons[needType];
                  const statusClass = `status-${quote.status.toLowerCase()}`;
                  const detail = getQuoteSecondaryDetail(need);
                  const rowLink = `/traveler/trips/${context.trip.id}#traveler-need-${need.id}`;

                  const isSent = quote.status === "SENT";
                  const pendingAction = pendingQuoteActions[quote.id];
                  const quoteActionsDisabled = pendingAction !== undefined;
                  const actionError = quoteActionErrors[quote.id] ?? null;

                  return (
                    <div
                      key={quote.id}
                      className={`traveler-proposal-row ${isSent ? "is-review" : ""}`}
                      role="listitem"
                      tabIndex={0}
                      aria-label={`${needTypeLabels[needType]} - ${quote.description}`}
                      onClick={() => navigate(rowLink)}
                      onKeyDown={(event) => {
                        if (event.key === "Enter" || event.key === " ") {
                          event.preventDefault();
                          navigate(rowLink);
                        }
                      }}
                    >
                      <span className="traveler-proposal-row__icon" aria-hidden="true">
                        <Icon size={22} />
                      </span>

                      <div className="traveler-proposal-row__service">
                        <span className="traveler-proposal-row__type">{needTypeEyebrows[needType]}</span>
                        <strong>{quote.description}</strong>
                        {detail && <p>{detail}</p>}
                      </div>

                      <div className="traveler-proposal-row__price">{formatPrice(quote.providerPrice, quote.currency)}</div>

                      <span className={`traveler-proposal-row__status ${statusClass}`}>
                        {quote.status === "SENT" ? <AlertCircle size={14} aria-hidden="true" /> : <Check size={14} aria-hidden="true" />}
                        {statusLabels[quote.status]}
                      </span>

                      {isSent ? (
                        <div className="traveler-proposal-row__actions" onClick={(event) => event.stopPropagation()}>
                          <button
                            type="button"
                            className="traveler-proposal-row__reject"
                            disabled={quoteActionsDisabled}
                            aria-label={`Refuser la proposition ${quote.description}`}
                            onClick={() => void handleQuoteAction(quote, "reject")}
                          >
                            {pendingAction === "reject" ? <LoaderCircle className="rotating" size={16} /> : null}
                            {pendingAction === "reject" ? "Refus…" : "Refuser"}
                          </button>
                          <button
                            type="button"
                            className="traveler-proposal-row__accept"
                            disabled={quoteActionsDisabled}
                            aria-label={`Accepter la proposition ${quote.description}`}
                            onClick={() => void handleQuoteAction(quote, "accept")}
                          >
                            {pendingAction === "accept" ? <LoaderCircle className="rotating" size={16} /> : null}
                            {pendingAction === "accept" ? "Acceptation…" : "Accepter"}
                          </button>
                        </div>
                      ) : (
                        <span className="traveler-proposal-row__actions-spacer" aria-hidden="true" />
                      )}

                      {actionError ? (
                        <p className="traveler-proposal-row__action-error" role="alert" onClick={(event) => event.stopPropagation()}>
                          {actionError}
                        </p>
                      ) : null}

                      <button
                        type="button"
                        className="traveler-proposal-row__chevron"
                        aria-label={`Voir le détail de la proposition ${quote.description}`}
                        onClick={(event) => {
                          event.stopPropagation();
                          navigate(rowLink);
                        }}
                      >
                        <ChevronRight size={18} />
                      </button>
                    </div>
                  );
                })}
              </div>
            </article>
          ))}
        </section>
      )}

      {!loading && !error && orphanQuotesCount > 0 && (
        <p className="traveler-proposals-note" role="status">
          {orphanQuotesCount} proposition{orphanQuotesCount > 1 ? "s" : ""} non affichée{orphanQuotesCount > 1 ? "s" : ""} car non rattachée à un
          voyage visible.
        </p>
      )}
    </div>
  );
}

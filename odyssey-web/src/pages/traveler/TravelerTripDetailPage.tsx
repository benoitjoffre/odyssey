import { useEffect, useRef, useState, type ReactNode } from "react";
import { ArrowLeft, BedDouble, Bus, CalendarDays, Car, Hotel, LoaderCircle, Luggage, Plane, RefreshCw, Route, Trash2 } from "lucide-react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { createBookingRequest } from "../../api/bookingRequests";
import { createTripCheckoutSession } from "../../api/payments";
import { acceptTravelerQuote, getTravelerQuotes, rejectTravelerQuote } from "../../api/travelerQuotes";
import { deleteTrip, getTripDetail } from "../../api/trips";
import { TravelerNeedCard } from "../../components/TravelerNeedCard";
import { TravelerNeedForm } from "../../components/TravelerNeedForm";
import { TravelerTripFinances, type SupplierServiceAmount } from "../../components/TravelerTripFinances";
import { TripNextAction } from "../../components/TripNextAction";
import { TripReadySummary } from "../../components/TripReadySummary";
import { deriveTravelerNeedState } from "../../helpers/travelerNeedState";
import {
  deriveTravelerTripProgress,
  deriveTravelerTripUxState,
  type TravelerNeedStateEntry,
  type TravelerTripUxState,
} from "../../helpers/travelerTripState";
import { getLatestAcceptedQuote, getLatestQuote } from "../../helpers/travelerQuotes";
import type { OrganizableNeedType } from "../../types/need";
import type { PaymentStatus } from "../../types/payment";
import type { TripDetail, TripNeed, TripNeedType } from "../../types/trip";
import type { TravelerQuote } from "../../types/travelerQuote";

const needLabels: Record<TripNeedType, string> = {
  ACCOMMODATION: "Hébergement",
  FLIGHT: "Vol",
  TRANSFER: "Transfert",
  CAR: "Voiture",
  BUS: "Bus",
};

const needIcons: Record<TripNeedType, ReactNode> = {
  ACCOMMODATION: <Hotel size={21} />,
  FLIGHT: <Plane size={21} />,
  TRANSFER: <Route size={21} />,
  CAR: <Car size={21} />,
  BUS: <Bus size={21} />,
};

const organizationChoices: Array<{ type: TripNeedType; label: string; available: boolean }> = [
  { type: "FLIGHT", label: "Vol", available: true },
  { type: "ACCOMMODATION", label: "Hébergement", available: true },
  { type: "TRANSFER", label: "Transfert", available: true },
  { type: "CAR", label: "Voiture", available: false },
  { type: "BUS", label: "Bus", available: false },
];

function formatDate(value: string) {
  return new Intl.DateTimeFormat("fr-FR", {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(new Date(`${value}T00:00:00`));
}

export function TravelerTripDetailPage() {
  const { tripId } = useParams<{ tripId: string }>();
  const navigate = useNavigate();
  const parsedTripId = Number(tripId);
  const hasInvalidTripId = !Number.isInteger(parsedTripId) || parsedTripId <= 0;
  const [trip, setTrip] = useState<TripDetail | null>(null);
  const [acceptedQuotesByNeedId, setAcceptedQuotesByNeedId] = useState<Record<number, TravelerQuote | null>>({});
  const [latestQuotesByNeedId, setLatestQuotesByNeedId] = useState<Record<number, TravelerQuote | null>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [requestVersion, setRequestVersion] = useState(0);
  const [selectedNeedType, setSelectedNeedType] = useState<OrganizableNeedType | null>(null);
  const [sendingNeedId, setSendingNeedId] = useState<number | null>(null);
  const [requestErrors, setRequestErrors] = useState<Record<number, string>>({});
  const [confirmingDeletion, setConfirmingDeletion] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [paymentStatus, setPaymentStatus] = useState<PaymentStatus | null>(null);
  const [startingCheckout, setStartingCheckout] = useState(false);
  const [checkoutError, setCheckoutError] = useState<string | null>(null);
  const [pendingQuoteAction, setPendingQuoteAction] = useState<{ quoteId: number; action: "accept" | "reject" } | null>(null);
  const [quoteActionErrors, setQuoteActionErrors] = useState<Record<number, string>>({});
  const sendingRequestRef = useRef(false);
  const deletingRef = useRef(false);
  const pendingQuoteActionRef = useRef(false);

  async function handleCheckout() {
    if (!trip || startingCheckout) return;
    setStartingCheckout(true);
    setCheckoutError(null);

    try {
      sessionStorage.setItem("odyssey-payment-trip-id", String(trip.id));
      const session = await createTripCheckoutSession(trip.id);
      window.location.href = session.checkoutUrl;
    } catch {
      sessionStorage.removeItem("odyssey-payment-trip-id");
      setCheckoutError("Impossible de démarrer le paiement pour le moment. Veuillez réessayer.");
      setStartingCheckout(false);
    }
  }

  async function handleDeleteTrip() {
    if (deletingRef.current || !trip) return;
    deletingRef.current = true;
    setDeleting(true);
    setDeleteError(null);

    try {
      await deleteTrip(trip.id);
      navigate("/traveler/trips", { replace: true });
    } catch {
      setDeleteError("Ce voyage n’a pas pu être supprimé. Réessayez dans un instant.");
    } finally {
      deletingRef.current = false;
      setDeleting(false);
    }
  }

  async function handleSendRequest(need: TripNeed) {
    if (sendingRequestRef.current || need.bookingRequestStatus) return;
    sendingRequestRef.current = true;
    setSendingNeedId(need.id);
    setRequestErrors((current) => {
      const next = { ...current };
      delete next[need.id];
      return next;
    });

    try {
      await createBookingRequest(need.id, need.notes);
      setRequestVersion((version) => version + 1);
    } catch {
      setRequestErrors((current) => ({ ...current, [need.id]: "La demande n’a pas pu être envoyée. Réessayez dans un instant." }));
    } finally {
      sendingRequestRef.current = false;
      setSendingNeedId(null);
    }
  }

  async function handleQuoteAction(quote: TravelerQuote, action: "accept" | "reject") {
    if (pendingQuoteActionRef.current) return;
    pendingQuoteActionRef.current = true;
    setPendingQuoteAction({ quoteId: quote.id, action });
    setQuoteActionErrors((current) => {
      const next = { ...current };
      delete next[quote.id];
      return next;
    });

    try {
      if (action === "accept") {
        await acceptTravelerQuote(quote.id);
      } else {
        await rejectTravelerQuote(quote.id);
      }
      setRequestVersion((version) => version + 1);
    } catch {
      setQuoteActionErrors((current) => ({
        ...current,
        [quote.id]: "Cette proposition n’a pas pu être mise à jour. Elle a peut-être déjà été traitée.",
      }));
    } finally {
      pendingQuoteActionRef.current = false;
      setPendingQuoteAction(null);
    }
  }

  function scrollToFinancialSummary() {
    document.getElementById("trip-financial-summary")?.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  function scrollToNeed(needId: number) {
    document.getElementById(`traveler-need-${needId}`)?.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  function scrollToOrganizeSection() {
    document.getElementById("organize-need-section")?.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  function getNextActionCtaHandler(tripUxState: TravelerTripUxState): (() => void) | null {
    if (tripUxState.kind !== "NEXT_ACTION") return null;

    switch (tripUxState.action) {
      case "PROPOSALS_READY":
      case "SUPPLIER_PAYMENTS_REQUIRED":
      case "NEEDS_TO_ORGANIZE": {
        const targetNeedId = tripUxState.targetNeedId;
        return targetNeedId !== null ? () => scrollToNeed(targetNeedId) : null;
      }
      case "ODYSSEY_PAYMENT_REQUIRED":
        return scrollToFinancialSummary;
      case "NO_ACTIVE_NEEDS":
        return scrollToOrganizeSection;
      default:
        return null;
    }
  }

  useEffect(() => {
    if (hasInvalidTripId) return;

    const controller = new AbortController();

    async function loadTrip() {
      setLoading(true);
      setError(null);

      try {
        const [tripDetail, travelerQuotes] = await Promise.all([
          getTripDetail(parsedTripId, controller.signal),
          getTravelerQuotes(controller.signal),
        ]);
        const quotesByBookingRequestId = travelerQuotes.reduce<Map<number, TravelerQuote[]>>((quotesByRequest, quote) => {
          const requestQuotes = quotesByRequest.get(quote.bookingRequestId) ?? [];
          requestQuotes.push(quote);
          quotesByRequest.set(quote.bookingRequestId, requestQuotes);
          return quotesByRequest;
        }, new Map());
        const acceptedQuoteEntries = tripDetail.needs.map(
          (need) => [need.id, need.bookingRequestId ? getLatestAcceptedQuote(quotesByBookingRequestId.get(need.bookingRequestId)) : null] as const,
        );
        const latestQuoteEntries = tripDetail.needs.map(
          (need) => [need.id, need.bookingRequestId ? getLatestQuote(quotesByBookingRequestId.get(need.bookingRequestId)) : null] as const,
        );
        const tripBookingRequestIds = new Set(tripDetail.needs.flatMap((need) => (need.bookingRequestId === null ? [] : [need.bookingRequestId])));
        const tripPaymentStatus = travelerQuotes.find(
          (quote) => tripBookingRequestIds.has(quote.bookingRequestId) && quote.paymentStatus !== null,
        )?.paymentStatus;

        setTrip(tripDetail);
        setAcceptedQuotesByNeedId(Object.fromEntries(acceptedQuoteEntries));
        setLatestQuotesByNeedId(Object.fromEntries(latestQuoteEntries));
        setPaymentStatus(tripPaymentStatus ?? null);
      } catch (requestError: unknown) {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        setError("Ce voyage est introuvable ou momentanément indisponible.");
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void loadTrip();
    return () => controller.abort();
  }, [hasInvalidTripId, parsedTripId, requestVersion]);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const paymentResult = params.get("payment");
    if (paymentResult !== "success" && paymentResult !== "cancelled") return;

    window.history.replaceState({}, "", window.location.pathname);
    sessionStorage.removeItem("odyssey-payment-trip-id");
    if (paymentResult === "cancelled") return;

    let attempts = 0;
    const interval = window.setInterval(() => {
      attempts += 1;
      setRequestVersion((version) => version + 1);
      if (attempts >= 4) window.clearInterval(interval);
    }, 1500);

    return () => window.clearInterval(interval);
  }, []);

  if (hasInvalidTripId) {
    return (
      <div className="traveler-page">
        <Link className="back-link" to="/traveler/trips">
          <ArrowLeft size={17} /> Retour à mes voyages
        </Link>
        <div className="state-panel error-panel" role="alert">
          <strong>L’identifiant du voyage est invalide.</strong>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="state-panel" role="status">
        <span className="spinner" aria-hidden="true" />
        <strong>Chargement du voyage…</strong>
        <p>Nous récupérons les étapes de votre voyage.</p>
      </div>
    );
  }

  if (error || !trip) {
    return (
      <div className="traveler-page">
        <Link className="back-link" to="/traveler/trips">
          <ArrowLeft size={17} /> Retour à mes voyages
        </Link>
        <div className="state-panel error-panel" role="alert">
          <RefreshCw size={24} aria-hidden="true" />
          <strong>{error ?? "Voyage introuvable."}</strong>
          <button type="button" className="secondary-button" onClick={() => setRequestVersion((version) => version + 1)}>
            <RefreshCw size={16} /> Réessayer
          </button>
        </div>
      </div>
    );
  }

  const needStates: TravelerNeedStateEntry[] = trip.needs.map((need) => ({
    needId: need.id,
    state: deriveTravelerNeedState(need, latestQuotesByNeedId[need.id] ?? null, paymentStatus),
  }));
  const needStateById = new Map(needStates.map((entry) => [entry.needId, entry.state]));
  const progress = deriveTravelerTripProgress(needStates);
  const tripUxState = deriveTravelerTripUxState(needStates, paymentStatus, trip.assistanceFee);
  const nextActionCtaHandler = getNextActionCtaHandler(tripUxState);
  const finalizedServices = trip.needs
    .filter((need) => needStateById.get(need.id) === "BOOKING_CONFIRMED")
    .map((need) => ({
      needId: need.id,
      icon: needIcons[need.type],
      label: needLabels[need.type],
      providerConfirmationId: need.providerConfirmationId,
    }));
  const supplierServices: SupplierServiceAmount[] = trip.needs
    .filter((need) => needStateById.get(need.id) !== "CANCELLED")
    .flatMap((need) => {
      const acceptedQuote = acceptedQuotesByNeedId[need.id] ?? null;
      if (!acceptedQuote) return [];
      return [
        {
          needId: need.id,
          icon: needIcons[need.type],
          label: needLabels[need.type],
          providerPrice: acceptedQuote.providerPrice,
          currency: acceptedQuote.currency,
          providerPaymentStatus: acceptedQuote.providerPaymentStatus,
          providerPaymentUrl: acceptedQuote.providerPaymentUrl,
        },
      ];
    });
  // Mirrors the backend eligibility rule as-is (trip must be CONFIRMED before
  // the Odyssey fee can be paid) instead of re-deriving it independently.
  const tripEligibleForPayment = trip.status === "CONFIRMED";

  return (
    <div className="traveler-page">
      <Link className="back-link" to="/traveler/trips">
        <ArrowLeft size={17} /> Retour à mes voyages
      </Link>

      <section className="traveler-trip-heading">
        <span className="eyebrow">Votre voyage</span>
        <h1>{trip.title || `Voyage #${trip.id}`}</h1>
        <p>
          <CalendarDays size={17} /> {formatDate(trip.startDate)} <span>au</span> {formatDate(trip.endDate)}
        </p>
        {trip.travelEventId === null && (
          <span className="traveler-direct-trip-label">
            <Luggage size={15} /> Voyage organisé directement
          </span>
        )}
        {progress && (
          <div className="trip-progress" role="status">
            <div className="trip-progress-bar">
              <div className="trip-progress-bar-fill" style={{ width: `${(progress.finalized / progress.total) * 100}%` }} />
            </div>
            <span className="trip-progress-label">
              {progress.finalized} service{progress.total > 1 ? "s" : ""} sur {progress.total} finalisé{progress.total > 1 ? "s" : ""}
            </span>
          </div>
        )}
      </section>

      {tripUxState.kind === "TRIP_READY" ? (
        <TripReadySummary services={finalizedServices} />
      ) : (
        <TripNextAction action={tripUxState.action} count={tripUxState.count} onCtaClick={nextActionCtaHandler} />
      )}

      <section aria-labelledby="trip-needs-title">
        <div className="section-heading traveler-needs-heading">
          <div>
            <h2 id="trip-needs-title">Votre voyage</h2>
            <p>Les éléments organisés avec votre conseiller Odyssey</p>
          </div>
        </div>

        {trip.needs.length === 0 ? (
          <div className="state-panel traveler-empty-needs">
            <BedDouble size={27} />
            <strong>Aucun besoin ajouté</strong>
            <p>Les éléments de ce voyage apparaîtront ici.</p>
          </div>
        ) : (
          <div className="traveler-needs-grid">
            {trip.needs.map((need) => {
              const derivedState = needStateById.get(need.id) ?? "TO_ORGANIZE";
              const quote = latestQuotesByNeedId[need.id] ?? null;
              const acceptedQuote = acceptedQuotesByNeedId[need.id] ?? null;
              return (
                <TravelerNeedCard
                  key={need.id}
                  need={need}
                  icon={needIcons[need.type]}
                  label={needLabels[need.type]}
                  derivedState={derivedState}
                  quote={quote}
                  acceptedQuote={acceptedQuote}
                  onSendRequest={() => void handleSendRequest(need)}
                  sendingRequest={sendingNeedId === need.id}
                  sendRequestDisabled={sendingNeedId !== null}
                  sendRequestError={requestErrors[need.id]}
                  onAcceptQuote={(quoteToAccept) => void handleQuoteAction(quoteToAccept, "accept")}
                  onRejectQuote={(quoteToReject) => void handleQuoteAction(quoteToReject, "reject")}
                  quoteActionPending={quote && pendingQuoteAction?.quoteId === quote.id ? pendingQuoteAction.action : null}
                  quoteActionsDisabled={pendingQuoteAction !== null}
                  quoteActionError={quote ? quoteActionErrors[quote.id] : undefined}
                  onScrollToPayment={scrollToFinancialSummary}
                />
              );
            })}
          </div>
        )}
      </section>

      <TravelerTripFinances
        assistanceFee={trip.assistanceFee}
        paymentStatus={paymentStatus}
        tripEligibleForPayment={tripEligibleForPayment}
        checkoutLoading={startingCheckout}
        checkoutError={checkoutError}
        onCheckout={() => void handleCheckout()}
        supplierServices={supplierServices}
      />

      <section id="organize-need-section" aria-labelledby="organize-trip-title">
        <div className="section-heading traveler-needs-heading">
          <div>
            <h2 id="organize-trip-title">Organiser mon voyage</h2>
            <p>Ajoutez les éléments pour lesquels vous souhaitez être accompagné par Odyssey.</p>
          </div>
        </div>

        <div className="traveler-organization-grid">
          {organizationChoices.map((choice) =>
            choice.available ? (
              <button
                type="button"
                className={`traveler-organization-choice${selectedNeedType === choice.type ? " selected" : ""}`}
                key={choice.type}
                onClick={() => setSelectedNeedType(choice.type as OrganizableNeedType)}
              >
                <span>{needIcons[choice.type]}</span>
                <strong>{choice.label}</strong>
              </button>
            ) : (
              <div className="traveler-organization-choice unavailable" key={choice.type} aria-disabled="true">
                <span>{needIcons[choice.type]}</span>
                <strong>{choice.label}</strong>
                <small>À venir</small>
              </div>
            ),
          )}
        </div>

        {selectedNeedType && (
          <TravelerNeedForm
            key={selectedNeedType}
            tripId={trip.id}
            type={selectedNeedType}
            onCancel={() => setSelectedNeedType(null)}
            onCreated={() => {
              setSelectedNeedType(null);
              setRequestVersion((version) => version + 1);
            }}
          />
        )}
      </section>

      <section className="traveler-trip-danger-zone" aria-labelledby="delete-trip-title">
        <div>
          <h2 id="delete-trip-title">Supprimer ce voyage</h2>
          <p>Le voyage et son organisation seront définitivement supprimés.</p>
        </div>

        {!confirmingDeletion ? (
          <button
            type="button"
            className="traveler-delete-button"
            onClick={() => {
              setConfirmingDeletion(true);
              setDeleteError(null);
            }}
          >
            <Trash2 size={17} /> Supprimer le voyage
          </button>
        ) : (
          <div className="traveler-delete-confirmation">
            <p>
              <strong>Confirmer la suppression de « {trip.title || `Voyage #${trip.id}`} » ?</strong> Cette action est irréversible.
            </p>
            {deleteError && (
              <p className="traveler-delete-error" role="alert">
                {deleteError}
              </p>
            )}
            <div>
              <button type="button" className="traveler-delete-button confirmed" disabled={deleting} onClick={() => void handleDeleteTrip()}>
                {deleting ? <LoaderCircle className="rotating" size={17} /> : <Trash2 size={17} />}
                {deleting ? "Suppression…" : "Oui, supprimer"}
              </button>
              <button
                type="button"
                className="traveler-cancel-button"
                disabled={deleting}
                onClick={() => {
                  setConfirmingDeletion(false);
                  setDeleteError(null);
                }}
              >
                Annuler
              </button>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}

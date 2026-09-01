import { useEffect, useState, type ReactNode } from "react";
import { ArrowLeft, BedDouble, Bus, CalendarDays, Car, Check, Clock3, Hotel, Plane, RefreshCw, Route } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { getTripDetail } from "../../api/trips";
import type { TripDetail, TripNeed, TripNeedType } from "../../types/trip";

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

interface TravelerNeedState {
  label: string;
  tone: "confirmed" | "pending" | "progress" | "requested" | "idle";
}

function getTravelerNeedState(need: TripNeed): TravelerNeedState {
  if (need.bookingStatus === "CONFIRMED") return { label: "Réservation confirmée", tone: "confirmed" };
  if (need.bookingStatus === "PENDING") return { label: "Réservation en attente de confirmation", tone: "pending" };
  if (need.bookingRequestStatus === "IN_PROGRESS") return { label: "Demande prise en charge", tone: "progress" };
  if (need.bookingRequestStatus === "REQUESTED") return { label: "Demande envoyée", tone: "requested" };
  return { label: "À organiser", tone: "idle" };
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("fr-FR", {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(new Date(`${value}T00:00:00`));
}

export function TravelerTripDetailPage() {
  const { tripId } = useParams<{ tripId: string }>();
  const parsedTripId = Number(tripId);
  const hasInvalidTripId = !Number.isInteger(parsedTripId) || parsedTripId <= 0;
  const [trip, setTrip] = useState<TripDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [requestVersion, setRequestVersion] = useState(0);

  useEffect(() => {
    if (hasInvalidTripId) return;

    const controller = new AbortController();

    async function loadTrip() {
      setLoading(true);
      setError(null);

      try {
        setTrip(await getTripDetail(parsedTripId, controller.signal));
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
      </section>

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
              const state = getTravelerNeedState(need);
              return (
                <article className="traveler-need-card" key={need.id}>
                  <div className="traveler-need-title">
                    <span>{needIcons[need.type]}</span>
                    <h3>{needLabels[need.type]}</h3>
                  </div>
                  <div className={`traveler-need-state ${state.tone}`}>
                    {state.tone === "confirmed" && <Check size={17} />}
                    {state.tone === "pending" && <Clock3 size={17} />}
                    <strong>{state.label}</strong>
                  </div>
                  {need.notes && <p className="traveler-need-notes">{need.notes}</p>}
                  {need.bookingStatus === "CONFIRMED" && need.providerConfirmationId && (
                    <div className="traveler-booking-reference">
                      <span>Référence de réservation</span>
                      <strong>{need.providerConfirmationId}</strong>
                    </div>
                  )}
                </article>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}

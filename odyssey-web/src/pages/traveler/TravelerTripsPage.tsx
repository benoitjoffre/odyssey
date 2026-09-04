import { useEffect, useState } from "react";
import { ArrowRight, CalendarDays, Inbox, Luggage, Plus, RefreshCw } from "lucide-react";
import { Link } from "react-router-dom";
import { getTravelerTrips } from "../../api/trips";
import type { Trip, TripStatus } from "../../types/trip";

const TRAVELER_ID = 1;

const tripStatusLabels: Record<TripStatus, string> = {
  DRAFT: "En préparation",
  CONFIRMED: "Confirmé",
  CANCELLED: "Annulé",
};

function formatDate(value: string) {
  return new Intl.DateTimeFormat("fr-FR", {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(new Date(`${value}T00:00:00`));
}

export function TravelerTripsPage() {
  const [trips, setTrips] = useState<Trip[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [requestVersion, setRequestVersion] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function loadTrips() {
      setLoading(true);
      setError(null);

      try {
        setTrips(await getTravelerTrips(TRAVELER_ID, controller.signal));
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

  return (
    <div className="traveler-page">
      <section className="traveler-page-heading traveler-trips-heading">
        <div>
          <span className="eyebrow">Votre carnet de voyage</span>
          <h1>Mes voyages</h1>
          <p>Suivez la préparation et les réservations de chacun de vos voyages.</p>
        </div>
        <Link className="primary-button" to="/traveler/trips/new"><Plus size={18} /> Nouveau voyage</Link>
      </section>

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
          <Link className="primary-button" to="/traveler/trips/new"><Plus size={17} /> Organiser mon voyage</Link>
        </div>
      )}

      {!loading && !error && trips.length > 0 && (
        <section className="traveler-trips-grid" aria-label="Vos voyages">
          {trips.map((trip) => (
            <article className="traveler-trip-card" key={trip.id}>
              <div className="traveler-trip-icon">
                <Luggage size={22} />
              </div>
              <div className="traveler-trip-copy">
                <span className={`trip-status status-${trip.status.toLowerCase()}`}>{tripStatusLabels[trip.status]}</span>
                <h2>{trip.title || `Voyage #${trip.id}`}</h2>
                <p>
                  <CalendarDays size={16} /> {formatDate(trip.startDate)} <span>au</span> {formatDate(trip.endDate)}
                </p>
              </div>
              <Link className="traveler-trip-link" to={`/traveler/trips/${trip.id}`}>
                Voir le voyage <ArrowRight size={17} />
              </Link>
            </article>
          ))}
        </section>
      )}
    </div>
  );
}

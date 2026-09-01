import { useEffect, useState } from "react";
import { ArrowRight, CalendarDays, Compass, Inbox, MapPin, RefreshCw } from "lucide-react";
import { Link } from "react-router-dom";
import { getTravelEvents } from "../../api/travelEvents";
import type { TravelEvent } from "../../types/travelEvent";

function formatDateRange(startDate: string, endDate: string) {
  const formatter = new Intl.DateTimeFormat("fr-FR", { day: "numeric", month: "long", year: "numeric" });
  return `${formatter.format(new Date(`${startDate}T00:00:00`))} au ${formatter.format(new Date(`${endDate}T00:00:00`))}`;
}

export function TravelerDiscoverPage() {
  const [events, setEvents] = useState<TravelEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [requestVersion, setRequestVersion] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function loadEvents() {
      setLoading(true);
      setError(null);

      try {
        setEvents(await getTravelEvents(controller.signal));
      } catch (requestError: unknown) {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        setError("Impossible de charger les événements pour le moment.");
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void loadEvents();
    return () => controller.abort();
  }, [requestVersion]);

  return (
    <div className="traveler-page">
      <section className="traveler-page-heading">
        <span className="eyebrow">L’inspiration avant l’itinéraire</span>
        <h1>Découvrir</h1>
        <p>Trouvez l’événement qui donnera naissance à votre prochain voyage.</p>
      </section>

      {loading && (
        <div className="state-panel" role="status">
          <span className="spinner" aria-hidden="true" />
          <strong>Chargement des événements…</strong>
          <p>Nous cherchons de nouvelles raisons de partir.</p>
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

      {!loading && !error && events.length === 0 && (
        <div className="state-panel">
          <Inbox size={28} aria-hidden="true" />
          <strong>Aucun événement à découvrir</strong>
          <p>De nouvelles expériences seront bientôt proposées.</p>
        </div>
      )}

      {!loading && !error && events.length > 0 && (
        <section className="traveler-events-grid" aria-label="Événements à découvrir">
          {events.map((event) => (
            <article className="traveler-event-card" key={event.id}>
              <div className="traveler-event-mark">
                <Compass size={23} />
              </div>
              <div className="traveler-event-main">
                <h2>{event.name}</h2>
                <div className="traveler-event-meta">
                  <span>
                    <MapPin size={16} /> {event.location}
                  </span>
                  <span>
                    <CalendarDays size={16} /> {formatDateRange(event.startDate, event.endDate)}
                  </span>
                </div>
                {event.description && <p>{event.description}</p>}
              </div>
              <Link className="traveler-event-link" to={`/traveler/events/${event.id}`}>
                Découvrir <ArrowRight size={17} />
              </Link>
            </article>
          ))}
        </section>
      )}
    </div>
  );
}

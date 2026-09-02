import { useEffect, useState } from "react";
import { CalendarDays, Inbox, MapPin, Plus, RefreshCw, Sparkles } from "lucide-react";
import { Link } from "react-router-dom";
import { getExperiences } from "../../api/experiences";
import { getTravelEvents } from "../../api/travelEvents";
import type { TravelEvent } from "../../types/travelEvent";

function formatDate(value: string) {
  return new Intl.DateTimeFormat("fr-FR", { dateStyle: "medium" }).format(new Date(`${value}T00:00:00`));
}

export function AgentEventsPage() {
  const [events, setEvents] = useState<TravelEvent[]>([]);
  const [experienceNames, setExperienceNames] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [requestVersion, setRequestVersion] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    async function loadEvents() {
      setLoading(true);
      setError(null);
      try {
        const [loadedEvents, experiences] = await Promise.all([getTravelEvents(controller.signal), getExperiences(controller.signal)]);
        setEvents(loadedEvents);
        setExperienceNames(Object.fromEntries(experiences.map((experience) => [experience.id, experience.title])));
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
    <div className="page-stack">
      <section className="page-heading agent-admin-heading">
        <div>
          <span className="eyebrow">Programmation Odyssey</span>
          <h1>Événements</h1>
          <p>Planifiez les occurrences concrètes auxquelles les voyageurs pourront participer.</p>
        </div>
        <Link className="primary-button" to="/agent/events/new">
          <Plus size={18} /> Nouvel événement
        </Link>
      </section>
      {loading && (
        <div className="state-panel" role="status">
          <span className="spinner" />
          <strong>Chargement des événements…</strong>
        </div>
      )}
      {!loading && error && (
        <div className="state-panel error-panel" role="alert">
          <RefreshCw size={24} />
          <strong>{error}</strong>
          <button className="secondary-button" type="button" onClick={() => setRequestVersion((value) => value + 1)}>
            <RefreshCw size={16} /> Réessayer
          </button>
        </div>
      )}
      {!loading && !error && events.length === 0 && (
        <div className="state-panel">
          <Inbox size={28} />
          <strong>Aucun événement</strong>
          <p>Programmez le premier événement associé à une expérience.</p>
        </div>
      )}
      {!loading && !error && events.length > 0 && (
        <section className="agent-event-list" aria-label="Événements programmés">
          {events.map((travelEvent) => (
            <article className="agent-event-row" key={travelEvent.id}>
              <div className="agent-event-date">
                <CalendarDays size={19} />
                <strong>{formatDate(travelEvent.startDate)}</strong>
                <span>au {formatDate(travelEvent.endDate)}</span>
              </div>
              <div className="agent-event-copy">
                <span>
                  <Sparkles size={14} /> {experienceNames[travelEvent.experienceId] ?? `Expérience #${travelEvent.experienceId}`}
                </span>
                <h2>{travelEvent.name}</h2>
                <p>{travelEvent.description || "Aucune description"}</p>
              </div>
              <div className="agent-event-location">
                <MapPin size={17} />
                <span>{travelEvent.location}</span>
              </div>
            </article>
          ))}
        </section>
      )}
    </div>
  );
}

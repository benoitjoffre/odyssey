import { useEffect, useRef, useState, type FormEvent, type ReactNode } from "react";
import { ArrowRight, CalendarDays, Clock3, Compass, Inbox, LoaderCircle, MapPin, RefreshCw, Sparkles } from "lucide-react";
import { Link } from "react-router-dom";
import { createIntent, getIntentRecommendations } from "../../api/intents";
import { getTravelEvents } from "../../api/travelEvents";
import { experienceCategoryLabels, experienceCategoryPrompts } from "../../helpers/experienceCategories";
import type { ExperienceCategory, ScoredExperienceResponse } from "../../types/intent";
import type { TravelEvent } from "../../types/travelEvent";

const TRAVELER_ID = 1;

const categoryChoices: Array<{ category: ExperienceCategory; icon: ReactNode }> = [
  { category: "DANCE", icon: "💃" },
  { category: "SURF", icon: "🏄" },
  { category: "NATURE", icon: "🌿" },
  { category: "ADVENTURE", icon: "🧗" },
  { category: "FOOD", icon: "🍴" },
  { category: "BEACH", icon: "🏖️" },
  { category: "CULTURE", icon: "🎨" },
  { category: "ROAD_TRIP", icon: "🚗" },
];

function formatDateRange(startDate: string, endDate: string) {
  const formatter = new Intl.DateTimeFormat("fr-FR", { day: "numeric", month: "long", year: "numeric" });
  return `${formatter.format(new Date(`${startDate}T00:00:00`))} au ${formatter.format(new Date(`${endDate}T00:00:00`))}`;
}

export function TravelerDiscoverPage() {
  const [inspiration, setInspiration] = useState("");
  const [intentLoading, setIntentLoading] = useState(false);
  const [recommendationLoading, setRecommendationLoading] = useState(false);
  const [recommendations, setRecommendations] = useState<ScoredExperienceResponse[] | null>(null);
  const [recommendationError, setRecommendationError] = useState<string | null>(null);
  const [selectedExperienceId, setSelectedExperienceId] = useState<number | null>(null);
  const [events, setEvents] = useState<TravelEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [requestVersion, setRequestVersion] = useState(0);
  const submittingRef = useRef(false);
  const eventsSectionRef = useRef<HTMLElement>(null);

  const visibleEvents = selectedExperienceId === null ? events : events.filter((event) => event.experienceId === selectedExperienceId);

  async function handleInspirationSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const description = inspiration.trim();
    if (!description || submittingRef.current) return;

    submittingRef.current = true;
    setIntentLoading(true);
    setRecommendationLoading(false);
    setRecommendationError(null);
    setRecommendations(null);
    setSelectedExperienceId(null);

    try {
      const intent = await createIntent({ title: "Envie de voyage", description, travelerId: TRAVELER_ID });
      setIntentLoading(false);
      setRecommendationLoading(true);
      setRecommendations(await getIntentRecommendations(intent.id));
    } catch {
      setRecommendationError("Impossible de trouver des recommandations pour le moment.");
    } finally {
      submittingRef.current = false;
      setIntentLoading(false);
      setRecommendationLoading(false);
    }
  }

  function showExperienceEvents(experienceId: number) {
    setSelectedExperienceId(experienceId);
    requestAnimationFrame(() => eventsSectionRef.current?.scrollIntoView({ behavior: "smooth", block: "start" }));
  }

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

      <section className="traveler-inspiration-panel" aria-labelledby="inspiration-title">
        <div className="traveler-inspiration-heading">
          <span>
            <Sparkles size={22} />
          </span>
          <div>
            <h2 id="inspiration-title">Qu’avez-vous envie de vivre ?</h2>
            <p>Décrivez librement l’expérience qui vous ferait partir.</p>
          </div>
        </div>
        <form className="traveler-inspiration-form" onSubmit={(event) => void handleInspirationSubmit(event)}>
          <label htmlFor="traveler-inspiration" className="sr-only">
            Votre envie de voyage
          </label>
          <textarea
            id="traveler-inspiration"
            rows={3}
            value={inspiration}
            onChange={(event) => {
              setInspiration(event.target.value);
              setRecommendationError(null);
            }}
            placeholder="Ex. Je veux apprendre la salsa à Cuba"
            disabled={intentLoading || recommendationLoading}
          />
          <button type="submit" className="primary-button" disabled={!inspiration.trim() || intentLoading || recommendationLoading}>
            {intentLoading || recommendationLoading ? <LoaderCircle className="rotating" size={18} /> : <Sparkles size={18} />}
            {intentLoading ? "Votre envie prend forme…" : recommendationLoading ? "Recherche d’expériences…" : "M’inspirer"}
          </button>
        </form>
        {recommendationError && (
          <p className="traveler-inspiration-error" role="alert">
            {recommendationError}
          </p>
        )}

        <div className="traveler-category-discovery">
          <h3>Ou explorez par envie</h3>
          <div className="traveler-category-grid">
            {categoryChoices.map((choice) => (
              <button
                type="button"
                key={choice.category}
                className={inspiration === experienceCategoryPrompts[choice.category] ? "selected" : ""}
                onClick={() => {
                  setInspiration(experienceCategoryPrompts[choice.category]);
                  setRecommendationError(null);
                }}
                disabled={intentLoading || recommendationLoading}
              >
                <span aria-hidden="true">{choice.icon}</span>
                <strong>{experienceCategoryLabels[choice.category]}</strong>
              </button>
            ))}
          </div>
        </div>
      </section>

      {(intentLoading || recommendationLoading || recommendations !== null || recommendationError) && (
        <section className="traveler-recommendations" aria-labelledby="recommendations-title">
          <div className="section-heading traveler-needs-heading">
            <div>
              <h2 id="recommendations-title">Expériences recommandées</h2>
              <p>Des expériences choisies à partir de votre envie.</p>
            </div>
          </div>
          {(intentLoading || recommendationLoading) && (
            <div className="state-panel traveler-recommendation-state" role="status">
              <span className="spinner" aria-hidden="true" />
              <strong>{intentLoading ? "Nous écoutons votre envie…" : "Nous cherchons les expériences qui vous correspondent…"}</strong>
            </div>
          )}
          {!intentLoading && !recommendationLoading && recommendations?.length === 0 && (
            <div className="state-panel traveler-recommendation-state">
              <Inbox size={26} />
              <strong>Aucune expérience ne correspond encore à cette envie.</strong>
            </div>
          )}
          {!intentLoading && !recommendationLoading && recommendations && recommendations.length > 0 && (
            <div className="traveler-recommendation-grid">
              {recommendations.map((recommendation) => (
                <article className="traveler-recommendation-card" key={recommendation.id}>
                  <div className="traveler-recommendation-topline">
                    <span>{experienceCategoryLabels[recommendation.category]}</span>
                    <strong>Correspondance : {recommendation.score} %</strong>
                  </div>
                  <h3>{recommendation.title}</h3>
                  <p className="traveler-recommendation-destination">
                    <MapPin size={16} /> {recommendation.destination}
                  </p>
                  <p>{recommendation.description}</p>
                  <div className="traveler-recommendation-footer">
                    <span>
                      <Clock3 size={16} /> {recommendation.durationDays} jours
                    </span>
                    <button type="button" className="traveler-event-link" onClick={() => showExperienceEvents(recommendation.id)}>
                      Découvrir cette expérience <ArrowRight size={17} />
                    </button>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      )}

      <section className="traveler-events-section" aria-labelledby="events-title" ref={eventsSectionRef}>
        <div className="section-heading traveler-needs-heading">
          <div>
            <h2 id="events-title">{selectedExperienceId === null ? "Événements à découvrir" : "Événements de cette expérience"}</h2>
            <p>Choisissez une date avant de préparer votre voyage.</p>
          </div>
          {selectedExperienceId !== null && (
            <button type="button" className="traveler-clear-filter" onClick={() => setSelectedExperienceId(null)}>
              Voir tous les événements
            </button>
          )}
        </div>

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

        {!loading && !error && visibleEvents.length === 0 && (
          <div className="state-panel">
            <Inbox size={28} aria-hidden="true" />
            <strong>{selectedExperienceId === null ? "Aucun événement à découvrir" : "Aucun événement programmé pour cette expérience"}</strong>
            <p>
              {selectedExperienceId === null
                ? "De nouvelles expériences seront bientôt proposées."
                : "Vous pouvez explorer les autres événements disponibles."}
            </p>
          </div>
        )}

        {!loading && !error && visibleEvents.length > 0 && (
          <section className="traveler-events-grid" aria-label="Événements à découvrir">
            {visibleEvents.map((event) => (
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
      </section>
    </div>
  );
}

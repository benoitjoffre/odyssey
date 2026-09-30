import { useEffect, useRef, useState, type FormEvent, type ReactNode } from "react";
import {
  ArrowRight,
  Inbox,
  Landmark,
  Leaf,
  MapPin,
  Mountain,
  Music4,
  RefreshCw,
  Search,
  Sparkles,
  SunMedium,
  Users,
  UtensilsCrossed,
} from "lucide-react";
import { Link } from "react-router-dom";
import { getExperiences } from "../../api/experiences";
import { createIntent, getIntentRecommendations } from "../../api/intents";
import { getTravelEvents } from "../../api/travelEvents";
import heroDiscoverBackground from "../../assets/header-discover.png";
import organizeDiscoverBackground from "../../assets/organize-discover.png";
import travelerEventFallbackImage from "../../assets/tortues.png";
import { ExperienceCard } from "../../components/experience/ExperienceCard";
import { experienceCategoryPrompts } from "../../helpers/experienceCategories";
import type { Experience } from "../../types/experience";
import type { ExperienceCategory, ScoredExperienceResponse } from "../../types/intent";
import type { TravelEvent } from "../../types/travelEvent";

const categoryChoices: Array<{ category: ExperienceCategory; icon: ReactNode; title: string; subtitle: string; accent: string }> = [
  { category: "CULTURE", icon: <Landmark size={25} />, title: "Culture", subtitle: "& patrimoine", accent: "culture" },
  { category: "FOOD", icon: <UtensilsCrossed size={25} />, title: "Gastronomie", subtitle: "& saveurs", accent: "food" },
  { category: "NATURE", icon: <Leaf size={25} />, title: "Nature", subtitle: "& aventure", accent: "nature" },
  { category: "DANCE", icon: <Music4 size={25} />, title: "Musique", subtitle: "& traditions", accent: "music" },
  { category: "BEACH", icon: <SunMedium size={25} />, title: "Détente", subtitle: "& bien-être", accent: "relax" },
  { category: "ADVENTURE", icon: <Mountain size={25} />, title: "Aventure", subtitle: "& sensations", accent: "adventure" },
  { category: "ROAD_TRIP", icon: <Users size={25} />, title: "Rencontres", subtitle: "& local", accent: "social" },
];

function formatEventDatePill(startDate: string, endDate: string) {
  const start = new Date(`${startDate}T00:00:00`);
  const end = new Date(`${endDate}T00:00:00`);
  const monthFormatter = new Intl.DateTimeFormat("fr-FR", { month: "short" });
  const startMonth = monthFormatter.format(start).replace(".", "").toUpperCase();
  const endMonth = monthFormatter.format(end).replace(".", "").toUpperCase();

  if (startMonth === endMonth && start.getFullYear() === end.getFullYear()) {
    return `${start.getDate()} - ${end.getDate()} ${endMonth}`;
  }

  return `${start.getDate()} ${startMonth} - ${end.getDate()} ${endMonth}`;
}

function getTravelEventImage(event: TravelEvent) {
  const imageUrl = (event as TravelEvent & { imageUrl?: string | null }).imageUrl;
  if (typeof imageUrl === "string" && imageUrl.trim().length > 0) {
    return imageUrl.trim();
  }
  return travelerEventFallbackImage;
}

export function TravelerDiscoverPage() {
  const [inspiration, setInspiration] = useState("");
  const [intentLoading, setIntentLoading] = useState(false);
  const [recommendationLoading, setRecommendationLoading] = useState(false);
  const [recommendations, setRecommendations] = useState<ScoredExperienceResponse[] | null>(null);
  const [recommendationError, setRecommendationError] = useState<string | null>(null);
  const [selectedExperienceId, setSelectedExperienceId] = useState<number | null>(null);
  const [catalogExperiences, setCatalogExperiences] = useState<Experience[]>([]);
  const [catalogLoading, setCatalogLoading] = useState(true);
  const [events, setEvents] = useState<TravelEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [requestVersion, setRequestVersion] = useState(0);
  const submittingRef = useRef(false);
  const eventsSectionRef = useRef<HTMLElement>(null);

  const visibleEvents = selectedExperienceId === null ? events : events.filter((event) => event.experienceId === selectedExperienceId);
  const now = new Date();
  const upcomingEvents = visibleEvents
    .filter((event) => new Date(`${event.endDate}T23:59:59`).getTime() >= now.getTime())
    .sort((first, second) => new Date(`${first.startDate}T00:00:00`).getTime() - new Date(`${second.startDate}T00:00:00`).getTime())
    .slice(0, 3);
  const displayExperiences = recommendations ?? catalogExperiences;

  async function runInspirationSearch(description: string) {
    const normalized = description.trim();
    if (!normalized || submittingRef.current) return;

    submittingRef.current = true;
    setIntentLoading(true);
    setRecommendationLoading(false);
    setRecommendationError(null);
    setRecommendations(null);
    setSelectedExperienceId(null);

    try {
      const intent = await createIntent({ title: "Envie de voyage", description: normalized });
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

  async function handleInspirationSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await runInspirationSearch(inspiration);
  }

  useEffect(() => {
    const controller = new AbortController();

    async function loadCatalog() {
      setCatalogLoading(true);

      try {
        setCatalogExperiences(await getExperiences(controller.signal));
      } catch (requestError: unknown) {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        setCatalogExperiences([]);
      } finally {
        if (!controller.signal.aborted) setCatalogLoading(false);
      }
    }

    void loadCatalog();
    return () => controller.abort();
  }, []);

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
      <section
        className="traveler-discover-hero"
        style={{
          backgroundImage: `url(${heroDiscoverBackground})`,
        }}
        aria-label="Découvrir des expériences"
      >
        <div className="traveler-discover-hero__inner">
          <div className="traveler-discover-hero__content">
            <span className="traveler-discover-hero__eyebrow">DÉCOUVRIR DES EXPÉRIENCES</span>
            <h1 className="traveler-discover-hero__title">
              <span>Qu’avez-vous envie</span>
              <span>
                de <span className="traveler-discover-hero__title-accent">vivre ?</span>
              </span>
            </h1>
            <p className="traveler-discover-hero__description">
              Décrivez une envie, une ambiance ou quelque chose que vous aimeriez découvrir. Nous vous proposons des expériences adaptées.
            </p>

            <form className="traveler-discover-hero__search" onSubmit={(event) => void handleInspirationSubmit(event)}>
              <label htmlFor="traveler-discover-hero-input" className="sr-only">
                Votre envie de voyage
              </label>
              <span className="traveler-discover-hero__search-icon" aria-hidden="true">
                <Search size={18} strokeWidth={2.2} />
              </span>
              <input
                id="traveler-discover-hero-input"
                type="text"
                value={inspiration}
                onChange={(event) => {
                  setInspiration(event.target.value);
                  setRecommendationError(null);
                }}
                placeholder="Ex. Je veux vivre une aventure en pleine nature..."
                aria-label="Votre envie de voyage"
                disabled={intentLoading || recommendationLoading}
              />
              <button
                type="submit"
                className="traveler-discover-hero__button"
                disabled={!inspiration.trim() || intentLoading || recommendationLoading}
              >
                <Sparkles size={16} />
                <span>M’inspirer</span>
              </button>
            </form>
          </div>
        </div>
      </section>

      <section className="traveler-inspiration-panel" aria-labelledby="inspiration-title">
        <div className="traveler-category-discovery">
          <h3>Explorer par envie</h3>
          <div className="traveler-category-grid">
            {categoryChoices.map((choice) => (
              <button
                type="button"
                key={choice.category}
                className={`traveler-category-tile ${choice.accent} ${inspiration === experienceCategoryPrompts[choice.category] ? "selected" : ""}`}
                onClick={() => {
                  const selectedPrompt = experienceCategoryPrompts[choice.category];
                  setInspiration(selectedPrompt);
                  setRecommendationError(null);
                  void runInspirationSearch(selectedPrompt);
                }}
                disabled={intentLoading || recommendationLoading}
                aria-pressed={inspiration === experienceCategoryPrompts[choice.category]}
              >
                <span className="traveler-category-tile__icon" aria-hidden="true">
                  {choice.icon}
                </span>
                <span className="traveler-category-tile__label">
                  <span>{choice.title}</span>
                  <span>{choice.subtitle}</span>
                </span>
              </button>
            ))}
          </div>
        </div>
      </section>

      <section className="traveler-discover-recommendations" aria-labelledby="recommendations-title">
        <div className="traveler-discover-recommendations__header">
          <div>
            <h2 id="recommendations-title">Expériences pour vous</h2>
            <p>Des idées d’expériences adaptées à vos envies et à vos voyages.</p>
          </div>
          <Link className="public-inline-link traveler-discover-recommendations__link" to="/traveler/discover">
            Voir toutes les expériences <ArrowRight size={17} />
          </Link>
        </div>

        {(intentLoading || recommendationLoading || catalogLoading) && (
          <div className="state-panel traveler-recommendation-state" role="status">
            <span className="spinner" aria-hidden="true" />
            <strong>
              {intentLoading || recommendationLoading ? "Nous cherchons les expériences qui vous correspondent…" : "Chargement des expériences…"}
            </strong>
          </div>
        )}

        {!catalogLoading && !intentLoading && !recommendationLoading && recommendationError && (
          <div className="state-panel traveler-recommendation-state" role="alert">
            <Inbox size={26} />
            <strong>Impossible de trouver des recommandations pour le moment.</strong>
            <p>Essayez une autre envie ou réessayez plus tard.</p>
          </div>
        )}

        {!catalogLoading && !intentLoading && !recommendationLoading && displayExperiences.length === 0 && (
          <div className="state-panel traveler-recommendation-state">
            <Inbox size={26} />
            <strong>Aucune expérience trouvée pour le moment.</strong>
            <p>Essayez une autre envie ou explorez une autre catégorie.</p>
          </div>
        )}

        {!catalogLoading && !intentLoading && !recommendationLoading && displayExperiences.length > 0 && (
          <div className="public-card-grid traveler-discover-recommendations__grid">
            {displayExperiences.map((experience) => (
              <ExperienceCard
                key={experience.id}
                experience={{
                  id: experience.id,
                  title: experience.title,
                  description: experience.description,
                  destination: "destination" in experience ? (experience.destination ?? undefined) : undefined,
                  category: experience.category,
                  durationDays: experience.durationDays,
                  imageUrl: "imageUrl" in experience ? (experience.imageUrl ?? undefined) : undefined,
                }}
              />
            ))}
          </div>
        )}
      </section>

      <section
        className="traveler-discover-organize"
        style={{
          backgroundImage: `url(${organizeDiscoverBackground})`,
        }}
        aria-labelledby="travel-organize-title"
      >
        <div className="traveler-discover-organize__inner">
          <div className="traveler-discover-organize__content">
            <span className="traveler-discover-organize__eyebrow">VOUS SAVEZ DÉJÀ OÙ VOUS ALLEZ ?</span>
            <h2 id="travel-organize-title" className="traveler-discover-organize__title">
              Organisez votre voyage
            </h2>
            <p className="traveler-discover-organize__description">
              Créez votre voyage et ajoutez les services dont vous avez besoin (hébergement, transport, activités...).
            </p>
          </div>
          <Link className="traveler-discover-organize__cta" to="/traveler/trips/new">
            <span>Créer mon voyage</span>
            <ArrowRight size={17} />
          </Link>
        </div>
      </section>

      <section className="traveler-events-section" aria-labelledby="events-title" ref={eventsSectionRef}>
        <div className="traveler-events-section__header">
          <div>
            <h2 id="events-title">Événements à venir</h2>
            <p>Des rendez-vous qui peuvent inspirer votre prochain voyage.</p>
          </div>
        </div>

        {loading && (
          <div className="state-panel" role="status">
            <span className="spinner" aria-hidden="true" />
            <strong>Chargement des événements à venir…</strong>
            <p>Nous préparons de nouvelles idées pour votre prochain voyage.</p>
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

        {!loading && !error && upcomingEvents.length === 0 && (
          <div className="state-panel">
            <Inbox size={28} aria-hidden="true" />
            <strong>Aucun événement à venir pour le moment.</strong>
            <p>De nouveaux rendez-vous seront bientôt proposés.</p>
          </div>
        )}

        {!loading && !error && upcomingEvents.length > 0 && (
          <section className="traveler-events-preview-grid" aria-label="Événements à venir">
            {upcomingEvents.map((event) => (
              <article className="traveler-events-preview-card" key={event.id}>
                <div className="traveler-events-preview-card__media">
                  <img src={getTravelEventImage(event)} alt={event.name} loading="lazy" />
                  <span className="traveler-events-preview-card__date">{formatEventDatePill(event.startDate, event.endDate)}</span>
                </div>
                <div className="traveler-events-preview-card__body">
                  <h3>{event.name}</h3>
                  <p className="traveler-events-preview-card__location">
                    <MapPin size={15} />
                    <span>{event.location}</span>
                  </p>
                  <p className="traveler-events-preview-card__description">
                    {event.description ?? "Un rendez-vous à ne pas manquer pour enrichir votre prochain voyage."}
                  </p>
                  <Link className="traveler-events-preview-card__cta" to={`/traveler/events/${event.id}`}>
                    <span>Découvrir</span>
                    <span className="traveler-events-preview-card__cta-arrow" aria-hidden="true">
                      <ArrowRight size={16} />
                    </span>
                  </Link>
                </div>
              </article>
            ))}
          </section>
        )}
      </section>
    </div>
  );
}

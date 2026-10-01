import { ArrowLeft, ArrowRight, CalendarDays, Clock3, MapPin, Sparkles } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getExperience } from "../api/experiences";
import { getTravelEvents } from "../api/travelEvents";
import { TravelerHeader } from "../components/TravelerHeader";
import { experienceCategoryLabels } from "../helpers/experienceCategories";
import { getExperienceImageUrl } from "../helpers/experienceImage";
import type { Experience } from "../types/experience";
import type { TravelEvent } from "../types/travelEvent";

export function ExperienceDetailPage() {
  const { id } = useParams();
  const parsedId = id ? Number(id) : NaN;
  const isInvalidId = !id || !Number.isInteger(parsedId) || parsedId <= 0;
  const [experience, setExperience] = useState<Experience | null>(null);
  const [events, setEvents] = useState<TravelEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isInvalidId) {
      return;
    }

    const controller = new AbortController();

    async function loadExperience() {
      setLoading(true);
      setError(null);

      try {
        const [loadedExperience, loadedEvents] = await Promise.all([getExperience(parsedId, controller.signal), getTravelEvents(controller.signal)]);

        setExperience(loadedExperience);
        setEvents(loadedEvents.filter((event) => event.experienceId === loadedExperience.id));
      } catch {
        setError("Impossible de charger cette expérience pour le moment.");
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    void loadExperience();
    return () => controller.abort();
  }, [id, isInvalidId, parsedId]);

  if (isInvalidId) {
    return (
      <div className="experience-detail-shell">
        <div className="experience-detail__state" role="alert">
          <p>Cette expérience est introuvable.</p>
          <Link className="public-inline-link" to="/">
            Retour à l’accueil
          </Link>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="experience-detail-shell">
        <div className="experience-detail__loading">Chargement de l’expérience…</div>
      </div>
    );
  }

  if (!experience) {
    return (
      <div className="experience-detail-shell">
        <div className="experience-detail__state" role="alert">
          <p>{error ?? "Cette expérience est introuvable."}</p>
          <Link className="public-inline-link" to="/">
            Retour à l’accueil
          </Link>
        </div>
      </div>
    );
  }

  const city = experience.destination?.city?.trim() ?? "";
  const country = experience.destination?.country?.trim() ?? "";
  const destinationLabel = [city, country].filter(Boolean).join(", ") || "Destination à confirmer";
  const hasValidDuration = Number.isFinite(experience.durationDays) && experience.durationDays > 0;
  const summaryDurationLabel = hasValidDuration ? `${experience.durationDays} ${experience.durationDays === 1 ? "jour" : "jours"}` : null;
  const categoryLabel = experienceCategoryLabels[experience.category];
  const normalizedDescription = experience.description?.trim() ?? "";
  const aboutParagraphs = normalizedDescription
    .split(/\r?\n+/)
    .map((paragraph) => paragraph.trim())
    .filter(Boolean);
  const hasAboutContent = aboutParagraphs.length > 0;
  const hasSummaryCategory = Boolean(categoryLabel);
  const hasSummaryDestination = destinationLabel !== "Destination à confirmer";
  const relevantEvents = events.filter((event) => event.experienceId === experience.id);

  const categoryClassName = `experience-hero__pill--${String(experience.category).toLowerCase()}`;

  function formatTravelEventDateLabel(value: string) {
    return new Intl.DateTimeFormat("fr-FR", { day: "numeric", month: "long", year: "numeric" }).format(new Date(`${value}T00:00:00`));
  }

  function formatTravelEventSchedule(event: TravelEvent) {
    const start = new Date(`${event.startDate}T00:00:00`);
    const end = new Date(`${event.endDate}T00:00:00`);
    const sameMonth = start.getMonth() === end.getMonth() && start.getFullYear() === end.getFullYear();

    if (sameMonth) {
      return `${start.getDate()} - ${end.getDate()} ${new Intl.DateTimeFormat("fr-FR", { month: "long", year: "numeric" }).format(end)}`;
    }

    return `${formatTravelEventDateLabel(event.startDate)} → ${formatTravelEventDateLabel(event.endDate)}`;
  }

  return (
    <div className="experience-detail-page traveler-shell">
      <TravelerHeader />

      <header className="experience-hero">
        <img className="experience-hero__image" src={getExperienceImageUrl(experience.imageUrl)} alt="" />
        <div className="experience-hero__overlay" />

        <Link className="experience-hero__back" to="/traveler/discover">
          <ArrowLeft size={17} /> Retour aux expériences
        </Link>

        <div className="experience-hero__content">
          {hasSummaryCategory && (
            <span className={`experience-hero__pill ${categoryClassName}`}>
              <Sparkles size={13} />
              <span>{categoryLabel.toUpperCase()}</span>
            </span>
          )}

          <h1>{experience.title}</h1>

          {normalizedDescription ? <p className="experience-hero__description">{experience.description}</p> : null}

          {(hasSummaryDestination || summaryDurationLabel) && (
            <div className="experience-hero__meta">
              {hasSummaryDestination && (
                <span>
                  <MapPin size={15} /> {destinationLabel}
                </span>
              )}
              {summaryDurationLabel && (
                <span>
                  <CalendarDays size={15} /> {summaryDurationLabel}
                </span>
              )}
            </div>
          )}
        </div>
      </header>

      <main className="experience-detail__content-grid">
        <div className="experience-detail__main-column">
          {hasAboutContent && (
            <section className="experience-about" aria-label="À propos de cette expérience">
              <h2>À propos de cette expérience</h2>
              <div className="experience-about__description">
                {aboutParagraphs.map((paragraph, index) => (
                  <p key={`${index}-${paragraph.slice(0, 20)}`}>{paragraph}</p>
                ))}
              </div>
            </section>
          )}

          <section className="experience-detail__events" aria-label="Quand vivre cette expérience ?">
            <h2>Quand vivre cette expérience ?</h2>
            {relevantEvents.length > 0 ? (
              <ul className="experience-detail__event-list">
                {relevantEvents.map((event) => {
                  const eventSchedule = formatTravelEventSchedule(event);

                  return (
                    <li key={event.id} className="travel-event-row">
                      <div className="travel-event-row__date">
                        <span className="travel-event-row__date-top">
                          {new Intl.DateTimeFormat("fr-FR", { day: "numeric" }).format(new Date(`${event.startDate}T00:00:00`))}
                          {event.startDate !== event.endDate
                            ? ` - ${new Intl.DateTimeFormat("fr-FR", { day: "numeric" }).format(new Date(`${event.endDate}T00:00:00`))}`
                            : ""}
                        </span>
                        <span className="travel-event-row__date-bottom">
                          {new Intl.DateTimeFormat("fr-FR", { month: "long" }).format(new Date(`${event.startDate}T00:00:00`))}
                        </span>
                      </div>

                      <div className="travel-event-row__details">
                        <div className="travel-event-row__title">{event.name}</div>
                        <div className="travel-event-row__meta">
                          <span>
                            <MapPin size={14} /> {event.location}
                          </span>
                          <span>
                            <CalendarDays size={14} /> {eventSchedule}
                          </span>
                        </div>
                      </div>

                      <Link className="travel-event-row__cta" to={`/traveler/events/${event.id}`}>
                        Choisir cette date
                        <ArrowRight size={15} />
                      </Link>
                    </li>
                  );
                })}
              </ul>
            ) : (
              <div className="experience-detail__event-placeholder">
                <p>Les événements associés à cette expérience seront affichés ici lors de la prochaine étape.</p>
              </div>
            )}
          </section>
        </div>

        {(hasSummaryCategory || hasSummaryDestination || summaryDurationLabel) && (
          <aside className="experience-summary" aria-label="Récapitulatif expérience">
            {hasSummaryCategory && (
              <div className="experience-summary__row">
                <span className="experience-summary__icon" aria-hidden="true">
                  <Sparkles size={20} />
                </span>
                <div className="experience-summary__text">
                  <p className="experience-summary__label">Catégorie</p>
                  <p className="experience-summary__value">{categoryLabel}</p>
                </div>
              </div>
            )}

            {hasSummaryDestination && (
              <div className="experience-summary__row">
                <span className="experience-summary__icon" aria-hidden="true">
                  <MapPin size={20} />
                </span>
                <div className="experience-summary__text">
                  <p className="experience-summary__label">Lieu</p>
                  <p className="experience-summary__value">{destinationLabel}</p>
                </div>
              </div>
            )}

            {summaryDurationLabel && (
              <div className="experience-summary__row">
                <span className="experience-summary__icon" aria-hidden="true">
                  <Clock3 size={20} />
                </span>
                <div className="experience-summary__text">
                  <p className="experience-summary__label">Durée</p>
                  <p className="experience-summary__value">{summaryDurationLabel}</p>
                </div>
              </div>
            )}
          </aside>
        )}
      </main>
    </div>
  );
}

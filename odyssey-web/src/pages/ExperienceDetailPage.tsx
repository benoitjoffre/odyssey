import { ArrowLeft, CalendarDays, MapPin, Sparkles } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getExperience } from "../api/experiences";
import { getTravelEvents } from "../api/travelEvents";
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

  const destinationLabel = experience.destination ? `${experience.destination.city}, ${experience.destination.country}` : "Destination à confirmer";
  const relevantEvents = events.filter((event) => event.experienceId === experience.id);

  return (
    <div className="experience-detail-shell">
      <div className="experience-detail__topbar">
        <Link className="public-inline-link" to="/">
          <ArrowLeft size={17} /> Retour à l’accueil
        </Link>
      </div>

      <article className="experience-detail">
        <div className="experience-detail__media" style={{ backgroundImage: `url("${getExperienceImageUrl(experience.imageUrl)}")` }} />

        <div className="experience-detail__content">
          <div className="experience-detail__eyebrow">
            <Sparkles size={16} />
            <span>{experienceCategoryLabels[experience.category]}</span>
          </div>

          <h1>{experience.title}</h1>
          <p className="experience-detail__description">{experience.description}</p>

          <div className="experience-detail__meta">
            <span>
              <MapPin size={16} /> {destinationLabel}
            </span>
            <span>
              <CalendarDays size={16} /> {experience.durationDays} jours
            </span>
          </div>

          <section className="experience-detail__events">
            <h2>Quand vivre cette expérience ?</h2>
            {relevantEvents.length > 0 ? (
              <ul className="experience-detail__event-list">
                {relevantEvents.map((event) => (
                  <li key={event.id}>
                    <strong>{event.name}</strong>
                    <span>{event.location}</span>
                    <small>
                      {new Intl.DateTimeFormat("fr-FR", { day: "numeric", month: "long", year: "numeric" }).format(
                        new Date(`${event.startDate}T00:00:00`),
                      )}
                      {event.startDate !== event.endDate
                        ? ` - ${new Intl.DateTimeFormat("fr-FR", { day: "numeric", month: "long", year: "numeric" }).format(new Date(`${event.endDate}T00:00:00`))}`
                        : ""}
                    </small>
                  </li>
                ))}
              </ul>
            ) : (
              <div className="experience-detail__event-placeholder">
                <p>Les événements associés à cette expérience seront affichés ici lors de la prochaine étape.</p>
              </div>
            )}
          </section>
        </div>
      </article>
    </div>
  );
}

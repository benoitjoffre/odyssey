import { useEffect, useRef, useState, type FormEvent } from "react";
import { ArrowLeft, CalendarPlus, LoaderCircle } from "lucide-react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { getExperiences } from "../../api/experiences";
import { createTravelEvent } from "../../api/travelEvents";
import type { Experience } from "../../types/experience";

export function AgentEventCreatePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const initialExperienceId = (location.state as { experienceId?: number } | null)?.experienceId;
  const [experiences, setExperiences] = useState<Experience[]>([]);
  const [loadingExperiences, setLoadingExperiences] = useState(true);
  const [name, setName] = useState("");
  const [experienceId, setExperienceId] = useState(initialExperienceId ? String(initialExperienceId) : "");
  const [eventLocation, setEventLocation] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const submittingRef = useRef(false);

  useEffect(() => {
    const controller = new AbortController();
    getExperiences(controller.signal)
      .then(setExperiences)
      .catch((requestError: unknown) => {
        if (!(requestError instanceof DOMException && requestError.name === "AbortError")) setError("Impossible de charger les expériences.");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoadingExperiences(false);
      });
    return () => controller.abort();
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submittingRef.current) return;
    if (!name.trim() || !eventLocation.trim() || !startDate || !endDate || !experienceId) {
      setError("Le nom, l’expérience, le lieu et les dates sont obligatoires.");
      return;
    }
    if (startDate > endDate) {
      setError("La date de début doit précéder ou être égale à la date de fin.");
      return;
    }

    submittingRef.current = true;
    setSubmitting(true);
    setError(null);
    try {
      await createTravelEvent({
        name: name.trim(),
        location: eventLocation.trim(),
        startDate,
        endDate,
        description: description.trim() || null,
        experienceId: Number(experienceId),
      });
      navigate("/agent/events");
    } catch {
      setError("L’événement n’a pas pu être créé. Vérifiez les informations puis réessayez.");
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  }

  return (
    <div className="page-stack">
      <Link className="back-link" to="/agent/events">
        <ArrowLeft size={17} /> Retour aux événements
      </Link>
      <section className="page-heading">
        <div>
          <span className="eyebrow">Programmation Odyssey</span>
          <h1>Nouvel événement</h1>
          <p>Associez une date et un lieu concrets à une expérience du catalogue.</p>
        </div>
      </section>
      <section className="agent-admin-form-card" aria-labelledby="event-form-title">
        <div className="detail-card-heading">
          <CalendarPlus size={20} />
          <h2 id="event-form-title">Informations de l’événement</h2>
        </div>
        <form className="agent-admin-form" onSubmit={(event) => void handleSubmit(event)} noValidate>
          <label className="form-field">
            <span>Nom *</span>
            <input
              value={name}
              onChange={(event) => {
                setName(event.target.value);
                setError(null);
              }}
              disabled={submitting}
            />
          </label>
          <label className="form-field">
            <span>Expérience *</span>
            <select
              value={experienceId}
              onChange={(event) => {
                setExperienceId(event.target.value);
                setError(null);
              }}
              disabled={submitting || loadingExperiences}
            >
              <option value="">{loadingExperiences ? "Chargement…" : "Sélectionner une expérience"}</option>
              {experiences.map((experience) => (
                <option value={experience.id} key={experience.id}>
                  {experience.title}
                </option>
              ))}
            </select>
          </label>
          <label className="form-field form-field-wide">
            <span>Lieu *</span>
            <input
              value={eventLocation}
              onChange={(event) => {
                setEventLocation(event.target.value);
                setError(null);
              }}
              disabled={submitting}
            />
          </label>
          <label className="form-field">
            <span>Date de début *</span>
            <input
              type="date"
              value={startDate}
              onChange={(event) => {
                setStartDate(event.target.value);
                setError(null);
              }}
              disabled={submitting}
            />
          </label>
          <label className="form-field">
            <span>Date de fin *</span>
            <input
              type="date"
              value={endDate}
              onChange={(event) => {
                setEndDate(event.target.value);
                setError(null);
              }}
              disabled={submitting}
            />
          </label>
          <label className="form-field form-field-wide">
            <span>Description</span>
            <textarea rows={4} value={description} onChange={(event) => setDescription(event.target.value)} disabled={submitting} />
          </label>
          {error && (
            <p className="agent-admin-form-error" role="alert">
              {error}
            </p>
          )}
          <button type="submit" className="primary-button agent-admin-submit" disabled={submitting || loadingExperiences}>
            {submitting ? <LoaderCircle className="rotating" size={18} /> : <CalendarPlus size={18} />}
            {submitting ? "Création…" : "Créer l’événement"}
          </button>
        </form>
      </section>
    </div>
  );
}

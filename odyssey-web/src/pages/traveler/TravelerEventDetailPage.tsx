import { useEffect, useState, type FormEvent } from "react";
import { ArrowLeft, CalendarDays, LoaderCircle, MapPin, RefreshCw, Sparkles } from "lucide-react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ApiError } from "../../api/client";
import { getTravelEvent } from "../../api/travelEvents";
import { createTrip } from "../../api/trips";
import type { TravelEvent } from "../../types/travelEvent";

const TRAVELER_ID = 1;

function formatDate(value: string) {
  return new Intl.DateTimeFormat("fr-FR", { day: "numeric", month: "long", year: "numeric" }).format(new Date(`${value}T00:00:00`));
}

function formatEventRange(startDate: string, endDate: string) {
  const start = new Date(`${startDate}T00:00:00`);
  const end = new Date(`${endDate}T00:00:00`);
  const sameMonth = start.getMonth() === end.getMonth() && start.getFullYear() === end.getFullYear();

  if (sameMonth) {
    const monthYear = new Intl.DateTimeFormat("fr-FR", { month: "long", year: "numeric" }).format(end);
    return `${start.getDate()} → ${end.getDate()} ${monthYear}`;
  }

  return `${formatDate(startDate)} → ${formatDate(endDate)}`;
}

export function TravelerEventDetailPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const parsedEventId = Number(eventId);
  const hasInvalidEventId = !Number.isInteger(parsedEventId) || parsedEventId <= 0;
  const navigate = useNavigate();
  const [event, setEvent] = useState<TravelEvent | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [requestVersion, setRequestVersion] = useState(0);
  const [preparing, setPreparing] = useState(false);
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [validationError, setValidationError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (hasInvalidEventId) return;
    const controller = new AbortController();

    async function loadEvent() {
      setLoading(true);
      setLoadError(null);
      try {
        const loadedEvent = await getTravelEvent(parsedEventId, controller.signal);
        setEvent(loadedEvent);
        setStartDate(loadedEvent.startDate);
        setEndDate(loadedEvent.endDate);
      } catch (requestError: unknown) {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        setLoadError(
          requestError instanceof ApiError && requestError.status === 404
            ? "Cet événement n’existe pas."
            : "Impossible de charger cet événement pour le moment.",
        );
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void loadEvent();
    return () => controller.abort();
  }, [hasInvalidEventId, parsedEventId, requestVersion]);

  function validateDates(currentEvent: TravelEvent) {
    if (!startDate || !endDate) return "Renseignez les dates d’arrivée et de départ.";
    if (startDate > endDate) return "La date d’arrivée doit précéder la date de départ.";
    if (startDate > currentEvent.startDate) return `Votre séjour doit commencer au plus tard le ${formatDate(currentEvent.startDate)}.`;
    if (endDate < currentEvent.endDate) return `Votre séjour doit se terminer au plus tôt le ${formatDate(currentEvent.endDate)}.`;
    return null;
  }

  async function handleSubmit(submitEvent: FormEvent<HTMLFormElement>) {
    submitEvent.preventDefault();
    if (!event || submitting) return;

    const dateError = validateDates(event);
    setValidationError(dateError);
    setSubmitError(null);
    if (dateError) return;

    setSubmitting(true);
    try {
      const trip = await createTrip({ title: event.name, startDate, endDate, travelerId: TRAVELER_ID, travelEventId: event.id });
      navigate(`/traveler/trips/${trip.id}`);
    } catch {
      setSubmitError("Votre voyage n’a pas pu être créé. Vérifiez les dates puis réessayez.");
    } finally {
      setSubmitting(false);
    }
  }

  if (hasInvalidEventId) {
    return (
      <div className="traveler-page">
        <Link className="back-link" to="/traveler/discover">
          <ArrowLeft size={17} /> Retour à la découverte
        </Link>
        <div className="state-panel error-panel" role="alert">
          <strong>L’identifiant de l’événement est invalide.</strong>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="state-panel" role="status">
        <span className="spinner" aria-hidden="true" />
        <strong>Chargement de l’événement…</strong>
        <p>Nous préparons cette découverte.</p>
      </div>
    );
  }

  if (loadError || !event) {
    return (
      <div className="traveler-page">
        <Link className="back-link" to="/traveler/discover">
          <ArrowLeft size={17} /> Retour à la découverte
        </Link>
        <div className="state-panel error-panel" role="alert">
          <RefreshCw size={24} />
          <strong>{loadError ?? "Événement introuvable."}</strong>
          <button type="button" className="secondary-button" onClick={() => setRequestVersion((version) => version + 1)}>
            <RefreshCw size={16} /> Réessayer
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="traveler-page">
      <Link className="back-link" to="/traveler/discover">
        <ArrowLeft size={17} /> Retour à la découverte
      </Link>

      {!preparing ? (
        <article className="traveler-event-detail">
          <span className="eyebrow">L’événement au cœur du voyage</span>
          <h1>{event.name}</h1>
          <div className="traveler-event-detail-meta">
            <span>
              <MapPin size={18} /> {event.location}
            </span>
            <span>
              <CalendarDays size={18} /> {formatEventRange(event.startDate, event.endDate)}
            </span>
          </div>
          {event.description && <p>{event.description}</p>}
          <button type="button" className="primary-button traveler-event-cta" onClick={() => setPreparing(true)}>
            <Sparkles size={18} /> Je veux y aller
          </button>
        </article>
      ) : (
        <section className="traveler-stay-preparation" aria-labelledby="stay-preparation-title">
          <span className="eyebrow">Autour de l’événement</span>
          <h1 id="stay-preparation-title">Préparons votre séjour</h1>
          <div className="traveler-preparation-event">
            <h2>{event.name}</h2>
            <span>
              <MapPin size={16} /> {event.location}
            </span>
            <span>
              <CalendarDays size={16} /> Événement : {formatEventRange(event.startDate, event.endDate)}
            </span>
          </div>
          <form className="traveler-stay-form" onSubmit={(formEvent) => void handleSubmit(formEvent)} noValidate>
            <label className="form-field">
              <span>Date d’arrivée</span>
              <input
                type="date"
                value={startDate}
                onChange={(changeEvent) => {
                  setStartDate(changeEvent.target.value);
                  setValidationError(null);
                }}
                disabled={submitting}
                required
              />
            </label>
            <label className="form-field">
              <span>Date de départ</span>
              <input
                type="date"
                value={endDate}
                onChange={(changeEvent) => {
                  setEndDate(changeEvent.target.value);
                  setValidationError(null);
                }}
                disabled={submitting}
                required
              />
            </label>
            {validationError && (
              <p className="traveler-form-error" role="alert">
                {validationError}
              </p>
            )}
            {submitError && (
              <p className="traveler-form-error" role="alert">
                {submitError}
              </p>
            )}
            <button type="submit" className="primary-button traveler-create-trip" disabled={submitting}>
              {submitting ? <LoaderCircle className="rotating" size={18} /> : <Sparkles size={18} />}
              {submitting ? "Création du voyage…" : "Créer mon voyage"}
            </button>
          </form>
        </section>
      )}
    </div>
  );
}

import { useEffect, useState, type FormEvent } from "react";
import { ArrowLeft, ArrowRight, CalendarDays, Leaf, MapPin, RefreshCw, Sparkles, UsersRound } from "lucide-react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ApiError } from "../../api/client";
import { getTravelEvent } from "../../api/travelEvents";
import { createTrip } from "../../api/trips";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { FormField } from "../../components/ui/FormField";
import { ErrorState, LoadingState } from "../../components/ui/State";
import { formatDateCompact, formatDateRange, getTravelEventImage } from "../../helpers/travelerTripPresentation";
import type { TravelEvent } from "../../types/travelEvent";

function parseEventDate(value: string) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return null;

  const timestamp = Date.parse(`${value}T00:00:00Z`);
  return Number.isFinite(timestamp) && new Date(timestamp).toISOString().slice(0, 10) === value ? timestamp : null;
}

function getEventDurationLabel(startDate: string, endDate: string) {
  const startTime = parseEventDate(startDate);
  const endTime = parseEventDate(endDate);

  if (startTime === null || endTime === null || endTime < startTime) return null;

  const days = Math.round((endTime - startTime) / 86400000) + 1;
  return `${days} jour${days > 1 ? "s" : ""}`;
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
  const eventImage = event ? getTravelEventImage(event) : null;
  const eventDuration = event ? getEventDurationLabel(event.startDate, event.endDate) : null;

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
    if (startDate > currentEvent.startDate) return `Votre séjour doit commencer au plus tard le ${formatDateCompact(currentEvent.startDate)}.`;
    if (endDate < currentEvent.endDate) return `Votre séjour doit se terminer au plus tôt le ${formatDateCompact(currentEvent.endDate)}.`;
    return null;
  }

  function handleStartPreparing() {
    setPreparing(true);
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
      const trip = await createTrip({ title: event.name, startDate, endDate, travelEventId: event.id });
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
        <ErrorState title="L’identifiant de l’événement est invalide." />
      </div>
    );
  }

  if (loading) {
    return <LoadingState title="Chargement de l’événement…" description="Nous préparons cette découverte." />;
  }

  if (loadError || !event) {
    return (
      <div className="traveler-page">
        <Link className="back-link" to="/traveler/discover">
          <ArrowLeft size={17} /> Retour à la découverte
        </Link>
        <ErrorState title={loadError ?? "Événement introuvable."} icon={<RefreshCw size={24} aria-hidden="true" />}>
          <Button variant="outline" onClick={() => setRequestVersion((version) => version + 1)}>
            <RefreshCw size={16} aria-hidden="true" /> Réessayer
          </Button>
        </ErrorState>
      </div>
    );
  }

  return (
    <div className="traveler-page traveler-event-page">
      <Link className="back-link traveler-event-back" to="/traveler/discover">
        <ArrowLeft size={17} /> Retour à la découverte
      </Link>

      {!preparing ? (
        <>
          <article aria-labelledby="traveler-event-title">
            <Card padding="none" className="traveler-event-hero">
              <div className="traveler-event-hero__media">
                <img src={eventImage ?? undefined} alt="" />
              </div>
              <div className="traveler-event-hero__content">
                <span className="eyebrow traveler-event-hero__eyebrow">L’événement au cœur du voyage</span>
                <h1 id="traveler-event-title">{event.name}</h1>
                <div className="traveler-event-detail-meta">
                  <span>
                    <MapPin size={17} aria-hidden="true" /> {event.location}
                  </span>
                  <span>
                    <CalendarDays size={17} aria-hidden="true" /> {formatDateRange(event.startDate, event.endDate)}
                  </span>
                </div>
                {event.description && <p className="traveler-event-hero__description">{event.description}</p>}
                <Button size="lg" className="traveler-event-cta" onClick={handleStartPreparing}>
                  Je veux y aller
                </Button>
                {eventDuration && (
                  <div className="traveler-event-tags" aria-label="Durée de l’événement">
                    <Badge className="traveler-event-duration">{eventDuration}</Badge>
                  </div>
                )}
              </div>
            </Card>
          </article>

          <Card padding="none" className="traveler-event-highlights" role="region" aria-label="Votre expérience avec Odyssey">
            <article className="traveler-event-highlight">
              <span className="traveler-event-highlight__icon" aria-hidden="true">
                <CalendarDays size={20} />
              </span>
              <div>
                <h2>Un temps fort du voyage</h2>
                <p>Découvrez un événement sélectionné pour enrichir votre séjour.</p>
              </div>
            </article>
            <article className="traveler-event-highlight">
              <span className="traveler-event-highlight__icon" aria-hidden="true">
                <UsersRound size={20} />
              </span>
              <div>
                <h2>Une expérience à vivre</h2>
                <p>Ajoutez cet événement à votre projet de voyage.</p>
              </div>
            </article>
            <article className="traveler-event-highlight">
              <span className="traveler-event-highlight__icon" aria-hidden="true">
                <Leaf size={20} />
              </span>
              <div>
                <h2>Odyssey vous accompagne</h2>
                <p>Organisez ensuite les autres étapes de votre séjour.</p>
              </div>
            </article>
          </Card>

          <section className="traveler-event-editorial" aria-label="À propos de l’événement">
            <div className="traveler-event-editorial__copy">
              <h2>À propos de cet événement</h2>
            </div>
            <Card padding="md" className="traveler-event-follow-up" role="complementary" aria-label="Votre projet de voyage">
              <div>
                <span className="eyebrow">Votre projet de voyage</span>
                <h2>Envie de vivre cet événement&nbsp;?</h2>
                <p>
                  Ajoutez-le à votre projet de voyage. Votre conseiller pourra ensuite vous accompagner pour organiser les autres étapes de votre
                  séjour.
                </p>
              </div>
              <Button variant="outline" className="traveler-event-follow-up__cta" onClick={handleStartPreparing}>
                Je veux y aller 
              </Button>
            </Card>
          </section>
        </>
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
              <CalendarDays size={16} /> Événement : {formatDateRange(event.startDate, event.endDate)}
            </span>
          </div>
          <form className="traveler-stay-form" onSubmit={(formEvent) => void handleSubmit(formEvent)} noValidate>
            <FormField label="Date d’arrivée" required>
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
            </FormField>
            <FormField label="Date de départ" required>
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
            </FormField>
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
            <Button type="submit" className="traveler-create-trip" loading={submitting} disabled={submitting}>
              <Sparkles size={18} aria-hidden="true" /> Créer mon voyage
            </Button>
          </form>
        </section>
      )}
    </div>
  );
}

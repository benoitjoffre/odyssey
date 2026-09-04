import { useRef, useState, type FormEvent } from "react";
import { ArrowLeft, CalendarDays, LoaderCircle, Luggage, Sparkles } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { createTrip } from "../../api/trips";

const TRAVELER_ID = 1;

export function TravelerTripCreatePage() {
  const navigate = useNavigate();
  const [title, setTitle] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const submittingRef = useRef(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submittingRef.current) return;

    if (!title.trim() || !startDate || !endDate) {
      setError("Le titre et les dates du voyage sont obligatoires.");
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
      const trip = await createTrip({
        title: title.trim(),
        startDate,
        endDate,
        travelerId: TRAVELER_ID,
      });
      navigate(`/traveler/trips/${trip.id}`);
    } catch {
      setError("Votre voyage n’a pas pu être créé. Vérifiez les informations puis réessayez.");
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  }

  return (
    <div className="traveler-page">
      <Link className="back-link" to="/traveler/discover">
        <ArrowLeft size={17} /> Retour aux choix de voyage
      </Link>

      <section className="traveler-direct-trip-intro">
        <span className="traveler-direct-trip-icon"><Luggage size={24} /></span>
        <div>
          <span className="eyebrow">Vous savez déjà où partir</span>
          <h1>Organiser un voyage</h1>
          <p>Créez votre voyage avec vos dates, puis ajoutez les services dont vous avez besoin.</p>
        </div>
      </section>

      <section className="traveler-direct-trip-form-card" aria-labelledby="direct-trip-form-title">
        <div className="traveler-direct-trip-form-heading">
          <CalendarDays size={21} />
          <div>
            <h2 id="direct-trip-form-title">Votre voyage</h2>
            <p>Vous pourrez organiser le vol et l’hébergement dès l’étape suivante.</p>
          </div>
        </div>

        <form className="traveler-direct-trip-form" onSubmit={(event) => void handleSubmit(event)} noValidate>
          <label className="form-field form-field-wide">
            <span>Titre du voyage *</span>
            <input
              value={title}
              onChange={(event) => {
                setTitle(event.target.value);
                setError(null);
              }}
              placeholder="Ex. Une semaine à Lisbonne"
              disabled={submitting}
              required
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
              required
            />
          </label>
          <label className="form-field">
            <span>Date de fin *</span>
            <input
              type="date"
              value={endDate}
              min={startDate || undefined}
              onChange={(event) => {
                setEndDate(event.target.value);
                setError(null);
              }}
              disabled={submitting}
              required
            />
          </label>
          {error && <p className="traveler-form-error" role="alert">{error}</p>}
          <button type="submit" className="primary-button traveler-create-trip" disabled={submitting}>
            {submitting ? <LoaderCircle className="rotating" size={18} /> : <Sparkles size={18} />}
            {submitting ? "Création du voyage…" : "Créer mon voyage"}
          </button>
        </form>
      </section>
    </div>
  );
}
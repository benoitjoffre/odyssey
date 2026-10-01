import { useRef, useState, type FormEvent } from "react";
import { ArrowLeft, CalendarDays, LoaderCircle, Luggage } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { createTrip } from "../../api/trips";
import backgroundCreate from "../../assets/background-create.png";

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
    <div className="traveler-page trip-create-page">
      <div className="trip-create-card">
        <Link className="trip-create-back" to="/traveler/discover">
          <ArrowLeft size={17} /> Retour aux choix de voyage
        </Link>

        <div className="trip-create-page__container">
          <section className="trip-create-form-panel" aria-labelledby="trip-create-form-title">
            <div className="trip-create-intro">
              <div className="trip-create-intro__icon" aria-hidden="true">
                <Luggage size={24} />
              </div>
              <div className="trip-create-intro__content">
                <div className="trip-create-intro__eyebrow">ORGANISER VOTRE VOYAGE</div>
                <h1 id="trip-create-form-title">
                  Où partez-vous <span className="trip-create-intro__title-tail">prochainement&nbsp;?</span>
                </h1>
                <p>Donnez un nom à votre voyage et indiquez vos dates. Odyssey vous accompagnera ensuite pour organiser chaque étape.</p>
              </div>
            </div>

            <form className="trip-create-form" onSubmit={(event) => void handleSubmit(event)} noValidate>
              <label className="trip-create-form__field trip-create-form__field--full">
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

              <div className="trip-create-form__dates">
                <label className="trip-create-form__field">
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

                <label className="trip-create-form__field">
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
              </div>

              {error && (
                <p className="traveler-form-error" role="alert">
                  {error}
                </p>
              )}

              <button type="submit" className="primary-button trip-create-form__submit" disabled={submitting}>
                {submitting ? <LoaderCircle className="rotating" size={18} /> : null}
                {submitting ? "Création du voyage…" : "Créer mon voyage"}
              </button>

              <div className="trip-create-info-panel" aria-label="Et après ?">
                <span className="trip-create-info-panel__icon" aria-hidden="true">
                  <CalendarDays size={18} />
                </span>
                <div className="trip-create-info-panel__content">
                  <strong>Et après ?</strong>
                  <p>
                    Vous pourrez ensuite ajouter votre vol, votre hébergement, vos transferts, une voiture de location et d&apos;autres services selon
                    vos besoins.
                  </p>
                </div>
              </div>
            </form>
          </section>

          <div
            className="trip-create-visual"
            aria-hidden="true"
            style={{
              backgroundImage: `url(${backgroundCreate})`,
            }}
          />
        </div>
      </div>
    </div>
  );
}

import { useRef, useState, type FormEvent } from "react";
import { ArrowLeft, LoaderCircle, Sparkles } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { createExperience } from "../../api/experiences";
import { experienceCategoryLabels } from "../../helpers/experienceCategories";
import type { ExperienceCategory } from "../../types/intent";

const categories = Object.entries(experienceCategoryLabels) as Array<[ExperienceCategory, string]>;

export function AgentExperienceCreatePage() {
  const navigate = useNavigate();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [destination, setDestination] = useState("");
  const [category, setCategory] = useState<ExperienceCategory | "">("");
  const [durationDays, setDurationDays] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const submittingRef = useRef(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submittingRef.current) return;
    const parsedDuration = Number(durationDays);
    if (!title.trim() || !description.trim() || !destination.trim() || !category || !durationDays) {
      setError("Tous les champs sont obligatoires.");
      return;
    }
    if (!Number.isFinite(parsedDuration) || parsedDuration <= 0) {
      setError("La durée doit être un nombre positif.");
      return;
    }

    submittingRef.current = true;
    setSubmitting(true);
    setError(null);
    try {
      await createExperience({
        title: title.trim(),
        description: description.trim(),
        destination: destination.trim(),
        category,
        durationDays: parsedDuration,
      });
      navigate("/agent/experiences");
    } catch {
      setError("L’expérience n’a pas pu être créée. Vérifiez les informations puis réessayez.");
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  }

  return (
    <div className="page-stack">
      <Link className="back-link" to="/agent/experiences">
        <ArrowLeft size={17} /> Retour aux expériences
      </Link>
      <section className="page-heading">
        <div>
          <span className="eyebrow">Catalogue Odyssey</span>
          <h1>Nouvelle expérience</h1>
          <p>Décrivez ce que le voyageur pourra vivre.</p>
        </div>
      </section>
      <section className="agent-admin-form-card" aria-labelledby="experience-form-title">
        <div className="detail-card-heading">
          <Sparkles size={20} />
          <h2 id="experience-form-title">Informations de l’expérience</h2>
        </div>
        <form className="agent-admin-form" onSubmit={(event) => void handleSubmit(event)} noValidate>
          <label className="form-field">
            <span>Titre *</span>
            <input
              value={title}
              onChange={(event) => {
                setTitle(event.target.value);
                setError(null);
              }}
              disabled={submitting}
            />
          </label>
          <label className="form-field">
            <span>Destination *</span>
            <input
              value={destination}
              onChange={(event) => {
                setDestination(event.target.value);
                setError(null);
              }}
              disabled={submitting}
            />
          </label>
          <label className="form-field">
            <span>Catégorie *</span>
            <select
              value={category}
              onChange={(event) => {
                setCategory(event.target.value as ExperienceCategory | "");
                setError(null);
              }}
              disabled={submitting}
            >
              <option value="">Sélectionner une catégorie</option>
              {categories.map(([value, label]) => (
                <option value={value} key={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
          <label className="form-field">
            <span>Durée en jours *</span>
            <input
              type="number"
              min="1"
              step="1"
              value={durationDays}
              onChange={(event) => {
                setDurationDays(event.target.value);
                setError(null);
              }}
              disabled={submitting}
            />
          </label>
          <label className="form-field form-field-wide">
            <span>Description *</span>
            <textarea
              rows={5}
              value={description}
              onChange={(event) => {
                setDescription(event.target.value);
                setError(null);
              }}
              disabled={submitting}
            />
          </label>
          {error && (
            <p className="agent-admin-form-error" role="alert">
              {error}
            </p>
          )}
          <button type="submit" className="primary-button agent-admin-submit" disabled={submitting}>
            {submitting ? <LoaderCircle className="rotating" size={18} /> : <Sparkles size={18} />}
            {submitting ? "Création…" : "Créer l’expérience"}
          </button>
        </form>
      </section>
    </div>
  );
}

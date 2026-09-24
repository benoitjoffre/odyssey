import { useEffect, useState } from "react";
import { ArrowRight, CalendarDays, Inbox, LoaderCircle, MapPin, Plus, RefreshCw, Sparkles, Trash2 } from "lucide-react";
import { Link } from "react-router-dom";
import { deleteExperience, getExperiences } from "../../api/experiences";
import { experienceCategoryLabels } from "../../helpers/experienceCategories";
import type { Experience } from "../../types/experience";

export function AgentExperiencesPage() {
  const [experiences, setExperiences] = useState<Experience[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [requestVersion, setRequestVersion] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    async function loadExperiences() {
      setLoading(true);
      setError(null);
      try {
        setExperiences(await getExperiences(controller.signal));
      } catch (requestError: unknown) {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        setError("Impossible de charger les expériences pour le moment.");
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }
    void loadExperiences();
    return () => controller.abort();
  }, [requestVersion]);

  async function handleDeleteExperience(experience: Experience) {
    if (deletingId !== null) return;

    const confirmed = window.confirm(`Supprimer l'expérience \"${experience.title}\" ?`);
    if (!confirmed) return;

    setDeletingId(experience.id);
    setDeleteError(null);

    try {
      await deleteExperience(experience.id);
      setExperiences((current) => current.filter((item) => item.id !== experience.id));
    } catch {
      setDeleteError("Suppression impossible. Vérifiez qu'aucun événement n'est lié à cette expérience.");
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div className="page-stack">
      <section className="page-heading agent-admin-heading">
        <div>
          <span className="eyebrow">Catalogue Odyssey</span>
          <h1>Expériences</h1>
          <p>Définissez ce que les voyageurs peuvent vivre avant de programmer des événements.</p>
        </div>
        <Link className="primary-button" to="/agent/experiences/new">
          <Plus size={18} /> Nouvelle expérience
        </Link>
      </section>

      {loading && (
        <div className="state-panel" role="status">
          <span className="spinner" />
          <strong>Chargement des expériences…</strong>
        </div>
      )}
      {!loading && error && (
        <div className="state-panel error-panel" role="alert">
          <RefreshCw size={24} />
          <strong>{error}</strong>
          <button className="secondary-button" type="button" onClick={() => setRequestVersion((value) => value + 1)}>
            <RefreshCw size={16} /> Réessayer
          </button>
        </div>
      )}
      {!loading && !error && experiences.length === 0 && (
        <div className="state-panel">
          <Inbox size={28} />
          <strong>Aucune expérience</strong>
          <p>Créez la première expérience du catalogue Odyssey.</p>
        </div>
      )}
      {!loading && !error && experiences.length > 0 && (
        <section className="agent-catalog-grid" aria-label="Catalogue des expériences">
          {experiences.map((experience) => (
            <article className="agent-catalog-card" key={experience.id}>
              <div className="agent-catalog-topline">
                <span>
                  <Sparkles size={15} /> {experienceCategoryLabels[experience.category]}
                </span>
                <strong>Expérience #{experience.id}</strong>
              </div>
              <h2>{experience.title}</h2>
              <p>{experience.description}</p>
              <div className="agent-catalog-meta">
                <span>
                  <MapPin size={16} /> {experience.destination}
                </span>
                <span>
                  <CalendarDays size={16} /> {experience.durationDays} jours
                </span>
              </div>
              <Link className="card-link" to="/agent/events/new" state={{ experienceId: experience.id }}>
                Programmer un événement <ArrowRight size={17} />
              </Link>
              <button
                type="button"
                className="secondary-button"
                disabled={deletingId !== null}
                onClick={() => void handleDeleteExperience(experience)}
              >
                {deletingId === experience.id ? <LoaderCircle className="rotating" size={16} /> : <Trash2 size={16} />}
                {deletingId === experience.id ? "Suppression…" : "Supprimer"}
              </button>
            </article>
          ))}
        </section>
      )}
      {deleteError && (
        <p className="agent-admin-form-error" role="alert">
          {deleteError}
        </p>
      )}
    </div>
  );
}

import { useEffect, useRef, useState, type FormEvent } from "react";
import { ArrowLeft, LoaderCircle, Sparkles, Trash2 } from "lucide-react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { getDestinations } from "../../api/destinations";
import { createExperience, getExperience, updateExperience } from "../../api/experiences";
import { uploadImage } from "../../api/images";
import { experienceCategoryLabels } from "../../helpers/experienceCategories";
import type { Destination } from "../../types/destination";
import type { ExperienceCategory } from "../../types/intent";

const categories = Object.entries(experienceCategoryLabels) as Array<[ExperienceCategory, string]>;

export function AgentExperienceCreatePage() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const experienceId = id ? Number(id) : null;
  const isEditing = id !== undefined;
  const hasValidExperienceId = experienceId !== null && Number.isInteger(experienceId) && experienceId > 0;
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [destinationId, setDestinationId] = useState("");
  const [destinations, setDestinations] = useState<Destination[]>([]);
  const [category, setCategory] = useState<ExperienceCategory | "">("");
  const [durationDays, setDurationDays] = useState("");
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [removeImage, setRemoveImage] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loadingDestinations, setLoadingDestinations] = useState(true);
  const [loadingExperience, setLoadingExperience] = useState(isEditing && hasValidExperienceId);
  const [submitting, setSubmitting] = useState(false);
  const submittingRef = useRef(false);

  useEffect(() => {
    const controller = new AbortController();

    async function loadDestinations() {
      try {
        setLoadingDestinations(true);
        setDestinations(await getDestinations(controller.signal));
      } catch (requestError: unknown) {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        setError("Impossible de charger les destinations pour le moment.");
      } finally {
        if (!controller.signal.aborted) setLoadingDestinations(false);
      }
    }

    void loadDestinations();
    return () => controller.abort();
  }, []);

  useEffect(() => {
    if (!isEditing) return;
    if (!hasValidExperienceId || experienceId === null) return;

    const controller = new AbortController();
    getExperience(experienceId, controller.signal)
      .then((experience) => {
        setTitle(experience.title);
        setDescription(experience.description);
        setDestinationId(String(experience.destination.id));
        setCategory(experience.category);
        setDurationDays(String(experience.durationDays));
        setImageUrl(experience.imageUrl ?? null);
      })
      .catch((requestError: unknown) => {
        if (!(requestError instanceof DOMException && requestError.name === "AbortError")) {
          setError("Impossible de charger cette expérience.");
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoadingExperience(false);
      });

    return () => controller.abort();
  }, [experienceId, hasValidExperienceId, isEditing]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submittingRef.current) return;
    const parsedDuration = Number(durationDays);
    const parsedDestinationId = Number(destinationId);
    if (!title.trim() || !description.trim() || !destinationId || !category || !durationDays) {
      setError("Tous les champs sont obligatoires.");
      return;
    }
    if (!Number.isInteger(parsedDestinationId) || parsedDestinationId <= 0) {
      setError("Veuillez sélectionner une destination.");
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
      const nextImageUrl = imageFile
        ? (await uploadImage(imageFile, "experiences")).imageUrl
        : removeImage
          ? null
          : imageUrl;
      const request = {
        title: title.trim(),
        description: description.trim(),
        destinationId: parsedDestinationId,
        category,
        durationDays: parsedDuration,
        imageUrl: nextImageUrl,
      };
      if (isEditing && experienceId !== null) {
        await updateExperience(experienceId, request);
      } else {
        await createExperience(request);
      }
      navigate("/agent/experiences");
    } catch {
      setError(`L’image ou l’expérience n’a pas pu être ${isEditing ? "modifiée" : "créée"}. Vérifiez les informations puis réessayez.`);
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
          <h1>{isEditing ? "Modifier l’expérience" : "Nouvelle expérience"}</h1>
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
            <select
              value={destinationId}
              onChange={(event) => {
                setDestinationId(event.target.value);
                setError(null);
              }}
              disabled={submitting || loadingDestinations || loadingExperience || destinations.length === 0 || (isEditing && !hasValidExperienceId)}
            >
              <option value="">{loadingDestinations ? "Chargement…" : "Sélectionner une destination"}</option>
              {destinations.map((destination) => (
                <option value={destination.id} key={destination.id}>
                  {destination.city}, {destination.country}
                </option>
              ))}
            </select>
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
            <span>Image (JPEG, PNG, WebP ou GIF, 10 Mo maximum)</span>
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp,image/gif"
              onChange={(event) => {
                const selectedFile = event.target.files?.[0] ?? null;
                if (selectedFile && selectedFile.size > 10 * 1024 * 1024) {
                  setImageFile(null);
                  setError("L’image ne doit pas dépasser 10 Mo.");
                  event.target.value = "";
                  return;
                }
                setImageFile(selectedFile);
                setRemoveImage(false);
                setError(null);
              }}
              disabled={submitting}
            />
            {imageFile && <span>{imageFile.name}</span>}
            {!imageFile && imageUrl && !removeImage && (
              <>
                <img src={imageUrl} alt="Image actuelle de l’expérience" style={{ maxWidth: "100%", maxHeight: 180, objectFit: "cover" }} />
                <button type="button" className="secondary-button" onClick={() => setRemoveImage(true)} disabled={submitting}>
                  <Trash2 size={16} /> Supprimer l’image actuelle
                </button>
              </>
            )}
            {removeImage && <span>L’image actuelle sera supprimée de cette expérience.</span>}
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
          {(error || (isEditing && !hasValidExperienceId && "L’identifiant de l’expérience est invalide.")) && (
            <p className="agent-admin-form-error" role="alert">
              {error ?? "L’identifiant de l’expérience est invalide."}
            </p>
          )}
          <button
            type="submit"
            className="primary-button agent-admin-submit"
            disabled={submitting || loadingDestinations || destinations.length === 0}
          >
            {submitting ? <LoaderCircle className="rotating" size={18} /> : <Sparkles size={18} />}
            {submitting ? (isEditing ? "Enregistrement…" : "Création…") : (isEditing ? "Enregistrer les modifications" : "Créer l’expérience")}
          </button>
        </form>
      </section>
    </div>
  );
}

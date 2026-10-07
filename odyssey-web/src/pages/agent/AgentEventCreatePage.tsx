import { useEffect, useRef, useState, type FormEvent } from "react";
import { ArrowLeft, CalendarPlus, LoaderCircle, Trash2 } from "lucide-react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { getExperiences } from "../../api/experiences";
import { uploadImage } from "../../api/images";
import { createTravelEvent, getTravelEvent, updateTravelEvent } from "../../api/travelEvents";
import type { Experience } from "../../types/experience";

export function AgentEventCreatePage() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const eventId = id ? Number(id) : null;
  const isEditing = id !== undefined;
  const hasValidEventId = eventId !== null && Number.isInteger(eventId) && eventId > 0;
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
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [removeImage, setRemoveImage] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loadingEvent, setLoadingEvent] = useState(isEditing && hasValidEventId);
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

  useEffect(() => {
    if (!isEditing) return;
    if (!hasValidEventId || eventId === null) return;

    const controller = new AbortController();
    getTravelEvent(eventId, controller.signal)
      .then((travelEvent) => {
        setName(travelEvent.name);
        setExperienceId(String(travelEvent.experienceId));
        setEventLocation(travelEvent.location);
        setStartDate(travelEvent.startDate);
        setEndDate(travelEvent.endDate);
        setDescription(travelEvent.description ?? "");
        setImageUrl(travelEvent.imageUrl);
      })
      .catch((requestError: unknown) => {
        if (!(requestError instanceof DOMException && requestError.name === "AbortError")) {
          setError("Impossible de charger cet événement.");
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoadingEvent(false);
      });

    return () => controller.abort();
  }, [eventId, hasValidEventId, isEditing]);

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
      const nextImageUrl = imageFile
        ? (await uploadImage(imageFile, "travel-events")).imageUrl
        : removeImage
          ? null
          : imageUrl;
      const request = {
        name: name.trim(),
        location: eventLocation.trim(),
        startDate,
        endDate,
        description: description.trim() || null,
        imageUrl: nextImageUrl,
        experienceId: Number(experienceId),
      };
      if (isEditing && eventId !== null) {
        await updateTravelEvent(eventId, request);
      } else {
        await createTravelEvent(request);
      }
      navigate("/agent/events");
    } catch {
      setError(`L’image ou l’événement n’a pas pu être ${isEditing ? "modifié" : "créé"}. Vérifiez les informations puis réessayez.`);
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
          <h1>{isEditing ? "Modifier l’événement" : "Nouvel événement"}</h1>
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
                <img src={imageUrl} alt="Image actuelle de l’événement" style={{ maxWidth: "100%", maxHeight: 180, objectFit: "cover" }} />
                <button type="button" className="secondary-button" onClick={() => setRemoveImage(true)} disabled={submitting}>
                  <Trash2 size={16} /> Supprimer l’image actuelle
                </button>
              </>
            )}
            {removeImage && <span>L’image actuelle sera supprimée de cet événement.</span>}
          </label>
          {(error || (isEditing && !hasValidEventId && "L’identifiant de l’événement est invalide.")) && (
            <p className="agent-admin-form-error" role="alert">
              {error ?? "L’identifiant de l’événement est invalide."}
            </p>
          )}
          <button type="submit" className="primary-button agent-admin-submit" disabled={submitting || loadingExperiences || loadingEvent || (isEditing && !hasValidEventId)}>
            {submitting ? <LoaderCircle className="rotating" size={18} /> : <CalendarPlus size={18} />}
            {submitting ? (isEditing ? "Enregistrement…" : "Création…") : (isEditing ? "Enregistrer les modifications" : "Créer l’événement")}
          </button>
        </form>
      </section>
    </div>
  );
}

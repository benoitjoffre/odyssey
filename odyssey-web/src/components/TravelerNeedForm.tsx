import { useRef, useState, type FormEvent } from "react";
import { BedDouble, LoaderCircle, Plane, X } from "lucide-react";
import { createNeed } from "../api/needs";
import type { CreateNeedRequest, OrganizableNeedType } from "../types/need";

interface TravelerNeedFormProps {
  tripId: number;
  type: OrganizableNeedType;
  onCancel: () => void;
  onCreated: () => void;
}

export function TravelerNeedForm({ tripId, type, onCancel, onCreated }: TravelerNeedFormProps) {
  const isFlight = type === "FLIGHT";
  const [place, setPlace] = useState("");
  const [destination, setDestination] = useState("");
  const [travelers, setTravelers] = useState(1);
  const [rooms, setRooms] = useState(1);
  const [notes, setNotes] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const submittingRef = useRef(false);

  function validate() {
    if (!place.trim()) return isFlight ? "Le lieu de départ est obligatoire." : "La ville est obligatoire.";
    if (isFlight && !destination.trim()) return "La destination est obligatoire.";
    if (!Number.isInteger(travelers) || travelers < 1) return "Le nombre de voyageurs doit être au moins égal à 1.";
    if (!isFlight && (!Number.isInteger(rooms) || rooms < 1)) return "Le nombre de chambres doit être au moins égal à 1.";
    return null;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submittingRef.current) return;

    const validationError = validate();
    setError(validationError);
    if (validationError) return;

    const common = { tripId, notes: notes.trim() || null };
    const request: CreateNeedRequest = isFlight
      ? {
          ...common,
          type: "FLIGHT",
          flightCriteria: { origin: place.trim(), destination: destination.trim(), travelers },
          accommodationCriteria: null,
        }
      : {
          ...common,
          type: "ACCOMMODATION",
          flightCriteria: null,
          accommodationCriteria: { city: place.trim(), travelers, rooms },
        };

    submittingRef.current = true;
    setSubmitting(true);
    try {
      await createNeed(request);
      onCreated();
    } catch {
      setError("Ce besoin n’a pas pu être ajouté. Vérifiez les informations puis réessayez.");
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  }

  return (
    <section className="traveler-need-form-card" aria-labelledby="need-form-title">
      <div className="traveler-need-form-heading">
        <span>{isFlight ? <Plane size={21} /> : <BedDouble size={21} />}</span>
        <div>
          <span className="eyebrow">Organiser mon voyage</span>
          <h3 id="need-form-title">Ajouter {isFlight ? "un vol" : "un hébergement"}</h3>
        </div>
        <button type="button" className="traveler-form-close" onClick={onCancel} aria-label="Fermer le formulaire" disabled={submitting}>
          <X size={18} />
        </button>
      </div>

      <form className="traveler-need-form" onSubmit={(event) => void handleSubmit(event)} noValidate>
        <label className="form-field">
          <span>{isFlight ? "Départ" : "Ville"}</span>
          <input
            value={place}
            onChange={(event) => {
              setPlace(event.target.value);
              setError(null);
            }}
            disabled={submitting}
            required
          />
        </label>
        {isFlight && (
          <label className="form-field">
            <span>Destination</span>
            <input
              value={destination}
              onChange={(event) => {
                setDestination(event.target.value);
                setError(null);
              }}
              disabled={submitting}
              required
            />
          </label>
        )}
        <label className="form-field">
          <span>Nombre de voyageurs</span>
          <input
            type="number"
            min="1"
            step="1"
            value={travelers}
            onChange={(event) => {
              setTravelers(Number(event.target.value));
              setError(null);
            }}
            disabled={submitting}
            required
          />
        </label>
        {!isFlight && (
          <label className="form-field">
            <span>Nombre de chambres</span>
            <input
              type="number"
              min="1"
              step="1"
              value={rooms}
              onChange={(event) => {
                setRooms(Number(event.target.value));
                setError(null);
              }}
              disabled={submitting}
              required
            />
          </label>
        )}
        <label className="form-field form-field-wide">
          <span>Notes (optionnel)</span>
          <textarea rows={3} value={notes} onChange={(event) => setNotes(event.target.value)} disabled={submitting} />
        </label>
        {error && (
          <p className="traveler-need-form-error" role="alert">
            {error}
          </p>
        )}
        <div className="traveler-need-form-actions">
          <button type="submit" className="primary-button" disabled={submitting}>
            {submitting && <LoaderCircle className="rotating" size={18} />}
            {submitting ? "Ajout en cours…" : "Ajouter à mon voyage"}
          </button>
          <button type="button" className="traveler-cancel-button" onClick={onCancel} disabled={submitting}>
            Annuler
          </button>
        </div>
      </form>
    </section>
  );
}

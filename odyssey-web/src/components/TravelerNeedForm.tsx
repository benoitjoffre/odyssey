import { useRef, useState, type FormEvent } from "react";
import { BedDouble, LoaderCircle, Plane, Route, X } from "lucide-react";
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
  const isAccommodation = type === "ACCOMMODATION";
  const isTransfer = type === "TRANSFER";
  const [place, setPlace] = useState("");
  const [destination, setDestination] = useState("");
  const [travelers, setTravelers] = useState(1);
  const [rooms, setRooms] = useState(1);
  const [notes, setNotes] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const submittingRef = useRef(false);

  let FormIcon = BedDouble;
  let formTitle = "un hébergement";
  let placeLabel = "Ville";
  let destinationLabel = "Destination";

  if (isFlight) {
    FormIcon = Plane;
    formTitle = "un vol";
    placeLabel = "Départ";
    destinationLabel = "Destination";
  } else if (isTransfer) {
    FormIcon = Route;
    formTitle = "un transfert";
    placeLabel = "Lieu de prise en charge";
    destinationLabel = "Lieu d’arrivée";
  }

  const showDestination = isFlight || isTransfer;
  const showRooms = isAccommodation;

  function validate() {
    if (isFlight) {
      if (!place.trim()) return "Le lieu de départ est obligatoire.";
      if (!destination.trim()) return "La destination est obligatoire.";
      if (!Number.isInteger(travelers) || travelers < 1) return "Le nombre de voyageurs doit être au moins égal à 1.";
      return null;
    }

    if (isAccommodation) {
      if (!place.trim()) return "La ville est obligatoire.";
      if (!Number.isInteger(travelers) || travelers < 1) return "Le nombre de voyageurs doit être au moins égal à 1.";
      if (!Number.isInteger(rooms) || rooms < 1) return "Le nombre de chambres doit être au moins égal à 1.";
      return null;
    }

    if (isTransfer) {
      if (!place.trim()) return "Le lieu de prise en charge est obligatoire.";
      if (!destination.trim()) return "Le lieu d’arrivée est obligatoire.";
      if (!Number.isInteger(travelers) || travelers < 1) return "Le nombre de voyageurs doit être au moins égal à 1.";
      return null;
    }

    return null;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submittingRef.current) return;

    const validationError = validate();
    setError(validationError);
    if (validationError) return;

    const common = { tripId, notes: notes.trim() || null };
    let request: CreateNeedRequest;

    if (isFlight) {
      request = {
        ...common,
        type: "FLIGHT",
        flightCriteria: { origin: place.trim(), destination: destination.trim(), travelers },
        accommodationCriteria: null,
        transferCriteria: null,
      };
    } else if (isAccommodation) {
      request = {
        ...common,
        type: "ACCOMMODATION",
        flightCriteria: null,
        accommodationCriteria: { city: place.trim(), travelers, rooms },
        transferCriteria: null,
      };
    } else {
      request = {
        ...common,
        type: "TRANSFER",
        flightCriteria: null,
        accommodationCriteria: null,
        transferCriteria: { pickupLocation: place.trim(), dropoffLocation: destination.trim(), travelers },
      };
    }

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
        <span>
          <FormIcon size={21} />
        </span>
        <div>
          <span className="eyebrow">Organiser mon voyage</span>
          <h3 id="need-form-title">Ajouter {formTitle}</h3>
        </div>
        <button type="button" className="traveler-form-close" onClick={onCancel} aria-label="Fermer le formulaire" disabled={submitting}>
          <X size={18} />
        </button>
      </div>

      <form className="traveler-need-form" onSubmit={(event) => void handleSubmit(event)} noValidate>
        <label className="form-field">
          <span>{placeLabel}</span>
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
        {showDestination && (
          <label className="form-field">
            <span>{destinationLabel}</span>
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
        {showRooms && (
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

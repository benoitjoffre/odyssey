import { useEffect, useRef, useState, type ReactNode } from "react";
import {
  ArrowLeft,
  BedDouble,
  Bus,
  CalendarDays,
  Car,
  Check,
  Clock3,
  Hotel,
  LoaderCircle,
  Plane,
  RefreshCw,
  Route,
  Send,
  Trash2,
} from "lucide-react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { createBookingRequest } from "../../api/bookingRequests";
import { deleteTrip, getTripDetail } from "../../api/trips";
import { TravelerNeedForm } from "../../components/TravelerNeedForm";
import type { OrganizableNeedType } from "../../types/need";
import type { TripDetail, TripNeed, TripNeedType } from "../../types/trip";

const needLabels: Record<TripNeedType, string> = {
  ACCOMMODATION: "Hébergement",
  FLIGHT: "Vol",
  TRANSFER: "Transfert",
  CAR: "Voiture",
  BUS: "Bus",
};

const needIcons: Record<TripNeedType, ReactNode> = {
  ACCOMMODATION: <Hotel size={21} />,
  FLIGHT: <Plane size={21} />,
  TRANSFER: <Route size={21} />,
  CAR: <Car size={21} />,
  BUS: <Bus size={21} />,
};

const organizationChoices: Array<{ type: TripNeedType; label: string; available: boolean }> = [
  { type: "FLIGHT", label: "Vol", available: true },
  { type: "ACCOMMODATION", label: "Hébergement", available: true },
  { type: "TRANSFER", label: "Transfert", available: false },
  { type: "CAR", label: "Voiture", available: false },
  { type: "BUS", label: "Bus", available: false },
];

interface TravelerNeedState {
  label: string;
  tone: "confirmed" | "pending" | "progress" | "requested" | "idle";
}

function getTravelerNeedState(need: TripNeed): TravelerNeedState {
  if (need.bookingStatus === "CONFIRMED") return { label: "Réservation confirmée", tone: "confirmed" };
  if (need.bookingStatus === "PENDING") return { label: "Réservation en attente de confirmation", tone: "pending" };
  if (need.bookingRequestStatus === "IN_PROGRESS") return { label: "Demande prise en charge", tone: "progress" };
  if (need.bookingRequestStatus === "REQUESTED") return { label: "Demande envoyée", tone: "requested" };
  return { label: "À organiser", tone: "idle" };
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("fr-FR", {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(new Date(`${value}T00:00:00`));
}

export function TravelerTripDetailPage() {
  const { tripId } = useParams<{ tripId: string }>();
  const navigate = useNavigate();
  const parsedTripId = Number(tripId);
  const hasInvalidTripId = !Number.isInteger(parsedTripId) || parsedTripId <= 0;
  const [trip, setTrip] = useState<TripDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [requestVersion, setRequestVersion] = useState(0);
  const [selectedNeedType, setSelectedNeedType] = useState<OrganizableNeedType | null>(null);
  const [sendingNeedId, setSendingNeedId] = useState<number | null>(null);
  const [requestErrors, setRequestErrors] = useState<Record<number, string>>({});
  const [confirmingDeletion, setConfirmingDeletion] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const sendingRequestRef = useRef(false);
  const deletingRef = useRef(false);

  async function handleDeleteTrip() {
    if (deletingRef.current || !trip) return;
    deletingRef.current = true;
    setDeleting(true);
    setDeleteError(null);

    try {
      await deleteTrip(trip.id);
      navigate("/traveler/trips", { replace: true });
    } catch {
      setDeleteError("Ce voyage n’a pas pu être supprimé. Réessayez dans un instant.");
    } finally {
      deletingRef.current = false;
      setDeleting(false);
    }
  }

  async function handleSendRequest(need: TripNeed) {
    if (sendingRequestRef.current || need.bookingRequestStatus) return;
    sendingRequestRef.current = true;
    setSendingNeedId(need.id);
    setRequestErrors((current) => {
      const next = { ...current };
      delete next[need.id];
      return next;
    });

    try {
      await createBookingRequest(need.id, need.notes);
      setRequestVersion((version) => version + 1);
    } catch {
      setRequestErrors((current) => ({ ...current, [need.id]: "La demande n’a pas pu être envoyée. Réessayez dans un instant." }));
    } finally {
      sendingRequestRef.current = false;
      setSendingNeedId(null);
    }
  }

  useEffect(() => {
    if (hasInvalidTripId) return;

    const controller = new AbortController();

    async function loadTrip() {
      setLoading(true);
      setError(null);

      try {
        setTrip(await getTripDetail(parsedTripId, controller.signal));
      } catch (requestError: unknown) {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        setError("Ce voyage est introuvable ou momentanément indisponible.");
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void loadTrip();
    return () => controller.abort();
  }, [hasInvalidTripId, parsedTripId, requestVersion]);

  if (hasInvalidTripId) {
    return (
      <div className="traveler-page">
        <Link className="back-link" to="/traveler/trips">
          <ArrowLeft size={17} /> Retour à mes voyages
        </Link>
        <div className="state-panel error-panel" role="alert">
          <strong>L’identifiant du voyage est invalide.</strong>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="state-panel" role="status">
        <span className="spinner" aria-hidden="true" />
        <strong>Chargement du voyage…</strong>
        <p>Nous récupérons les étapes de votre voyage.</p>
      </div>
    );
  }

  if (error || !trip) {
    return (
      <div className="traveler-page">
        <Link className="back-link" to="/traveler/trips">
          <ArrowLeft size={17} /> Retour à mes voyages
        </Link>
        <div className="state-panel error-panel" role="alert">
          <RefreshCw size={24} aria-hidden="true" />
          <strong>{error ?? "Voyage introuvable."}</strong>
          <button type="button" className="secondary-button" onClick={() => setRequestVersion((version) => version + 1)}>
            <RefreshCw size={16} /> Réessayer
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="traveler-page">
      <Link className="back-link" to="/traveler/trips">
        <ArrowLeft size={17} /> Retour à mes voyages
      </Link>

      <section className="traveler-trip-heading">
        <span className="eyebrow">Votre voyage</span>
        <h1>{trip.title || `Voyage #${trip.id}`}</h1>
        <p>
          <CalendarDays size={17} /> {formatDate(trip.startDate)} <span>au</span> {formatDate(trip.endDate)}
        </p>
      </section>

      <section aria-labelledby="trip-needs-title">
        <div className="section-heading traveler-needs-heading">
          <div>
            <h2 id="trip-needs-title">Votre voyage</h2>
            <p>Les éléments organisés avec votre conseiller Odyssey</p>
          </div>
        </div>

        {trip.needs.length === 0 ? (
          <div className="state-panel traveler-empty-needs">
            <BedDouble size={27} />
            <strong>Aucun besoin ajouté</strong>
            <p>Les éléments de ce voyage apparaîtront ici.</p>
          </div>
        ) : (
          <div className="traveler-needs-grid">
            {trip.needs.map((need) => {
              const state = getTravelerNeedState(need);
              return (
                <article className="traveler-need-card" key={need.id}>
                  <div className="traveler-need-title">
                    <span>{needIcons[need.type]}</span>
                    <h3>{needLabels[need.type]}</h3>
                  </div>
                  <div className={`traveler-need-state ${state.tone}`}>
                    {state.tone === "confirmed" && <Check size={17} />}
                    {state.tone === "pending" && <Clock3 size={17} />}
                    <strong>{state.label}</strong>
                  </div>
                  {need.notes && <p className="traveler-need-notes">{need.notes}</p>}
                  {need.status === "DRAFT" && !need.bookingRequestStatus && (
                    <div className="traveler-need-request-action">
                      {requestErrors[need.id] && <p role="alert">{requestErrors[need.id]}</p>}
                      <button type="button" className="primary-button" disabled={sendingNeedId !== null} onClick={() => void handleSendRequest(need)}>
                        {sendingNeedId === need.id ? <LoaderCircle className="rotating" size={17} /> : <Send size={17} />}
                        {sendingNeedId === need.id ? "Envoi en cours…" : "Envoyer ma demande"}
                      </button>
                    </div>
                  )}
                  {need.bookingStatus === "CONFIRMED" && need.providerConfirmationId && (
                    <div className="traveler-booking-reference">
                      <span>Référence de réservation</span>
                      <strong>{need.providerConfirmationId}</strong>
                    </div>
                  )}
                </article>
              );
            })}
          </div>
        )}
      </section>

      <section aria-labelledby="organize-trip-title">
        <div className="section-heading traveler-needs-heading">
          <div>
            <h2 id="organize-trip-title">Organiser mon voyage</h2>
            <p>Ajoutez les éléments pour lesquels vous souhaitez être accompagné par Odyssey.</p>
          </div>
        </div>

        <div className="traveler-organization-grid">
          {organizationChoices.map((choice) =>
            choice.available ? (
              <button
                type="button"
                className={`traveler-organization-choice${selectedNeedType === choice.type ? " selected" : ""}`}
                key={choice.type}
                onClick={() => setSelectedNeedType(choice.type as OrganizableNeedType)}
              >
                <span>{needIcons[choice.type]}</span>
                <strong>{choice.label}</strong>
              </button>
            ) : (
              <div className="traveler-organization-choice unavailable" key={choice.type} aria-disabled="true">
                <span>{needIcons[choice.type]}</span>
                <strong>{choice.label}</strong>
                <small>À venir</small>
              </div>
            ),
          )}
        </div>

        {selectedNeedType && (
          <TravelerNeedForm
            key={selectedNeedType}
            tripId={trip.id}
            type={selectedNeedType}
            onCancel={() => setSelectedNeedType(null)}
            onCreated={() => {
              setSelectedNeedType(null);
              setRequestVersion((version) => version + 1);
            }}
          />
        )}
      </section>

      <section className="traveler-trip-danger-zone" aria-labelledby="delete-trip-title">
        <div>
          <h2 id="delete-trip-title">Supprimer ce voyage</h2>
          <p>Le voyage et son organisation seront définitivement supprimés.</p>
        </div>

        {!confirmingDeletion ? (
          <button
            type="button"
            className="traveler-delete-button"
            onClick={() => {
              setConfirmingDeletion(true);
              setDeleteError(null);
            }}
          >
            <Trash2 size={17} /> Supprimer le voyage
          </button>
        ) : (
          <div className="traveler-delete-confirmation">
            <p>
              <strong>Confirmer la suppression de « {trip.title || `Voyage #${trip.id}`} » ?</strong> Cette action est irréversible.
            </p>
            {deleteError && (
              <p className="traveler-delete-error" role="alert">
                {deleteError}
              </p>
            )}
            <div>
              <button type="button" className="traveler-delete-button confirmed" disabled={deleting} onClick={() => void handleDeleteTrip()}>
                {deleting ? <LoaderCircle className="rotating" size={17} /> : <Trash2 size={17} />}
                {deleting ? "Suppression…" : "Oui, supprimer"}
              </button>
              <button
                type="button"
                className="traveler-cancel-button"
                disabled={deleting}
                onClick={() => {
                  setConfirmingDeletion(false);
                  setDeleteError(null);
                }}
              >
                Annuler
              </button>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}

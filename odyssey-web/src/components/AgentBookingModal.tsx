import React, { useState } from "react";
import { CheckCircle2, ExternalLink, X } from "lucide-react";
import type { Booking, ProviderPaymentStatus } from "../types/booking";
import { confirmBooking, updateBookingProviderDetails } from "../api/bookings";

interface AgentBookingModalProps {
  booking: Booking;
  onClose: () => void;
  onSaved: (booking: Booking) => void;
}

const AgentBookingModal: React.FC<AgentBookingModalProps> = ({ booking, onClose, onSaved }) => {
  const [providerReference, setProviderReference] = useState(booking.providerReference ?? "");

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [providerPaymentUrl, setProviderPaymentUrl] = useState(booking.providerPaymentUrl ?? "");

  const [providerPaymentStatus, setProviderPaymentStatus] = useState<ProviderPaymentStatus>(booking.providerPaymentStatus ?? "NOT_REQUIRED_YET");
  const [confirming, setConfirming] = useState(false);

  const handleSave = async () => {
    if (saving) return;

    setSaving(true);
    setError(null);

    try {
      const updatedBooking = await updateBookingProviderDetails(booking.id, {
        providerReference,
        providerPaymentUrl,
        providerPaymentStatus,
      });

      onSaved(updatedBooking);
    } catch {
      setError("Impossible d’enregistrer les informations fournisseur.");
    } finally {
      setSaving(false);
    }
  };

  const canConfirm = booking.status === "PENDING" && Boolean(providerReference.trim());

  const handleConfirm = async () => {
    if (!canConfirm || confirming) return;

    setConfirming(true);
    setError(null);

    try {
      const updatedBooking = await confirmBooking(booking.id);

      onSaved(updatedBooking);
    } catch {
      setError("Impossible de confirmer la réservation.");
    } finally {
      setConfirming(false);
    }
  };

  const hasChanges =
    providerReference !== (booking.providerReference ?? "") ||
    providerPaymentUrl !== (booking.providerPaymentUrl ?? "") ||
    providerPaymentStatus !== booking.providerPaymentStatus;

  return (
    <div className="modal-backdrop">
      <div className="booking-modal" role="dialog" aria-modal="true" aria-labelledby="booking-modal-title">
        <div className="booking-modal-header">
          <div>
            <span className="eyebrow">Réservation fournisseur</span>
            <h2 id="booking-modal-title">Finaliser la réservation</h2>
          </div>

          <button type="button" className="icon-button" aria-label="Fermer" onClick={onClose}>
            <X size={20} />
          </button>
        </div>

        <p>Réservation #{booking.id}</p>
        {booking.status === "CONFIRMED" ? (
          <div className="booking-confirmed-summary">
            <div className="booking-confirmed-heading">
              <CheckCircle2 size={24} />
              <div>
                <strong>Réservation confirmée</strong>
                <p>La réservation fournisseur a bien été finalisée.</p>
              </div>
            </div>

            <dl className="booking-confirmed-details">
              <div>
                <dt>Référence fournisseur</dt>
                <dd>{booking.providerReference}</dd>
              </div>

              <div>
                <dt>Paiement fournisseur</dt>
                <dd>
                  <select
                    className="booking-form-field-select"
                    value={providerPaymentStatus}
                    onChange={(event) => setProviderPaymentStatus(event.target.value as ProviderPaymentStatus)}
                  >
                    <option value="NOT_REQUIRED_YET">Pas encore déterminé</option>
                    <option value="PAYMENT_REQUIRED">À payer par le voyageur</option>
                    <option value="PAID_TO_PROVIDER">Payé au fournisseur</option>
                    <option value="UNKNOWN">Statut inconnu</option>
                  </select>
                </dd>
              </div>
            </dl>

            {booking.providerPaymentUrl && (
              <a href={booking.providerPaymentUrl} target="_blank" rel="noreferrer" className="secondary-button">
                Ouvrir le paiement fournisseur
                <ExternalLink size={16} />
              </a>
            )}

            <div className="booking-modal-actions">
              <button type="button" className="primary-button" onClick={onClose}>
                Fermer
              </button>
            </div>
          </div>
        ) : (
          <div className="booking-modal-form">
            <label className="booking-form-field">
              <span>Référence fournisseur</span>
              <input
                type="text"
                value={providerReference}
                onChange={(event) => setProviderReference(event.target.value)}
                placeholder="Ex. ABC-123456"
              />
            </label>

            <label className="booking-form-field">
              <span>Lien de paiement fournisseur</span>
              <input
                type="url"
                value={providerPaymentUrl}
                onChange={(event) => setProviderPaymentUrl(event.target.value)}
                placeholder="https://..."
              />
            </label>

            <label className="booking-form-field">
              <span>Paiement fournisseur</span>
              <select value={providerPaymentStatus} onChange={(event) => setProviderPaymentStatus(event.target.value as ProviderPaymentStatus)}>
                <option value="NOT_REQUIRED_YET">Pas encore déterminé</option>
                <option value="PAYMENT_REQUIRED">À payer par le voyageur</option>
                <option value="PAID_TO_PROVIDER">Payé au fournisseur</option>
                <option value="UNKNOWN">Statut inconnu</option>
              </select>
            </label>
          </div>
        )}

        {error && (
          <p className="action-error" role="alert">
            {error}
          </p>
        )}
        <div className="booking-modal-actions">
          <button type="button" className="secondary-button" onClick={onClose}>
            Annuler
          </button>
          <button type="button" className="primary-button" onClick={handleSave} disabled={saving || !hasChanges}>
            {saving ? "Enregistrement…" : "Enregistrer"}
          </button>
          <button
            type="button"
            className="primary-button"
            disabled={!canConfirm || confirming || saving || hasChanges}
            onClick={() => void handleConfirm()}
          >
            {confirming ? "Confirmation…" : "Confirmer la réservation"}
          </button>
        </div>
      </div>
    </div>
  );
};

export { AgentBookingModal };

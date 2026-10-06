import { useState } from "react";
import { ClipboardCheck, LoaderCircle } from "lucide-react";
import { createBooking } from "../api/bookings";
import type { Booking } from "../types/booking";
import type { AgentQuoteResponse } from "../types/quote";
import { AgentBookingModal } from "./AgentBookingModal";

export interface BookingRequestBookingWorkflowSectionProps {
  acceptedQuote: AgentQuoteResponse | null;
  booking: Booking | null;
  onBookingChanged: (booking: Booking) => void;
}

export function BookingRequestBookingWorkflowSection({ acceptedQuote, booking, onBookingChanged }: BookingRequestBookingWorkflowSectionProps) {
  const [creatingBooking, setCreatingBooking] = useState(false);
  const [bookingError, setBookingError] = useState<string | null>(null);
  const [bookingModalOpen, setBookingModalOpen] = useState(false);

  async function handleCreateBooking() {
    if (!acceptedQuote || creatingBooking) return;

    setCreatingBooking(true);
    setBookingError(null);

    try {
      onBookingChanged(await createBooking(acceptedQuote.id));
    } catch {
      setBookingError("La réservation n’a pas pu être créée. Veuillez réessayer.");
    } finally {
      setCreatingBooking(false);
    }
  }

  const handleBookingSaved = (updatedBooking: Booking) => {
    onBookingChanged(updatedBooking);
  };

  return (
    <>
      {acceptedQuote && (
        <section className={`booking-workflow-card${booking?.status === "CONFIRMED" ? " confirmed" : ""}`} aria-labelledby="booking-workflow-title">
          <div className="booking-workflow-heading">
            <span className="booking-workflow-icon">
              <ClipboardCheck size={21} />
            </span>
            <div>
              <span className="eyebrow">Réservation fournisseur</span>
              <h2 id="booking-workflow-title">
                {!booking && "Proposition acceptée par le client"}
                {booking?.status === "PENDING" && "Réservation en attente de confirmation"}
                {booking?.status === "CONFIRMED" && "Réservation confirmée"}
              </h2>
            </div>
            {booking && <span className={`booking-status status-${booking.status.toLowerCase()}`}>{booking.status}</span>}
          </div>

          {booking?.status === "CONFIRMED" && (
            <div className="provider-confirmation">
              <span>Référence fournisseur</span>
              <strong>{booking.providerConfirmationId}</strong>
            </div>
          )}

          {booking?.status === "PENDING" && (
            <button type="button" className="primary-button" onClick={() => setBookingModalOpen(true)}>
              <ClipboardCheck size={18} />
              Finaliser la réservation
            </button>
          )}

          {booking?.status === "CONFIRMED" && (
            <button type="button" className="secondary-button" onClick={() => setBookingModalOpen(true)}>
              <ClipboardCheck size={18} />
              Voir la réservation
            </button>
          )}

          {bookingError && (
            <p className="booking-error" role="alert">
              {bookingError}
            </p>
          )}

          {!booking && (
            <button type="button" className="primary-button" onClick={handleCreateBooking} disabled={creatingBooking}>
              {creatingBooking ? <LoaderCircle className="rotating" size={18} /> : <ClipboardCheck size={18} />}
              {creatingBooking ? "Création de la réservation…" : "Créer la réservation"}
            </button>
          )}

          {booking?.status === "PENDING" && !booking.providerReference && (
            <p className="booking-payment-pending">Renseignez la référence fournisseur avant de pouvoir confirmer.</p>
          )}
        </section>
      )}

      {bookingModalOpen && booking && <AgentBookingModal booking={booking} onClose={() => setBookingModalOpen(false)} onSaved={handleBookingSaved} />}
    </>
  );
}

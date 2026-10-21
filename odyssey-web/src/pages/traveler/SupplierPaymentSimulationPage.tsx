import { useEffect, useRef, useState } from "react";
import { ArrowLeft, CheckCircle2, CreditCard, FlaskConical, LoaderCircle } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { ApiError } from "../../api/client";
import { getSupplierPaymentSimulation, simulateSupplierPayment, type SupplierPaymentSimulation } from "../../api/supplierPaymentSimulation";
import { ErrorState, LoadingState } from "../../components/ui/State";
import "./SupplierPaymentSimulationPage.css";

const serviceLabels = {
  FLIGHT: "Vol",
  ACCOMMODATION: "Hébergement",
  TRANSFER: "Transfert",
  CAR: "Voiture",
  BUS: "Bus",
};

function errorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 404) return "Cette simulation est désactivée ou cette réservation est introuvable.";
    if (error.status === 403 || error.status === 401) return "Vous n’avez pas accès à cette réservation. Reconnectez-vous avec le compte voyageur concerné.";
    if (error.status === 409) return "Cette réservation n’est plus payable. Retournez au voyage pour consulter son état.";
  }
  return "Impossible de joindre le service. Réessayez pour vérifier le statut du paiement.";
}

function formatDate(date: string) {
  return new Intl.DateTimeFormat("fr-FR", { dateStyle: "long" }).format(new Date(`${date}T12:00:00`));
}

export function SupplierPaymentSimulationPage() {
  const { bookingId } = useParams<{ bookingId: string }>();
  const parsedBookingId = Number(bookingId);
  const validId = Number.isSafeInteger(parsedBookingId) && parsedBookingId > 0;
  const [payment, setPayment] = useState<SupplierPaymentSimulation | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [requestVersion, setRequestVersion] = useState(0);
  const submittingRef = useRef(false);

  useEffect(() => {
    if (!validId) return;
    const controller = new AbortController();
    async function loadPayment() {
      setLoading(true);
      setError(null);
      setSubmitError(null);
      try {
        const result = await getSupplierPaymentSimulation(parsedBookingId, controller.signal);
        if (!controller.signal.aborted) setPayment(result);
      } catch (requestError: unknown) {
        if (!controller.signal.aborted) setError(errorMessage(requestError));
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }
    void loadPayment();
    return () => controller.abort();
  }, [parsedBookingId, validId, requestVersion]);

  async function handlePayment() {
    if (!payment || submittingRef.current || payment.providerPaymentStatus !== "PAYMENT_REQUIRED") return;
    submittingRef.current = true;
    setSubmitting(true);
    setSubmitError(null);
    try {
      setPayment(await simulateSupplierPayment(payment.bookingId));
    } catch (requestError: unknown) {
      setSubmitError(errorMessage(requestError));
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  }

  if (!validId) return <ErrorState title="Réservation invalide" description="Ouvrez cette page depuis le paiement fournisseur de votre voyage." />;
  if (loading) return <LoadingState title="Chargement de votre réservation…" />;
  if (error || !payment) return (
    <ErrorState title={error ?? "Réservation indisponible"}>
      <button type="button" className="secondary-button" onClick={() => setRequestVersion((version) => version + 1)}>Réessayer</button>
      <Link className="back-link" to="/traveler/trips">Retour à mes voyages</Link>
    </ErrorState>
  );

  const paid = payment.providerPaymentStatus === "PAID_TO_PROVIDER";
  const amount = new Intl.NumberFormat("fr-FR", { style: "currency", currency: payment.currency }).format(payment.amount);
  const travelerName = [payment.travelerFirstName, payment.travelerLastName].filter(Boolean).join(" ") || "Non renseigné";

  return (
    <div className="supplier-simulation">
      <Link className="back-link" to={`/traveler/trips/${payment.tripId}`}><ArrowLeft size={17} aria-hidden="true" /> Retour à mon voyage</Link>
      <div className="supplier-simulation__notice" role="note">
        <FlaskConical size={24} aria-hidden="true" />
        <div>
          <strong>Simulation — aucun débit</strong>
          <p>Page de test Odyssey, pas un site fournisseur. Aucune carte bancaire ne vous sera demandée.
            La validation modifie réellement le statut de cette réservation dans votre environnement de test.</p>
        </div>
      </div>
      <header>
        <span className="eyebrow">Paiement fournisseur · environnement de test</span>
        <h1>{paid ? "Paiement simulé enregistré" : "Finaliser votre réservation"}</h1>
        <p>{payment.tripTitle}</p>
      </header>
      <div className="supplier-simulation__grid">
        <section className="supplier-simulation__card" aria-labelledby="supplier-booking-title">
          <span className="eyebrow">{serviceLabels[payment.serviceType]} · Réservation #{payment.bookingId}</span>
          <h2 id="supplier-booking-title">{payment.provider}</h2>
          {payment.description && <p className="supplier-simulation__description">{payment.description}</p>}
          <dl className="supplier-simulation__details">
            <div><dt>Référence fournisseur</dt><dd>{payment.providerReference || "Non renseignée"}</dd></div>
            <div><dt>Dates du voyage</dt><dd>{formatDate(payment.tripStartDate)} – {formatDate(payment.tripEndDate)}</dd></div>
            <div><dt>Voyageur</dt><dd>{travelerName}</dd></div>
            <div><dt>E-mail</dt><dd>{payment.travelerEmail}</dd></div>
            {payment.origin && <div><dt>Départ / prise en charge</dt><dd>{payment.origin}</dd></div>}
            {payment.destination && <div><dt>Destination / arrivée</dt><dd>{payment.destination}</dd></div>}
            {payment.city && <div><dt>Ville</dt><dd>{payment.city}</dd></div>}
            {payment.travelers !== null && <div><dt>Nombre de voyageurs</dt><dd>{payment.travelers}</dd></div>}
            {payment.rooms !== null && <div><dt>Nombre de chambres</dt><dd>{payment.rooms}</dd></div>}
          </dl>
          {payment.notes && <p className="supplier-simulation__description">{payment.notes}</p>}
        </section>
        <section className="supplier-simulation__card supplier-simulation__checkout" aria-labelledby="supplier-total-title">
          <CreditCard size={28} aria-hidden="true" />
          <h2 id="supplier-total-title">Montant fournisseur</h2>
          <strong className="supplier-simulation__amount">{amount}</strong>
          <p>Prix de la proposition acceptée. Les frais d’accompagnement Odyssey ne sont pas inclus et restent séparés.</p>
          {paid ? (
            <div className="supplier-simulation__success" role="status">
              <CheckCircle2 size={28} aria-hidden="true" />
              <strong>Simulation réussie — aucun montant débité</strong>
              <p>La réservation est maintenant marquée « Réglé au fournisseur ». Sa confirmation reste gérée par votre agent.</p>
              <Link className="primary-button" to={`/traveler/trips/${payment.tripId}`}>Voir mon voyage actualisé</Link>
            </div>
          ) : (
            <>
              <p>Ce bouton simule un succès et enregistre le règlement fournisseur dans Odyssey.</p>
              {submitError && <p className="traveler-form-error" role="alert">{submitError}</p>}
              <button type="button" className="primary-button" disabled={submitting} onClick={() => void handlePayment()}>
                {submitting ? <LoaderCircle className="rotating" size={18} aria-hidden="true" /> : <FlaskConical size={18} aria-hidden="true" />}
                {submitting ? "Enregistrement…" : `Simuler le paiement de ${amount}`}
              </button>
              <Link className="back-link" to={`/traveler/trips/${payment.tripId}`}>Annuler et retourner au voyage</Link>
            </>
          )}
        </section>
      </div>
    </div>
  );
}

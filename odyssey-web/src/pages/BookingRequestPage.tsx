import { useEffect, useState, type FormEvent } from "react";
import {
  ArrowLeft,
  ArrowRight,
  BedDouble,
  CalendarDays,
  Check,
  ClipboardCheck,
  CircleUserRound,
  FileCheck2,
  Hotel,
  LoaderCircle,
  Mail,
  MapPin,
  Plane,
  RefreshCw,
  Search,
  Users,
  Car,
} from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { createBooking, getBookingByQuote } from "../api/bookings";
import { claimBookingRequest, getBookingRequest, searchBookingRequestOffers } from "../api/bookingRequests";
import { createQuote, getAgentBookingRequestQuotes } from "../api/quotes";
import type { Booking } from "../types/booking";
import type { BookingRequest, BookingRequestStatus, NeedType } from "../types/bookingRequest";
import { isAccommodationOffer, isTransferOffer, type ProviderOffer } from "../types/providerOffer";
import type { AgentQuoteResponse, QuoteResponse } from "../types/quote";
import { AgentBookingModal } from "../components/AgentBookingModal";

const statusLabels: Record<BookingRequestStatus, string> = {
  REQUESTED: "Demandée",
  IN_PROGRESS: "En cours",
  COMPLETED: "Terminée",
  CANCELLED: "Annulée",
};

const needTypeLabels: Record<NeedType, string> = {
  ACCOMMODATION: "Hébergement",
  FLIGHT: "Vol",
  CAR: "Voiture",
  TRANSFER: "Transfert",
  BUS: "Bus",
};

function formatDate(value: string) {
  return new Intl.DateTimeFormat("fr-FR", { dateStyle: "medium" }).format(new Date(`${value}T00:00:00`));
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("fr-FR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function formatPrice(price: number, currency: string) {
  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency,
    maximumFractionDigits: 2,
  }).format(price);
}

function getOfferDescription(offer: ProviderOffer) {
  if (isAccommodationOffer(offer)) {
    return `${offer.hotelName} - ${offer.roomType}`;
  }

  if (isTransferOffer(offer)) {
    return `Transfert - ${offer.vehicleType}`;
  }
  return `${offer.airline} - ${offer.origin} → ${offer.destination}`;
}

function toAgentQuoteResponse(quote: QuoteResponse): AgentQuoteResponse {
  return {
    ...quote,
    paymentStatus: null,
    providerPaymentUrl: null,
    providerPaymentStatus: "NOT_REQUIRED_YET",
  };
}

export function BookingRequestPage() {
  const { id } = useParams<{ id: string }>();
  const bookingRequestId = Number(id);
  const hasInvalidId = !Number.isInteger(bookingRequestId) || bookingRequestId <= 0;
  const [bookingRequest, setBookingRequest] = useState<BookingRequest | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadVersion, setReloadVersion] = useState(0);
  const [claiming, setClaiming] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [offers, setOffers] = useState<ProviderOffer[]>([]);
  const [searchingOffers, setSearchingOffers] = useState(false);
  const [selectedOffer, setSelectedOffer] = useState<ProviderOffer | null>(null);
  const [quoteDescription, setQuoteDescription] = useState("");
  const [creatingQuote, setCreatingQuote] = useState(false);
  const [quoteError, setQuoteError] = useState<string | null>(null);
  const [agentQuotes, setAgentQuotes] = useState<AgentQuoteResponse[]>([]);
  const [booking, setBooking] = useState<Booking | null>(null);
  const [creatingBooking, setCreatingBooking] = useState(false);
  const [bookingError, setBookingError] = useState<string | null>(null);

  const acceptedQuote = agentQuotes.find((quote) => quote.status === "ACCEPTED") ?? null;
  const [bookingModalOpen, setBookingModalOpen] = useState(false);

  useEffect(() => {
    if (hasInvalidId) return;

    const controller = new AbortController();

    async function loadBookingRequest() {
      setLoading(true);
      setError(null);

      try {
        const request = await getBookingRequest(bookingRequestId, controller.signal);
        setBookingRequest(request);

        if (request.assignedAgentId !== null) {
          try {
            const quotes = await getAgentBookingRequestQuotes(request.id, controller.signal);

            const sortedQuotes = quotes.sort((first, second) => second.id - first.id);

            setAgentQuotes(sortedQuotes);

            const acceptedQuote = sortedQuotes.find((quote) => quote.status === "ACCEPTED");

            if (acceptedQuote) {
              const existingBooking = await getBookingByQuote(acceptedQuote.id);
              setBooking(existingBooking);
            } else {
              setBooking(null);
            }
          } catch (quotesError: unknown) {
            if (quotesError instanceof DOMException && quotesError.name === "AbortError") throw quotesError;
            setAgentQuotes([]);
          }
        } else {
          setAgentQuotes([]);
        }
      } catch (requestError: unknown) {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        setError("Impossible de charger cette demande pour le moment.");
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void loadBookingRequest();

    return () => controller.abort();
  }, [bookingRequestId, hasInvalidId, reloadVersion]);

  // Synchronise le formulaire "infos fournisseur" sur le Booking courant
  // (nouvelle réservation créée, ou rechargement) sans passer par un effect :
  // on ajuste l'état pendant le rendu, comme recommandé par React pour
  // "réinitialiser un formulaire quand une donnée externe change".
  const [providerDetailsBookingId, setProviderDetailsBookingId] = useState<number | null>(null);
  if (booking && providerDetailsBookingId !== booking.id) {
    setProviderDetailsBookingId(booking.id);
  }

  async function handleClaim() {
    setClaiming(true);
    setActionError(null);

    try {
      await claimBookingRequest(bookingRequestId);
      setReloadVersion((version) => version + 1);
    } catch {
      setActionError("La prise en charge a échoué. Veuillez réessayer.");
    } finally {
      setClaiming(false);
    }
  }

  async function handleSearchOffers() {
    setSearchingOffers(true);
    setActionError(null);
    setOffers([]);
    setSelectedOffer(null);

    try {
      setOffers(await searchBookingRequestOffers(bookingRequestId));
    } catch {
      setActionError("La recherche d’offres a échoué. Veuillez réessayer.");
    } finally {
      setSearchingOffers(false);
    }
  }

  function handleSelectOffer(offer: ProviderOffer) {
    setSelectedOffer(offer);
    setQuoteDescription(getOfferDescription(offer));
    setQuoteError(null);
  }

  async function handleCreateQuote(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedOffer) return;

    if (!quoteDescription.trim()) {
      setQuoteError("La description est obligatoire.");
      return;
    }

    setCreatingQuote(true);
    setQuoteError(null);

    try {
      const quote = await createQuote(bookingRequestId, {
        provider: selectedOffer.provider,
        externalOfferId: selectedOffer.externalId,
        providerPrice: selectedOffer.price,
        assistanceFee: 0,
        currency: selectedOffer.currency,
        description: quoteDescription.trim(),
        expiresAt: null,
      });
      setAgentQuotes((currentQuotes) => [toAgentQuoteResponse(quote), ...currentQuotes.filter((currentQuote) => currentQuote.id !== quote.id)]);
      setSelectedOffer(null);
    } catch {
      setQuoteError("La création de la proposition a échoué. Veuillez réessayer.");
    } finally {
      setCreatingQuote(false);
    }
  }

  async function handleCreateBooking() {
    if (!acceptedQuote || creatingBooking) return;

    setCreatingBooking(true);
    setBookingError(null);

    try {
      setBooking(await createBooking(acceptedQuote.id));
    } catch {
      setBookingError("La réservation n’a pas pu être créée. Veuillez réessayer.");
    } finally {
      setCreatingBooking(false);
    }
  }

  const handleBookingSaved = (updatedBooking: Booking) => {
    setBooking(updatedBooking);
  };

  if (hasInvalidId) {
    return (
      <div className="page-stack">
        <Link className="back-link" to="/agent">
          <ArrowLeft size={17} />
          Retour au dashboard
        </Link>
        <div className="state-panel error-panel" role="alert">
          <strong>L’identifiant de la demande est invalide.</strong>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="state-panel" role="status">
        <span className="spinner" aria-hidden="true" />
        <strong>Chargement de la demande…</strong>
        <p>Nous récupérons les informations du voyage.</p>
      </div>
    );
  }

  if (error || !bookingRequest) {
    return (
      <div className="page-stack">
        <Link className="back-link" to="/agent">
          <ArrowLeft size={17} />
          Retour au dashboard
        </Link>
        <div className="state-panel error-panel" role="alert">
          <RefreshCw size={24} aria-hidden="true" />
          <strong>{error ?? "Demande introuvable."}</strong>
          <button type="button" className="secondary-button" onClick={() => setReloadVersion((version) => version + 1)}>
            <RefreshCw size={16} />
            Réessayer
          </button>
        </div>
      </div>
    );
  }

  const { need, traveler, trip } = bookingRequest;
  const canClaim = bookingRequest.status === "REQUESTED" && bookingRequest.assignedAgentId === null;
  const canSearchOffers = bookingRequest.status === "IN_PROGRESS" && bookingRequest.assignedAgentId !== null;
  return (
    <div className="page-stack booking-request-page">
      <Link className="back-link" to="/agent/booking-requests">
        <ArrowLeft size={17} />
        Retour aux demandes
      </Link>
      <section className="page-heading compact">
        <div>
          <span className="eyebrow">Détail de la demande</span>
          <div className="title-with-status">
            <h1>Demande #{bookingRequest.id}</h1>
            <span className={`request-status status-${bookingRequest.status.toLowerCase()}`}>{statusLabels[bookingRequest.status]}</span>
          </div>
          {bookingRequest.notes && <p>{bookingRequest.notes}</p>}
        </div>
      </section>

      <div className="detail-grid">
        <section className="detail-card">
          <div className="detail-card-heading">
            <CircleUserRound size={20} />
            <h2>Client</h2>
          </div>
          <dl className="detail-list">
            <div>
              <dt>Prénom</dt>
              <dd>{traveler.firstName}</dd>
            </div>
            <div>
              <dt>
                <Mail size={15} /> Email
              </dt>
              <dd>{traveler.email}</dd>
            </div>
          </dl>
        </section>

        <section className="detail-card">
          <div className="detail-card-heading">
            <CalendarDays size={20} />
            <h2>Voyage</h2>
          </div>
          <dl className="detail-list">
            {trip.title.trim() && (
              <div>
                <dt>Titre</dt>
                <dd>{trip.title}</dd>
              </div>
            )}
            <div>
              <dt>Début</dt>
              <dd>{formatDate(trip.startDate)}</dd>
            </div>
            <div>
              <dt>Fin</dt>
              <dd>{formatDate(trip.endDate)}</dd>
            </div>
          </dl>
        </section>

        <section className="detail-card detail-card-wide">
          <div className="detail-card-heading">
            {need.type === "FLIGHT" ? <Plane size={20} /> : <BedDouble size={20} />}
            <h2>Besoin</h2>
          </div>
          <dl className="detail-list">
            <div>
              <dt>Type</dt>
              <dd>{needTypeLabels[need.type]}</dd>
            </div>
            {need.notes && (
              <div>
                <dt>Notes</dt>
                <dd>{need.notes}</dd>
              </div>
            )}
          </dl>
        </section>
      </div>

      {need.type === "ACCOMMODATION" && need.accommodationCriteria && (
        <section className="criteria-card">
          <div className="detail-card-heading">
            <Hotel size={20} />
            <h2>Hébergement</h2>
          </div>
          <div className="criteria-grid">
            <div>
              <MapPin size={18} />
              <span>Ville</span>
              <strong>{need.accommodationCriteria.city}</strong>
            </div>
            <div>
              <Users size={18} />
              <span>Voyageurs</span>
              <strong>{need.accommodationCriteria.travelers}</strong>
            </div>
            <div>
              <BedDouble size={18} />
              <span>Chambres</span>
              <strong>{need.accommodationCriteria.rooms}</strong>
            </div>
          </div>
        </section>
      )}

      {need.type === "FLIGHT" && need.flightCriteria && (
        <section className="criteria-card">
          <div className="detail-card-heading">
            <Plane size={20} />
            <h2>Vol</h2>
          </div>
          <div className="flight-route">
            <strong>{need.flightCriteria.origin}</strong>
            <ArrowRight size={21} />
            <strong>{need.flightCriteria.destination}</strong>
          </div>
          <div className="travelers-line">
            <Users size={17} /> Voyageurs : {need.flightCriteria.travelers}
          </div>
        </section>
      )}

      {need.type === "TRANSFER" && need.transferCriteria && (
        <section className="criteria-card">
          <div className="detail-card-heading">
            <Car size={20} />
            <h2>Transfert</h2>
          </div>
          <div className="criteria-grid">
            <div>
              <MapPin size={18} />
              <span>Lieu de départ</span>
              <strong>{need.transferCriteria.pickupLocation}</strong>
            </div>
            <div>
              <MapPin size={18} />
              <span>Lieu d'arrivée</span>
              <strong>{need.transferCriteria.dropoffLocation}</strong>
            </div>
            <div>
              <Users size={18} />
              <span>Voyageurs</span>
              <strong>{need.transferCriteria.travelers}</strong>
            </div>
          </div>
        </section>
      )}

      <section className="request-actions" aria-label="Actions sur la demande">
        {canClaim && (
          <button type="button" className="primary-button" onClick={handleClaim} disabled={claiming}>
            {claiming ? <LoaderCircle className="rotating" size={18} /> : <Check size={18} />}
            {claiming ? "Prise en charge…" : "Prendre en charge"}
          </button>
        )}

        {bookingRequest.assignedAgentId !== null && (
          <p className="assignment-message">
            <Check size={17} />
            Demande prise en charge par l’agent #{bookingRequest.assignedAgentId}
          </p>
        )}

        {canSearchOffers && (
          <button type="button" className="primary-button" onClick={handleSearchOffers} disabled={searchingOffers}>
            {searchingOffers ? <LoaderCircle className="rotating" size={18} /> : <Search size={18} />}
            {searchingOffers ? "Recherche des offres..." : "Rechercher les offres"}
          </button>
        )}

        {actionError && (
          <p className="action-error" role="alert">
            {actionError}
          </p>
        )}
      </section>

      {!searchingOffers && offers.length > 0 && (
        <section aria-labelledby="offers-title">
          <div className="section-heading">
            <div>
              <h2 id="offers-title">Offres disponibles</h2>
              <p>
                {offers.length} offre{offers.length > 1 ? "s" : ""} trouvée{offers.length > 1 ? "s" : ""}
              </p>
            </div>
          </div>
          <div className="offers-grid">
            {offers.map((offer) => {
              const selected = selectedOffer?.externalId === offer.externalId;

              return (
                <article className={`offer-card${selected ? " selected" : ""}`} key={`${offer.provider}-${offer.externalId}`}>
                  {selected && (
                    <span className="selected-label">
                      <Check size={14} /> Offre sélectionnée
                    </span>
                  )}
                  {isAccommodationOffer(offer) ? (
                    <>
                      <Hotel size={22} className="offer-icon" />
                      <h3>{offer.hotelName}</h3>
                      <p className="offer-location">
                        <MapPin size={15} />
                        {offer.city}
                      </p>
                      <p>{offer.roomType}</p>
                      <p className="offer-dates">
                        {formatDate(offer.checkIn)} <ArrowRight size={15} /> {formatDate(offer.checkOut)}
                      </p>
                    </>
                  ) : isTransferOffer(offer) ? (
                    <>
                      <Car size={22} className="offer-icon" />
                      <h3>{offer.vehicleType}</h3>
                      <p className="offer-route">
                        {offer.pickupLocation} <ArrowRight size={17} /> {offer.dropoffLocation}
                      </p>
                      <p>Voyageurs : {offer.travelers}</p>
                    </>
                  ) : (
                    <>
                      <Plane size={22} className="offer-icon" />
                      <h3>{offer.airline}</h3>
                      <p className="offer-route">
                        {offer.origin} <ArrowRight size={17} /> {offer.destination}
                      </p>
                      <p className="offer-dates stacked">
                        Départ : {formatDateTime(offer.departure)}
                        <br />
                        Arrivée : {formatDateTime(offer.arrival)}
                      </p>
                    </>
                  )}
                  <strong className="offer-price">{formatPrice(offer.price, offer.currency)}</strong>
                  <span className="provider-name">Provider : {offer.provider}</span>
                  <button type="button" className={selected ? "selected-button" : "offer-button"} onClick={() => handleSelectOffer(offer)}>
                    {selected ? (
                      <>
                        <Check size={16} /> Offre retenue
                      </>
                    ) : (
                      "Retenir cette offre"
                    )}
                  </button>
                </article>
              );
            })}
          </div>
        </section>
      )}

      {selectedOffer && (
        <section className="quote-creation-card" aria-labelledby="quote-creation-title">
          <div className="detail-card-heading">
            <FileCheck2 size={20} />
            <h2 id="quote-creation-title">Préparer cette offre</h2>
          </div>
          <form className="quote-form" onSubmit={handleCreateQuote}>
            <div className="quote-price-summary">
              <span>Prix fournisseur</span>
              <strong>{formatPrice(selectedOffer.price, selectedOffer.currency)}</strong>
            </div>
            <label className="form-field form-field-wide">
              <span>Description</span>
              <input type="text" value={quoteDescription} onChange={(event) => setQuoteDescription(event.target.value)} required />
            </label>
            {quoteError && (
              <p className="quote-error" role="alert">
                {quoteError}
              </p>
            )}
            <button type="submit" className="primary-button quote-submit" disabled={creatingQuote}>
              {creatingQuote ? <LoaderCircle className="rotating" size={18} /> : <FileCheck2 size={18} />}
              {creatingQuote ? "Enregistrement…" : "Enregistrer l’offre"}
            </button>
          </form>
        </section>
      )}

      {agentQuotes.length > 0 && (
        <section className="agent-quotes-section" aria-labelledby="agent-quotes-title">
          <div className="section-heading">
            <div>
              <h2 id="agent-quotes-title">Propositions du dossier</h2>
              <p>
                {agentQuotes.length} proposition{agentQuotes.length > 1 ? "s" : ""}
              </p>
            </div>
          </div>
          <div className="agent-quotes-list">
            {agentQuotes.map((quote) => (
              <article className="quote-success-card" key={quote.id}>
                <div className="quote-success-heading">
                  <span className="quote-success-icon">
                    <FileCheck2 size={20} />
                  </span>
                  <div>
                    <span className="eyebrow">Proposition #{quote.id}</span>
                    <h3>{quote.provider}</h3>
                  </div>
                  <span className="quote-status">{quote.status}</span>
                </div>
                <div className="quote-result-grid">
                  <div>
                    <span>Prix fournisseur</span>
                    <strong>{formatPrice(quote.providerPrice, quote.currency)}</strong>
                  </div>
                  <div className="quote-description">
                    <span>Description</span>
                    <strong>{quote.description}</strong>
                  </div>
                </div>
                {quote.status === "SENT" && (
                  <p className="quote-sent-confirmation">
                    <Check size={18} /> Proposition envoyée au client
                  </p>
                )}
              </article>
            ))}
          </div>
        </section>
      )}

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
    </div>
  );
}

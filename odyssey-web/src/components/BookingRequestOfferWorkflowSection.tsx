import { useState, type FormEvent } from "react";
import { ArrowRight, Check, FileCheck2, Hotel, LoaderCircle, MapPin, Plane, Search, Car } from "lucide-react";
import { searchBookingRequestOffers } from "../api/bookingRequests";
import { createQuote } from "../api/quotes";
import type { AgentQuoteResponse, QuoteResponse } from "../types/quote";
import { isAccommodationOffer, isTransferOffer, type ProviderOffer } from "../types/providerOffer";

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

export interface BookingRequestOfferWorkflowSectionProps {
  bookingRequestId: number;
  canSearchOffers: boolean;
  currentQuote: AgentQuoteResponse | null;
  onQuoteCreated: (quote: QuoteResponse) => void;
}

export function BookingRequestOfferWorkflowSection({
  bookingRequestId,
  canSearchOffers,
  currentQuote,
  onQuoteCreated,
}: BookingRequestOfferWorkflowSectionProps) {
  const [offers, setOffers] = useState<ProviderOffer[]>([]);
  const [searchingOffers, setSearchingOffers] = useState(false);
  const [selectedOffer, setSelectedOffer] = useState<ProviderOffer | null>(null);
  const [quoteDescription, setQuoteDescription] = useState("");
  const [creatingQuote, setCreatingQuote] = useState(false);
  const [quoteError, setQuoteError] = useState<string | null>(null);
  const [searchError, setSearchError] = useState<string | null>(null);

  async function handleSearchOffers() {
    setSearchingOffers(true);
    setSearchError(null);
    setOffers([]);
    setSelectedOffer(null);
    setQuoteDescription("");

    try {
      setOffers(await searchBookingRequestOffers(bookingRequestId));
    } catch {
      setSearchError("La recherche d’offres a échoué. Veuillez réessayer.");
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

      onQuoteCreated(quote);
      setSelectedOffer(null);
      setQuoteDescription("");
    } catch {
      setQuoteError("La création de la proposition a échoué. Veuillez réessayer.");
    } finally {
      setCreatingQuote(false);
    }
  }

  return (
    <>
      {currentQuote && currentQuote.status === "REJECTED" && (
        <section className="quote-creation-card" aria-labelledby="rejected-quote-title">
          <div className="detail-card-heading">
            <FileCheck2 size={20} />
            <h2 id="rejected-quote-title">Proposition refusée</h2>
          </div>
          <div className="quote-result-grid">
            <div>
              <span>Fournisseur</span>
              <strong>{currentQuote.provider}</strong>
            </div>
            <div>
              <span>Prix fournisseur</span>
              <strong>{formatPrice(currentQuote.providerPrice, currentQuote.currency)}</strong>
            </div>
            <div className="quote-description">
              <span>Description</span>
              <strong>{currentQuote.description}</strong>
            </div>
          </div>
          <p className="quote-sent-confirmation" style={{ marginTop: "1rem" }}>
            <Check size={18} /> Le voyageur a refusé cette proposition.
          </p>
          {canSearchOffers && (
            <button type="button" className="primary-button quote-submit" onClick={handleSearchOffers} disabled={searchingOffers}>
              {searchingOffers ? <LoaderCircle className="rotating" size={18} /> : <Search size={18} />}
              {searchingOffers ? "Recherche des offres…" : "Proposer une autre solution"}
            </button>
          )}
        </section>
      )}

      {canSearchOffers && (
        <section className="request-actions" aria-label="Actions sur la demande">
          <button type="button" className="primary-button" onClick={handleSearchOffers} disabled={searchingOffers}>
            {searchingOffers ? <LoaderCircle className="rotating" size={18} /> : <Search size={18} />}
            {searchingOffers ? "Recherche des offres..." : "Rechercher les offres"}
          </button>
          {searchError && (
            <p className="action-error" role="alert">
              {searchError}
            </p>
          )}
        </section>
      )}

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
    </>
  );
}

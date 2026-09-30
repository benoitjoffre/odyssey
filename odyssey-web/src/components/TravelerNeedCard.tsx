import type { ReactNode } from "react";
import { AlertCircle, AlertTriangle, Check, ChevronRight, Circle, Clock3, CreditCard, LoaderCircle, Minus, Send, X } from "lucide-react";
import { SUPPLIER_PAYMENT_CTA_LABEL, SUPPLIER_PAYMENT_DISCLAIMER } from "../helpers/supplierPayment";
import type { TravelerNeedUxState } from "../helpers/travelerNeedState";
import { getNeedStatusLabel, getNeedVisualAsset, getNeedVisualLabel } from "../helpers/travelerNeedPresentation";
import type { TripNeed } from "../types/trip";
import type { TravelerQuote } from "../types/travelerQuote";

type TravelerNeedTone = "neutral" | "progress" | "action" | "success" | "error" | "muted";

interface TravelerNeedStateContent {
  tone: TravelerNeedTone;
  title: string;
  message: string;
  secondaryMessage?: string;
}

const toneIcons: Record<TravelerNeedTone, ReactNode> = {
  neutral: <Circle size={16} />,
  progress: <Clock3 size={16} />,
  action: <AlertCircle size={16} />,
  success: <Check size={16} />,
  error: <AlertTriangle size={16} />,
  muted: <Minus size={16} />,
};

const travelerNeedStateContent: Record<TravelerNeedUxState, TravelerNeedStateContent> = {
  TO_ORGANIZE: {
    tone: "neutral",
    title: "À organiser",
    message: "Envoyez votre demande pour qu'un agent Odyssey s'en occupe.",
  },
  REQUEST_SENT: {
    tone: "progress",
    title: "Demande envoyée",
    message: "Votre demande a bien été transmise. Un agent va bientôt s'en occuper.",
  },
  SEARCH_IN_PROGRESS: {
    tone: "progress",
    title: "Recherche en cours",
    message: "Votre agent recherche une solution adaptée à votre voyage.",
    secondaryMessage: "Vous n'avez rien à faire pour le moment.",
  },
  PROPOSAL_READY: {
    tone: "action",
    title: "Une proposition vous attend",
    message: "Votre agent a trouvé une offre pour ce service.",
  },
  PROPOSAL_EXPIRED: {
    tone: "muted",
    title: "Proposition expirée",
    message: "Cette offre n'est plus disponible. Votre agent va rechercher une nouvelle solution.",
  },
  PROPOSAL_REJECTED: {
    tone: "muted",
    title: "Proposition refusée",
    message: "Vous avez refusé cette offre. Votre agent va rechercher une nouvelle solution.",
  },
  PROPOSAL_ACCEPTED_AGENT_PROCESSING: {
    tone: "progress",
    title: "Proposition acceptée",
    message: "Votre conseiller prépare maintenant votre réservation auprès du fournisseur.",
  },
  AGENT_FINALIZING: {
    tone: "progress",
    title: "Votre agent finalise la réservation",
    message: "Odyssey s'occupe maintenant de la réservation auprès du fournisseur.",
    secondaryMessage: "Vous n'avez rien à faire pour le moment.",
  },
  SUPPLIER_PAYMENT_REQUIRED: {
    tone: "action",
    title: "Paiement fournisseur requis",
    message: "Le fournisseur demande maintenant votre règlement pour finaliser cette réservation.",
  },
  SUPPLIER_PAYMENT_DONE_WAITING_CONFIRMATION: {
    tone: "progress",
    title: "Paiement effectué",
    message: "Votre agent finalise maintenant la confirmation avec le fournisseur.",
  },
  BOOKING_CONFIRMED: {
    tone: "success",
    title: "Réservation confirmée",
    message: "Votre réservation auprès du fournisseur est confirmée.",
  },
  BOOKING_CONFIRMED_SUPPLIER_PAYMENT_REQUIRED: {
    tone: "action",
    title: "Paiement fournisseur à finaliser",
    message: "La réservation est confirmée auprès du fournisseur. Il reste à effectuer le règlement.",
  },
  BOOKING_FAILED: {
    tone: "error",
    title: "Un problème est survenu",
    message: "Votre agent s'occupe de résoudre ce point et vous recontactera si nécessaire.",
  },
  CANCELLED: {
    tone: "muted",
    title: "Service annulé",
    message: "Ce service a été annulé pour ce voyage.",
  },
};

function formatPrice(price: number, currency: string) {
  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency,
    maximumFractionDigits: 2,
  }).format(price);
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("fr-FR", {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(new Date(value));
}

export interface TravelerNeedCardProps {
  need: TripNeed;
  icon: ReactNode;
  label: string;
  /** Single derived UX state for this Need — see helpers/travelerNeedState.ts. */
  derivedState: TravelerNeedUxState;
  /** Latest Quote for this Need's BookingRequest, regardless of status. */
  quote: TravelerQuote | null;
  /** Accepted Quote for this Need's BookingRequest, if any (settled price). */
  acceptedQuote: TravelerQuote | null;
  onSendRequest: () => void;
  sendingRequest: boolean;
  sendRequestDisabled: boolean;
  sendRequestError?: string;
  onAcceptQuote: (quote: TravelerQuote) => void;
  onRejectQuote: (quote: TravelerQuote) => void;
  quoteActionPending: "accept" | "reject" | null;
  quoteActionsDisabled: boolean;
  quoteActionError?: string;
}

export function TravelerNeedCard({
  need,
  icon,
  label,
  derivedState,
  quote,
  acceptedQuote,
  onSendRequest,
  sendingRequest,
  sendRequestDisabled,
  sendRequestError,
  onAcceptQuote,
  onRejectQuote,
  quoteActionPending,
  quoteActionsDisabled,
  quoteActionError,
}: TravelerNeedCardProps) {
  const content = travelerNeedStateContent[derivedState];
  const visualAsset = getNeedVisualAsset(need.type);
  const visualLabel = getNeedVisualLabel(need.type);
  const statusLabel = getNeedStatusLabel(derivedState);
  const showsSupplierPaymentCta =
    (derivedState === "SUPPLIER_PAYMENT_REQUIRED" || derivedState === "BOOKING_CONFIRMED_SUPPLIER_PAYMENT_REQUIRED") &&
    Boolean(quote?.providerPaymentUrl);
  const showsSupplierPaymentLinkPendingMessage =
    (derivedState === "SUPPLIER_PAYMENT_REQUIRED" || derivedState === "BOOKING_CONFIRMED_SUPPLIER_PAYMENT_REQUIRED") && !quote?.providerPaymentUrl;
  const showsBookingReference =
    (derivedState === "BOOKING_CONFIRMED" || derivedState === "BOOKING_CONFIRMED_SUPPLIER_PAYMENT_REQUIRED") && Boolean(need.providerConfirmationId);

  const priceRow = acceptedQuote
    ? { label: "Offre retenue", value: formatPrice(acceptedQuote.providerPrice, acceptedQuote.currency) }
    : derivedState === "PROPOSAL_READY" && quote
      ? { label: "Prix proposé", value: formatPrice(quote.providerPrice, quote.currency) }
      : { label: "Offre retenue", value: "Offre à définir" };

  return (
    <article className={`traveler-need-card traveler-need-card--${content.tone}`} id={`traveler-need-${need.id}`}>
      <div className="traveler-need-card__media">
        <img src={visualAsset} alt={visualLabel} loading="lazy" />
        <span className={`traveler-need-card__status-pill traveler-need-card__status-pill--${content.tone}`}>{statusLabel}</span>
      </div>

      <div className="traveler-need-card__body">
        <div className="traveler-need-card__heading">
          <span className="traveler-need-card__icon" aria-hidden="true">
            {icon}
          </span>
          <h3>{label}</h3>
        </div>

        {need.notes && <p className="traveler-need-card__summary">{need.notes}</p>}

        {need.type === "TRANSFER" && need.transferCriteria && (
          <p className="traveler-need-card__summary">
            {need.transferCriteria.pickupLocation} → {need.transferCriteria.dropoffLocation}
            <br />
            {need.transferCriteria.travelers} {need.transferCriteria.travelers > 1 ? "voyageurs" : "voyageur"}
          </p>
        )}

        <div className={`traveler-need-card__status traveler-need-card__status--${content.tone}`}>
          <span className="traveler-need-card__status-icon" aria-hidden="true">
            {toneIcons[content.tone]}
          </span>
          <div className="traveler-need-card__status-copy">
            <strong>{content.title}</strong>
            <p>{content.message}</p>
            {derivedState === "PROPOSAL_READY" && quote?.expiresAt && (
              <p className="traveler-need-card__status-secondary">Offre valable jusqu'au {formatDate(quote.expiresAt)}</p>
            )}
            {content.secondaryMessage && <p className="traveler-need-card__status-secondary">{content.secondaryMessage}</p>}
          </div>
        </div>

        {showsSupplierPaymentCta && quote?.providerPaymentUrl && (
          <div className="traveler-need-card__action-block">
            <a className="primary-button traveler-need-card__action" href={quote.providerPaymentUrl} target="_blank" rel="noopener noreferrer">
              <CreditCard size={18} /> {SUPPLIER_PAYMENT_CTA_LABEL}
            </a>
            <p className="traveler-provider-payment-hint">{SUPPLIER_PAYMENT_DISCLAIMER}</p>
          </div>
        )}

        {showsSupplierPaymentLinkPendingMessage && (
          <p className="traveler-provider-payment-hint">
            Le paiement auprès du fournisseur est nécessaire. Votre conseiller prépare actuellement le lien de paiement.
          </p>
        )}

        {showsBookingReference && (
          <div className="traveler-booking-reference">
            <span>Référence de réservation</span>
            <strong>{need.providerConfirmationId}</strong>
          </div>
        )}

        {derivedState === "TO_ORGANIZE" && (
          <div className="traveler-need-card__action-block">
            {sendRequestError && (
              <p role="alert" className="traveler-need-form-error">
                {sendRequestError}
              </p>
            )}
            <button type="button" className="primary-button traveler-need-card__action" disabled={sendRequestDisabled} onClick={onSendRequest}>
              {sendingRequest ? <LoaderCircle className="rotating" size={17} /> : <Send size={17} />}
              {sendingRequest ? "Envoi en cours…" : "Envoyer ma demande"}
            </button>
          </div>
        )}

        {derivedState === "PROPOSAL_READY" && quote && (
          <div className="traveler-quote-actions traveler-need-card__quote-actions">
            {quoteActionError && (
              <p className="traveler-action-error" role="alert">
                {quoteActionError}
              </p>
            )}
            <button
              type="button"
              className="primary-button traveler-need-card__action"
              disabled={quoteActionsDisabled}
              onClick={() => onAcceptQuote(quote)}
            >
              {quoteActionPending === "accept" ? <LoaderCircle className="rotating" size={18} /> : <Check size={18} />}
              {quoteActionPending === "accept" ? "Acceptation…" : "Accepter la proposition"}
            </button>
            <button
              type="button"
              className="traveler-reject-button traveler-need-card__secondary-action"
              disabled={quoteActionsDisabled}
              onClick={() => onRejectQuote(quote)}
            >
              {quoteActionPending === "reject" ? <LoaderCircle className="rotating" size={18} /> : <X size={18} />}
              {quoteActionPending === "reject" ? "Refus…" : "Refuser"}
            </button>
          </div>
        )}

        <div className="traveler-need-card__footer">
          <div className="traveler-need-card__price">
            <span>{priceRow.label}</span>
            <strong>{priceRow.value}</strong>
          </div>
          <span className="traveler-need-card__chevron" aria-hidden="true">
            <ChevronRight size={20} strokeWidth={2.25} />
          </span>
        </div>
      </div>
    </article>
  );
}

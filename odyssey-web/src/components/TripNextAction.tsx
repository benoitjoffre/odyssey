import type { ReactNode } from "react";
import { AlertCircle, AlertTriangle, Circle, Clock3 } from "lucide-react";
import type { TravelerTripNextActionKind } from "../helpers/travelerTripState";

type TripNextActionTone = "action" | "progress" | "neutral" | "critical";

interface TripNextActionContent {
  tone: TripNextActionTone;
  eyebrow: string;
  title: string;
  message: string;
  secondaryMessage?: string;
  ctaLabel?: string;
}

const toneIcons: Record<TripNextActionTone, ReactNode> = {
  neutral: <Circle size={31} strokeWidth={2.2} />,
  progress: <Clock3 size={31} strokeWidth={2.2} />,
  action: <AlertCircle size={31} strokeWidth={2.2} />,
  critical: <AlertTriangle size={31} strokeWidth={2.2} />,
};

function getNextActionContent(action: TravelerTripNextActionKind, count: number): TripNextActionContent {
  const isPlural = count > 1;

  switch (action) {
    case "PROPOSALS_READY":
      return {
        tone: "action",
        eyebrow: "Action nécessaire",
        title: isPlural ? `${count} propositions vous attendent` : "Une proposition vous attend",
        message: "Votre agent a trouvé une offre pour un service de votre voyage. Consultez-la pour avancer.",
        ctaLabel: isPlural ? "Voir les propositions" : "Voir la proposition",
      };
    case "SUPPLIER_PAYMENTS_REQUIRED":
      return {
        tone: "action",
        eyebrow: "Action nécessaire",
        title: isPlural ? "Des paiements fournisseurs sont requis" : "Un paiement fournisseur est requis",
        message: "Un fournisseur attend votre règlement direct pour finaliser une réservation.",
        ctaLabel: isPlural ? "Voir les paiements" : "Voir le paiement fournisseur",
      };
    case "ODYSSEY_PAYMENT_REQUIRED":
      return {
        tone: "action",
        eyebrow: "Action nécessaire",
        title: "Régler les frais d’accompagnement",
        message: "Vos réservations sont finalisées. Vous pouvez maintenant régler les frais d’accompagnement Odyssey.",
        ctaLabel: "Voir le paiement Odyssey",
      };
    case "ODYSSEY_PAYMENT_PENDING":
      return {
        tone: "progress",
        eyebrow: "Prochaine étape",
        title: "Paiement Odyssey en cours",
        message: "Votre paiement des frais d’accompagnement a été initié. Vous pouvez le reprendre depuis la section paiement ci-dessous.",
      };
    case "NEEDS_TO_ORGANIZE":
      return {
        tone: "action",
        eyebrow: "Action nécessaire",
        title: isPlural ? `${count} services restent à organiser` : "Un service reste à organiser",
        message: "Envoyez votre demande pour qu'un agent Odyssey s'en occupe.",
        ctaLabel: isPlural ? "Voir les services à organiser" : "Organiser ce service",
      };
    case "BOOKING_FAILED":
      return {
        tone: "critical",
        eyebrow: "Prochaine étape",
        title: "Votre agent s'occupe d'un problème avec un service",
        message: "Rien à faire de votre côté pour le moment : votre agent vous recontactera si nécessaire.",
      };
    case "CLIENT_REJECTED":
      return {
        tone: "neutral",
        eyebrow: "Refus du client",
        title: isPlural ? `${count} services ont été refusés` : "Vous avez refusé ce service",
        message: "Vous avez refusé cette proposition. Votre agent va chercher une nouvelle solution adaptée.",
        secondaryMessage: "Ce service n'est pas en cours : il a été refusé par le client.",
      };
    case "AGENT_WORKING":
      return {
        tone: "progress",
        eyebrow: "Prochaine étape",
        title: "Votre agent s'occupe de votre voyage",
        message: "Vous n'avez rien à faire pour le moment.",
        secondaryMessage: "Nous vous préviendrons dès qu'une mise à jour sera disponible.",
      };
    case "NO_ACTIVE_NEEDS":
      return {
        tone: "neutral",
        eyebrow: "Prochaine étape",
        title: "Commencez à organiser votre voyage",
        message: "Ajoutez un vol, un hébergement ou un transfert pour que votre agent Odyssey puisse s'en occuper.",
        ctaLabel: "Ajouter un service",
      };
  }
}

export interface TripNextActionProps {
  action: TravelerTripNextActionKind;
  count: number;
  onCtaClick: (() => void) | null;
}

export function TripNextAction({ action, count, onCtaClick }: TripNextActionProps) {
  const content = getNextActionContent(action, count);

  return (
    <section className={`trip-next-action trip-next-action--${content.tone}`} aria-labelledby="trip-next-action-title">
      <span className="trip-next-action-ornament" aria-hidden="true" />
      <span className="trip-next-action-icon" aria-hidden="true">
        {toneIcons[content.tone]}
      </span>
      <div className="trip-next-action-copy">
        <span className="trip-next-action-eyebrow">{content.eyebrow}</span>
        <strong id="trip-next-action-title">{content.title}</strong>
        <p>{content.message}</p>
        {content.secondaryMessage && <p className="trip-next-action-secondary">{content.secondaryMessage}</p>}
      </div>
      {content.ctaLabel && onCtaClick && (
        <button type="button" className="primary-button trip-next-action-cta" onClick={onCtaClick}>
          {content.ctaLabel}
        </button>
      )}
    </section>
  );
}

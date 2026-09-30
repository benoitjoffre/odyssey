import accomodationImage from "../assets/accomodation.png";
import flightImage from "../assets/flight.png";
import transferImage from "../assets/transfer.png";
import carImage from "../assets/car.png";
import busImage from "../assets/bus.png";
import type { TripNeed } from "../types/trip";
import type { TravelerNeedUxState } from "./travelerNeedState";

const needStatusLabels: Record<TravelerNeedUxState, string> = {
  TO_ORGANIZE: "À organiser",
  REQUEST_SENT: "Demande envoyée",
  SEARCH_IN_PROGRESS: "Recherche en cours",
  PROPOSAL_READY: "Proposition prête",
  PROPOSAL_EXPIRED: "Proposition expirée",
  PROPOSAL_REJECTED: "Proposition refusée",
  PROPOSAL_ACCEPTED_AGENT_PROCESSING: "Proposition acceptée",
  AGENT_FINALIZING: "Réservation en cours",
  SUPPLIER_PAYMENT_REQUIRED: "Paiement requis",
  SUPPLIER_PAYMENT_DONE_WAITING_CONFIRMATION: "Paiement effectué",
  BOOKING_CONFIRMED: "Réservé",
  BOOKING_CONFIRMED_SUPPLIER_PAYMENT_REQUIRED: "Réservé, paiement à finaliser",
  BOOKING_FAILED: "Réservation en échec",
  CANCELLED: "Annulé",
};

const needVisualAssets: Record<TripNeed["type"], { src: string; label: string }> = {
  ACCOMMODATION: { src: accomodationImage, label: "Illustration d'hébergement" },
  FLIGHT: { src: flightImage, label: "Illustration de vol" },
  TRANSFER: { src: transferImage, label: "Illustration de transfert" },
  CAR: { src: carImage, label: "Illustration de véhicule" },
  BUS: { src: busImage, label: "Illustration de transport" },
};

export function getNeedStatusLabel(state: TravelerNeedUxState): string {
  return needStatusLabels[state];
}

export function getNeedVisualAsset(type: TripNeed["type"]): string {
  return needVisualAssets[type].src;
}

export function getNeedVisualLabel(type: TripNeed["type"]): string {
  return needVisualAssets[type].label;
}

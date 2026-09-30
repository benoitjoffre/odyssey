import heroImageFallback from "../assets/hero.png";
import type { TravelEvent } from "../types/travelEvent";
import type { TripStatus } from "../types/trip";

export const tripStatusLabels: Record<TripStatus, string> = {
  DRAFT: "En préparation",
  CONFIRMED: "Confirmé",
  CANCELLED: "Annulé",
};

export function parseLocalDate(value: string) {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day);
}

export function startOfToday() {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate());
}

function toUtcDayNumber(value: Date) {
  return Date.UTC(value.getFullYear(), value.getMonth(), value.getDate()) / 86400000;
}

export function computeDaysUntil(startDate: string) {
  const tripStart = parseLocalDate(startDate);
  const today = startOfToday();
  return toUtcDayNumber(tripStart) - toUtcDayNumber(today);
}

export function formatDateCompact(value: string) {
  return new Intl.DateTimeFormat("fr-FR", {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(parseLocalDate(value));
}

export function formatDateRange(startDate: string, endDate: string) {
  const start = parseLocalDate(startDate);
  const end = parseLocalDate(endDate);

  if (start.getFullYear() === end.getFullYear() && start.getMonth() === end.getMonth()) {
    const monthYear = new Intl.DateTimeFormat("fr-FR", {
      month: "long",
      year: "numeric",
    }).format(end);
    return `${start.getDate()} → ${end.getDate()} ${monthYear}`;
  }

  return `${formatDateCompact(startDate)} → ${formatDateCompact(endDate)}`;
}

export function formatDateBadge(startDate: string, endDate: string) {
  const start = parseLocalDate(startDate);
  const end = parseLocalDate(endDate);
  const monthFormatter = new Intl.DateTimeFormat("fr-FR", { month: "short" });
  const startMonth = monthFormatter.format(start).replace(".", "").toUpperCase();
  const endMonth = monthFormatter.format(end).replace(".", "").toUpperCase();

  if (start.getFullYear() === end.getFullYear() && start.getMonth() === end.getMonth()) {
    return `${start.getDate()} → ${end.getDate()} ${endMonth}. ${end.getFullYear()}`;
  }

  return `${start.getDate()} ${startMonth}. → ${end.getDate()} ${endMonth}. ${end.getFullYear()}`;
}

export function getTravelEventImage(event: TravelEvent | null) {
  const imageUrl = (event as (TravelEvent & { imageUrl?: string | null }) | null)?.imageUrl;
  if (typeof imageUrl === "string" && imageUrl.trim().length > 0) {
    return imageUrl.trim();
  }
  return heroImageFallback;
}

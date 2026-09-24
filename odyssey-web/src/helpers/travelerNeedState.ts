import type { TripNeed } from "../types/trip";
import type { TravelerQuote } from "../types/travelerQuote";

/**
 * Single traveler-facing state for a trip Need. Every backend combination of
 * Need / BookingRequest / Quote / Booking statuses must resolve to exactly one
 * of these values. Rendering (label, tone, icon, CTA) is derived from this
 * value alone — no screen should re-inspect the raw backend statuses.
 */
export type TravelerNeedUxState =
  | "TO_ORGANIZE"
  | "REQUEST_SENT"
  | "SEARCH_IN_PROGRESS"
  | "PROPOSAL_READY"
  | "PROPOSAL_EXPIRED"
  | "PROPOSAL_REJECTED"
  | "PROPOSAL_ACCEPTED_AGENT_PROCESSING"
  | "AGENT_FINALIZING"
  | "SUPPLIER_PAYMENT_REQUIRED"
  | "SUPPLIER_PAYMENT_DONE_WAITING_CONFIRMATION"
  | "BOOKING_CONFIRMED"
  | "BOOKING_CONFIRMED_SUPPLIER_PAYMENT_REQUIRED"
  | "BOOKING_FAILED"
  | "CANCELLED";

/**
 * Derives the traveler-facing UX state of a trip Need from:
 * - the Need itself (its own status plus the mirrored BookingRequest/Booking statuses)
 * - the latest traveler-visible Quote for that Need's BookingRequest, if any (any
 *   quote status — not just ACCEPTED)
 *
 * Precedence (first match wins), from most terminal/important to least:
 * 1. Cancelled (need, request or booking)
 * 2. Booking failed
 * 3. Booking confirmed (or the BookingRequest itself reports COMPLETED) — refined
 *    by the quote's supplier payment status: a confirmed booking can still
 *    require a supplier payment (e.g. pay-on-confirmation providers), which
 *    must NOT be reported as fully settled
 * 4. Booking pending — refined by the quote's supplier payment status
 * 5. Quote accepted, no booking yet — agent is preparing supplier booking
 * 6. A quote was sent — refined by the quote's own status (sent/expired/rejected)
 * 7. Request claimed by an agent, still searching
 * 8. Request sent, not yet claimed
 * 9. Nothing sent yet
 */
export function deriveTravelerNeedState(need: TripNeed, quote: TravelerQuote | null): TravelerNeedUxState {
  if (need.status === "CANCELLED" || need.bookingRequestStatus === "CANCELLED" || need.bookingStatus === "CANCELLED") {
    return "CANCELLED";
  }

  if (need.bookingStatus === "FAILED") {
    return "BOOKING_FAILED";
  }

  if (need.bookingStatus === "CONFIRMED" || need.bookingRequestStatus === "COMPLETED") {
    if (need.bookingStatus === "CONFIRMED" && quote?.providerPaymentStatus === "PAYMENT_REQUIRED") {
      return "BOOKING_CONFIRMED_SUPPLIER_PAYMENT_REQUIRED";
    }
    return "BOOKING_CONFIRMED";
  }

  if (need.bookingStatus === "PENDING") {
    if (quote?.providerPaymentStatus === "PAYMENT_REQUIRED") return "SUPPLIER_PAYMENT_REQUIRED";
    if (quote?.providerPaymentStatus === "PAID_TO_PROVIDER") return "SUPPLIER_PAYMENT_DONE_WAITING_CONFIRMATION";
    return "AGENT_FINALIZING";
  }

  if (need.bookingRequestStatus === "CONFIRMED") {
    return "PROPOSAL_ACCEPTED_AGENT_PROCESSING";
  }

  if (need.bookingRequestStatus === "QUOTED") {
    if (quote?.status === "SENT") return "PROPOSAL_READY";
    if (quote?.status === "EXPIRED") return "PROPOSAL_EXPIRED";
    if (quote?.status === "REJECTED") return "PROPOSAL_REJECTED";
    return "SEARCH_IN_PROGRESS";
  }

  if (need.bookingRequestStatus === "IN_PROGRESS") {
    return "SEARCH_IN_PROGRESS";
  }

  if (need.bookingRequestStatus === "REQUESTED") {
    return "REQUEST_SENT";
  }

  return "TO_ORGANIZE";
}

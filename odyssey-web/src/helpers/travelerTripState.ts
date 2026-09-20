import type { PaymentStatus } from "../types/payment";
import type { TravelerNeedUxState } from "./travelerNeedState";

/** A Need's already-derived UX state, tagged with its Need id for scroll targeting. */
export interface TravelerNeedStateEntry {
  needId: number;
  state: TravelerNeedUxState;
}

export type TravelerTripNextActionKind =
  | "PROPOSALS_READY"
  | "SUPPLIER_PAYMENTS_REQUIRED"
  | "ODYSSEY_PAYMENT_REQUIRED"
  | "NEEDS_TO_ORGANIZE"
  | "BOOKING_FAILED"
  | "AGENT_WORKING"
  | "NO_ACTIVE_NEEDS";

export type TravelerTripUxState =
  | { kind: "TRIP_READY" }
  | {
      kind: "NEXT_ACTION";
      action: TravelerTripNextActionKind;
      /** Number of active Needs concerned by this action (for wording like "3 propositions"). */
      count: number;
      /** Need to scroll to for this action's CTA, when the CTA targets a service card. */
      targetNeedId: number | null;
    };

export interface TravelerTripProgress {
  finalized: number;
  total: number;
}

function needsInStates(needStates: TravelerNeedStateEntry[], states: TravelerNeedUxState[]) {
  return needStates.filter((entry) => states.includes(entry.state));
}

/**
 * Progress is counted over active (non-cancelled) Needs only, and only
 * BOOKING_CONFIRMED counts as finalized — a confirmed booking that still
 * requires a supplier payment is intentionally NOT counted, since a traveler
 * action remains. Returns null when there is nothing meaningful to show
 * (no active Needs), to avoid a misleading "0 sur 0".
 */
export function deriveTravelerTripProgress(needStates: TravelerNeedStateEntry[]): TravelerTripProgress | null {
  const activeNeeds = needStates.filter((entry) => entry.state !== "CANCELLED");
  if (activeNeeds.length === 0) return null;

  const finalized = activeNeeds.filter((entry) => entry.state === "BOOKING_CONFIRMED").length;
  return { finalized, total: activeNeeds.length };
}

/**
 * Derives the single most relevant "what happens next" state for the whole
 * Trip, from the already-derived per-Need states. Does NOT re-derive any
 * Need-level business state — it only aggregates and prioritizes the values
 * produced by deriveTravelerNeedState.
 *
 * Deterministic priority (first non-empty match wins):
 * 1. A proposal is ready to review
 * 2. A supplier payment is required (including on an already-confirmed booking)
 * 3. The Odyssey assistance fee is owed (only when there is actually something to pay)
 * 4. A service still needs to be sent to an agent
 * 5. A booking failed (calm, informational — no CTA)
 * 6. The agent is working and nothing is required from the traveler
 * 7. There are no active Needs at all
 * 8. Every active Need is fully finalized → TRIP_READY
 */
export function deriveTravelerTripUxState(
  needStates: TravelerNeedStateEntry[],
  odysseyPaymentStatus: PaymentStatus | null,
  assistanceFee: number,
): TravelerTripUxState {
  const activeNeeds = needStates.filter((entry) => entry.state !== "CANCELLED");

  if (activeNeeds.length === 0) {
    return { kind: "NEXT_ACTION", action: "NO_ACTIVE_NEEDS", count: 0, targetNeedId: null };
  }

  const proposalsReady = needsInStates(activeNeeds, ["PROPOSAL_READY"]);
  if (proposalsReady.length > 0) {
    return { kind: "NEXT_ACTION", action: "PROPOSALS_READY", count: proposalsReady.length, targetNeedId: proposalsReady[0].needId };
  }

  const supplierPaymentsRequired = needsInStates(activeNeeds, ["SUPPLIER_PAYMENT_REQUIRED", "BOOKING_CONFIRMED_SUPPLIER_PAYMENT_REQUIRED"]);
  if (supplierPaymentsRequired.length > 0) {
    return {
      kind: "NEXT_ACTION",
      action: "SUPPLIER_PAYMENTS_REQUIRED",
      count: supplierPaymentsRequired.length,
      targetNeedId: supplierPaymentsRequired[0].needId,
    };
  }

  // A Need only reaches PROPOSAL_ACCEPTED_WAITING_ODYSSEY_PAYMENT when the Trip's
  // fee has not been paid yet (see deriveTravelerNeedState). Still, only surface it
  // as the trip's next action when there is actually something to pay.
  const odysseyPaymentOwed =
    assistanceFee > 0 && odysseyPaymentStatus !== "PAID" ? needsInStates(activeNeeds, ["PROPOSAL_ACCEPTED_WAITING_ODYSSEY_PAYMENT"]) : [];
  if (odysseyPaymentOwed.length > 0) {
    return { kind: "NEXT_ACTION", action: "ODYSSEY_PAYMENT_REQUIRED", count: odysseyPaymentOwed.length, targetNeedId: null };
  }

  const toOrganize = needsInStates(activeNeeds, ["TO_ORGANIZE"]);
  if (toOrganize.length > 0) {
    return { kind: "NEXT_ACTION", action: "NEEDS_TO_ORGANIZE", count: toOrganize.length, targetNeedId: toOrganize[0].needId };
  }

  const failed = needsInStates(activeNeeds, ["BOOKING_FAILED"]);
  if (failed.length > 0) {
    return { kind: "NEXT_ACTION", action: "BOOKING_FAILED", count: failed.length, targetNeedId: null };
  }

  // Everything else the agent is actively handling, including an accepted
  // proposal whose Odyssey fee is free or already settled — nothing left for
  // the traveler to do right now.
  const stillWorking = needsInStates(activeNeeds, [
    "REQUEST_SENT",
    "SEARCH_IN_PROGRESS",
    "AGENT_FINALIZING",
    "SUPPLIER_PAYMENT_DONE_WAITING_CONFIRMATION",
    "PROPOSAL_EXPIRED",
    "PROPOSAL_REJECTED",
    "PROPOSAL_ACCEPTED_WAITING_ODYSSEY_PAYMENT",
  ]);
  if (stillWorking.length > 0) {
    return { kind: "NEXT_ACTION", action: "AGENT_WORKING", count: stillWorking.length, targetNeedId: null };
  }

  const allFinalized = activeNeeds.every((entry) => entry.state === "BOOKING_CONFIRMED");
  if (allFinalized) {
    return { kind: "TRIP_READY" };
  }

  // Defensive fallback: every reachable TravelerNeedUxState is covered above,
  // this only guards against future states being added without updating this function.
  return { kind: "NEXT_ACTION", action: "AGENT_WORKING", count: activeNeeds.length, targetNeedId: null };
}

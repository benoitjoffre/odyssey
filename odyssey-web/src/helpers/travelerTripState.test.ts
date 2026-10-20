import { deriveTravelerTripUxState } from "./travelerTripState.ts";
import { deriveTravelerNeedState, type TravelerNeedUxState } from "./travelerNeedState.ts";
import type { TripNeed } from "../types/trip";
import type { TravelerQuote } from "../types/travelerQuote";

const needStates = [{ needId: 1, state: "BOOKING_CONFIRMED" as const }];

const requiredResult = deriveTravelerTripUxState(needStates, null, true, 0);
if (requiredResult.kind !== "NEXT_ACTION" || requiredResult.action !== "ODYSSEY_PAYMENT_REQUIRED") {
  throw new Error(`Expected ODYSSEY_PAYMENT_REQUIRED, got ${JSON.stringify(requiredResult)}`);
}

const pendingResult = deriveTravelerTripUxState(needStates, "PENDING", true, 120);
if (pendingResult.kind !== "NEXT_ACTION" || pendingResult.action !== "ODYSSEY_PAYMENT_PENDING") {
  throw new Error(`Expected ODYSSEY_PAYMENT_PENDING, got ${JSON.stringify(pendingResult)}`);
}

const rejectedResult = deriveTravelerTripUxState([{ needId: 42, state: "PROPOSAL_REJECTED" }], null, false, 0);
if (rejectedResult.kind !== "NEXT_ACTION" || rejectedResult.action !== "CLIENT_REJECTED") {
  throw new Error(`Expected CLIENT_REJECTED, got ${JSON.stringify(rejectedResult)}`);
}

console.log("travelerTripState tests passed");

const need: TripNeed = {
  id: 10,
  type: "FLIGHT",
  status: "REQUESTED",
  notes: null,
  bookingRequestId: 20,
  bookingRequestStatus: "IN_PROGRESS",
  bookingStatus: null,
  providerConfirmationId: null,
};
const quote: TravelerQuote = {
  id: 30,
  bookingRequestId: 20,
  providerPrice: 200,
  assistanceFee: 0,
  totalAmount: 200,
  currency: "EUR",
  description: "Vol direct",
  status: "SENT",
  createdAt: "2026-10-20T12:00:00Z",
  expiresAt: null,
  paymentStatus: null,
  providerPaymentUrl: null,
  providerPaymentStatus: "NOT_REQUIRED_YET",
};

function expectNeedState(input: TripNeed, proposal: TravelerQuote | null, expected: TravelerNeedUxState) {
  const actual = deriveTravelerNeedState(input, proposal);
  if (actual !== expected) {
    throw new Error(`Expected ${expected}, got ${actual}`);
  }
}

for (const bookingRequestStatus of ["IN_PROGRESS", "REQUESTED", "QUOTED"] as const) {
  expectNeedState({ ...need, bookingRequestStatus }, quote, "PROPOSAL_READY");
  expectNeedState({ ...need, bookingRequestStatus }, { ...quote, status: "ACCEPTED" }, "PROPOSAL_ACCEPTED_AGENT_PROCESSING");
  expectNeedState({ ...need, bookingRequestStatus }, { ...quote, status: "REJECTED" }, "PROPOSAL_REJECTED");
  expectNeedState({ ...need, bookingRequestStatus }, { ...quote, status: "EXPIRED" }, "PROPOSAL_EXPIRED");
}

expectNeedState(need, null, "SEARCH_IN_PROGRESS");
expectNeedState({ ...need, bookingRequestStatus: "REQUESTED" }, null, "REQUEST_SENT");
expectNeedState({ ...need, status: "CANCELLED" }, quote, "CANCELLED");
expectNeedState({ ...need, bookingStatus: "FAILED" }, quote, "BOOKING_FAILED");
expectNeedState({ ...need, bookingStatus: "PENDING" }, { ...quote, status: "ACCEPTED" }, "AGENT_FINALIZING");
expectNeedState({ ...need, bookingStatus: "CONFIRMED" }, { ...quote, status: "ACCEPTED" }, "BOOKING_CONFIRMED");
expectNeedState({ ...need, bookingStatus: "CONFIRMED" },
  { ...quote, status: "ACCEPTED", providerPaymentStatus: "PAYMENT_REQUIRED" },
  "BOOKING_CONFIRMED_SUPPLIER_PAYMENT_REQUIRED");

const proposals = [10, 11, 12].map((needId) => ({
  needId,
  state: deriveTravelerNeedState({ ...need, id: needId }, quote),
}));
const proposalsResult = deriveTravelerTripUxState(proposals, null, false, 0);
if (proposalsResult.kind !== "NEXT_ACTION" || proposalsResult.action !== "PROPOSALS_READY"
  || proposalsResult.count !== 3 || proposalsResult.targetNeedId !== 10) {
  throw new Error(`Expected three proposals with a CTA to service 10, got ${JSON.stringify(proposalsResult)}`);
}
const mixedResult = deriveTravelerTripUxState(
  [{ needId: 9, state: "SEARCH_IN_PROGRESS" }, proposals[0]], null, false, 0,
);
if (mixedResult.kind !== "NEXT_ACTION" || mixedResult.action !== "PROPOSALS_READY" || mixedResult.count !== 1) {
  throw new Error(`Expected a proposal to take priority over agent work, got ${JSON.stringify(mixedResult)}`);
}

console.log("travelerNeedState and proposal aggregation tests passed");

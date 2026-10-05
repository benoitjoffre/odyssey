import { deriveTravelerTripUxState } from "./travelerTripState.ts";

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

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

console.log("travelerTripState tests passed");

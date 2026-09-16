import { apiFetch } from "./client";
import type { CheckoutSessionResponse } from "../types/payment";

export function createTripCheckoutSession(tripId: number): Promise<CheckoutSessionResponse> {
  return apiFetch<CheckoutSessionResponse>(`/api/trips/${tripId}/payment/checkout`, {
    method: "POST",
  });
}

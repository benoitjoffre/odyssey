import { apiFetch } from "./client";
import type { CheckoutSessionResponse } from "../types/payment";

export function createCheckoutSession(quoteId: number, travelerId: number): Promise<CheckoutSessionResponse> {
  return apiFetch<CheckoutSessionResponse>(`/api/quotes/${quoteId}/payment/checkout?travelerId=${travelerId}`, {
    method: "POST",
  });
}

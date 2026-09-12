import { apiFetch } from "./client";
import type { CheckoutSessionResponse } from "../types/payment";

export function createCheckoutSession(quoteId: number, travelerId?: number): Promise<CheckoutSessionResponse> {
  const url = travelerId == null ? `/api/quotes/${quoteId}/payment/checkout` : `/api/quotes/${quoteId}/payment/checkout?travelerId=${travelerId}`;

  return apiFetch<CheckoutSessionResponse>(url, {
    method: "POST",
  });
}

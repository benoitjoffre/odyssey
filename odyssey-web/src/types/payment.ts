export type PaymentStatus = "PENDING" | "PAID" | "FAILED";

export interface CheckoutSessionResponse {
  paymentId: number;
  checkoutUrl: string;
}

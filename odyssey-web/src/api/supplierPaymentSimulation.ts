import { apiFetch } from "./client";
import type { ProviderPaymentStatus } from "../types/booking";
import type { TripNeedType } from "../types/trip";

export interface SupplierPaymentSimulation {
  bookingId: number;
  tripId: number;
  tripTitle: string;
  tripStartDate: string;
  tripEndDate: string;
  travelerFirstName: string | null;
  travelerLastName: string | null;
  travelerEmail: string;
  provider: string;
  providerReference: string | null;
  description: string | null;
  serviceType: TripNeedType;
  notes: string | null;
  origin: string | null;
  destination: string | null;
  city: string | null;
  travelers: number | null;
  rooms: number | null;
  amount: number;
  currency: string;
  providerPaymentStatus: ProviderPaymentStatus;
}

export function getSupplierPaymentSimulation(bookingId: number, signal?: AbortSignal) {
  return apiFetch<SupplierPaymentSimulation>(`/api/bookings/${bookingId}/supplier-payment-simulation`, { signal });
}

export function simulateSupplierPayment(bookingId: number) {
  return apiFetch<SupplierPaymentSimulation>(`/api/bookings/${bookingId}/supplier-payment-simulation`, { method: "POST" });
}

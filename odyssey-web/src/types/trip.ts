export type TripStatus = "DRAFT" | "CONFIRMED" | "CANCELLED";

export type TripNeedType = "ACCOMMODATION" | "FLIGHT" | "TRANSFER" | "CAR" | "BUS";

export type TripNeedStatus = "DRAFT" | "REQUESTED" | "QUOTED" | "BOOKED" | "CANCELLED";

export type TripBookingRequestStatus = "REQUESTED" | "IN_PROGRESS" | "QUOTED" | "CONFIRMED" | "CANCELLED" | "COMPLETED";

export type TripBookingStatus = "PENDING" | "CONFIRMED" | "FAILED" | "CANCELLED";

export interface Trip {
  id: number;
  title: string;
  startDate: string;
  endDate: string;
  status: TripStatus;
  travelerId: number;
  travelEventId: number | null;
}

export interface TripNeed {
  id: number;
  type: TripNeedType;
  status: TripNeedStatus;
  notes: string | null;
  bookingRequestStatus: TripBookingRequestStatus | null;
  bookingStatus: TripBookingStatus | null;
  providerConfirmationId: string | null;
}

export interface TripDetail extends Trip {
  needs: TripNeed[];
}

export interface CreateTripRequest {
  title: string;
  startDate: string;
  endDate: string;
  travelerId: number;
  travelEventId?: number;
}

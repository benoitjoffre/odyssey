import type { AccommodationCriteria, FlightCriteria, NeedType } from "./bookingRequest";

export interface Need {
  id: number;
  type: NeedType;
  status: "DRAFT" | "REQUESTED" | "QUOTED" | "BOOKED" | "CANCELLED";
  notes: string | null;
  tripId: number;
}

interface CreateNeedBase {
  notes: string | null;
  tripId: number;
}

export interface CreateFlightNeedRequest extends CreateNeedBase {
  type: "FLIGHT";
  flightCriteria: FlightCriteria;
  accommodationCriteria: null;
}

export interface CreateAccommodationNeedRequest extends CreateNeedBase {
  type: "ACCOMMODATION";
  flightCriteria: null;
  accommodationCriteria: AccommodationCriteria;
}

export type CreateNeedRequest = CreateFlightNeedRequest | CreateAccommodationNeedRequest;
export type OrganizableNeedType = CreateNeedRequest["type"];

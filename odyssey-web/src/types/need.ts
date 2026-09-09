import type { AccommodationCriteria, FlightCriteria, NeedType, TransferCriteria } from "./bookingRequest";

export interface Need {
  id: number;
  type: NeedType;
  status: "DRAFT" | "REQUESTED" | "QUOTED" | "BOOKED" | "CANCELLED";
  notes: string | null;
  tripId: number;
  transferCriteria: TransferCriteria | null;
}

interface CreateNeedBase {
  notes: string | null;
  tripId: number;
}

export interface CreateFlightNeedRequest extends CreateNeedBase {
  type: "FLIGHT";
  flightCriteria: FlightCriteria;
  accommodationCriteria: null;
  transferCriteria: null;
}

export interface CreateAccommodationNeedRequest extends CreateNeedBase {
  type: "ACCOMMODATION";
  flightCriteria: null;
  accommodationCriteria: AccommodationCriteria;
  transferCriteria: null;
}

export interface CreateTransferNeedRequest extends CreateNeedBase {
  type: "TRANSFER";
  flightCriteria: null;
  accommodationCriteria: null;
  transferCriteria: TransferCriteria;
}

export type CreateNeedRequest = CreateFlightNeedRequest | CreateAccommodationNeedRequest | CreateTransferNeedRequest;
export type OrganizableNeedType = CreateNeedRequest["type"];

export type BookingRequestStatus = "REQUESTED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export type NeedType = "FLIGHT" | "ACCOMMODATION" | "CAR" | "TRANSFER" | "BUS";

export interface FlightCriteria {
  origin: string;
  destination: string;
  travelers: number;
}

export interface AccommodationCriteria {
  city: string;
  travelers: number;
  rooms: number;
}

export interface BookingRequest {
  id: number;
  status: BookingRequestStatus;
  notes: string | null;
  needId: number;
  assignedAgentId: number | null;
  need: {
    id: number;
    type: NeedType;
    status: string;
    notes: string | null;
    flightCriteria: FlightCriteria | null;
    accommodationCriteria: AccommodationCriteria | null;
  };
  trip: {
    id: number;
    title: string;
    startDate: string;
    endDate: string;
  };
  traveler: {
    id: number;
    firstName: string;
    email: string;
  };
}

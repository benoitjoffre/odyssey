import type { NeedType } from "./bookingRequest";
import type { TripStatus } from "./trip";

export interface AgentNotification {
  id: number;
  message: string;
  read: boolean;
  createdAt: string;
  bookingRequestId: number;
  tripId: number | null;
  tripStatus: TripStatus | null;
  tripTitle: string | null;
  tripStartDate: string | null;
  tripEndDate: string | null;
  travelerFirstName: string | null;
  travelerLastName: string | null;
  needType: NeedType | null;
}

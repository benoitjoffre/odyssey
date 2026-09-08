import type { NeedType } from "./bookingRequest";

export interface AgentNotification {
  id: number;
  message: string;
  read: boolean;
  createdAt: string;
  bookingRequestId: number;
  tripId: number | null;
  tripTitle: string | null;
  tripStartDate: string | null;
  tripEndDate: string | null;
  travelerFirstName: string | null;
  travelerLastName: string | null;
  needType: NeedType | null;
}

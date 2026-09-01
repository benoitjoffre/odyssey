export interface AgentNotification {
  id: number;
  message: string;
  read: boolean;
  createdAt: string;
  bookingRequestId: number;
}

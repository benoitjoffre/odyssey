export interface TravelEvent {
  id: number;
  name: string;
  location: string;
  startDate: string;
  endDate: string;
  description: string | null;
  experienceId: number;
}

export interface CreateTravelEventRequest {
  name: string;
  location: string;
  startDate: string;
  endDate: string;
  description: string | null;
  experienceId: number;
}

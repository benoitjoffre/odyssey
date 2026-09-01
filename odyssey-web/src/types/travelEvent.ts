export interface TravelEvent {
  id: number;
  name: string;
  location: string;
  startDate: string;
  endDate: string;
  description: string | null;
  experienceId: number;
}

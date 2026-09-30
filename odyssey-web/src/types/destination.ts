export type Destination = {
  id: number;
  city: string;
  country: string;
  countryCode: string;
};

export type CreateDestinationRequest = {
  city: string;
  country: string;
  countryCode: string;
};

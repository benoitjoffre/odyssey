interface ProviderOfferBase {
  provider: string;
  externalId: string;
  price: number;
  currency: string;
}

export interface AccommodationOffer extends ProviderOfferBase {
  hotelName: string;
  city: string;
  checkIn: string;
  checkOut: string;
  roomType: string;
}

export interface FlightOffer extends ProviderOfferBase {
  origin: string;
  destination: string;
  departure: string;
  arrival: string;
  airline: string;
}

export type ProviderOffer = AccommodationOffer | FlightOffer;

export function isAccommodationOffer(offer: ProviderOffer): offer is AccommodationOffer {
  return "hotelName" in offer;
}

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

export interface TransferOffer extends ProviderOfferBase {
  pickupLocation: string;
  dropoffLocation: string;
  travelers: number;
  vehicleType: string;
}

export function isAccommodationOffer(offer: ProviderOffer): offer is AccommodationOffer {
  return "hotelName" in offer;
}

export function isTransferOffer(offer: ProviderOffer): offer is TransferOffer {
  return "vehicleType" in offer;
}

export type ProviderOffer = AccommodationOffer | FlightOffer | TransferOffer;

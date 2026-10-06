import { ArrowRight, BedDouble, CalendarDays, Car, CircleUserRound, Hotel, Mail, MapPin, Plane, Users } from "lucide-react";
import type { BookingRequest, NeedType } from "../types/bookingRequest";

const needTypeLabels: Record<NeedType, string> = {
  ACCOMMODATION: "Hébergement",
  FLIGHT: "Vol",
  CAR: "Voiture",
  TRANSFER: "Transfert",
  BUS: "Bus",
};

function formatDate(value: string) {
  return new Intl.DateTimeFormat("fr-FR", { dateStyle: "medium" }).format(new Date(`${value}T00:00:00`));
}

export interface BookingRequestOverviewSectionProps {
  bookingRequest: BookingRequest;
}

export function BookingRequestOverviewSection({ bookingRequest }: BookingRequestOverviewSectionProps) {
  const { need, traveler, trip } = bookingRequest;

  return (
    <>
      <div className="detail-grid">
        <section className="detail-card">
          <div className="detail-card-heading">
            <CircleUserRound size={20} />
            <h2>Client</h2>
          </div>
          <dl className="detail-list">
            <div>
              <dt>Prénom</dt>
              <dd>{traveler.firstName}</dd>
            </div>
            <div>
              <dt>
                <Mail size={15} /> Email
              </dt>
              <dd>{traveler.email}</dd>
            </div>
          </dl>
        </section>

        <section className="detail-card">
          <div className="detail-card-heading">
            <CalendarDays size={20} />
            <h2>Voyage</h2>
          </div>
          <dl className="detail-list">
            {trip.title.trim() && (
              <div>
                <dt>Titre</dt>
                <dd>{trip.title}</dd>
              </div>
            )}
            <div>
              <dt>Début</dt>
              <dd>{formatDate(trip.startDate)}</dd>
            </div>
            <div>
              <dt>Fin</dt>
              <dd>{formatDate(trip.endDate)}</dd>
            </div>
          </dl>
        </section>

        <section className="detail-card detail-card-wide">
          <div className="detail-card-heading">
            {need.type === "FLIGHT" ? <Plane size={20} /> : <BedDouble size={20} />}
            <h2>Besoin</h2>
          </div>
          <dl className="detail-list">
            <div>
              <dt>Type</dt>
              <dd>{needTypeLabels[need.type]}</dd>
            </div>
            {need.notes && (
              <div>
                <dt>Notes</dt>
                <dd>{need.notes}</dd>
              </div>
            )}
          </dl>
        </section>
      </div>

      {need.type === "ACCOMMODATION" && need.accommodationCriteria && (
        <section className="criteria-card">
          <div className="detail-card-heading">
            <Hotel size={20} />
            <h2>Hébergement</h2>
          </div>
          <div className="criteria-grid">
            <div>
              <MapPin size={18} />
              <span>Ville</span>
              <strong>{need.accommodationCriteria.city}</strong>
            </div>
            <div>
              <Users size={18} />
              <span>Voyageurs</span>
              <strong>{need.accommodationCriteria.travelers}</strong>
            </div>
            <div>
              <BedDouble size={18} />
              <span>Chambres</span>
              <strong>{need.accommodationCriteria.rooms}</strong>
            </div>
          </div>
        </section>
      )}

      {need.type === "FLIGHT" && need.flightCriteria && (
        <section className="criteria-card">
          <div className="detail-card-heading">
            <Plane size={20} />
            <h2>Vol</h2>
          </div>
          <div className="flight-route">
            <strong>{need.flightCriteria.origin}</strong>
            <ArrowRight size={21} />
            <strong>{need.flightCriteria.destination}</strong>
          </div>
          <div className="travelers-line">
            <Users size={17} /> Voyageurs : {need.flightCriteria.travelers}
          </div>
        </section>
      )}

      {need.type === "TRANSFER" && need.transferCriteria && (
        <section className="criteria-card">
          <div className="detail-card-heading">
            <Car size={20} />
            <h2>Transfert</h2>
          </div>
          <div className="criteria-grid">
            <div>
              <MapPin size={18} />
              <span>Lieu de départ</span>
              <strong>{need.transferCriteria.pickupLocation}</strong>
            </div>
            <div>
              <MapPin size={18} />
              <span>Lieu d'arrivée</span>
              <strong>{need.transferCriteria.dropoffLocation}</strong>
            </div>
            <div>
              <Users size={18} />
              <span>Voyageurs</span>
              <strong>{need.transferCriteria.travelers}</strong>
            </div>
          </div>
        </section>
      )}
    </>
  );
}

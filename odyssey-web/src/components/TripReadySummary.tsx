import type { ReactNode } from "react";
import { Check } from "lucide-react";

export interface TripReadyService {
  needId: number;
  icon: ReactNode;
  label: string;
  providerConfirmationId: string | null;
}

export interface TripReadySummaryProps {
  services: TripReadyService[];
}

export function TripReadySummary({ services }: TripReadySummaryProps) {
  return (
    <section className="trip-ready-summary" aria-labelledby="trip-ready-title">
      <div className="trip-ready-headline">
        <span className="trip-ready-icon" aria-hidden="true">
          <Check size={20} />
        </span>
        <div>
          <strong id="trip-ready-title">Votre voyage est prêt</strong>
          <p>Vos réservations sont finalisées. Retrouvez ci-dessous les informations utiles pour votre voyage.</p>
        </div>
      </div>
      <ul className="trip-ready-list">
        {services.map((service) => (
          <li key={service.needId}>
            <span className="trip-ready-list-icon" aria-hidden="true">
              {service.icon}
            </span>
            <span className="trip-ready-list-label">{service.label}</span>
            <span className="trip-ready-list-status">Confirmé</span>
            {service.providerConfirmationId && <span className="trip-ready-list-reference">{service.providerConfirmationId}</span>}
          </li>
        ))}
      </ul>
    </section>
  );
}

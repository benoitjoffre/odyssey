import { ArrowRight, CalendarDays, MapPin } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import fallbackExperienceImage from "../../assets/experience-placeholder.svg";
import { Card } from "../ui/Card";
import { experienceCategoryLabels } from "../../helpers/experienceCategories";
import { getExperienceImageUrl } from "../../helpers/experienceImage";
import type { Experience } from "../../types/experience";

type ExperienceCardInput = {
  id: number;
  title: string;
  description: string;
  category: Experience["category"];
  durationDays: number;
  destination?: Experience["destination"] | string | null;
  imageUrl?: string | null;
};

interface ExperienceCardProps {
  experience: ExperienceCardInput;
}

function getDestinationLabel(destination: Experience["destination"] | string | null | undefined): string {
  if (!destination) {
    return "Destination à confirmer";
  }

  if (typeof destination === "string") {
    const trimmed = destination.trim();
    if (!trimmed) {
      return "Destination à confirmer";
    }

    return trimmed;
  }

  const city = destination.city?.trim();
  const country = destination.country?.trim();

  if (city && country) {
    return `${city}, ${country}`;
  }

  if (city) {
    return city;
  }

  if (country) {
    return country;
  }

  return "Destination à confirmer";
}

export function ExperienceCard({ experience }: ExperienceCardProps) {
  const [imageSrc, setImageSrc] = useState(() => getExperienceImageUrl(experience.imageUrl));
  const destinationLabel = getDestinationLabel(experience.destination);
  const categoryLabel = experienceCategoryLabels[experience.category] ?? "Découverte";
  const categoryClassName = `experience-card__category--${String(experience.category).toLowerCase()}`;
  const durationLabel = experience.durationDays ? `${experience.durationDays} jours` : "Durée flexible";

  return (
    <Link className="experience-card" to={`/experiences/${experience.id}`} aria-label={`Voir l'expérience ${experience.title}`}>
      <Card className="experience-card__surface">
        <div className="experience-card__media">
          <img className="experience-card__image" src={imageSrc} alt={experience.title} onError={() => setImageSrc(fallbackExperienceImage)} />
          <span className={`experience-card__category ${categoryClassName}`}>{categoryLabel.toUpperCase()}</span>
        </div>
        <div className="experience-card__content">
          <h3>{experience.title}</h3>

          <div className="experience-card__meta">
            <span>
              <MapPin size={14} /> {destinationLabel}
            </span>
            <span>
              <CalendarDays size={14} /> {durationLabel}
            </span>
          </div>
        </div>

        <span className="experience-card__action" aria-hidden="true">
          <ArrowRight size={16} />
        </span>
      </Card>
    </Link>
  );
}

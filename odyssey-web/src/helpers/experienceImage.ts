import fallbackExperienceImage from "../assets/experience-placeholder.svg";

export function getExperienceImageUrl(imageUrl?: string | null): string {
  if (typeof imageUrl === "string" && imageUrl.trim().length > 0) {
    return imageUrl.trim();
  }

  return fallbackExperienceImage;
}

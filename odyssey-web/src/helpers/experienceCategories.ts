import type { ExperienceCategory } from "../types/intent";

export const experienceCategoryLabels: Record<ExperienceCategory, string> = {
  DANCE: "Danse",
  CULTURE: "Culture",
  NATURE: "Nature",
  ADVENTURE: "Aventure",
  FOOD: "Gastronomie",
  BEACH: "Plage",
  ROAD_TRIP: "Road trip",
  SURF: "Surf",
};

export const experienceCategoryPrompts: Record<ExperienceCategory, string> = {
  DANCE: "Je veux vivre un voyage autour de la danse",
  CULTURE: "Je veux découvrir une culture et son patrimoine",
  NATURE: "Je cherche de la nature et de grands espaces",
  ADVENTURE: "Je veux vivre un voyage plein d’aventure",
  FOOD: "Je veux découvrir une destination par sa gastronomie",
  BEACH: "Je veux profiter de la plage et de la mer",
  ROAD_TRIP: "Je veux partir en road trip",
  SURF: "Je veux faire du surf au bord de l’océan",
};

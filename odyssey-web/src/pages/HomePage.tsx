import { useEffect, useState, type ComponentType } from "react";
import {
  ArrowDown,
  ArrowRight,
  Bike,
  CalendarDays,
  ChefHat,
  Compass,
  Footprints,
  MapPin,
  Mountain,
  Music2,
  Plane,
  Sparkles,
  Trees,
  UsersRound,
  Waves,
} from "lucide-react";
import { Link } from "react-router-dom";
import { getExperiences } from "../api/experiences";
import { experienceCategoryLabels } from "../helpers/experienceCategories";
import type { Experience } from "../types/experience";

interface Inspiration {
  label: string;
  detail: string;
  icon: ComponentType<{ size?: number; strokeWidth?: number }>;
  tone: string;
}

const inspirations: Inspiration[] = [
  { label: "Danse", detail: "Bouger au rythme d’ailleurs", icon: Music2, tone: "coral" },
  { label: "Surf", detail: "Suivre les plus belles vagues", icon: Waves, tone: "ocean" },
  { label: "Nature", detail: "Retrouver les grands espaces", icon: Trees, tone: "forest" },
  { label: "Gastronomie", detail: "Goûter un territoire", icon: ChefHat, tone: "saffron" },
  { label: "Culture", detail: "Rencontrer une autre histoire", icon: UsersRound, tone: "clay" },
  { label: "Aventure", detail: "Sortir des sentiers connus", icon: Mountain, tone: "stone" },
];

const journeySteps = [
  {
    number: "01",
    title: "Dites-nous votre envie",
    copy: "« Je veux apprendre la salsa à Cuba. » Tout commence par ce qui vous fait vibrer.",
    icon: Sparkles,
  },
  {
    number: "02",
    title: "Découvrez des expériences",
    copy: "Odyssey vous propose des idées qui correspondent à votre envie, dans des destinations faites pour la vivre.",
    icon: Compass,
  },
  {
    number: "03",
    title: "Partez vivre l’expérience",
    copy: "Choisissez une date, créez votre voyage et organisez ensuite transport, hébergement et transferts.",
    icon: Plane,
  },
];

export function HomePage() {
  const [experiences, setExperiences] = useState<Experience[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    document.title = "Odyssey | Voyagez pour ce que vous aimez";
    const controller = new AbortController();

    getExperiences(controller.signal)
      .then((items) => setExperiences(items.slice(0, 6)))
      .catch((requestError: unknown) => {
        if (!(requestError instanceof DOMException && requestError.name === "AbortError")) setExperiences([]);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, []);

  return (
    <div className="public-home">
      <header className="public-header">
        <div className="public-header-inner">
          <Link className="public-brand" to="/" aria-label="Odyssey, accueil">
            <span className="public-brand-mark">
              <Plane size={21} strokeWidth={2.2} />
            </span>
            <span>Odyssey</span>
          </Link>
          <nav className="public-nav" aria-label="Navigation principale">
            <Link className="public-nav-link" to="/traveler/discover">
              Découvrir
            </Link>
            <Link className="public-agent-link" to="/agent">
              <UsersRound size={17} />
              <span>Espace Agent</span>
            </Link>
          </nav>
        </div>
      </header>

      <main>
        <section className="public-hero">
          <div className="public-hero-shade" />
          <div className="public-hero-content">
            <span className="public-kicker">
              <Compass size={16} /> Le voyage commence par une envie
            </span>
            <h1>Voyagez pour ce que vous aimez.</h1>
            <p>
              Dites-nous ce que vous avez envie de vivre. Odyssey vous aide à trouver les expériences et événements qui vous correspondent, puis à
              organiser votre voyage.
            </p>
            <Link className="public-primary-cta" to="/traveler/discover">
              Découvrir des expériences <ArrowRight size={19} />
            </Link>
          </div>
          <a className="public-scroll-cue" href="#how-it-works">
            <span>Voir comment</span>
            <ArrowDown size={18} />
          </a>
        </section>

        <section className="public-section public-how" id="how-it-works">
          <div className="public-section-inner">
            <div className="public-section-heading">
              <span>De l’envie au départ</span>
              <h2>Comment ça marche ?</h2>
              <p>Une façon plus naturelle d’imaginer un voyage : partir de ce que vous voulez vivre, puis construire le reste.</p>
            </div>
            <div className="public-steps">
              {journeySteps.map((step) => {
                const Icon = step.icon;
                return (
                  <article className="public-step" key={step.number}>
                    <div className="public-step-top">
                      <span>{step.number}</span>
                      <Icon size={24} />
                    </div>
                    <h3>{step.title}</h3>
                    <p>{step.copy}</p>
                  </article>
                );
              })}
            </div>
          </div>
        </section>

        <section className="public-difference">
          <div className="public-section-inner public-difference-grid">
            <div className="public-difference-copy">
              <span>La différence Odyssey</span>
              <h2>Plus qu’un moteur de réservation.</h2>
              <p>Vous ne commencez pas par chercher un vol ou un hôtel. Vous commencez par ce que vous avez envie de vivre.</p>
              <Link to="/traveler/discover">
                Commencer par mon envie <ArrowRight size={18} />
              </Link>
            </div>
            <div className="public-journey" aria-label="De l’envie au voyage">
              <div>
                <Sparkles size={22} />
                <span>Votre envie</span>
                <small>Ce qui vous inspire</small>
              </div>
              <ArrowRight className="public-journey-arrow" size={22} />
              <div>
                <Footprints size={22} />
                <span>L’expérience</span>
                <small>Ce que vous vivrez</small>
              </div>
              <ArrowRight className="public-journey-arrow" size={22} />
              <div>
                <CalendarDays size={22} />
                <span>L’événement</span>
                <small>Le bon lieu, au bon moment</small>
              </div>
              <ArrowRight className="public-journey-arrow" size={22} />
              <div>
                <Plane size={22} />
                <span>Le voyage</span>
                <small>Tout organisé autour</small>
              </div>
            </div>
          </div>
        </section>

        <section className="public-section public-experiences">
          <div className="public-section-inner">
            <div className="public-section-heading public-section-heading-row">
              <div>
                <span>Explorez autrement</span>
                <h2>Qu’avez-vous envie de vivre ?</h2>
              </div>
              <p>Une passion, une curiosité ou un défi peuvent devenir le point de départ de votre prochain voyage.</p>
            </div>
            <div className="public-inspiration-grid">
              {inspirations.map((inspiration) => {
                const Icon = inspiration.icon;
                return (
                  <article className={`public-inspiration ${inspiration.tone}`} key={inspiration.label}>
                    <Icon size={28} strokeWidth={1.8} />
                    <div>
                      <h3>{inspiration.label}</h3>
                      <p>{inspiration.detail}</p>
                    </div>
                  </article>
                );
              })}
            </div>

            <div className="public-live-heading">
              <span>
                <span className="public-live-dot" /> En ce moment sur Odyssey
              </span>
              <h3>Des expériences à découvrir</h3>
            </div>
            {loading && (
              <div className="public-experience-state" role="status">
                <span className="spinner" /> Nous cherchons de nouvelles inspirations…
              </div>
            )}
            {!loading && experiences.length === 0 && (
              <div className="public-experience-state">
                <Compass size={25} />
                <strong>Le catalogue se prépare.</strong>
                <span>Décrivez déjà votre envie : Odyssey vous guidera vers votre prochaine expérience.</span>
                <Link to="/traveler/discover">
                  Lancer ma recherche <ArrowRight size={17} />
                </Link>
              </div>
            )}
            {!loading && experiences.length > 0 && (
              <div className="public-experience-grid">
                {experiences.map((experience) => (
                  <article className="public-experience-card" key={experience.id}>
                    <div className={`public-experience-visual category-${experience.category.toLowerCase().replace("_", "-")}`}>
                      <span>{experienceCategoryLabels[experience.category]}</span>
                    </div>
                    <div className="public-experience-body">
                      <div className="public-experience-meta">
                        <span>
                          <MapPin size={15} /> {experience.destination}
                        </span>
                        <span>
                          <CalendarDays size={15} /> {experience.durationDays} jours
                        </span>
                      </div>
                      <h3>{experience.title}</h3>
                      <p>{experience.description}</p>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </div>
        </section>

        <section className="public-final-cta">
          <div className="public-final-cta-inner">
            <Bike size={34} />
            <span>Votre prochaine histoire commence ici</span>
            <h2>Une envie de partir ?</h2>
            <p>Ne cherchez pas encore la destination. Dites-nous d’abord ce que vous rêvez de vivre.</p>
            <Link className="public-primary-cta light" to="/traveler/discover">
              Trouver mon expérience <ArrowRight size={19} />
            </Link>
          </div>
        </section>
      </main>

      <footer className="public-footer">
        <div>
          <span className="public-brand">
            <span className="public-brand-mark">
              <Plane size={19} />
            </span>
            <span>Odyssey</span>
          </span>
          <p>Voyagez pour ce que vous aimez.</p>
        </div>
      </footer>
    </div>
  );
}

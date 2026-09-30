import { useAuth0 } from "@auth0/auth0-react";
import { ArrowRight, Compass, Heart, MapPin, Search, ShieldCheck, Sparkles, Trees, UtensilsCrossed, Waves } from "lucide-react";
import { type FormEvent, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { getExperiences } from "../api/experiences";
import { ExperienceCard } from "../components/experience/ExperienceCard";
import type { Experience } from "../types/experience";
import odysseyLogo from "../assets/odyssey-bird.png";
const categoryCards = [
  { category: "CULTURE", label: "Culture & patrimoine", accent: "green" },
  { category: "FOOD", label: "Gastronomie & saveurs", accent: "yellow" },
  { category: "NATURE", label: "Nature & aventure", accent: "green-soft" },
  { category: "DANCE", label: "Musique & traditions", accent: "blue-soft" },
  { category: "BEACH", label: "Détente & bien-être", accent: "blue" },
  { category: "ADVENTURE", label: "Découverte & rencontres", accent: "amber" },
] as const;

const whyItems = [
  {
    title: "Des expériences uniques",
    copy: "Sélectionnées avec soin pour leur authenticité et leur richesse humaine.",
    icon: Sparkles,
    accent: "blue",
  },
  {
    title: "Des experts locaux",
    copy: "Une assistance personnalisée tout au long de votre voyage.",
    icon: ShieldCheck,
    accent: "red",
  },
  {
    title: "Un voyage à votre image",
    copy: "Des expériences adaptées à vos envies, à votre rythme et à votre curiosité.",
    icon: Compass,
    accent: "yellow",
  },
  {
    title: "Des souvenirs durables",
    copy: "Plus que des voyages, des rencontres et des moments inoubliables.",
    icon: Heart,
    accent: "green",
  },
] as const;

export function HomePage() {
  const navigate = useNavigate();
  const { isAuthenticated, loginWithRedirect, user } = useAuth0();
  const [experiences, setExperiences] = useState<Experience[]>([]);
  const [heroInput, setHeroInput] = useState("");
  const [isHeaderScrolled, setIsHeaderScrolled] = useState(false);

  useEffect(() => {
    document.title = "Odyssey | Voyagez pour ce que vous aimez";

    const onScroll = () => {
      setIsHeaderScrolled(window.scrollY > 12);
    };

    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });

    const controller = new AbortController();

    getExperiences(controller.signal)
      .then((items) => {
        setExperiences(items.slice(0, 4));
      })
      .catch((requestError: unknown) => {
        if (!(requestError instanceof DOMException && requestError.name === "AbortError")) {
          setExperiences([]);
        }
      });

    return () => {
      controller.abort();
      window.removeEventListener("scroll", onScroll);
    };
  }, []);

  function handleHeroSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void navigate("/traveler/discover");
  }

  const displayName = user?.given_name ?? user?.name ?? "Voyageur";

  return (
    <div className="public-home">
      <header className={`public-header${isHeaderScrolled ? " is-scrolled" : ""}`}>
        <div className="public-header__inner">
          <Link className="public-brand" to="/" aria-label="Odyssey, accueil">
            <img src={odysseyLogo} alt="Odyssey" className="public-brand__logo" />
          </Link>

          <nav className="public-nav" aria-label="Navigation principale">
            <Link className="public-nav__link is-active" to="/traveler/discover">
              Expériences
            </Link>
            <Link className="public-nav__link" to="/traveler/trips">
              Mes voyages
            </Link>
            <Link className="public-nav__link" to="/">
              À propos
            </Link>
          </nav>

          <div className="public-header__actions">
            <button type="button" className="public-header__search" aria-label="Recherche">
              <Search size={17} />
            </button>
            {isAuthenticated ? (
              <Link className="public-header__button public-header__button--ghost" to="/traveler/trips">
                {displayName}
              </Link>
            ) : (
              <>
                <button type="button" className="public-header__button public-header__button--ghost" onClick={() => void loginWithRedirect()}>
                  Se connecter
                </button>
                <button type="button" className="public-header__button public-header__button--primary" onClick={() => void loginWithRedirect()}>
                  Créer un compte
                </button>
              </>
            )}
          </div>
        </div>
      </header>

      <main>
        <section className="public-hero">
          <div className="public-hero__layout">
            <div className="public-hero__content">
              <h1>
                Trouvez une
                <br />
                expérience qui vous
                <br />
                <span>ressemble.</span>
              </h1>
              <p>Culture, gastronomie, aventure, musique... Découvrez des expériences uniques et créez des souvenirs inoubliables.</p>

              <form className="public-hero__search" onSubmit={handleHeroSubmit}>
                <input
                  type="text"
                  value={heroInput}
                  onChange={(event) => setHeroInput(event.target.value)}
                  placeholder="Qu'avez-vous envie de vivre ?"
                  aria-label="Qu'avez-vous envie de vivre ?"
                />
                <button type="submit">
                  Découvrir <ArrowRight size={16} />
                </button>
              </form>

              <div className="public-hero__examples" aria-label="Exemples d’envies">
                <span className="public-hero__examples-label">Exemples :</span>
                {["découvrir la culture latino", "road trip nature", "gastronomie locale"].map((example) => (
                  <span key={example}>{example}</span>
                ))}
              </div>
            </div>

            <div className="public-hero__floating-note" aria-label="Plus que des voyages, des expériences">
              <span>Plus que des</span>
              <span>voyages, des</span>
              <span>expériences.</span>
            </div>
          </div>
        </section>

        <section className="public-section">
          <div className="public-section__inner">
            <div className="public-section__heading">
              <h2>Explorez selon vos envies</h2>
            </div>

            <div className="public-category-grid">
              {categoryCards.map((card) => (
                <Link key={card.category} className={`public-category-card ${card.accent}`} to="/traveler/discover">
                  <span className="public-category-card__icon" aria-hidden="true">
                    {card.category === "CULTURE" && <MapPin size={19} />}
                    {card.category === "FOOD" && <UtensilsCrossed size={19} />}
                    {card.category === "NATURE" && <Trees size={19} />}
                    {card.category === "DANCE" && <Sparkles size={19} />}
                    {card.category === "BEACH" && <Waves size={19} />}
                    {card.category === "ADVENTURE" && <Compass size={19} />}
                  </span>
                  <span>{card.label}</span>
                </Link>
              ))}
            </div>
          </div>
        </section>

        <section className="public-section public-section--featured">
          <div className="public-section__inner">
            <div className="public-section__heading public-section__heading--split">
              <h2>Expériences à découvrir</h2>
              <Link className="public-inline-link" to="/traveler/discover">
                Voir toutes les expériences <ArrowRight size={17} />
              </Link>
            </div>

            {experiences.length === 0 ? (
              <div className="public-state-panel">
                <p>Le catalogue se prépare. Revenez bientôt pour découvrir les prochaines expériences Odyssey.</p>
              </div>
            ) : (
              <div className="public-card-grid">
                {experiences.map((experience) => (
                  <ExperienceCard key={experience.id} experience={experience} />
                ))}
              </div>
            )}
          </div>
        </section>

        <section className="public-editorial">
          <div className="public-editorial__media" aria-hidden="true">
            <img className="public-editorial__image" src="/src/assets/tortues.png" alt="" />
          </div>

          <div className="public-editorial__content">
            <span className="public-editorial__kicker">Vivez l’extraordinaire</span>
            <h2>
              Des expériences
              <br />
              authentiques
              <br />
              dans des lieux
              <br />
              <span className="public-editorial__highlight">exceptionnels.</span>
            </h2>
            <p>
              Partez à la rencontre de cultures, de saveurs et de paysages uniques, avec des expériences pensées pour vous faire vivre plus qu’un
              simple voyage.
            </p>
          </div>

          <img className="public-editorial__plumes" src="/src/assets/plumes.png" alt="" aria-hidden="true" />
        </section>

        <section className="public-section public-section--why">
          <div className="public-section__inner">
            <div className="public-section__heading">
              <h2>Pourquoi Odyssey ?</h2>
            </div>

            <div className="public-why-grid">
              {whyItems.map(({ title, copy, icon: Icon, accent }) => (
                <article key={title} className={`public-why-item public-why-item--${accent}`}>
                  <span className="public-why-item__icon">
                    <Icon size={20} />
                  </span>
                  <div className="public-why-item__copy">
                    <h3>{title}</h3>
                    <p>{copy}</p>
                  </div>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section className="public-final-cta">
          <div className="public-final-cta__inner">
            <div className="public-final-cta__content">
              <div className="public-final-cta__copy">
                <p>Prêt à vivre</p>
                <h2>votre prochaine expérience ?</h2>
              </div>

              <Link className="public-primary-button public-final-cta__button" to="/traveler/discover">
                Explorer maintenant <ArrowRight size={18} />
              </Link>
            </div>
          </div>
        </section>

        <footer className="public-final-footer">
          <div className="public-final-footer__inner">
            <div className="public-final-footer__brand">
              <img src="/src/assets/logo.png" alt="Odyssey" className="public-final-footer__logo" />
              <p>Des voyages pensés pour vivre autrement.</p>
            </div>

            <div className="public-final-footer__group">
              <h3>Odyssey</h3>
              <ul>
                <li>
                  <Link to="/traveler/discover">Expériences</Link>
                </li>
                <li>
                  <Link to="/traveler/trips">Mes voyages</Link>
                </li>
                <li>
                  <Link to="/">À propos</Link>
                </li>
              </ul>
            </div>

            <div className="public-final-footer__group">
              <h3>Support</h3>
              <ul>
                <li>
                  <Link to="/">Contact</Link>
                </li>
                <li>
                  <Link to="/">FAQ</Link>
                </li>
                <li>
                  <Link to="/">Aide</Link>
                </li>
              </ul>
            </div>

            <div className="public-final-footer__group">
              <h3>Légal</h3>
              <ul>
                <li>
                  <Link to="/">Mentions légales</Link>
                </li>
                <li>
                  <Link to="/">CGV</Link>
                </li>
                <li>
                  <Link to="/">Confidentialité</Link>
                </li>
              </ul>
            </div>
          </div>

          <div className="public-final-footer__bottom">
            <span>© 2026 Odyssey</span>
            <span>Tout droit réservé</span>
          </div>
        </footer>
      </main>
    </div>
  );
}

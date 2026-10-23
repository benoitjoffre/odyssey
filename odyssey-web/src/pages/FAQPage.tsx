import { useEffect, useMemo, useRef, useState } from "react";
import { ArrowLeft, ArrowRight, Check, ChevronDown, CircleHelp, Compass, CreditCard, Luggage, Search, Sparkles, UserRound, X } from "lucide-react";
import { Link } from "react-router-dom";
import odysseyLogo from "../assets/odyssey-bird.png";
import "./FAQPage.css";

const categories = [
  { label: "Toutes les questions", icon: CircleHelp },
  { label: "Découvrir Odyssey", icon: Compass },
  { label: "Organiser mon voyage", icon: Luggage },
  { label: "Propositions", icon: Sparkles },
  { label: "Réservation & paiement", icon: CreditCard },
  { label: "Mon compte", icon: UserRound },
] as const;

type FAQCategory = (typeof categories)[number]["label"];

const questions: { id: string; category: Exclude<FAQCategory, "Toutes les questions">; question: string; answer: string }[] = [
  {
    id: "odyssey",
    category: "Découvrir Odyssey",
    question: "Qu’est-ce qu’Odyssey ?",
    answer:
      "Odyssey vous aide à organiser des voyages autour de ce que vous aimez vivre. Découvrez des expériences, préparez votre voyage et retrouvez au même endroit les propositions liées à vos besoins.",
  },
  {
    id: "browse",
    category: "Découvrir Odyssey",
    question: "Comment découvrir les expériences proposées ?",
    answer:
      "Parcourez les expériences par envie — culture, gastronomie, nature, musique ou détente — puis ouvrez une fiche pour en savoir plus. Vous pouvez aussi démarrer depuis votre espace voyageur.",
  },
  {
    id: "account",
    category: "Mon compte",
    question: "Dois-je créer un compte pour organiser un voyage ?",
    answer:
      "Oui. Connectez-vous ou créez votre compte pour accéder à votre espace voyageur, créer vos voyages et retrouver les propositions qui vous sont adressées.",
  },
  {
    id: "create-trip",
    category: "Organiser mon voyage",
    question: "Comment créer un voyage ?",
    answer:
      "Depuis votre espace voyageur, ouvrez « Mes voyages », puis choisissez « Créer un voyage ». Donnez-lui un titre et indiquez ses dates. Vous pourrez ensuite préciser vos besoins dans le détail du voyage.",
  },
  {
    id: "add-needs",
    category: "Organiser mon voyage",
    question: "Quels besoins puis-je ajouter à mon voyage ?",
    answer:
      "Vous pouvez préciser différents besoins de transport et de séjour, comme un vol, un hébergement, un transfert, une voiture ou un bus. Les informations demandées dépendent du type de besoin choisi.",
  },
  {
    id: "trip-status",
    category: "Organiser mon voyage",
    question: "Où retrouver les informations de mon voyage ?",
    answer:
      "Ouvrez « Mes voyages » depuis votre espace voyageur, puis sélectionnez le voyage concerné. Vous y retrouverez ses étapes, vos besoins et les propositions associées.",
  },
  {
    id: "receive-proposal",
    category: "Propositions",
    question: "Comment recevoir une proposition ?",
    answer:
      "Après avoir renseigné un besoin dans votre voyage, un conseiller peut préparer une proposition correspondante. Elle apparaît dans « Mes propositions » et dans le voyage concerné lorsqu’elle est disponible.",
  },
  {
    id: "accept-proposal",
    category: "Propositions",
    question: "Puis-je accepter ou refuser une proposition ?",
    answer:
      "Oui. Dans « Mes propositions », examinez les détails puis choisissez d’accepter ou de refuser la proposition. Une proposition déjà traitée ou expirée ne peut plus être modifiée.",
  },
  {
    id: "proposal-price",
    category: "Propositions",
    question: "Le prix affiché comprend-il tous les frais ?",
    answer:
      "Vérifiez le détail de chaque proposition et les informations de votre voyage avant de confirmer. Les frais du fournisseur et les frais d’accompagnement Odyssey peuvent être présentés séparément.",
  },
  {
    id: "payment",
    category: "Réservation & paiement",
    question: "Comment régler une réservation ?",
    answer:
      "Les modalités de paiement dépendent de la réservation et du fournisseur. Après avoir accepté une proposition, consultez les informations affichées dans votre voyage et suivez le lien de paiement lorsqu’il est proposé.",
  },
  {
    id: "cancel",
    category: "Réservation & paiement",
    question: "Comment modifier ou annuler une réservation ?",
    answer:
      "Les conditions de modification et d’annulation dépendent du fournisseur et de la réservation. Consultez les conditions indiquées dans votre proposition ou vos documents de réservation avant toute démarche.",
  },
  {
    id: "security",
    category: "Mon compte",
    question: "Je n’arrive pas à accéder à mon compte, que faire ?",
    answer:
      "Vérifiez l’adresse utilisée lors de votre inscription et réessayez de vous connecter. Si le problème persiste, utilisez la procédure de récupération proposée sur la page de connexion.",
  },
];

function normalize(value: string) {
  return value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLocaleLowerCase("fr");
}

export function FAQPage() {
  const [search, setSearch] = useState("");
  const [activeCategory, setActiveCategory] = useState<FAQCategory>("Toutes les questions");
  const [openQuestion, setOpenQuestion] = useState<string | null>(null);
  const searchInputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    document.title = "FAQ | Odyssey";

    const onSearchShortcut = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        searchInputRef.current?.focus();
      }
    };

    window.addEventListener("keydown", onSearchShortcut);
    return () => window.removeEventListener("keydown", onSearchShortcut);
  }, []);

  const filteredQuestions = useMemo(() => {
    const normalizedSearch = normalize(search.trim());

    return questions.filter((item) => {
      const matchesCategory = activeCategory === "Toutes les questions" || item.category === activeCategory;
      const matchesSearch =
        normalizedSearch.length === 0 ||
        normalize(`${item.question} ${item.answer} ${item.category}`).includes(normalizedSearch);
      return matchesCategory && matchesSearch;
    });
  }, [activeCategory, search]);

  return (
    <div className="faq-page">
      <header className="faq-header">
        <Link className="faq-brand" to="/" aria-label="Odyssey, accueil">
          <img src={odysseyLogo} alt="" />
        </Link>
        <nav className="faq-header__nav" aria-label="Navigation principale">
          <Link to="/traveler/discover">Expériences</Link>
          <Link to="/traveler/trips">Mes voyages</Link>
          <Link className="is-current" to="/faq" aria-current="page">Aide & FAQ</Link>
        </nav>
        <Link className="faq-header__account" to="/login">
          Mon espace <ArrowRight size={16} aria-hidden="true" />
        </Link>
      </header>

      <main>
        <section className="faq-hero" aria-labelledby="faq-title">
          <div className="faq-hero__inner">
            <Link className="faq-back-link" to="/">
              <ArrowLeft size={16} aria-hidden="true" /> Retour à l’accueil
            </Link>
            <div className="faq-hero__copy">
              <span className="faq-eyebrow"><Sparkles size={15} aria-hidden="true" /> ON EST LÀ POUR VOUS</span>
              <h1 id="faq-title">Une question ?<br /><span>On vous éclaire.</span></h1>
              <p>Tout ce qu’il faut savoir pour imaginer et organiser votre prochaine expérience.</p>
            </div>
            <div className="faq-hero__decoration" aria-hidden="true">
              <span className="faq-hero__orb faq-hero__orb--one" />
              <span className="faq-hero__orb faq-hero__orb--two" />
              <div className="faq-hero__note"><CircleHelp size={22} /><span>Le voyage commence<br />par une bonne question.</span></div>
            </div>
          </div>
        </section>

        <section className="faq-content" aria-label="Questions fréquentes">
          <div className="faq-search">
            <Search size={20} aria-hidden="true" />
            <label className="faq-sr-only" htmlFor="faq-search-input">Rechercher une question</label>
            <input
              ref={searchInputRef}
              id="faq-search-input"
              type="search"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Rechercher un sujet, un mot-clé…"
              autoComplete="off"
              aria-keyshortcuts="Control+K Meta+K"
            />
            {search && (
              <button type="button" className="faq-search__clear" onClick={() => setSearch("")} aria-label="Effacer la recherche">
                <X size={17} aria-hidden="true" />
              </button>
            )}
            <span className="faq-search__shortcut" aria-hidden="true">⌘ K</span>
          </div>

          <div className="faq-layout">
            <aside className="faq-categories" aria-label="Catégories de questions">
              <p className="faq-categories__title">Parcourir par thème</p>
              <div className="faq-categories__list">
                {categories.map(({ label, icon: Icon }) => (
                  <button
                    key={label}
                    type="button"
                    className={`faq-category${activeCategory === label ? " is-active" : ""}`}
                    aria-pressed={activeCategory === label}
                    onClick={() => {
                      setActiveCategory(label);
                      setOpenQuestion(null);
                    }}
                  >
                    <Icon size={17} aria-hidden="true" />
                    <span>{label}</span>
                    {activeCategory === label && <Check size={15} className="faq-category__check" aria-hidden="true" />}
                  </button>
                ))}
              </div>
              <div className="faq-help-card">
                <span className="faq-help-card__icon"><CircleHelp size={19} aria-hidden="true" /></span>
                <strong>Besoin d’aide sur votre voyage ?</strong>
                <p>Retrouvez vos informations et vos propositions dans votre espace voyageur.</p>
                <Link to="/login">Accéder à mon espace <ArrowRight size={15} aria-hidden="true" /></Link>
              </div>
            </aside>

            <div className="faq-results">
              <div className="faq-results__heading">
                <div>
                  <span className="faq-results__eyebrow">CENTRE D’AIDE</span>
                  <h2>{activeCategory === "Toutes les questions" ? "Les questions fréquentes" : activeCategory}</h2>
                </div>
                <p aria-live="polite" aria-atomic="true">{filteredQuestions.length} réponse{filteredQuestions.length !== 1 ? "s" : ""}</p>
              </div>
              {filteredQuestions.length > 0 ? (
                <div className="faq-accordion">
                  {filteredQuestions.map((item, index) => {
                    const isOpen = openQuestion === item.id;
                    const answerId = `faq-answer-${item.id}`;
                    return (
                      <article className={`faq-item${isOpen ? " is-open" : ""}`} key={item.id}>
                        <h3>
                          <button
                            className="faq-item__trigger"
                            type="button"
                            id={`faq-question-${item.id}`}
                            aria-expanded={isOpen}
                            aria-controls={answerId}
                            onClick={() => setOpenQuestion(isOpen ? null : item.id)}
                          >
                            <span className="faq-item__number">{String(index + 1).padStart(2, "0")}</span>
                            <span>{item.question}</span>
                            <ChevronDown className="faq-item__chevron" size={20} aria-hidden="true" />
                          </button>
                        </h3>
                        <div className="faq-item__answer" id={answerId} role="region" aria-labelledby={`faq-question-${item.id}`} hidden={!isOpen}>
                          <p>{item.answer}</p>
                        </div>
                      </article>
                    );
                  })}
                </div>
              ) : (
                <div className="faq-empty" role="status">
                  <span className="faq-empty__icon"><Search size={22} aria-hidden="true" /></span>
                  <h3>Aucune réponse trouvée</h3>
                  <p>Essayez un autre mot-clé ou choisissez une catégorie différente.</p>
                  <button
                    type="button"
                    onClick={() => {
                      setSearch("");
                      setActiveCategory("Toutes les questions");
                    }}
                  >
                    Afficher toutes les questions
                  </button>
                </div>
              )}
              <p className="faq-note">Vous ne trouvez pas votre réponse ? <Link to="/login">Connectez-vous à votre espace voyageur</Link> pour retrouver les détails de votre voyage.</p>
            </div>
          </div>
        </section>
      </main>

      <footer className="faq-footer">
        <Link className="faq-footer__brand" to="/"><img src={odysseyLogo} alt="" /> Des voyages pensés pour vivre autrement.</Link>
        <span>© 2026 Odyssey</span>
      </footer>
    </div>
  );
}

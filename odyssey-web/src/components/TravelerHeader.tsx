import { useAuth0 } from "@auth0/auth0-react";
import { ChevronDown, Compass, FileText, Luggage, Menu, Search, UserRound, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Link, NavLink } from "react-router-dom";
import odysseyBird from "../assets/odyssey-bird.png";

export function TravelerHeader() {
  const { isAuthenticated, isLoading, logout, user } = useAuth0();
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isAccountMenuOpen, setIsAccountMenuOpen] = useState(false);
  const accountMenuRef = useRef<HTMLDivElement | null>(null);

  const userDisplayName = user?.name ?? user?.nickname ?? user?.email ?? "Voyageur";
  const userInitial = userDisplayName.trim().charAt(0).toUpperCase() || "V";

  const navItems = [
    { label: "Découvrir", to: "/traveler/discover", icon: Compass },
    { label: "Mes voyages", to: "/traveler/trips", icon: Luggage },
    { label: "Mes propositions", to: "/traveler/quotes", icon: FileText },
  ];

  useEffect(() => {
    if (!isAccountMenuOpen) return;

    const onPointerDown = (event: MouseEvent) => {
      if (!accountMenuRef.current?.contains(event.target as Node)) {
        setIsAccountMenuOpen(false);
      }
    };

    const onEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setIsAccountMenuOpen(false);
      }
    };

    document.addEventListener("mousedown", onPointerDown);
    document.addEventListener("keydown", onEscape);

    return () => {
      document.removeEventListener("mousedown", onPointerDown);
      document.removeEventListener("keydown", onEscape);
    };
  }, [isAccountMenuOpen]);

  const logoutHandler = () =>
    logout({
      logoutParams: {
        returnTo: window.location.origin,
      },
    });

  return (
    <header className="traveler-header">
      <div className="traveler-header-inner">
        <div className="traveler-brand" aria-label="Odyssey">
          <img src={odysseyBird} alt="Odyssey" className="traveler-brand__mark" />
          <span className="traveler-brand__text">Odyssey</span>
        </div>

        <nav className="traveler-nav" aria-label="Navigation voyageur">
          {navItems.map(({ label, to, icon: Icon }) => (
            <NavLink key={to} aria-label={label} className={({ isActive }) => `traveler-nav-item${isActive ? " active" : ""}`} to={to}>
              <Icon size={17} strokeWidth={2.2} />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="traveler-profile">
          <button type="button" className="traveler-nav-search" aria-label="Recherche">
            <Search size={16} strokeWidth={2.2} />
          </button>

          {isLoading ? (
            <div className="traveler-user-summary">
              <span className="traveler-user-avatar">V</span>
              <div className="traveler-user-meta">
                <strong>Chargement...</strong>
                <span>Connexion</span>
              </div>
            </div>
          ) : isAuthenticated ? (
            <div className="traveler-account-menu" ref={accountMenuRef}>
              <button
                type="button"
                className={`traveler-account-trigger${isAccountMenuOpen ? " open" : ""}`}
                aria-haspopup="menu"
                aria-expanded={isAccountMenuOpen}
                aria-label="Ouvrir le menu du compte"
                onClick={() => setIsAccountMenuOpen((open) => !open)}
              >
                <div className="traveler-user-summary">
                  <span className="traveler-user-avatar" aria-label={userDisplayName}>
                    {user?.picture ? <img src={user.picture} alt={userDisplayName} /> : userInitial}
                  </span>
                  <div className="traveler-user-meta">
                    <strong>{userDisplayName}</strong>
                    <span>Mon espace</span>
                  </div>
                </div>
                <ChevronDown size={14} strokeWidth={2.3} className="traveler-account-trigger__chevron" />
              </button>

              <div className={`traveler-account-dropdown${isAccountMenuOpen ? " open" : ""}`} role="menu" aria-label="Menu du compte voyageur">
                <NavLink
                  to="/traveler/profile"
                  className={({ isActive }) => `traveler-account-dropdown__item${isActive ? " active" : ""}`}
                  onClick={() => setIsAccountMenuOpen(false)}
                >
                  <UserRound size={15} strokeWidth={2.2} />
                  <span>Mon profil</span>
                </NavLink>

                <button
                  type="button"
                  className="traveler-account-dropdown__item"
                  onClick={() => {
                    setIsAccountMenuOpen(false);
                    logoutHandler();
                  }}
                >
                  <span>Se déconnecter</span>
                </button>
              </div>
            </div>
          ) : (
            <>
              <div className="traveler-user-summary">
                <span className="traveler-user-avatar" aria-label="Voyageur">
                  V
                </span>
                <div className="traveler-user-meta">
                  <strong>Voyageur</strong>
                  <span>Non connecté</span>
                </div>
              </div>
              <Link to="/login" className="traveler-auth-button">
                Se connecter
              </Link>
            </>
          )}

          <button
            type="button"
            className="traveler-mobile-menu-toggle"
            aria-label={isMenuOpen ? "Fermer le menu" : "Ouvrir le menu"}
            aria-expanded={isMenuOpen}
            onClick={() => setIsMenuOpen((open) => !open)}
          >
            {isMenuOpen ? <X size={18} strokeWidth={2.2} /> : <Menu size={18} strokeWidth={2.2} />}
          </button>
        </div>
      </div>

      <nav className={`traveler-mobile-menu${isMenuOpen ? " open" : ""}`} aria-label="Menu mobile">
        {navItems.map(({ label, to, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) => `traveler-mobile-menu-item${isActive ? " active" : ""}`}
            onClick={() => setIsMenuOpen(false)}
          >
            <Icon size={16} strokeWidth={2.2} />
            <span>{label}</span>
          </NavLink>
        ))}

        {isAuthenticated ? (
          <>
            <div className="traveler-mobile-menu-divider" role="presentation" />
            <p className="traveler-mobile-menu-label">Compte</p>

            <NavLink
              to="/traveler/profile"
              className={({ isActive }) => `traveler-mobile-menu-item${isActive ? " active" : ""}`}
              onClick={() => setIsMenuOpen(false)}
            >
              <UserRound size={16} strokeWidth={2.2} />
              <span>Mon profil</span>
            </NavLink>

            <button
              type="button"
              className="traveler-mobile-logout"
              onClick={() => {
                setIsMenuOpen(false);
                logoutHandler();
              }}
            >
              Se déconnecter
            </button>
          </>
        ) : (
          <Link to="/login" className="traveler-mobile-logout" onClick={() => setIsMenuOpen(false)}>
            Se connecter
          </Link>
        )}
      </nav>
    </header>
  );
}

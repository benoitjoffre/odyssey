import { useAuth0 } from "@auth0/auth0-react";
import { Compass, FileText, Luggage, Menu, Search, X } from "lucide-react";
import { useState } from "react";
import { NavLink } from "react-router-dom";
import odysseyBird from "../assets/odyssey-bird.png";

export function TravelerHeader() {
  const { isAuthenticated, isLoading, loginWithRedirect, logout, user } = useAuth0();
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  const userDisplayName = user?.name ?? user?.nickname ?? user?.email ?? "Voyageur";
  const userInitial = userDisplayName.trim().charAt(0).toUpperCase() || "V";

  const navItems = [
    { label: "Découvrir", to: "/traveler/discover", icon: Compass },
    { label: "Mes voyages", to: "/traveler/trips", icon: Luggage },
    { label: "Mes propositions", to: "/traveler/quotes", icon: FileText },
  ];

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
            <>
              <div className="traveler-user-summary">
                <span className="traveler-user-avatar" aria-label={userDisplayName}>
                  {user?.picture ? <img src={user.picture} alt={userDisplayName} /> : userInitial}
                </span>
                <div className="traveler-user-meta">
                  <strong>{userDisplayName}</strong>
                  <span>Mon espace</span>
                </div>
              </div>
              <button type="button" className="traveler-auth-button" onClick={logoutHandler}>
                Se déconnecter
              </button>
            </>
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
              <button type="button" className="traveler-auth-button" onClick={() => void loginWithRedirect()}>
                Se connecter
              </button>
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
      </nav>
    </header>
  );
}

import { useEffect } from "react";
import { Compass, FileText, Luggage, Plane, UserRound } from "lucide-react";
import { NavLink, Outlet } from "react-router-dom";

export function TravelerLayout() {
  useEffect(() => {
    document.title = "Odyssey | Espace Traveler";
    return () => {
      document.title = "Odyssey | Espace Agent";
    };
  }, []);

  return (
    <div className="traveler-shell">
      <header className="traveler-header">
        <div className="traveler-header-inner">
          <div className="traveler-brand" aria-label="Odyssey">
            <span className="brand-mark">
              <Plane size={20} strokeWidth={2.2} />
            </span>
            <span>Odyssey</span>
          </div>

          <nav className="traveler-nav" aria-label="Navigation voyageur">
            <NavLink aria-label="Découvrir" className={({ isActive }) => `traveler-nav-item${isActive ? " active" : ""}`} to="/traveler/discover">
              <Compass size={18} />
              <span>Découvrir</span>
            </NavLink>
            <NavLink aria-label="Mes voyages" className={({ isActive }) => `traveler-nav-item${isActive ? " active" : ""}`} to="/traveler/trips">
              <Luggage size={18} />
              <span>Mes voyages</span>
            </NavLink>
            <NavLink
              aria-label="Mes propositions"
              className={({ isActive }) => `traveler-nav-item${isActive ? " active" : ""}`}
              to="/traveler/quotes"
            >
              <FileText size={18} />
              <span>Mes propositions</span>
            </NavLink>
          </nav>

          <div className="traveler-profile">
            <span className="traveler-avatar">
              <UserRound size={18} />
            </span>
            <div>
              <strong>Voyageur #1</strong>
              <span>Mon espace</span>
            </div>
          </div>
        </div>
      </header>

      <main className="traveler-content">
        <Outlet />
      </main>
    </div>
  );
}

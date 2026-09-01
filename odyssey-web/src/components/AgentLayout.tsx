import { Bell, FileText, LayoutDashboard, MapPin, Plane } from "lucide-react";
import { NavLink, Outlet } from "react-router-dom";

const navigation = [
  { label: "Dashboard", to: "/agent", icon: LayoutDashboard, end: true },
  { label: "Demandes", to: "/agent/booking-requests", icon: Bell },
  { label: "Propositions", to: "/agent/quotes", icon: FileText },
];

export function AgentLayout() {
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand" aria-label="Odyssey">
          <span className="brand-mark">
            <Plane size={20} strokeWidth={2.2} />
          </span>
          <span>Odyssey</span>
        </div>

        <nav className="sidebar-nav" aria-label="Navigation agent">
          {navigation.map(({ label, to, icon: Icon, end }) => (
            <NavLink key={to} to={to} end={end} className={({ isActive }) => `nav-link${isActive ? " active" : ""}`}>
              <Icon size={19} />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-destination">
          <MapPin size={18} />
          <div>
            <span>Destination du mois</span>
            <strong>Kyoto, Japon</strong>
          </div>
        </div>
      </aside>

      <div className="main-column">
        <header className="topbar">
          <div className="topbar-context">
            <span>Espace professionnel</span>
            <strong>Équipe Agent</strong>
          </div>
          <div className="agent-profile">
            <div className="agent-avatar" aria-hidden="true">
              A
            </div>
            <div>
              <strong>Agent #1</strong>
              <span>Connecté</span>
            </div>
          </div>
        </header>

        <main className="main-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

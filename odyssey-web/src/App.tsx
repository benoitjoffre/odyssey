import { useAuth0 } from "@auth0/auth0-react";
import { useEffect, useState, type ReactNode } from "react";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { getCurrentUser } from "./api/currentUser";
import { configureApiClient } from "./api/client";
import { AgentLayout } from "./components/AgentLayout";
import { TravelerLayout } from "./components/TravelerLayout";
import type { CurrentUser } from "./types/currentUser";
import { AgentDashboardPage } from "./pages/AgentDashboardPage";
import { AgentEventCreatePage } from "./pages/agent/AgentEventCreatePage";
import { AgentEventsPage } from "./pages/agent/AgentEventsPage";
import { AgentExperienceCreatePage } from "./pages/agent/AgentExperienceCreatePage";
import { AgentExperiencesPage } from "./pages/agent/AgentExperiencesPage";
import { AgentBookingRequestsPage } from "./pages/agent/AgentBookingRequestsPage";
import { BookingRequestPage } from "./pages/BookingRequestPage";
import { ComingSoonPage } from "./pages/ComingSoonPage";
import { HomePage } from "./pages/HomePage";
import { TravelerDiscoverPage } from "./pages/traveler/TravelerDiscoverPage";
import { TravelerEventDetailPage } from "./pages/traveler/TravelerEventDetailPage";
import { TravelerQuotesPage } from "./pages/traveler/TravelerQuotesPage";
import { TravelerTripDetailPage } from "./pages/traveler/TravelerTripDetailPage";
import { TravelerTripCreatePage } from "./pages/traveler/TravelerTripCreatePage";
import { TravelerTripsPage } from "./pages/traveler/TravelerTripsPage";

function getDashboardPathFromRoles(roles: string[]): string {
  if (roles.includes("AGENT")) return "/agent";
  if (roles.includes("TRAVELER")) return "/traveler";
  return "/";
}

function useCurrentUser() {
  const { isAuthenticated, isLoading } = useAuth0();
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(() => isAuthenticated);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isAuthenticated) {
      return;
    }

    let active = true;
    const controller = new AbortController();

    const loadUser = async () => {
      setLoading(true);
      setError(null);

      try {
        const user = await getCurrentUser(controller.signal);
        if (!active) return;
        setCurrentUser(user);
      } catch (requestError: unknown) {
        if (!active) return;
        if (requestError instanceof DOMException && requestError.name === "AbortError") {
          return;
        }
        setCurrentUser(null);
        setError("Impossible de charger le profil utilisateur.");
      } finally {
        if (active && !controller.signal.aborted) {
          setLoading(false);
        }
      }
    };

    void loadUser();

    return () => {
      active = false;
      controller.abort();
    };
  }, [isAuthenticated]);

  return {
    currentUser,
    isLoading: isLoading || loading || (isAuthenticated && currentUser === null && error === null),
    error,
  };
}

function AuthGuard({ children, requiredRole }: { children: ReactNode; requiredRole?: "TRAVELER" | "AGENT" }) {
  const location = useLocation();
  const { isAuthenticated, isLoading, loginWithRedirect } = useAuth0();
  const { currentUser, isLoading: isUserLoading } = useCurrentUser();
  const roles = currentUser?.roles ?? [];

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      void loginWithRedirect({ appState: { targetUrl: location.pathname } });
    }
  }, [isAuthenticated, isLoading, location.pathname, loginWithRedirect]);

  if (isLoading || (isAuthenticated && isUserLoading)) {
    return <div style={{ display: "grid", placeItems: "center", minHeight: "100vh", color: "#1d3c39", fontWeight: 600 }}>Chargement…</div>;
  }

  if (!isAuthenticated) {
    return (
      <div style={{ display: "grid", placeItems: "center", minHeight: "100vh", color: "#1d3c39", fontWeight: 600 }}>
        Redirection vers la connexion…
      </div>
    );
  }

  if (requiredRole && !roles.includes(requiredRole)) {
    return <Navigate to={getDashboardPathFromRoles(roles)} replace />;
  }

  return <>{children}</>;
}

function App() {
  const { getAccessTokenSilently } = useAuth0();

  useEffect(() => {
    configureApiClient(() => getAccessTokenSilently());

    return () => {
      configureApiClient(null);
    };
  }, [getAccessTokenSilently]);

  return (
    <Routes>
      <Route path="/" element={<HomePage />} />

      <Route
        path="/agent"
        element={
          <AuthGuard requiredRole="AGENT">
            <AgentLayout />
          </AuthGuard>
        }
      >
        <Route index element={<AgentDashboardPage />} />
        <Route path="booking-requests" element={<AgentBookingRequestsPage />} />
        <Route path="booking-requests/:id" element={<BookingRequestPage />} />
        <Route path="quotes" element={<ComingSoonPage title="Propositions" />} />
        <Route path="experiences" element={<AgentExperiencesPage />} />
        <Route path="experiences/new" element={<AgentExperienceCreatePage />} />
        <Route path="events" element={<AgentEventsPage />} />
        <Route path="events/new" element={<AgentEventCreatePage />} />
      </Route>

      <Route
        path="/traveler"
        element={
          <AuthGuard requiredRole="TRAVELER">
            <TravelerLayout />
          </AuthGuard>
        }
      >
        <Route index element={<Navigate to="discover" replace />} />
        <Route path="discover" element={<TravelerDiscoverPage />} />
        <Route path="events/:eventId" element={<TravelerEventDetailPage />} />
        <Route path="trips" element={<TravelerTripsPage />} />
        <Route path="trips/new" element={<TravelerTripCreatePage />} />
        <Route path="trips/:tripId" element={<TravelerTripDetailPage />} />
        <Route path="quotes" element={<TravelerQuotesPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;

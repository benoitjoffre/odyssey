import { useAuth0 } from "@auth0/auth0-react";
import { useEffect, type ReactNode } from "react";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { configureApiClient } from "./api/client";
import { AgentLayout } from "./components/AgentLayout";
import { TravelerLayout } from "./components/TravelerLayout";
import { CurrentUserProvider } from "./auth/CurrentUserProvider";
import { useCurrentUser } from "./auth/useCurrentUser";
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
import { TravelerOnboardingPage } from "./pages/traveler/TravelerOnboardingPage";
import { TravelerQuotesPage } from "./pages/traveler/TravelerQuotesPage";
import { TravelerTripDetailPage } from "./pages/traveler/TravelerTripDetailPage";
import { TravelerTripCreatePage } from "./pages/traveler/TravelerTripCreatePage";
import { TravelerTripsPage } from "./pages/traveler/TravelerTripsPage";

function getDashboardPathFromRoles(roles: string[]): string {
  if (roles.includes("AGENT")) return "/agent";
  if (roles.includes("TRAVELER")) return "/traveler";
  return "/";
}

function AuthGuard({ children, requiredRole }: { children: ReactNode; requiredRole?: "TRAVELER" | "AGENT" }) {
  const location = useLocation();
  const { isAuthenticated, isLoading, loginWithRedirect } = useAuth0();
  const { currentUser, isLoading: isUserLoading } = useCurrentUser();
  const roles = currentUser?.roles ?? [];

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      void loginWithRedirect({ appState: { targetUrl: `${location.pathname}${location.search}` } });
    }
  }, [isAuthenticated, isLoading, location.pathname, location.search, loginWithRedirect]);

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

  if (requiredRole === "TRAVELER" && roles.includes("TRAVELER")) {
    const onboardingCompleted = currentUser?.onboardingCompleted;

    if (onboardingCompleted === false && location.pathname !== "/onboarding") {
      return <Navigate to="/onboarding" replace />;
    }

    if (onboardingCompleted === true && location.pathname === "/onboarding") {
      return <Navigate to={getDashboardPathFromRoles(roles)} replace />;
    }
  }

  return <>{children}</>;
}

function App() {
  const { getAccessTokenSilently } = useAuth0();

  useEffect(() => {
    configureApiClient(async () => {
      // auth0-spa-js may resolve to `undefined` (e.g. when its internal
      // session ceiling is reached) instead of throwing. The API client
      // contract requires a real token, so treat a missing token as an
      // explicit failure rather than silently forwarding `undefined`.
      const token = await getAccessTokenSilently();
      if (!token) {
        throw new Error("getAccessTokenSilently() did not return a token");
      }
      return token;
    });

    return () => {
      configureApiClient(null);
    };
  }, [getAccessTokenSilently]);

  return (
    <CurrentUserProvider>
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
          path="/onboarding"
          element={
            <AuthGuard requiredRole="TRAVELER">
              <TravelerOnboardingPage />
            </AuthGuard>
          }
        />

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
    </CurrentUserProvider>
  );
}

export default App;

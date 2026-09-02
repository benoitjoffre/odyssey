import { Navigate, Route, Routes } from "react-router-dom";
import { AgentLayout } from "./components/AgentLayout";
import { TravelerLayout } from "./components/TravelerLayout";
import { AgentDashboardPage } from "./pages/AgentDashboardPage";
import { AgentEventCreatePage } from "./pages/agent/AgentEventCreatePage";
import { AgentEventsPage } from "./pages/agent/AgentEventsPage";
import { AgentExperienceCreatePage } from "./pages/agent/AgentExperienceCreatePage";
import { AgentExperiencesPage } from "./pages/agent/AgentExperiencesPage";
import { BookingRequestPage } from "./pages/BookingRequestPage";
import { ComingSoonPage } from "./pages/ComingSoonPage";
import { TravelerDiscoverPage } from "./pages/traveler/TravelerDiscoverPage";
import { TravelerEventDetailPage } from "./pages/traveler/TravelerEventDetailPage";
import { TravelerQuotesPage } from "./pages/traveler/TravelerQuotesPage";
import { TravelerTripDetailPage } from "./pages/traveler/TravelerTripDetailPage";
import { TravelerTripsPage } from "./pages/traveler/TravelerTripsPage";

function App() {
  return (
    <Routes>
      <Route path="/agent" element={<AgentLayout />}>
        <Route index element={<AgentDashboardPage />} />
        <Route path="booking-requests" element={<ComingSoonPage title="Demandes" />} />
        <Route path="booking-requests/:id" element={<BookingRequestPage />} />
        <Route path="quotes" element={<ComingSoonPage title="Propositions" />} />
        <Route path="experiences" element={<AgentExperiencesPage />} />
        <Route path="experiences/new" element={<AgentExperienceCreatePage />} />
        <Route path="events" element={<AgentEventsPage />} />
        <Route path="events/new" element={<AgentEventCreatePage />} />
      </Route>
      <Route path="/traveler" element={<TravelerLayout />}>
        <Route index element={<Navigate to="discover" replace />} />
        <Route path="discover" element={<TravelerDiscoverPage />} />
        <Route path="events/:eventId" element={<TravelerEventDetailPage />} />
        <Route path="trips" element={<TravelerTripsPage />} />
        <Route path="trips/:tripId" element={<TravelerTripDetailPage />} />
        <Route path="quotes" element={<TravelerQuotesPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/agent" replace />} />
    </Routes>
  );
}

export default App;

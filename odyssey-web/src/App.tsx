import { Navigate, Route, Routes } from "react-router-dom";
import { AgentLayout } from "./components/AgentLayout";
import { AgentDashboardPage } from "./pages/AgentDashboardPage";
import { BookingRequestPage } from "./pages/BookingRequestPage";
import { ComingSoonPage } from "./pages/ComingSoonPage";

function App() {
  return (
    <Routes>
      <Route path="/agent" element={<AgentLayout />}>
        <Route index element={<AgentDashboardPage />} />
        <Route path="booking-requests" element={<ComingSoonPage title="Demandes" />} />
        <Route path="booking-requests/:id" element={<BookingRequestPage />} />
        <Route path="quotes" element={<ComingSoonPage title="Propositions" />} />
      </Route>
      <Route path="*" element={<Navigate to="/agent" replace />} />
    </Routes>
  );
}

export default App;

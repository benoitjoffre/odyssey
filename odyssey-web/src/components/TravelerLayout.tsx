import { useEffect } from "react";
import { Outlet } from "react-router-dom";
import { TravelerHeader } from "./TravelerHeader";

export function TravelerLayout() {
  useEffect(() => {
    document.title = "Odyssey | Espace Traveler";
    return () => {
      document.title = "Odyssey | Espace Agent";
    };
  }, []);

  return (
    <div className="traveler-shell">
      <TravelerHeader />

      <main className="traveler-content">
        <Outlet />
      </main>
    </div>
  );
}

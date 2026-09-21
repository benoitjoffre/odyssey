import { useCallback, useEffect, useState, type ReactNode } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import { getCurrentUser } from "../api/currentUser";
import type { CurrentUser } from "../types/currentUser";
import { CurrentUserContext } from "./CurrentUserContext";

export function CurrentUserProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated, isLoading: isAuthLoading } = useAuth0();
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

  // Not called from any effect: invoked imperatively (e.g. after the onboarding
  // submit succeeds) to refresh the shared current-user state on demand.
  const refreshCurrentUser = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const user = await getCurrentUser();
      setCurrentUser(user);
    } catch {
      setCurrentUser(null);
      setError("Impossible de charger le profil utilisateur.");
    } finally {
      setLoading(false);
    }
  }, []);

  return (
    <CurrentUserContext.Provider
      value={{
        currentUser,
        isLoading: isAuthLoading || loading || (isAuthenticated && currentUser === null && error === null),
        error,
        refreshCurrentUser,
      }}
    >
      {children}
    </CurrentUserContext.Provider>
  );
}

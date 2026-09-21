import { createContext } from "react";
import type { CurrentUser } from "../types/currentUser";

export interface CurrentUserContextValue {
  currentUser: CurrentUser | null;
  isLoading: boolean;
  error: string | null;
  refreshCurrentUser: () => Promise<void>;
}

export const CurrentUserContext = createContext<CurrentUserContextValue | null>(null);

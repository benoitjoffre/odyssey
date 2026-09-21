import { useContext } from "react";
import { CurrentUserContext, type CurrentUserContextValue } from "./CurrentUserContext";

export function useCurrentUser(): CurrentUserContextValue {
  const context = useContext(CurrentUserContext);
  if (!context) {
    throw new Error("useCurrentUser must be used within a CurrentUserProvider");
  }
  return context;
}

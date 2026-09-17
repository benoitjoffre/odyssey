import type { ReactNode } from "react";
import { Auth0Provider, type AppState } from "@auth0/auth0-react";
import { useNavigate } from "react-router-dom";

interface AuthProviderProps {
  children: ReactNode;
}

export default function AuthProvider({ children }: AuthProviderProps) {
  const navigate = useNavigate();

  const handleRedirectCallback = (appState?: AppState) => {
    navigate(appState?.targetUrl ?? "/", { replace: true });
  };

  return (
    <Auth0Provider
      domain="odysseyvoyage.eu.auth0.com"
      clientId="7H4ozdtnerU21MAs51SScSZAUe8bgZYW"
      authorizationParams={{
        redirect_uri: window.location.origin,
        audience: "https://api.odyssey.app",
      }}
      onRedirectCallback={handleRedirectCallback}
    >
      {children}
    </Auth0Provider>
  );
}

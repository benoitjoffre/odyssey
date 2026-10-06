import { useAuth0 } from "@auth0/auth0-react";
import { ArrowLeft, ArrowRight, Globe, LoaderCircle, ShieldCheck, Users } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link, Navigate, useSearchParams } from "react-router-dom";
import { useCurrentUser } from "../auth/useCurrentUser";
import odysseyBird from "../assets/odyssey-bird.png";
import { Button } from "../components/ui/Button";
import plumesImage from "../assets/plumes.png";
import onboardBackground from "../assets/onboard-background.png";

function getDashboardPathFromRoles(roles: string[]): string {
  if (roles.includes("AGENT")) return "/agent";
  if (roles.includes("TRAVELER")) return "/traveler";
  return "/";
}

export function LoginPage() {
  const { isAuthenticated, isLoading, loginWithRedirect } = useAuth0();
  const { currentUser, isLoading: isUserLoading } = useCurrentUser();
  const [searchParams] = useSearchParams();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    document.title = "Odyssey | Connexion";
  }, []);

  const returnTo = useMemo(() => {
    const raw = searchParams.get("returnTo");
    if (!raw || !raw.startsWith("/")) return "/";
    return raw;
  }, [searchParams]);

  if (isLoading || (isAuthenticated && isUserLoading)) {
    return (
      <main className="login-shell" aria-live="polite" aria-busy="true">
        <div className="login-loading">
          <LoaderCircle className="rotating" size={22} aria-hidden="true" />
          <p>Chargement de votre espace Odyssey…</p>
        </div>
      </main>
    );
  }

  if (isAuthenticated) {
    const roles = currentUser?.roles ?? [];
    const onboardingCompleted = currentUser?.onboardingCompleted;

    if (roles.includes("TRAVELER") && onboardingCompleted === false) {
      return <Navigate to="/onboarding" replace />;
    }

    return <Navigate to={getDashboardPathFromRoles(roles)} replace />;
  }

  async function handleLogin() {
    if (submitting) return;

    setSubmitting(true);
    setError(null);

    try {
      await loginWithRedirect({
        appState: {
          targetUrl: returnTo,
        },
      });
    } catch {
      setError("La connexion n’a pas pu être lancée. Réessayez dans un instant.");
      setSubmitting(false);
    }
  }

  async function handleSignup() {
    if (submitting) return;

    setSubmitting(true);
    setError(null);

    try {
      await loginWithRedirect({
        appState: {
          targetUrl: returnTo,
        },
        authorizationParams: {
          screen_hint: "signup",
        },
      });
    } catch {
      setError("La création de compte n’a pas pu être lancée. Réessayez dans un instant.");
      setSubmitting(false);
    }
  }

  return (
    <div className="login-page">
      <div className="login-shell">
        <header className="login-topbar">
          <div className="login-topbar__inner">
            <Link className="login-brand" to="/" aria-label="Odyssey, accueil">
              <img src={odysseyBird} alt="Odyssey" className="login-brand__mark" />
              <span>Odyssey</span>
            </Link>

            <Link className="login-home-link" to="/">
              <ArrowLeft size={15} aria-hidden="true" />
              <span className="login-home-link__desktop">Retour à l&apos;accueil</span>
              <span className="login-home-link__mobile">Retour</span>
            </Link>
          </div>
        </header>

        <main className="login-showcase">
          <aside className="login-visual" aria-hidden="true">
            <img src={onboardBackground} alt="" className="login-visual__bg" />
            <div className="login-visual__overlay" />
            <img src={plumesImage} alt="" className="login-visual__plumes" />
            <div className="login-visual__card">
              <span className="login-visual__card-line" />
              <p>Expériences locales sélectionnées</p>
              <strong>Votre voyage, accompagné de l&apos;envie jusqu&apos;au départ.</strong>
            </div>
          </aside>

          <section className="login-card" aria-label="Connexion Odyssey">
            <span className="eyebrow">Bienvenue sur Odyssey</span>
            <h1>
              Votre prochain
              <br />
              voyage commence ici.
            </h1>
            <p className="login-description">
              Découvrez des expériences qui vous ressemblent et laissez Odyssey vous accompagner dans l&apos;organisation de votre voyage.
            </p>

            <Button size="lg" fullWidth loading={submitting} onClick={() => void handleLogin()} disabled={submitting || isLoading}>
              {submitting ? <LoaderCircle className="rotating" size={18} aria-hidden="true" /> : <ShieldCheck size={18} aria-hidden="true" />}
              {submitting ? "Connexion en cours…" : "Se connecter"}
            </Button>

            <div className="login-divider" aria-hidden="true">
              <span />
              <span>ou</span>
              <span />
            </div>

            <p className="login-signup-caption">Pas encore de compte ?</p>

            <Button variant="outline" fullWidth loading={submitting} onClick={() => void handleSignup()} disabled={submitting || isLoading}>
              Créer un compte
              <ArrowRight size={16} aria-hidden="true" />
            </Button>

            <section className="login-reassurance" aria-label="Nos garanties">
              <article className="login-reassurance__item">
                <ShieldCheck size={15} aria-hidden="true" />
                <p>Connexion sécurisée</p>
              </article>
              <article className="login-reassurance__item">
                <Users size={15} aria-hidden="true" />
                <p>Accompagnement par des experts</p>
              </article>
              <article className="login-reassurance__item">
                <Globe size={15} aria-hidden="true" />
                <p>Des voyages authentiques</p>
              </article>
            </section>

            {error ? (
              <p className="traveler-form-error" role="alert">
                {error}
              </p>
            ) : null}
          </section>
        </main>
      </div>
    </div>
  );
}

import { useRef, useState, type FormEvent } from "react";
import { LoaderCircle, Sparkles, UserRound } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { updateTravelerOnboarding } from "../../api/travelers";
import { useCurrentUser } from "../../auth/useCurrentUser";
import { TravelerHeader } from "../../components/TravelerHeader";
import { FormField } from "../../components/ui/FormField";
import onboardBackground from "../../assets/onboard-background.png";
import plumesImage from "../../assets/plumes.png";

const PREFERRED_LANGUAGES: { value: string; label: string }[] = [
  { value: "fr", label: "Français" },
  { value: "en", label: "English" },
  { value: "es", label: "Español" },
];

export function TravelerOnboardingPage() {
  const navigate = useNavigate();
  const { refreshCurrentUser } = useCurrentUser();
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [whatsappNumber, setWhatsappNumber] = useState("");
  const [preferredLanguage, setPreferredLanguage] = useState("fr");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const submittingRef = useRef(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submittingRef.current) return;

    if (!firstName.trim() || !lastName.trim() || !phoneNumber.trim() || !preferredLanguage) {
      setError("Merci de renseigner tous les champs obligatoires.");
      return;
    }

    submittingRef.current = true;
    setSubmitting(true);
    setError(null);

    try {
      await updateTravelerOnboarding({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        phoneNumber: phoneNumber.trim(),
        whatsappNumber: whatsappNumber.trim() || undefined,
        preferredLanguage,
      });
      await refreshCurrentUser();
      navigate("/traveler");
    } catch {
      setError("Impossible d’enregistrer vos informations. Vérifiez-les puis réessayez.");
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  }

  return (
    <div className="traveler-shell onboarding-page">
      <TravelerHeader />

      <main className="onboarding-main">
        <div className="onboarding-page__container">
          <section className="onboarding-shell" aria-label="Bienvenue sur Odyssey">
            <div className="onboarding-visual">
              <img src={onboardBackground} alt="Paysage de voyage méditerranéen" className="onboarding-visual-image" />
              <div className="onboarding-visual-overlay" aria-hidden="true" />
              <img src={plumesImage} alt="" className="onboarding-visual-ornament" aria-hidden="true" />

              <div className="onboarding-visual-content">
                <p className="onboarding-eyebrow">BIENVENUE SUR ODYSSEY</p>
                <h1 className="onboarding-visual-title">Votre voyage commence ici.</h1>
                <p className="onboarding-visual-description">
                  Partagez quelques informations pour que nous puissions vous accompagner et vous contacter au sujet de votre voyage.
                </p>
              </div>
            </div>

            <section className="onboarding-form-card" aria-labelledby="onboarding-form-title">
              <header className="onboarding-form-header">
                <div className="onboarding-form-kicker">
                  <span className="onboarding-form-kicker__icon" aria-hidden="true">
                    <UserRound size={24} />
                  </span>
                  <p className="onboarding-form-kicker__text">VOTRE ESPACE ODYSSEY</p>
                </div>

                <h2 id="onboarding-form-title" className="onboarding-form-title">
                  Faisons connaissance
                </h2>
                <p className="onboarding-form-description">Quelques informations pour mieux vous accompagner dans la préparation de votre voyage.</p>
              </header>

              <form className="onboarding-form" onSubmit={(event) => void handleSubmit(event)} noValidate>
                <div className="onboarding-name-grid">
                  <FormField label="Prénom" required>
                    <input
                      value={firstName}
                      onChange={(event) => {
                        setFirstName(event.target.value);
                        setError(null);
                      }}
                      placeholder="Ex. John"
                      disabled={submitting}
                      autoComplete="given-name"
                      required
                    />
                  </FormField>

                  <FormField label="Nom" required>
                    <input
                      value={lastName}
                      onChange={(event) => {
                        setLastName(event.target.value);
                        setError(null);
                      }}
                      placeholder="Ex. Doe"
                      disabled={submitting}
                      autoComplete="family-name"
                      required
                    />
                  </FormField>
                </div>

                <FormField label="Téléphone" required>
                  <input
                    type="tel"
                    value={phoneNumber}
                    onChange={(event) => {
                      setPhoneNumber(event.target.value);
                      setError(null);
                    }}
                    placeholder="+33612345678"
                    disabled={submitting}
                    autoComplete="tel"
                    required
                  />
                </FormField>

                <FormField label="WhatsApp (optionnel)" helperText="Laissez vide pour utiliser le même numéro que le téléphone">
                  <input
                    type="tel"
                    value={whatsappNumber}
                    onChange={(event) => {
                      setWhatsappNumber(event.target.value);
                      setError(null);
                    }}
                    placeholder="Laissez vide pour utiliser le même numéro que le téléphone"
                    disabled={submitting}
                  />
                </FormField>

                <FormField label="Langue préférée" required>
                  <select
                    value={preferredLanguage}
                    onChange={(event) => {
                      setPreferredLanguage(event.target.value);
                      setError(null);
                    }}
                    disabled={submitting}
                    required
                  >
                    {PREFERRED_LANGUAGES.map((language) => (
                      <option key={language.value} value={language.value}>
                        {language.label}
                      </option>
                    ))}
                  </select>
                </FormField>

                {error && (
                  <p className="traveler-form-error" role="alert">
                    {error}
                  </p>
                )}

                <button type="submit" className="primary-button onboarding-submit" disabled={submitting}>
                  {submitting ? <LoaderCircle className="rotating" size={18} /> : <Sparkles size={18} />}
                  {submitting ? "Enregistrement…" : "Commencer mon voyage →"}
                </button>
              </form>
            </section>
          </section>
        </div>
      </main>
    </div>
  );
}

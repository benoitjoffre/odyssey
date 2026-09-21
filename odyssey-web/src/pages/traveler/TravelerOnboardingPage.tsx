import { useRef, useState, type FormEvent } from "react";
import { Compass, LoaderCircle, Sparkles } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { updateTravelerOnboarding } from "../../api/travelers";
import { useCurrentUser } from "../../auth/useCurrentUser";

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
    <div className="traveler-content">
      <div className="traveler-page traveler-onboarding-page">
        <section className="traveler-direct-trip-intro">
          <span className="traveler-direct-trip-icon">
            <Compass size={24} />
          </span>
          <div>
            <h1>Bienvenue sur Odyssey</h1>
            <p>Quelques informations nous permettront de vous accompagner et de vous contacter au sujet de votre voyage.</p>
          </div>
        </section>

        <section className="traveler-direct-trip-form-card" aria-labelledby="onboarding-form-title">
          <form className="traveler-direct-trip-form" onSubmit={(event) => void handleSubmit(event)} noValidate>
            <label className="form-field">
              <span>Prénom *</span>
              <input
                value={firstName}
                onChange={(event) => {
                  setFirstName(event.target.value);
                  setError(null);
                }}
                placeholder="Ex. Benoît"
                disabled={submitting}
                autoComplete="given-name"
                required
              />
            </label>
            <label className="form-field">
              <span>Nom *</span>
              <input
                value={lastName}
                onChange={(event) => {
                  setLastName(event.target.value);
                  setError(null);
                }}
                placeholder="Ex. Joffre"
                disabled={submitting}
                autoComplete="family-name"
                required
              />
            </label>
            <label className="form-field">
              <span>Téléphone *</span>
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
            </label>
            <label className="form-field">
              <span>WhatsApp (optionnel)</span>
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
            </label>
            <label className="form-field form-field-wide">
              <span>Langue préférée *</span>
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
            </label>
            {error && (
              <p className="traveler-form-error" role="alert">
                {error}
              </p>
            )}
            <button type="submit" className="primary-button traveler-create-trip" disabled={submitting}>
              {submitting ? <LoaderCircle className="rotating" size={18} /> : <Sparkles size={18} />}
              {submitting ? "Enregistrement…" : "Commencer mon voyage"}
            </button>
          </form>
        </section>
      </div>
    </div>
  );
}

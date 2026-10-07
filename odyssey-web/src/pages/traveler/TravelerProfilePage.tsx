import { useEffect, useMemo, useState, type FormEvent } from "react";
import { updateCurrentTravelerProfile } from "../../api/currentUser";
import { useCurrentUser } from "../../auth/useCurrentUser";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { FormField } from "../../components/ui/FormField";
import { ErrorState, LoadingState } from "../../components/ui/State";
import { emitToast } from "../../components/ui/Toast";
import { PREFERRED_LANGUAGES } from "./travelerProfileConstants";

type ProfileFormValues = {
  firstName: string;
  lastName: string;
  phoneNumber: string;
  whatsappNumber: string;
  preferredLanguage: string;
};

function toProfileValues(input: {
  firstName: string | null;
  lastName: string | null;
  phoneNumber: string | null;
  whatsappNumber: string | null;
  preferredLanguage: string | null;
}): ProfileFormValues {
  return {
    firstName: input.firstName ?? "",
    lastName: input.lastName ?? "",
    phoneNumber: input.phoneNumber ?? "",
    whatsappNumber: input.whatsappNumber ?? "",
    preferredLanguage: input.preferredLanguage ?? "fr",
  };
}

function isSameProfileValues(a: ProfileFormValues, b: ProfileFormValues): boolean {
  return a.firstName === b.firstName
    && a.lastName === b.lastName
    && a.phoneNumber === b.phoneNumber
    && a.whatsappNumber === b.whatsappNumber
    && a.preferredLanguage === b.preferredLanguage;
}

export function TravelerProfilePage() {
  const { currentUser, isLoading, error, refreshCurrentUser } = useCurrentUser();

  useEffect(() => {
    document.title = "Odyssey | Mon profil";
  }, []);

  if (isLoading && !currentUser) {
    return (
      <section className="traveler-page traveler-profile-page" aria-live="polite" aria-busy="true">
        <LoadingState title="Chargement du profil..." description="Récupération de vos informations personnelles." />
      </section>
    );
  }

  if (!currentUser) {
    return (
      <section className="traveler-page traveler-profile-page">
        <ErrorState title={error ?? "Impossible de charger votre profil."} description="Veuillez réessayer dans un instant." />
      </section>
    );
  }

  const initialValues = toProfileValues(currentUser);
  const profileResetKey = [
    currentUser.firstName ?? "",
    currentUser.lastName ?? "",
    currentUser.phoneNumber ?? "",
    currentUser.whatsappNumber ?? "",
    currentUser.preferredLanguage ?? "",
    currentUser.email ?? "",
  ].join("|");

  return (
    <section className="traveler-page traveler-profile-page" aria-labelledby="traveler-profile-title">
      <header className="traveler-profile-heading">
        <p className="eyebrow">MON ESPACE</p>
        <h1 id="traveler-profile-title">Mon profil</h1>
      </header>

      <TravelerProfileForm
        key={profileResetKey}
        initialValues={initialValues}
        email={currentUser.email ?? ""}
        refreshCurrentUser={refreshCurrentUser}
      />
    </section>
  );
}

function TravelerProfileForm({
  initialValues,
  email,
  refreshCurrentUser,
}: {
  initialValues: ProfileFormValues;
  email: string;
  refreshCurrentUser: () => Promise<void>;
}) {
  const [formValues, setFormValues] = useState<ProfileFormValues>(initialValues);
  const [savedValues, setSavedValues] = useState<ProfileFormValues>(initialValues);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const isDirty = useMemo(() => !isSameProfileValues(formValues, savedValues), [formValues, savedValues]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) return;

    const firstName = formValues.firstName.trim();
    const lastName = formValues.lastName.trim();
    const phoneNumber = formValues.phoneNumber.trim();
    const whatsappNumber = formValues.whatsappNumber.trim();
    const preferredLanguage = formValues.preferredLanguage;

    if (!firstName || !lastName || !phoneNumber || !preferredLanguage) {
      setSubmitError("Merci de renseigner tous les champs obligatoires.");
      return;
    }

    setSubmitting(true);
    setSubmitError(null);

    try {
      const updatedCurrentUser = await updateCurrentTravelerProfile({
        firstName,
        lastName,
        phoneNumber,
        whatsappNumber: whatsappNumber || null,
        preferredLanguage,
      });

      const updatedValues = toProfileValues(updatedCurrentUser);
      setFormValues(updatedValues);
      setSavedValues(updatedValues);
      emitToast({
        status: "success",
        message: "Profil mis à jour.",
      });

      await refreshCurrentUser();
    } catch {
      setSubmitError("Impossible d’enregistrer vos modifications. Vérifiez les champs puis réessayez.");
      emitToast({
        status: "error",
        message: "Échec de la sauvegarde du profil.",
      });
    } finally {
      setSubmitting(false);
    }
  }

  function handleCancel() {
    setFormValues(savedValues);
    setSubmitError(null);
  }

  return (
    <Card padding="lg" className="traveler-profile-card">
      <div className="traveler-profile-card__header">
        <h2>Informations personnelles</h2>
      </div>

      <form className="traveler-profile-form" onSubmit={(event) => void handleSubmit(event)} noValidate>
        <div className="traveler-profile-name-grid">
          <FormField label="Prénom" required>
            <input
              value={formValues.firstName}
              onChange={(event) => {
                setFormValues((previous) => ({ ...previous, firstName: event.target.value }));
                setSubmitError(null);
              }}
              placeholder="Ex. John"
              autoComplete="given-name"
              disabled={submitting}
              required
            />
          </FormField>

          <FormField label="Nom" required>
            <input
              value={formValues.lastName}
              onChange={(event) => {
                setFormValues((previous) => ({ ...previous, lastName: event.target.value }));
                setSubmitError(null);
              }}
              placeholder="Ex. Doe"
              autoComplete="family-name"
              disabled={submitting}
              required
            />
          </FormField>
        </div>

        <FormField label="Email">
          <input value={email} type="email" readOnly className="traveler-profile-email-input" aria-readonly="true" />
        </FormField>

        <FormField label="Téléphone" required>
          <input
            value={formValues.phoneNumber}
            type="tel"
            onChange={(event) => {
              setFormValues((previous) => ({ ...previous, phoneNumber: event.target.value }));
              setSubmitError(null);
            }}
            placeholder="+33612345678"
            autoComplete="tel"
            disabled={submitting}
            required
          />
        </FormField>

        <FormField label="WhatsApp (optionnel)">
          <input
            value={formValues.whatsappNumber}
            type="tel"
            onChange={(event) => {
              setFormValues((previous) => ({ ...previous, whatsappNumber: event.target.value }));
              setSubmitError(null);
            }}
            placeholder="Laissez vide si non utilisé"
            disabled={submitting}
          />
        </FormField>

        <FormField label="Langue préférée" required>
          <select
            value={formValues.preferredLanguage}
            onChange={(event) => {
              setFormValues((previous) => ({ ...previous, preferredLanguage: event.target.value }));
              setSubmitError(null);
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

        {submitError && (
          <p className="traveler-form-error" role="alert">
            {submitError}
          </p>
        )}

        <div className="traveler-profile-actions">
          <Button variant="outline" type="button" onClick={handleCancel} disabled={submitting || !isDirty}>
            Annuler
          </Button>
          <Button type="submit" loading={submitting} disabled={submitting || !isDirty}>
            Enregistrer
          </Button>
        </div>
      </form>
    </Card>
  );
}
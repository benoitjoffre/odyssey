import { useEffect, useRef, useState, type FormEvent } from "react";
import { LoaderCircle, MapPin, Plus, RefreshCw } from "lucide-react";
import { createDestination, getDestinations } from "../../api/destinations";
import { EmptyState, ErrorState, LoadingState } from "../../components/ui/State";
import type { Destination } from "../../types/destination";

function sortDestinations(items: Destination[]) {
  return [...items].sort((first, second) => {
    const countryCompare = first.country.localeCompare(second.country, "fr", { sensitivity: "base" });
    if (countryCompare !== 0) return countryCompare;
    return first.city.localeCompare(second.city, "fr", { sensitivity: "base" });
  });
}

export function AgentDestinationsPage() {
  const [destinations, setDestinations] = useState<Destination[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingError, setLoadingError] = useState<string | null>(null);
  const [city, setCity] = useState("");
  const [country, setCountry] = useState("");
  const [countryCode, setCountryCode] = useState("");
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [requestVersion, setRequestVersion] = useState(0);
  const submittingRef = useRef(false);

  useEffect(() => {
    const controller = new AbortController();

    async function loadDestinations() {
      setLoading(true);
      setLoadingError(null);

      try {
        const items = await getDestinations(controller.signal);
        setDestinations(sortDestinations(items));
      } catch (requestError: unknown) {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        setLoadingError("Impossible de charger les destinations pour le moment.");
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void loadDestinations();
    return () => controller.abort();
  }, [requestVersion]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submittingRef.current) return;

    const normalizedCode = countryCode.trim().toUpperCase();
    if (!city.trim() || !country.trim() || !normalizedCode) {
      setSubmitError("Tous les champs sont obligatoires.");
      return;
    }

    if (!/^[A-Z]{2}$/.test(normalizedCode)) {
      setSubmitError("Le code pays doit contenir exactement 2 lettres (ex: FR, ES).");
      return;
    }

    submittingRef.current = true;
    setSubmitting(true);
    setSubmitError(null);

    try {
      const createdDestination = await createDestination({
        city: city.trim(),
        country: country.trim(),
        countryCode: normalizedCode,
      });

      setDestinations((current) => sortDestinations([...current, createdDestination]));
      setCity("");
      setCountry("");
      setCountryCode("");
    } catch {
      setSubmitError("La destination n’a pas pu être créée. Vérifiez les informations puis réessayez.");
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  }

  return (
    <div className="page-stack">
      <section className="page-heading agent-admin-heading">
        <div>
          <span className="eyebrow">Catalogue Odyssey</span>
          <h1>Destinations</h1>
          <p>Gérez les destinations disponibles pour la création des expériences.</p>
        </div>
      </section>

      <section className="agent-admin-form-card" aria-labelledby="destination-form-title">
        <div className="detail-card-heading">
          <Plus size={20} />
          <h2 id="destination-form-title">Ajouter une destination</h2>
        </div>
        <form className="agent-admin-form" onSubmit={(event) => void handleSubmit(event)} noValidate>
          <label className="form-field">
            <span>Ville *</span>
            <input
              value={city}
              onChange={(event) => {
                setCity(event.target.value);
                setSubmitError(null);
              }}
              disabled={submitting}
            />
          </label>
          <label className="form-field">
            <span>Pays *</span>
            <input
              value={country}
              onChange={(event) => {
                setCountry(event.target.value);
                setSubmitError(null);
              }}
              disabled={submitting}
            />
          </label>
          <label className="form-field">
            <span>Code pays (ISO-2) *</span>
            <input
              value={countryCode}
              onChange={(event) => {
                setCountryCode(event.target.value);
                setSubmitError(null);
              }}
              maxLength={2}
              disabled={submitting}
            />
          </label>
          {submitError && (
            <p className="agent-admin-form-error" role="alert">
              {submitError}
            </p>
          )}
          <button type="submit" className="primary-button agent-admin-submit" disabled={submitting}>
            {submitting ? <LoaderCircle className="rotating" size={18} /> : <Plus size={18} />}
            {submitting ? "Création…" : "Ajouter la destination"}
          </button>
        </form>
      </section>

      {loading && <LoadingState title="Chargement des destinations…" icon={<span className="ui-state-spinner" aria-hidden="true" />} />}

      {!loading && loadingError && (
        <ErrorState title={loadingError} icon={<RefreshCw size={24} />}>
          <button className="secondary-button" type="button" onClick={() => setRequestVersion((value) => value + 1)}>
            <RefreshCw size={16} /> Réessayer
          </button>
        </ErrorState>
      )}

      {!loading && !loadingError && destinations.length === 0 && (
        <EmptyState title="Aucune destination" description="Ajoutez la première destination du catalogue." icon={<MapPin size={28} />} />
      )}

      {!loading && !loadingError && destinations.length > 0 && (
        <section className="agent-catalog-grid" aria-label="Catalogue des destinations">
          {destinations.map((destination) => (
            <article className="agent-catalog-card" key={destination.id}>
              <div className="agent-catalog-topline">
                <span>
                  <MapPin size={15} /> {destination.countryCode}
                </span>
                <strong>Destination #{destination.id}</strong>
              </div>
              <h2>{destination.city}</h2>
              <p>{destination.country}</p>
            </article>
          ))}
        </section>
      )}
    </div>
  );
}

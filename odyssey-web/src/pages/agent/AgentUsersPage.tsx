import { useEffect, useState, type FormEvent } from "react";
import { Search, Users } from "lucide-react";
import { ApiError } from "../../api/client";
import { getUsers, type ProfileType, type UserPage } from "../../api/users";
import { EmptyState, ErrorState, LoadingState } from "../../components/ui/State";
import "./AgentUsersPage.css";

const agentStatusLabels = {
  AVAILABLE: "Disponible",
  BUSY: "Occupé",
  OFFLINE: "Hors ligne",
};

function display(value: string | null) {
  return value?.trim() || "Non renseigné";
}

export function AgentUsersPage() {
  const [profileType, setProfileType] = useState<ProfileType>("TRAVELER");
  const [searchInput, setSearchInput] = useState("");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<UserPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [requestVersion, setRequestVersion] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function loadUsers() {
      setLoading(true);
      setError(null);
      try {
        const result = await getUsers(profileType, search, page, controller.signal);
        if (!controller.signal.aborted) setData(result);
      } catch (requestError: unknown) {
        if (controller.signal.aborted) return;
        setError(requestError instanceof ApiError && requestError.status === 403
          ? "Vous n’avez pas accès aux informations des utilisateurs."
          : "Impossible de charger les utilisateurs. Veuillez réessayer.");
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void loadUsers();
    return () => controller.abort();
  }, [profileType, search, page, requestVersion]);

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSearch(searchInput.trim());
    setPage(0);
    setRequestVersion((version) => version + 1);
  }

  return (
    <div className="page-stack">
      <section className="page-heading">
        <div>
          <span className="eyebrow">Espace Agent</span>
          <h1>Utilisateurs</h1>
          <p>Consultez les profils enregistrés sur Odyssey. Ces informations sont en lecture seule.</p>
        </div>
      </section>

      <section className="agent-admin-form-card">
        <form className="agent-users-filters" onSubmit={handleSearch}>
          <label className="form-field">
            <span>Type de profil</span>
            <select value={profileType} onChange={(event) => {
              setProfileType(event.target.value === "AGENT" ? "AGENT" : "TRAVELER");
              setPage(0);
            }}>
              <option value="TRAVELER">Voyageurs</option>
              <option value="AGENT">Agents</option>
            </select>
          </label>
          <label className="form-field">
            <span>Nom, prénom ou e-mail</span>
            <input type="search" maxLength={120} value={searchInput} onChange={(event) => setSearchInput(event.target.value)} />
          </label>
          <button className="primary-button" type="submit"><Search size={18} aria-hidden="true" /> Rechercher</button>
        </form>
        <p>Un compte possédant les deux profils figure dans chaque catégorie.</p>
      </section>

      {loading && <LoadingState title="Chargement des utilisateurs…" />}
      {!loading && error && (
        <ErrorState title={error}>
          <button className="secondary-button" type="button" onClick={() => setRequestVersion((version) => version + 1)}>Réessayer</button>
        </ErrorState>
      )}
      {!loading && !error && data && (
        <>
          <p role="status">{data.totalElements} profil{data.totalElements > 1 ? "s" : ""} trouvé{data.totalElements > 1 ? "s" : ""}</p>
          {data.items.length === 0 ? (
            <EmptyState title="Aucun utilisateur trouvé" description="Essayez une autre recherche ou un autre type de profil." icon={<Users size={28} />} />
          ) : (
            <section className="agent-users-table-wrapper" aria-label="Profils utilisateurs" tabIndex={0}>
              <table className="agent-users-table">
                <caption>Profils {profileType === "TRAVELER" ? "voyageurs" : "agents"} — lecture seule</caption>
                <thead>
                  <tr>
                    <th scope="col">Prénom</th><th scope="col">Nom</th><th scope="col">E-mail</th>
                    {profileType === "TRAVELER" ? (
                      <><th scope="col">Téléphone</th><th scope="col">WhatsApp</th><th scope="col">Langue</th><th scope="col">Onboarding</th></>
                    ) : <th scope="col">Statut agent</th>}
                  </tr>
                </thead>
                <tbody>
                  {data.items.map((user) => (
                    <tr key={`${user.profileType}-${user.id}`}>
                      <td>{display(user.firstName)}</td><td>{display(user.lastName)}</td><td>{user.email}</td>
                      {profileType === "TRAVELER" ? (
                        <>
                          <td>{display(user.phoneNumber)}</td><td>{display(user.whatsappNumber)}</td>
                          <td>{display(user.preferredLanguage)}</td>
                          <td>{user.onboardingCompleted === null ? "Non renseigné" : user.onboardingCompleted ? "Terminé" : "À compléter"}</td>
                        </>
                      ) : <td>{user.agentStatus ? agentStatusLabels[user.agentStatus] : "Non renseigné"}</td>}
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          )}
          {data.totalPages > 1 && (
            <nav className="agent-users-pagination" aria-label="Pagination des utilisateurs">
              <button className="secondary-button" type="button" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>Précédent</button>
              <span>Page {data.page + 1} sur {data.totalPages}</span>
              <button className="secondary-button" type="button" disabled={page + 1 >= data.totalPages} onClick={() => setPage((value) => value + 1)}>Suivant</button>
            </nav>
          )}
        </>
      )}
    </div>
  );
}

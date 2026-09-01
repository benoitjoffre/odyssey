import { useEffect, useState } from "react";
import { ArrowRight, Bell, CalendarDays, Inbox, RefreshCw } from "lucide-react";
import { Link } from "react-router-dom";
import { openAgentNotificationStream } from "../api/agentNotificationStream";
import { getAgentNotifications } from "../api/agents";
import type { AgentNotification } from "../types/agent";

const CURRENT_AGENT_ID = 1;

function formatDate(value: string) {
  return new Intl.DateTimeFormat("fr-FR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export function AgentDashboardPage() {
  const [notifications, setNotifications] = useState<AgentNotification[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [requestVersion, setRequestVersion] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let eventSource: EventSource | null = null;
    let disposed = false;

    async function loadNotifications() {
      setLoading(true);
      setError(null);

      try {
        const data = await getAgentNotifications(CURRENT_AGENT_ID, controller.signal);
        setNotifications(data);
      } catch (requestError) {
        if (requestError instanceof DOMException && requestError.name === "AbortError") {
          return;
        }
        setError("Impossible de charger les demandes pour le moment.");
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }

      if (!disposed) {
        eventSource = openAgentNotificationStream(CURRENT_AGENT_ID, (notification) => {
          setNotifications((current) => {
            if (current.some((item) => item.id === notification.id)) {
              return current;
            }

            return [notification, ...current];
          });
        });
      }
    }

    void loadNotifications();
    return () => {
      disposed = true;
      controller.abort();
      eventSource?.close();
    };
  }, [requestVersion]);

  const unreadCount = notifications.filter((notification) => !notification.read).length;

  return (
    <div className="page-stack">
      <section className="page-heading">
        <div>
          <span className="eyebrow">Vue d’ensemble</span>
          <h1>Bonjour, Agent #1</h1>
          <p>Retrouvez les nouvelles demandes de voyage qui nécessitent votre attention.</p>
        </div>
        {!loading && !error && (
          <div className="summary-stat" aria-label={`${unreadCount} nouvelles demandes`}>
            <span>{unreadCount}</span>
            <p>Nouvelles demandes</p>
          </div>
        )}
      </section>

      <section aria-labelledby="notifications-title">
        <div className="section-heading">
          <div>
            <h2 id="notifications-title">Demandes récentes</h2>
            <p>Notifications assignées à votre compte</p>
          </div>
          <Bell size={20} aria-hidden="true" />
        </div>

        {loading && (
          <div className="state-panel" role="status">
            <span className="spinner" aria-hidden="true" />
            <strong>Chargement des demandes…</strong>
            <p>Nous récupérons vos dernières notifications.</p>
          </div>
        )}

        {!loading && error && (
          <div className="state-panel error-panel" role="alert">
            <RefreshCw size={24} aria-hidden="true" />
            <strong>{error}</strong>
            <p>Vérifiez que l’API Odyssey est accessible puis réessayez.</p>
            <button type="button" className="secondary-button" onClick={() => setRequestVersion((value) => value + 1)}>
              <RefreshCw size={16} />
              Réessayer
            </button>
          </div>
        )}

        {!loading && !error && notifications.length === 0 && (
          <div className="state-panel">
            <Inbox size={28} aria-hidden="true" />
            <strong>Aucune demande en attente</strong>
            <p>Les nouvelles demandes qui vous sont assignées apparaîtront ici.</p>
          </div>
        )}

        {!loading && !error && notifications.length > 0 && (
          <div className="notification-grid">
            {notifications.map((notification) => (
              <article className={`notification-card${notification.read ? "" : " unread"}`} key={notification.id}>
                <div className="notification-card-top">
                  <span className={`status-badge ${notification.read ? "read" : "new"}`}>{notification.read ? "Consultée" : "Nouvelle"}</span>
                  <span className="notification-date">
                    <CalendarDays size={15} />
                    {formatDate(notification.createdAt)}
                  </span>
                </div>
                <div className="notification-body">
                  <span className="request-label">Demande #{notification.bookingRequestId}</span>
                  <h3>{notification.message}</h3>
                </div>
                <Link className="card-link" to={`/agent/booking-requests/${notification.bookingRequestId}`}>
                  Voir la demande
                  <ArrowRight size={17} />
                </Link>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

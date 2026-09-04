import { useEffect, useState } from "react";
import { ArrowRight, Bell, BellRing, Check, Inbox, RefreshCw } from "lucide-react";
import { Link } from "react-router-dom";
import { openAgentNotificationStream } from "../api/agentNotificationStream";
import { getAgentNotifications } from "../api/agents";
import type { AgentNotification } from "../types/agent";

const CURRENT_AGENT_ID = 1;

interface NotificationPresentation {
  label: string;
  tone: "request" | "accepted" | "neutral";
  travelerName: string | null;
}

function getNotificationPresentation(message: string): NotificationPresentation {
  if (message === "Nouvelle demande de réservation à prendre en charge") {
    return { label: "Nouvelle demande", tone: "request", travelerName: null };
  }

  const acceptedMatch = message.match(/^(.+?) a accepté votre proposition/);
  if (acceptedMatch) {
    return { label: "Proposition acceptée", tone: "accepted", travelerName: acceptedMatch[1] };
  }

  return { label: "Notification", tone: "neutral", travelerName: null };
}

function getVisibleNotifications(notifications: AgentNotification[]) {
  const acceptedRequestIds = new Set(
    notifications
      .filter((notification) => getNotificationPresentation(notification.message).tone === "accepted")
      .map((notification) => notification.bookingRequestId),
  );

  return notifications.filter((notification) => {
    const isSupersededRequest =
      getNotificationPresentation(notification.message).tone === "request" && acceptedRequestIds.has(notification.bookingRequestId);
    return !isSupersededRequest;
  });
}

function formatRelativeDate(value: string) {
  const date = new Date(value);
  const elapsedMilliseconds = Date.now() - date.getTime();

  if (!Number.isFinite(elapsedMilliseconds) || elapsedMilliseconds < 0) {
    return new Intl.DateTimeFormat("fr-FR", { day: "numeric", month: "short" }).format(date);
  }

  const elapsedMinutes = Math.floor(elapsedMilliseconds / 60_000);
  if (elapsedMinutes < 1) return "À l’instant";
  if (elapsedMinutes < 60) return `Il y a ${elapsedMinutes} min`;

  const elapsedHours = Math.floor(elapsedMinutes / 60);
  if (elapsedHours < 24) return `Il y a ${elapsedHours} h`;
  if (elapsedHours < 48) return "Hier";

  return new Intl.DateTimeFormat("fr-FR", { day: "numeric", month: "short" }).format(date);
}

function formatExactDate(value: string) {
  return new Intl.DateTimeFormat("fr-FR", {
    dateStyle: "long",
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

  const visibleNotifications = getVisibleNotifications(notifications);
  const unreadCount = visibleNotifications.filter((notification) => !notification.read).length;

  return (
    <div className="page-stack">
      <section className="page-heading">
        <div>
          <span className="eyebrow">Vue d’ensemble</span>
          <h1>Bonjour, Agent #1</h1>
          <p>Retrouvez les nouvelles demandes de voyage qui nécessitent votre attention.</p>
        </div>
        {!loading && !error && (
          <div className="summary-stat" role="status" aria-label={`${unreadCount} notifications non lues`}>
            <span>{unreadCount}</span>
            <p>Notifications non lues</p>
          </div>
        )}
      </section>

      <section className="notification-section" aria-labelledby="notifications-title">
        <div className="section-heading notification-heading">
          <div>
            <h2 id="notifications-title">Notifications</h2>
            <p>Suivez les dernières activités sur vos demandes.</p>
          </div>
          <div className="notification-heading-status">
            {!loading && !error && unreadCount > 0 && (
              <span>
                {unreadCount} non {unreadCount === 1 ? "lue" : "lues"}
              </span>
            )}
            <Bell size={20} aria-hidden="true" />
          </div>
        </div>

        {loading && (
          <div className="notification-skeleton-list" role="status" aria-label="Chargement des notifications">
            {[0, 1, 2].map((item) => (
              <div className="notification-skeleton-row" key={item} aria-hidden="true">
                <span className="notification-skeleton-icon" />
                <span className="notification-skeleton-copy">
                  <span />
                  <span />
                  <span />
                </span>
              </div>
            ))}
          </div>
        )}

        {!loading && error && (
          <div className="state-panel error-panel" role="alert">
            <RefreshCw size={24} aria-hidden="true" />
            <strong>Impossible de charger les notifications.</strong>
            <p>Vérifiez que l’API Odyssey est accessible puis réessayez.</p>
            <button type="button" className="secondary-button" onClick={() => setRequestVersion((value) => value + 1)}>
              <RefreshCw size={16} />
              Réessayer
            </button>
          </div>
        )}

        {!loading && !error && visibleNotifications.length === 0 && (
          <div className="state-panel notification-empty-state">
            <Inbox size={28} aria-hidden="true" />
            <strong>Vous n’avez aucune nouvelle notification.</strong>
            <p>Les activités sur vos demandes apparaîtront ici.</p>
          </div>
        )}

        {!loading && !error && visibleNotifications.length > 0 && (
          <ul className="notification-feed">
            {visibleNotifications.map((notification) => {
              const presentation = getNotificationPresentation(notification.message);
              return (
                <li key={notification.id}>
                  <Link
                    className={`notification-feed-item ${presentation.tone}${notification.read ? "" : " unread"}`}
                    to={`/agent/booking-requests/${notification.bookingRequestId}`}
                    aria-label={`${presentation.label}, demande ${notification.bookingRequestId}. ${notification.message}`}
                  >
                    <span className="notification-feed-icon" aria-hidden="true">
                      {presentation.tone === "accepted" ? (
                        <Check size={18} />
                      ) : presentation.tone === "request" ? (
                        <BellRing size={18} />
                      ) : (
                        <Bell size={18} />
                      )}
                    </span>
                    <span className="notification-feed-content">
                      <span className="notification-feed-topline">
                        <span className={`notification-type-badge ${presentation.tone}`}>{presentation.label}</span>
                        <time dateTime={notification.createdAt} title={formatExactDate(notification.createdAt)}>
                          {formatRelativeDate(notification.createdAt)}
                        </time>
                      </span>
                      <span className="notification-feed-reference">
                        Demande #{notification.bookingRequestId}
                        {presentation.travelerName && <> · {presentation.travelerName}</>}
                      </span>
                      <span className="notification-feed-message">{notification.message}</span>
                    </span>
                    <ArrowRight className="notification-feed-arrow" size={18} aria-hidden="true" />
                  </Link>
                </li>
              );
            })}
          </ul>
        )}
      </section>
    </div>
  );
}

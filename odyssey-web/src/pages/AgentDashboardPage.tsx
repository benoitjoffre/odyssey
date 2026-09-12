import { useEffect, useState } from "react";
import { ArrowRight, BedDouble, Bell, Bus, Car, Plane, Route, RefreshCw, Inbox } from "lucide-react";
import { Link } from "react-router-dom";
import { openAgentNotificationStream } from "../api/agentNotificationStream";
import { getAgentNotifications } from "../api/agents";
import type { AgentNotification } from "../types/agent";
import type { NeedType } from "../types/bookingRequest";

const needPresentation: Record<NeedType, { label: string; icon: typeof Plane }> = {
  FLIGHT: { label: "Vol", icon: Plane },
  ACCOMMODATION: { label: "Hébergement", icon: BedDouble },
  TRANSFER: { label: "Transfert", icon: Route },
  CAR: { label: "Voiture", icon: Car },
  BUS: { label: "Bus", icon: Bus },
};

interface NotificationPresentation {
  label: string;
  tone: "request" | "accepted" | "neutral";
}

function getNotificationPresentation(message: string): NotificationPresentation {
  if (message.startsWith("Nouvelle demande")) {
    return { label: "Nouvelle demande", tone: "request" };
  }

  if (/^(.+?) a accepté votre proposition/.test(message)) {
    return { label: "Proposition acceptée", tone: "accepted" };
  }

  return { label: "Notification", tone: "neutral" };
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

// Notifications are never merged in the data model: each keeps its own id,
// bookingRequestId and timestamp. Grouping by tripId is a presentation-only
// concern, applied here on top of the flat, deduplicated notification list.
interface NotificationTripGroup {
  key: string;
  tripId: number | null;
  title: string | null;
  travelerName: string | null;
  startDate: string | null;
  endDate: string | null;
  notifications: AgentNotification[];
}

function groupNotificationsByTrip(notifications: AgentNotification[]): NotificationTripGroup[] {
  const groups = new Map<string, NotificationTripGroup>();

  // `notifications` is already deduplicated by id and sorted newest first, so
  // the first notification encountered for a given trip is its most recent
  // one: the Map preserves insertion order, which gives us trip groups
  // already sorted by "most recently active trip first" for free.
  notifications.forEach((notification) => {
    const key = notification.tripId != null ? `trip-${notification.tripId}` : `request-${notification.bookingRequestId}`;
    const existing = groups.get(key);

    if (existing) {
      existing.notifications.push(notification);
      return;
    }

    const travelerName = [notification.travelerFirstName, notification.travelerLastName].filter(Boolean).join(" ") || null;

    groups.set(key, {
      key,
      tripId: notification.tripId,
      title: notification.tripTitle,
      travelerName,
      startDate: notification.tripStartDate,
      endDate: notification.tripEndDate,
      notifications: [notification],
    });
  });

  return [...groups.values()];
}

function mergeNotifications(current: AgentNotification[], incoming: AgentNotification[]) {
  const notificationsById = new Map(current.map((notification) => [notification.id, notification]));
  incoming.forEach((notification) => notificationsById.set(notification.id, notification));
  return [...notificationsById.values()].sort((first, second) => new Date(second.createdAt).getTime() - new Date(first.createdAt).getTime());
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

function formatTripDates(startDate: string, endDate: string) {
  const dateFormatter = new Intl.DateTimeFormat("fr-FR", { day: "numeric", month: "short" });
  const yearFormatter = new Intl.DateTimeFormat("fr-FR", { year: "numeric" });
  const start = new Date(`${startDate}T00:00:00`);
  const end = new Date(`${endDate}T00:00:00`);
  return `${dateFormatter.format(start)} → ${dateFormatter.format(end)} ${yearFormatter.format(end)}`;
}

export function AgentDashboardPage() {
  const [notifications, setNotifications] = useState<AgentNotification[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [requestVersion, setRequestVersion] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    void openAgentNotificationStream((notification) => {
      setNotifications((current) => mergeNotifications(current, [notification]));
    }, controller.signal).catch((error) => {
      if (error instanceof DOMException && error.name === "AbortError") {
        return;
      }

      if (import.meta.env.DEV) {
        console.warn("SSE connection interrupted", error);
      }
    });

    async function loadNotifications() {
      setLoading(true);
      setError(null);

      try {
        const data = await getAgentNotifications(controller.signal);
        setNotifications((current) => mergeNotifications(current, data));
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
    }

    void loadNotifications();
    return () => {
      controller.abort();
    };
  }, [requestVersion]);

  const visibleNotifications = getVisibleNotifications(notifications);
  const unreadCount = visibleNotifications.filter((notification) => !notification.read).length;
  const tripGroups = groupNotificationsByTrip(visibleNotifications);

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
            <p>Suivez les dernières activités sur vos demandes, groupées par voyage.</p>
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

        {!loading && !error && tripGroups.length === 0 && (
          <div className="state-panel notification-empty-state">
            <Inbox size={28} aria-hidden="true" />
            <strong>Vous n’avez aucune nouvelle notification.</strong>
            <p>Les activités sur vos demandes apparaîtront ici.</p>
          </div>
        )}

        {!loading && !error && tripGroups.length > 0 && (
          <div className="notification-trip-list">
            {tripGroups.map((group) => (
              <section className="notification-trip-group" key={group.key} aria-labelledby={`notification-trip-${group.key}-title`}>
                <header className="notification-trip-heading">
                  <div className="notification-trip-heading-copy">
                    <h3 id={`notification-trip-${group.key}-title`}>
                      {group.title ?? (group.tripId != null ? `Voyage #${group.tripId}` : `Demande #${group.notifications[0].bookingRequestId}`)}
                    </h3>
                    {(group.travelerName || (group.startDate && group.endDate)) && (
                      <p>
                        {group.travelerName}
                        {group.travelerName && group.startDate && group.endDate ? " · " : ""}
                        {group.startDate && group.endDate ? formatTripDates(group.startDate, group.endDate) : null}
                      </p>
                    )}
                  </div>
                  <span className="notification-trip-count">
                    {group.notifications.length} {group.notifications.length > 1 ? "notifications" : "notification"}
                  </span>
                </header>

                <ul className="notification-trip-items">
                  {group.notifications.map((notification) => {
                    const presentation = getNotificationPresentation(notification.message);
                    const need = notification.needType ? needPresentation[notification.needType] : null;
                    const NeedIcon = need?.icon ?? Bell;

                    return (
                      <li key={notification.id}>
                        <Link
                          className={`notification-trip-item ${presentation.tone}${notification.read ? "" : " unread"}`}
                          to={`/agent/booking-requests/${notification.bookingRequestId}`}
                          aria-label={`${need?.label ?? "Notification"}, ${presentation.label}, demande ${notification.bookingRequestId}`}
                        >
                          <span className={`notification-trip-item-icon ${notification.needType?.toLowerCase() ?? ""}`} aria-hidden="true">
                            <NeedIcon size={16} />
                          </span>
                          <span className="notification-trip-item-copy">
                            <span className="notification-trip-item-topline">
                              <span className="notification-trip-item-need">{need?.label ?? "Notification"}</span>
                              <time dateTime={notification.createdAt} title={formatExactDate(notification.createdAt)}>
                                {formatRelativeDate(notification.createdAt)}
                              </time>
                            </span>
                            <span className={`notification-trip-item-action ${presentation.tone}`}>{presentation.label}</span>
                          </span>
                          <ArrowRight className="notification-trip-item-arrow" size={16} aria-hidden="true" />
                        </Link>
                      </li>
                    );
                  })}
                </ul>
              </section>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

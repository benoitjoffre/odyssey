import type { ReactNode } from "react";

interface StatePanelProps {
  title: ReactNode;
  description?: ReactNode;
  icon?: ReactNode;
  children?: ReactNode;
  variant?: "default" | "error";
  role?: "status" | "alert";
  className?: string;
}

export function StatePanel({ title, description, icon, children, variant = "default", role = "status", className = "" }: StatePanelProps) {
  return (
    <div className={["ui-state-panel", variant === "error" ? "ui-state-panel--error" : "", className].filter(Boolean).join(" ")} role={role}>
      {icon ? <span className="ui-state-panel__icon">{icon}</span> : null}
      <strong>{title}</strong>
      {description ? <p>{description}</p> : null}
      {children ? <div className="ui-state-panel__action">{children}</div> : null}
    </div>
  );
}

export function LoadingState({ title, description, icon }: { title: ReactNode; description?: ReactNode; icon?: ReactNode }) {
  return <StatePanel title={title} description={description} icon={icon ?? <span className="ui-state-spinner" aria-hidden="true" />} role="status" />;
}

export function ErrorState({
  title,
  description,
  icon,
  children,
}: {
  title: ReactNode;
  description?: ReactNode;
  icon?: ReactNode;
  children?: ReactNode;
}) {
  return (
    <StatePanel title={title} description={description} icon={icon} variant="error" role="alert">
      {children}
    </StatePanel>
  );
}

export function EmptyState({ title, description, icon }: { title: ReactNode; description?: ReactNode; icon?: ReactNode }) {
  return <StatePanel title={title} description={description} icon={icon} role="status" />;
}

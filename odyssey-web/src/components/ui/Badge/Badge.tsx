import type { ComponentPropsWithoutRef, ReactNode } from "react";
import "./Badge.css";

export type BadgeVariant = "neutral" | "success" | "warning" | "danger";

type BadgeProps = Omit<ComponentPropsWithoutRef<"span">, "children"> & {
  variant?: BadgeVariant;
  children: ReactNode;
};

export function Badge({ variant = "neutral", className, children, ...badgeProps }: BadgeProps) {
  const classes = ["ui-badge", `ui-badge--${variant}`, className].filter(Boolean).join(" ");

  return (
    <span {...badgeProps} className={classes}>
      {children}
    </span>
  );
}

import type { HTMLAttributes, ReactNode } from "react";
import "./Card.css";

export type CardPadding = "none" | "sm" | "md" | "lg";

export type CardProps = HTMLAttributes<HTMLDivElement> & {
  children: ReactNode;
  padding?: CardPadding;
};

export function Card({ padding = "none", className, children, ...cardProps }: CardProps) {
  const classes = ["ui-card", `ui-card--padding-${padding}`, className].filter(Boolean).join(" ");

  return (
    <div {...cardProps} className={classes}>
      {children}
    </div>
  );
}

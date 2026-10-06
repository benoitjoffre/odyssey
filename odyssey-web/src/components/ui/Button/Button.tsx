import type { ButtonHTMLAttributes, ReactNode } from "react";
import "./Button.css";

type ButtonVariant = "primary" | "secondary" | "outline" | "ghost" | "danger";
type ButtonSize = "sm" | "md" | "lg";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  size?: ButtonSize;
  loading?: boolean;
  fullWidth?: boolean;
  children: ReactNode;
};

export function Button({
  variant = "primary",
  size = "md",
  loading = false,
  fullWidth = false,
  className,
  disabled,
  type = "button",
  children,
  ...buttonProps
}: ButtonProps) {
  const isDisabled = disabled || loading;

  const classes = [
    "ui-button",
    `ui-button--${variant}`,
    `ui-button--${size}`,
    fullWidth ? "ui-button--full-width" : "",
    loading ? "ui-button--loading" : "",
    className ?? "",
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <button {...buttonProps} type={type} className={classes} disabled={isDisabled} aria-busy={loading || undefined}>
      <span className="ui-button__content">
        <span className="ui-button__label">{children}</span>
        {loading ? <span className="ui-button__spinner" aria-hidden="true" /> : null}
      </span>
    </button>
  );
}

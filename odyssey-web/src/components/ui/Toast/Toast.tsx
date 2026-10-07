import { X } from "lucide-react";
import { useEffect } from "react";
import "./Toast.css";

type ToastVariant = "success" | "error" | "info";

type ToastProps = {
  open: boolean;
  message: string;
  variant?: ToastVariant;
  onClose: () => void;
  autoHideMs?: number;
};

export function Toast({
  open,
  message,
  variant = "info",
  onClose,
  autoHideMs = 3200,
}: ToastProps) {
  useEffect(() => {
    if (!open) return;

    const timeoutId = window.setTimeout(() => {
      onClose();
    }, autoHideMs);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [autoHideMs, onClose, open]);

  if (!open) return null;

  return (
    <div className={`ui-toast ui-toast--${variant}`} role="status" aria-live="polite">
      <span className="ui-toast__message">{message}</span>
      <button type="button" className="ui-toast__close" onClick={onClose} aria-label="Fermer la notification">
        <X size={16} />
      </button>
    </div>
  );
}
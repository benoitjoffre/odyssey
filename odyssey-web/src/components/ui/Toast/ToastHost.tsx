import { useEffect, useState } from "react";
import { Toast } from "./Toast";
import { APP_TOAST_EVENT_NAME, type ToastPayload } from "./toastEvents";

type ToastState = {
  id: number;
  message: string;
  status: ToastPayload["status"];
  autoHideMs: number;
};

export function ToastHost() {
  const [toast, setToast] = useState<ToastState | null>(null);

  useEffect(() => {
    const handleToastEvent = (event: Event) => {
      const customEvent = event as CustomEvent<ToastPayload>;
      const payload = customEvent.detail;
      if (!payload?.message) return;

      setToast({
        id: Date.now(),
        message: payload.message,
        status: payload.status,
        autoHideMs: payload.autoHideMs ?? 3200,
      });
    };

    window.addEventListener(APP_TOAST_EVENT_NAME, handleToastEvent);

    return () => {
      window.removeEventListener(APP_TOAST_EVENT_NAME, handleToastEvent);
    };
  }, []);

  return (
    <Toast
      key={toast?.id ?? 0}
      open={Boolean(toast)}
      message={toast?.message ?? ""}
      variant={toast?.status ?? "info"}
      autoHideMs={toast?.autoHideMs ?? 3200}
      onClose={() => setToast(null)}
    />
  );
}
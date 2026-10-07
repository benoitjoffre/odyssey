export type ToastStatus = "success" | "error" | "info";

export type ToastPayload = {
  status: ToastStatus;
  message: string;
  autoHideMs?: number;
};

export const APP_TOAST_EVENT_NAME = "odyssey:toast";

export function emitToast(payload: ToastPayload): void {
  window.dispatchEvent(new CustomEvent<ToastPayload>(APP_TOAST_EVENT_NAME, { detail: payload }));
}
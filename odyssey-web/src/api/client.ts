export const API_BASE_URL = import.meta.env.VITE_API_URL ?? "";

export class ApiError extends Error {
  readonly status: number;

  constructor(status: number) {
    super(`La requête a échoué (${status})`);
    this.name = "ApiError";
    this.status = status;
  }
}

type GetAccessToken = () => Promise<string>;

let configuredGetAccessToken: GetAccessToken | null = null;

export function configureApiClient(getAccessToken: GetAccessToken | null): void {
  configuredGetAccessToken = getAccessToken;
}

export async function apiFetch<T>(path: string, options?: RequestInit, getAccessToken?: GetAccessToken): Promise<T> {
  const resolvedGetAccessToken = getAccessToken ?? configuredGetAccessToken;
  let token: string | null = null;

  if (resolvedGetAccessToken) {
    try {
      token = await resolvedGetAccessToken();
    } catch {
      token = null;
    }
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      Accept: "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options?.headers,
    },
  });

  if (!response.ok) {
    throw new ApiError(response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export async function getApiAccessToken(): Promise<string> {
  if (!configuredGetAccessToken) {
    throw new Error("API client is not configured");
  }

  return configuredGetAccessToken();
}

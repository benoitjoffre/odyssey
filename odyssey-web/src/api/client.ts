const API_URL = import.meta.env.VITE_API_URL ?? "";

export async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      Accept: "application/json",
      ...options?.headers,
    },
  });

  if (!response.ok) {
    throw new Error(`La requête a échoué (${response.status})`);
  }

  return response.json() as Promise<T>;
}

import type { ApiError } from '../types/contract';

const TOKEN_KEY = 'oliveconnect_jwt';

export const tokenStore = {
  get: (): string | null => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

export class ApiClientError extends Error {
  readonly apiError: ApiError;
  constructor(apiError: ApiError) {
    super(apiError.message);
    this.apiError = apiError;
  }
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (!headers.has('Content-Type') && !(init.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }
  const token = tokenStore.get();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const res = await fetch(`/api/v1${path}`, { ...init, headers });
  if (!res.ok) {
    let err: ApiError;
    try {
      err = (await res.json()) as ApiError;
    } catch {
      err = {
        timestamp: new Date().toISOString(),
        status: res.status,
        code: 'INTERNAL_ERROR',
        message: `Erreur HTTP ${res.status}`,
        fieldErrors: {},
      };
    }
    throw new ApiClientError(err);
  }
  return (await res.json()) as T;
}

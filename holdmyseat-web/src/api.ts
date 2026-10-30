// fetch wrapper: adds the JWT and refreshes once on 401.

export type Tokens = { accessToken: string; refreshToken: string };

export type SeatRow = { row: string; seats: number; category: string; priceMultiplier: number };

export type ShowSummary = {
  id: string; title: string; language: string; startsAt: string;
  basePrice: number; venueName: string; city: string;
};

export type ShowDetail = {
  id: string; title: string; language: string; startsAt: string; basePrice: number;
  venueName: string; screenName: string; layout: SeatRow[];
};

export type Booking = {
  id: string; showId: string; seats: string[]; amount: number;
  status: 'HELD' | 'PAYMENT_PENDING' | 'CONFIRMED' | 'CANCELLED' | 'FAILED' | 'EXPIRED';
  expiresAt: string; createdAt: string;
};

export type Availability = { showId: string; held: string[]; booked: string[] };

export type Me = { id: string; email: string; fullName: string; role: string };

export type Payment = { status: string; amount: number; failureReason: string | null };

const KEY = 'holdmyseat.tokens';

export function getTokens(): Tokens | null {
  try {
    return JSON.parse(localStorage.getItem(KEY) ?? 'null');
  } catch {
    return null;
  }
}

export function setTokens(t: Tokens | null) {
  try {
    if (t) localStorage.setItem(KEY, JSON.stringify({ accessToken: t.accessToken, refreshToken: t.refreshToken }));
    else localStorage.removeItem(KEY);
  } catch {
    /* storage unavailable: session lasts until reload */
  }
}

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

let refreshing: Promise<boolean> | null = null;

async function refresh(): Promise<boolean> {
  const t = getTokens();
  if (!t) return false;
  const r = await fetch('/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken: t.refreshToken }),
  });
  if (!r.ok) {
    setTokens(null);
    return false;
  }
  setTokens(await r.json());
  return true;
}

export async function api<T>(path: string, opts: { method?: string; body?: unknown } = {}, retry = true): Promise<T> {
  const t = getTokens();
  const headers: Record<string, string> = {};
  if (opts.body !== undefined) headers['Content-Type'] = 'application/json';
  if (t) headers.Authorization = `Bearer ${t.accessToken}`;

  const r = await fetch(path, {
    method: opts.method ?? 'GET',
    headers,
    body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
  });

  if (r.status === 401 && retry && t) {
    refreshing ??= refresh().finally(() => (refreshing = null));
    if (await refreshing) return api<T>(path, opts, false);
  }
  if (!r.ok) {
    let message = '';
    try {
      const j = await r.json();
      message = j.message || j.detail || '';
    } catch {
      /* no JSON body */
    }
    throw new ApiError(r.status, message || defaultMessage(r.status));
  }
  if (r.status === 204) return undefined as T;
  return r.json();
}

function defaultMessage(status: number): string {
  switch (status) {
    case 401: return 'Your session ended. Sign in again.';
    case 403: return "You don't have access to that.";
    case 404: return "That doesn't exist anymore.";
    case 409: return 'That was just changed by someone else. Try again.';
    case 429: return 'Too many tries in a row. Wait a few seconds and try again.';
    default: return status >= 500 ? 'A service is down or still starting. Try again in a moment.' : 'Something in the request was not accepted.';
  }
}

export const rupees = (n: number) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n);

export const showTime = (iso: string) =>
  new Intl.DateTimeFormat('en-IN', { hour: 'numeric', minute: '2-digit' }).format(new Date(iso));

export const showDate = (iso: string) =>
  new Intl.DateTimeFormat('en-IN', { weekday: 'short', day: 'numeric', month: 'short' }).format(new Date(iso));

const API = '';

export interface FeedItem {
  id: number;
  externalId: string;
  title: string;
  url: string | null;
  source: string;
  author: string | null;
  score: number;
  tags: string[];
  createdAt: string;
  rankScore: number;
  reason: string;
  exploration: boolean;
}

export interface Preferences {
  weights: { tag: string; weight: number }[];
  embeddingProvider: string;
  hasPreferenceVector: boolean;
  vectorDim: number;
  totalInteractions: number;
}

function authHeaders(): HeadersInit {
  const token = localStorage.getItem('feedrank_token');
  return token ? { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' } : { 'Content-Type': 'application/json' };
}

export async function register(username: string, password: string) {
  const r = await fetch(`${API}/api/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  if (!r.ok) throw new Error((await r.json()).error ?? 'register failed');
  const data = await r.json();
  localStorage.setItem('feedrank_token', data.token);
  localStorage.setItem('feedrank_user', data.username);
  return data;
}

export async function login(username: string, password: string) {
  const r = await fetch(`${API}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  if (!r.ok) throw new Error('invalid credentials');
  const data = await r.json();
  localStorage.setItem('feedrank_token', data.token);
  localStorage.setItem('feedrank_user', data.username);
  return data;
}

export function logout() {
  localStorage.removeItem('feedrank_token');
  localStorage.removeItem('feedrank_user');
}

export async function getFeed(mode: string, limit = 30): Promise<FeedItem[]> {
  const r = await fetch(`${API}/api/feed?mode=${mode}&limit=${limit}&explore=true`, { headers: authHeaders() });
  if (!r.ok) throw new Error('feed failed: ' + r.status);
  return r.json();
}

export async function logInteraction(itemId: number, interactionType: string, dwellTimeMs?: number) {
  await fetch(`${API}/api/interactions`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ itemId, interactionType, dwellTimeMs }),
  });
}

export async function getPreferences(): Promise<Preferences> {
  const r = await fetch(`${API}/api/preferences`, { headers: authHeaders() });
  if (!r.ok) throw new Error('prefs failed');
  return r.json();
}

export async function triggerIngest() {
  const r = await fetch(`${API}/api/feed/ingest`, { method: 'POST', headers: authHeaders() });
  return r.json();
}

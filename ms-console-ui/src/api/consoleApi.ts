import type {
  ActionResult,
  AuthStatus,
  AutonomyRun,
  ChatResponse,
  FailedTracesResponse,
  HistoryResponse,
  IssuesResponse,
  MetricsSnapshot,
} from './types'

const TOKEN_KEY = 'msConsoleToken'

function resolveBasePath(): string {
  const path = window.location.pathname.replace(/\/index\.html$/, '').replace(/\/$/, '')
  return path || '/ms-console'
}

export function getApiBase(): string {
  return `${resolveBasePath()}/api`
}

export function getStoredToken(): string {
  return sessionStorage.getItem(TOKEN_KEY) || ''
}

export function setStoredToken(token: string): void {
  if (token) {
    sessionStorage.setItem(TOKEN_KEY, token)
  } else {
    sessionStorage.removeItem(TOKEN_KEY)
  }
}

export class UnauthorizedError extends Error {
  constructor() {
    super('Unauthorized')
    this.name = 'UnauthorizedError'
  }
}

export async function apiFetch(url: string, token: string, options: RequestInit = {}): Promise<Response> {
  const headers = new Headers(options.headers)
  if (token) {
    headers.set('X-MS-Console-Token', token)
  }
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const res = await fetch(url, { ...options, headers })
  if (res.status === 401) {
    throw new UnauthorizedError()
  }
  return res
}

export function streamUrl(token: string): string {
  let url = `${getApiBase()}/stream`
  if (token) {
    url += `?token=${encodeURIComponent(token)}`
  }
  return url
}

export async function fetchAuthStatus(): Promise<AuthStatus> {
  const res = await fetch(`${getApiBase()}/auth/status`)
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`)
  }
  const data = (await res.json()) as Partial<AuthStatus>
  return {
    authRequired: !!data.authRequired,
    adoptionMode: data.adoptionMode || 'audit-only',
  }
}

export async function fetchIssues(token: string): Promise<IssuesResponse> {
  const res = await apiFetch(`${getApiBase()}/issues`, token)
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`)
  }
  return res.json() as Promise<IssuesResponse>
}

export async function fetchHistory(token: string): Promise<HistoryResponse> {
  const res = await apiFetch(`${getApiBase()}/history`, token)
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`)
  }
  return res.json() as Promise<HistoryResponse>
}

export async function fetchRun(token: string, runId: string): Promise<AutonomyRun> {
  const res = await apiFetch(`${getApiBase()}/runs/${runId}`, token)
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`)
  }
  return res.json() as Promise<AutonomyRun>
}

export async function fetchFailedTraces(token: string, limit = 10): Promise<FailedTracesResponse> {
  const res = await apiFetch(`${getApiBase()}/traces/failed?limit=${limit}`, token)
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`)
  }
  return res.json() as Promise<FailedTracesResponse>
}

export async function sendChat(token: string, message: string, runId: string | null): Promise<ChatResponse> {
  const res = await apiFetch(`${getApiBase()}/chat`, token, {
    method: 'POST',
    body: JSON.stringify({ message, runId }),
  })
  return res.json() as Promise<ChatResponse>
}

export async function fetchMetrics(token: string): Promise<MetricsSnapshot> {
  const res = await apiFetch(`${getApiBase()}/metrics`, token)
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`)
  }
  return res.json() as Promise<MetricsSnapshot>
}

export async function adoptRecommendation(
  token: string,
  recommendationId: string,
  runId: string,
  accept: boolean,
): Promise<ActionResult & { ok: boolean }> {
  const path = accept ? 'accept' : 'reject'
  const res = await apiFetch(`${getApiBase()}/recommendations/${recommendationId}/${path}`, token, {
    method: 'POST',
    body: JSON.stringify({ runId, operator: 'console' }),
  })
  const data = (await res.json()) as ActionResult
  return { ...data, ok: res.ok || !!data.success }
}

export async function publishRecommendationDraft(
  token: string,
  recommendationId: string,
  runId: string,
): Promise<ActionResult & { ok: boolean }> {
  const res = await apiFetch(`${getApiBase()}/recommendations/${recommendationId}/publish`, token, {
    method: 'POST',
    body: JSON.stringify({ runId, operator: 'console' }),
  })
  const data = (await res.json()) as ActionResult
  return { ...data, ok: res.ok || !!data.success }
}

export async function adoptAdvisedAction(
  token: string,
  runId: string,
  rank: number,
): Promise<ActionResult & { ok: boolean }> {
  const res = await apiFetch(`${getApiBase()}/runs/${runId}/actions/${rank}/accept`, token, {
    method: 'POST',
    body: JSON.stringify({ operator: 'console' }),
  })
  const data = (await res.json()) as ActionResult
  return { ...data, ok: res.ok || !!data.success }
}

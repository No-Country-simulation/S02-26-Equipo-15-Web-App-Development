import type { PagedResponse, SessionDetail } from '@/admin/types/api'

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isSessionDetail(value: unknown): value is SessionDetail {
  if (!(isObject(value) && isObject(value.session) && Array.isArray(value.events) && Array.isArray(value.orders))) {
    return false
  }
  if (!('integrations' in value)) {
    return true
  }
  return Array.isArray(value.integrations)
}

function toFiniteNumber(value: unknown, fallback: number) {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback
}

function invalidResponseError(endpoint: string) {
  return new Error(
    `Respuesta invalida en ${endpoint}. Verifica VITE_API_URL y la disponibilidad del backend.`,
  )
}

export function ensurePagedResponse<T>(payload: unknown, endpoint: string): PagedResponse<T> {
  if (Array.isArray(payload)) {
    return {
      items: payload as T[],
      limit: payload.length,
      offset: 0,
    }
  }

  if (isObject(payload) && Array.isArray(payload.items)) {
    return {
      items: payload.items as T[],
      limit: toFiniteNumber(payload.limit, payload.items.length),
      offset: toFiniteNumber(payload.offset, 0),
    }
  }

  throw invalidResponseError(endpoint)
}

export function ensureSessionDetail(payload: unknown, endpoint: string): SessionDetail {
  if (!isSessionDetail(payload)) {
    throw invalidResponseError(endpoint)
  }

  return {
    ...payload,
    integrations: Array.isArray(payload.integrations) ? payload.integrations : [],
  }
}

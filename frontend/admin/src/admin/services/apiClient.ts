import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'

import type { ApiError } from '@/admin/types/api'

const rawApiBaseUrl = (import.meta.env.VITE_API_URL ?? '').trim()
const apiBaseUrl = rawApiBaseUrl.replace(/\/+$/, '')
const hasAbsoluteApiBaseUrl = /^https?:\/\//i.test(apiBaseUrl)

if (!hasAbsoluteApiBaseUrl) {
  console.error(
    'VITE_API_URL must be configured with an absolute URL (e.g. https://api.example.com). Requests are blocked.',
  )
}

export const AUTH_STORAGE_KEY = 'nocountry_admin_token'
export const API_BASE_URL = apiBaseUrl
export const HAS_ABSOLUTE_API_BASE_URL = hasAbsoluteApiBaseUrl
const INVALID_BASE_URL = 'http://invalid.local'

export const apiClient = axios.create({
  baseURL: hasAbsoluteApiBaseUrl ? apiBaseUrl : INVALID_BASE_URL,
  timeout: 15000,
})

apiClient.interceptors.request.use((config) => {
  const requestUrl = resolveRequestUrl(config)
  if (!hasAbsoluteApiBaseUrl) {
    throw new ApiClientConfigError(
      'VITE_API_URL is required and must be an absolute URL. Configure it before making API calls.',
      requestUrl,
    )
  }
  if (!requestUrl) {
    throw new ApiClientConfigError('Unable to resolve request URL for API call.', config.url)
  }

  config.baseURL = undefined
  config.url = requestUrl

  const token = window.localStorage.getItem(AUTH_STORAGE_KEY)
  if (token) {
    config.headers.Authorization = token
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (axios.isAxiosError(error) && !error.response) {
      const requestUrl = resolveRequestUrl(error.config)
      error.message = `No se pudo conectar con el backend (${requestUrl ?? 'URL desconocida'}). Verifica VITE_API_URL y CORS.`
    }
    return Promise.reject(error)
  },
)

export interface HttpClientError {
  status?: number
  message: string
  details: string[]
  code: string
  requestUrl?: string
}

class ApiClientConfigError extends Error {
  code = 'API_CONFIG_ERROR'
  requestUrl?: string

  constructor(message: string, requestUrl?: string) {
    super(message)
    this.name = 'ApiClientConfigError'
    this.requestUrl = requestUrl
  }
}

export function normalizeHttpError(error: unknown): HttpClientError {
  if (error instanceof ApiClientConfigError) {
    return {
      code: error.code,
      message: error.message,
      details: [],
      requestUrl: error.requestUrl,
    }
  }

  if (axios.isAxiosError(error)) {
    return mapAxiosError(error)
  }

  if (error instanceof Error && hasRequestUrl(error)) {
    return {
      code: 'UNKNOWN_ERROR',
      message: error.message,
      details: [],
      requestUrl: error.requestUrl,
    }
  }

  return {
    code: 'UNKNOWN_ERROR',
    message: error instanceof Error ? error.message : 'Unexpected error',
    details: [],
  }
}

function mapAxiosError(error: AxiosError): HttpClientError {
  const payload = error.response?.data as ApiError | undefined
  const requestUrl = resolveRequestUrl(error.config)
  const message = payload?.message ?? error.message
  return {
    status: error.response?.status,
    code: payload?.error ?? 'HTTP_ERROR',
    message,
    details: payload?.details ?? [],
    requestUrl,
  }
}

function resolveRequestUrl(config?: Pick<InternalAxiosRequestConfig, 'baseURL' | 'url'>): string | undefined {
  if (!config?.url) {
    return undefined
  }
  if (/^https?:\/\//i.test(config.url)) {
    return config.url
  }
  if (!config.baseURL) {
    return config.url
  }
  try {
    return new URL(config.url, config.baseURL).toString()
  } catch {
    return config.url
  }
}

function hasRequestUrl(error: Error): error is Error & { requestUrl: string } {
  return typeof (error as { requestUrl?: unknown }).requestUrl === 'string'
}

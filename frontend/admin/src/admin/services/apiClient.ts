import axios, { AxiosError } from 'axios'

import type { ApiError } from '@/admin/types/api'

const apiBaseUrl = import.meta.env.VITE_API_URL

if (!apiBaseUrl) {
  console.warn('VITE_API_URL is not configured. Requests will use relative URLs.')
}

export const AUTH_STORAGE_KEY = 'nocountry_admin_token'

export const apiClient = axios.create({
  baseURL: apiBaseUrl || undefined,
  timeout: 15000,
})

apiClient.interceptors.request.use((config) => {
  const token = window.localStorage.getItem(AUTH_STORAGE_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export interface HttpClientError {
  status?: number
  message: string
  details: string[]
  code: string
}

export function normalizeHttpError(error: unknown): HttpClientError {
  if (axios.isAxiosError(error)) {
    return mapAxiosError(error)
  }

  return {
    code: 'UNKNOWN_ERROR',
    message: error instanceof Error ? error.message : 'Unexpected error',
    details: [],
  }
}

function mapAxiosError(error: AxiosError): HttpClientError {
  const payload = error.response?.data as ApiError | undefined
  return {
    status: error.response?.status,
    code: payload?.error ?? 'HTTP_ERROR',
    message: payload?.message ?? error.message,
    details: payload?.details ?? [],
  }
}

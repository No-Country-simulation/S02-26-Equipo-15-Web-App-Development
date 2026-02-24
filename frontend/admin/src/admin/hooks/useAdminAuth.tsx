import { createContext, useContext, useMemo, useState } from 'react'

import { API_BASE_URL, AUTH_STORAGE_KEY, HAS_ABSOLUTE_API_BASE_URL } from '@/admin/services/apiClient'

const BASIC_PREFIX = 'Basic '

interface AuthContextValue {
  isAuthenticated: boolean
  isLoggingIn: boolean
  login: (username: string, password: string) => Promise<{ ok: boolean; message?: string }>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AdminAuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(() => {
    const stored = window.localStorage.getItem(AUTH_STORAGE_KEY)
    if (stored?.startsWith(BASIC_PREFIX)) {
      return stored
    }
    window.localStorage.removeItem(AUTH_STORAGE_KEY)
    return null
  })
  const [isLoggingIn, setIsLoggingIn] = useState(false)

  const value = useMemo<AuthContextValue>(() => {
    return {
      isAuthenticated: Boolean(token),
      isLoggingIn,
      login: async (username: string, password: string) => {
        if (!HAS_ABSOLUTE_API_BASE_URL) {
          return { ok: false, message: 'VITE_API_URL invalida o no configurada' }
        }

        const normalizedUsername = username.trim()
        if (!normalizedUsername || !password) {
          return { ok: false, message: 'Completa usuario y password' }
        }

        const credentials = window.btoa(`${normalizedUsername}:${password}`)
        const authHeader = `${BASIC_PREFIX}${credentials}`

        try {
          setIsLoggingIn(true)
          const response = await fetch(`${API_BASE_URL}/api/admin/health`, {
            method: 'GET',
            headers: {
              Authorization: authHeader,
            },
          })

          if (response.status === 401 || response.status === 403) {
            return { ok: false, message: 'Credenciales invalidas' }
          }
          if (!response.ok) {
            return { ok: false, message: 'No se pudo validar acceso admin' }
          }

          window.localStorage.setItem(AUTH_STORAGE_KEY, authHeader)
          setToken(authHeader)
          return { ok: true }
        } catch {
          return { ok: false, message: 'No se pudo conectar con el backend' }
        } finally {
          setIsLoggingIn(false)
        }
      },
      logout: () => {
        window.localStorage.removeItem(AUTH_STORAGE_KEY)
        setToken(null)
      },
    }
  }, [isLoggingIn, token])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAdminAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAdminAuth must be used inside AdminAuthProvider')
  }
  return context
}

import { createContext, useContext, useMemo, useState } from 'react'

import { AUTH_STORAGE_KEY } from '@/admin/services/apiClient'

const DEMO_TOKEN = 'nocountry-admin-demo-token'
const DEFAULT_PASSWORD = 'nocountry-admin'

interface AuthContextValue {
  isAuthenticated: boolean
  login: (password: string) => { ok: boolean; message?: string }
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AdminAuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(() => {
    return window.localStorage.getItem(AUTH_STORAGE_KEY)
  })

  const value = useMemo<AuthContextValue>(() => {
    return {
      isAuthenticated: Boolean(token),
      login: (password: string) => {
        const expectedPassword = import.meta.env.VITE_ADMIN_DEMO_PASSWORD ?? DEFAULT_PASSWORD
        if (password !== expectedPassword) {
          return { ok: false, message: 'Credenciales invalidas' }
        }
        window.localStorage.setItem(AUTH_STORAGE_KEY, DEMO_TOKEN)
        setToken(DEMO_TOKEN)
        return { ok: true }
      },
      logout: () => {
        window.localStorage.removeItem(AUTH_STORAGE_KEY)
        setToken(null)
      },
    }
  }, [token])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAdminAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAdminAuth must be used inside AdminAuthProvider')
  }
  return context
}

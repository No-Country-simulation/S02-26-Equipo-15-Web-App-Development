import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { LockKeyhole } from 'lucide-react'

import { Button } from '@/admin/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/admin/components/ui/card'
import { Input } from '@/admin/components/ui/input'
import { useAdminAuth } from '@/admin/hooks/useAdminAuth'

export function LoginPage() {
  const navigate = useNavigate()
  const { isAuthenticated, isLoggingIn, login } = useAdminAuth()
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')

  if (isAuthenticated) {
    return <Navigate to="/admin/dashboard" replace />
  }

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError('')
    const result = await login(username, password)
    if (!result.ok) {
      setError(result.message ?? 'No autorizado')
      return
    }
    navigate('/admin/dashboard', { replace: true })
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-grid [background-size:24px_24px] p-6">
      <Card className="w-full max-w-md">
        <CardHeader>
          <div className="mb-3 inline-flex h-10 w-10 items-center justify-center rounded-xl bg-accent/20 text-accent">
            <LockKeyhole className="h-5 w-5" />
          </div>
          <CardTitle>Admin Login</CardTitle>
          <CardDescription>Acceso protegido con credenciales del backend.</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="space-y-4" onSubmit={onSubmit}>
            <div className="space-y-2">
              <label htmlFor="username" className="text-sm text-muted">
                Usuario
              </label>
              <Input
                id="username"
                type="text"
                placeholder="admin"
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                autoComplete="username"
                required
              />
            </div>

            <div className="space-y-2">
              <label htmlFor="password" className="text-sm text-muted">
                Password
              </label>
              <Input
                id="password"
                type="password"
                placeholder="Ingresa password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                autoComplete="current-password"
                required
              />
            </div>

            {error ? <p className="text-sm text-red-300">{error}</p> : null}

            <Button type="submit" className="w-full" disabled={isLoggingIn}>
              {isLoggingIn ? 'Validando...' : 'Ingresar'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </main>
  )
}

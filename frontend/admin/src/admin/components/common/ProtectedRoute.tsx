import { Navigate, Outlet } from 'react-router-dom'

import { useAdminAuth } from '@/admin/hooks/useAdminAuth'

export function ProtectedRoute() {
  const { isAuthenticated } = useAdminAuth()

  if (!isAuthenticated) {
    return <Navigate to="/admin/login" replace />
  }

  return <Outlet />
}

import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

import { ProtectedRoute } from '@/admin/components/common/ProtectedRoute'
import { AdminLayout } from '@/admin/components/layout/AdminLayout'
import { AdminAuthProvider, useAdminAuth } from '@/admin/hooks/useAdminAuth'
import { DashboardPage } from '@/admin/pages/DashboardPage'
import { EventsPage } from '@/admin/pages/EventsPage'
import { LoginPage } from '@/admin/pages/LoginPage'
import { SessionTracePage } from '@/admin/pages/SessionTracePage'
import { SessionsPage } from '@/admin/pages/SessionsPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

function RootRedirect() {
  const { isAuthenticated } = useAdminAuth()
  return <Navigate to={isAuthenticated ? '/admin/dashboard' : '/admin/login'} replace />
}

export function AdminRouter() {
  return (
    <QueryClientProvider client={queryClient}>
      <AdminAuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<RootRedirect />} />
            <Route path="/admin/login" element={<LoginPage />} />
            <Route path="/admin" element={<ProtectedRoute />}>
              <Route element={<AdminLayout />}>
                <Route index element={<Navigate to="/admin/dashboard" replace />} />
                <Route path="dashboard" element={<DashboardPage />} />
                <Route path="sessions" element={<SessionsPage />} />
                <Route path="sessions/:eventId" element={<SessionTracePage />} />
                <Route path="events" element={<EventsPage />} />
              </Route>
            </Route>
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </AdminAuthProvider>
    </QueryClientProvider>
  )
}

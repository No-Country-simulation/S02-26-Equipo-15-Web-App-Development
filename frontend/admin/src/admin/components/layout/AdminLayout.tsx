import { Outlet } from 'react-router-dom'

import { AdminSidebar } from '@/admin/components/layout/AdminSidebar'

export function AdminLayout() {
  return (
    <div className="flex min-h-screen bg-grid [background-size:20px_20px]">
      <AdminSidebar />
      <main className="flex-1">
        <div className="mx-auto max-w-[1500px] p-6 lg:p-8">
          <Outlet />
        </div>
      </main>
    </div>
  )
}

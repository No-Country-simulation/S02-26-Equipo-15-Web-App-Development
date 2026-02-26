import { useEffect, useState } from 'react'
import { Menu } from 'lucide-react'
import { Outlet, useLocation } from 'react-router-dom'

import { AdminSidebar } from '@/admin/components/layout/AdminSidebar'
import { TrackSureBrand } from '@/admin/components/layout/TrackSureBrand'
import { Button } from '@/admin/components/ui/button'
import { cn } from '@/lib/utils'

export function AdminLayout() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false)
  const location = useLocation()

  useEffect(() => {
    setIsSidebarOpen(false)
  }, [location.pathname])

  useEffect(() => {
    if (!isSidebarOpen) {
      return
    }

    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = previousOverflow
    }
  }, [isSidebarOpen])

  return (
    <div className="flex min-h-screen bg-[#070F2B] bg-grid [background-size:20px_20px]">
      <AdminSidebar className="hidden lg:flex" />

      <div
        className={cn(
          'fixed inset-0 z-40 bg-slate-950/70 transition-opacity duration-200 lg:hidden',
          isSidebarOpen ? 'opacity-100' : 'pointer-events-none opacity-0',
        )}
        onClick={() => setIsSidebarOpen(false)}
        aria-hidden="true"
      />

      <AdminSidebar
        className={cn(
          'fixed inset-y-0 left-0 z-50 -translate-x-full transition-transform duration-200 lg:hidden',
          isSidebarOpen ? 'translate-x-0' : '',
        )}
        onNavigate={() => setIsSidebarOpen(false)}
        onClose={() => setIsSidebarOpen(false)}
      />

      <main className="min-w-0 flex-1">
        <div className="mx-auto max-w-[1500px] px-4 pb-6 pt-4 sm:px-6 lg:p-8">
          <div className="mb-4 flex items-center gap-3">
            <Button type="button" variant="outline" className="lg:hidden" onClick={() => setIsSidebarOpen(true)}>
              <Menu className="mr-2 h-4 w-4" />
              Menu
            </Button>
            <TrackSureBrand className="ml-auto" />
          </div>
          <Outlet />
        </div>
      </main>
    </div>
  )
}

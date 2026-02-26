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
          <div className="relative min-h-[calc(100vh-2rem)] overflow-hidden rounded-[28px] border border-border/80 bg-[linear-gradient(180deg,rgba(17,31,74,0.92)_0%,rgba(10,20,56,0.92)_100%)] p-4 shadow-panel backdrop-blur sm:p-6 lg:p-8">
            <div aria-hidden="true" className="pointer-events-none absolute -right-20 -top-20 h-56 w-56 rounded-full bg-[#2D8CFF]/12 blur-3xl" />
            <div aria-hidden="true" className="pointer-events-none absolute -left-20 top-1/3 h-56 w-56 rounded-full bg-[#FF1FB3]/10 blur-3xl" />

            <div className="relative mb-4 flex items-center gap-3">
              <Button type="button" variant="outline" className="lg:hidden" onClick={() => setIsSidebarOpen(true)}>
                <Menu className="mr-2 h-4 w-4" />
                Menu
              </Button>
              <TrackSureBrand className="ml-auto" />
            </div>

            <div className="relative">
              <Outlet />
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}

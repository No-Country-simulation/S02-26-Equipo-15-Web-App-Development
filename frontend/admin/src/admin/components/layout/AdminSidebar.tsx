import { Activity, BarChart3, LogOut, ScanSearch, TableProperties, X } from 'lucide-react'
import { NavLink } from 'react-router-dom'

import { TrackSureBrand } from '@/admin/components/layout/TrackSureBrand'
import { Button } from '@/admin/components/ui/button'
import { useAdminAuth } from '@/admin/hooks/useAdminAuth'
import { cn } from '@/lib/utils'

const navigation = [
  { to: '/admin/dashboard', label: 'Dashboard', icon: BarChart3 },
  { to: '/admin/sessions', label: 'Sessions', icon: ScanSearch },
  { to: '/admin/events', label: 'Events', icon: TableProperties },
]

interface AdminSidebarProps {
  className?: string
  onNavigate?: () => void
  onClose?: () => void
}

export function AdminSidebar({ className, onNavigate, onClose }: AdminSidebarProps) {
  const { logout } = useAdminAuth()

  return (
    <aside className={cn('flex h-screen w-[260px] shrink-0 flex-col border-r border-border/70 bg-[#0A1438]/92 p-4 backdrop-blur', className)}>
      <div className="mb-8 flex items-center gap-3">
        <div className="rounded-xl bg-accent/20 p-2 text-accent">
          <Activity className="h-5 w-5" />
        </div>
        <TrackSureBrand className="w-full" />
        {onClose ? (
          <Button
            type="button"
            variant="ghost"
            size="icon"
            className="h-8 w-8 text-muted"
            onClick={onClose}
            aria-label="Cerrar menu"
          >
            <X className="h-4 w-4" />
          </Button>
        ) : null}
      </div>

      <nav className="flex-1 space-y-2">
        {navigation.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            onClick={onNavigate}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 rounded-xl border border-transparent px-3 py-2.5 text-sm font-medium text-slate-200 transition-colors',
                isActive
                  ? 'border-[#2D8CFF]/60 bg-[linear-gradient(90deg,rgba(255,31,179,0.18)_0%,rgba(45,140,255,0.18)_100%)] text-white shadow-[0_0_14px_rgba(45,140,255,0.24)]'
                  : 'hover:border-border/70 hover:bg-slate-800/75 hover:text-slate-100',
              )
            }
          >
            <item.icon className="h-4 w-4" />
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>

      <Button
        variant="ghost"
        className="justify-start text-muted"
        onClick={() => {
          logout()
          onNavigate?.()
        }}
      >
        <LogOut className="mr-2 h-4 w-4" />
        Cerrar sesion
      </Button>
    </aside>
  )
}

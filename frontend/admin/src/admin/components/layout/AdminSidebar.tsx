import { Activity, BarChart3, LogOut, ScanSearch, TableProperties } from 'lucide-react'
import { NavLink } from 'react-router-dom'

import { Button } from '@/admin/components/ui/button'
import { useAdminAuth } from '@/admin/hooks/useAdminAuth'
import { cn } from '@/lib/utils'

const navigation = [
  { to: '/admin/dashboard', label: 'Dashboard', icon: BarChart3 },
  { to: '/admin/sessions', label: 'Sessions', icon: ScanSearch },
  { to: '/admin/events', label: 'Events', icon: TableProperties },
]

export function AdminSidebar() {
  const { logout } = useAdminAuth()

  return (
    <aside className="flex h-screen w-[260px] flex-col border-r border-border bg-surface/90 p-4 backdrop-blur">
      <div className="mb-8 flex items-center gap-3 rounded-2xl border border-border bg-card/80 px-4 py-3">
        <div className="rounded-xl bg-accent/20 p-2 text-accent">
          <Activity className="h-5 w-5" />
        </div>
        <div>
          <p className="text-sm font-semibold">NoCountry Admin</p>
          <p className="text-xs text-muted">Observability Panel</p>
        </div>
      </div>

      <nav className="flex-1 space-y-2">
        {navigation.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 rounded-xl border border-transparent px-3 py-2.5 text-sm font-medium text-slate-200 transition-colors',
                isActive
                  ? 'border-cyan-500/40 bg-cyan-500/10 text-cyan-200'
                  : 'hover:border-border hover:bg-slate-800/80 hover:text-slate-100',
              )
            }
          >
            <item.icon className="h-4 w-4" />
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>

      <Button variant="ghost" className="justify-start text-muted" onClick={logout}>
        <LogOut className="mr-2 h-4 w-4" />
        Cerrar sesion
      </Button>
    </aside>
  )
}

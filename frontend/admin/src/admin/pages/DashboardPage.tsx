import { useCallback, useMemo, useState } from 'react'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import type { NameType, ValueType } from 'recharts/types/component/DefaultTooltipContent'
import type { TooltipContentProps } from 'recharts/types/component/Tooltip'

import { KpiCard } from '@/admin/components/dashboard/KpiCard'
import { ChartCard } from '@/admin/components/dashboard/ChartCard'
import { EmptyState } from '@/admin/components/common/EmptyState'
import { ErrorAlert } from '@/admin/components/common/ErrorAlert'
import { KpiSkeletonGrid } from '@/admin/components/common/Skeletons'
import { StickyFiltersPanel } from '@/admin/components/common/StickyFiltersPanel'
import { StatusChip } from '@/admin/components/common/StatusChip'
import { DateRangeFilter } from '@/admin/components/common/DateRangeFilter'
import { PageHeader } from '@/admin/components/layout/PageHeader'
import { Button } from '@/admin/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/admin/components/ui/card'
import { useDebouncedValue } from '@/admin/hooks/useDebouncedValue'
import { useDashboardStats } from '@/admin/hooks/useDashboardStats'
import { normalizeHttpError } from '@/admin/services/apiClient'
import { formatCurrency } from '@/lib/utils'

const STATUS_COLOR_MAP: Record<string, string> = {
  SUCCESS: '#34d399',
  FAILED: '#f87171',
  PENDING: '#fbbf24',
  UNKNOWN: '#94a3b8',
  SENT: '#60a5fa',
  ERROR: '#f87171',
}

const TOOLTIP_STYLE = {
  backgroundColor: 'rgba(2, 6, 23, 0.96)',
  border: '1px solid #334155',
  borderRadius: '8px',
  boxShadow: '0 10px 25px rgba(2, 6, 23, 0.6)',
}

const TOOLTIP_LABEL_STYLE = { color: '#f8fafc', fontWeight: 700 }
const TOOLTIP_ITEM_STYLE = { color: '#e2e8f0' }

function toIso(value: string, endOfDay = false) {
  if (!value) {
    return undefined
  }
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return undefined
  }

  const hasTime = /T\d{2}:\d{2}/.test(value)
  if (!hasTime) {
    if (endOfDay) {
      parsed.setHours(23, 59, 59, 999)
    } else {
      parsed.setHours(0, 0, 0, 0)
    }
  }
  return parsed.toISOString()
}

export function DashboardPage() {
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const debouncedFrom = useDebouncedValue(from, 450)
  const debouncedTo = useDebouncedValue(to, 450)

  const query = useDashboardStats({
    from: toIso(debouncedFrom),
    to: toIso(debouncedTo, true),
  })

  const chartData = query.data?.ordersByStatus ?? []
  const revenueData = query.data?.revenueByDay ?? []
  const statusTotals = new Map((query.data?.ordersByStatus ?? []).map((row) => [row.status.toUpperCase(), row.total]))
  const summaryBars = [
    { name: 'SUCCESS', value: statusTotals.get('SUCCESS') ?? 0 },
    { name: 'FAILED', value: statusTotals.get('FAILED') ?? 0 },
    { name: 'PENDING', value: statusTotals.get('PENDING') ?? 0 },
    { name: 'UNKNOWN', value: statusTotals.get('UNKNOWN') ?? 0 },
  ]

  const error = useMemo(() => (query.error ? normalizeHttpError(query.error) : null), [query.error])
  const handleClearFilters = useCallback(() => {
    setFrom('')
    setTo('')
  }, [])

  return (
    <section>
      <PageHeader
        title="Admin Dashboard"
        description="Monitoreo operativo de funnel, ordenes y correlacion por eventId"
      />

      <StickyFiltersPanel className="mb-4">
        <div className="grid gap-3 lg:grid-cols-[minmax(0,1fr)_220px]">
          <DateRangeFilter from={from} to={to} onFromChange={setFrom} onToChange={setTo} />
          <Button variant="secondary" onClick={handleClearFilters}>
            Limpiar filtros
          </Button>
        </div>
      </StickyFiltersPanel>

      {query.isPending ? <KpiSkeletonGrid /> : null}
      {error ? <ErrorAlert error={error} /> : null}

      {query.data ? (
        <div className="space-y-6">
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <KpiCard title="Total sesiones" value={query.data.totalSessions} />
            <KpiCard title="Total eventos" value={query.data.totalEvents} />
            <KpiCard title="Total ordenes" value={query.data.totalOrders} />
            <KpiCard title="Revenue total" value={formatCurrency(query.data.revenue)} />
            <KpiCard
              title="SUCCESS vs FAILED"
              value={`${query.data.successOrders} / ${query.data.failedOrders}`}
            />
            <KpiCard title="Sesiones UNKNOWN" value={query.data.unknownSessions} subtext="Sesiones sin orden asociada" />
            <KpiCard
              title="Integrations Health GA4"
              value={query.data.ga4Health == null ? 'N/A' : `${Math.round(query.data.ga4Health)}%`}
              subtext="Basado en integrations_log (sin SKIPPED)"
            />
            <KpiCard
              title="Integrations Health Meta"
              value={query.data.metaHealth == null ? 'N/A' : `${Math.round(query.data.metaHealth)}%`}
              subtext="Basado en integrations_log (sin SKIPPED)"
            />
            <KpiCard
              title="Integrations Health Pipedrive"
              value={query.data.pipedriveHealth == null ? 'N/A' : `${Math.round(query.data.pipedriveHealth)}%`}
              subtext="Basado en integrations_log (sin SKIPPED)"
            />
            <KpiCard title="Conversion rate" value={`${(query.data.conversionRate * 100).toFixed(2)}%`} />
          </div>

          <div className="grid gap-6 xl:grid-cols-3">
            <ChartCard
              title="Revenue diario"
              className="xl:col-span-2"
              chartClassName="h-[300px] min-h-[280px]"
              isLoading={query.isPending}
              hasData={revenueData.length > 0}
              emptyState={
                <EmptyState
                  title="Sin revenue en el rango seleccionado"
                  description="Ajusta el rango de fechas para visualizar ingresos."
                />
              }
            >
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={revenueData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                  <XAxis dataKey="date" stroke="#94a3b8" />
                  <YAxis stroke="#94a3b8" />
                  <Tooltip contentStyle={TOOLTIP_STYLE} labelStyle={TOOLTIP_LABEL_STYLE} itemStyle={TOOLTIP_ITEM_STYLE} />
                  <Line type="monotone" dataKey="revenue" stroke="#22d3ee" strokeWidth={2} dot={false} />
                </LineChart>
              </ResponsiveContainer>
            </ChartCard>

            <ChartCard
              title="Distribucion business_status"
              chartClassName="h-[300px] min-h-[280px]"
              isLoading={query.isPending}
              hasData={chartData.length > 0}
              emptyState={<EmptyState title="Sin ordenes" description="Aun no hay ordenes en el periodo seleccionado." />}
            >
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={chartData} dataKey="total" nameKey="status" innerRadius={50} outerRadius={90}>
                    {chartData.map((entry) => (
                      <Cell key={entry.status} fill={resolveStatusColor(entry.status)} />
                    ))}
                  </Pie>
                  <Tooltip content={StatusPieTooltip} />
                </PieChart>
              </ResponsiveContainer>
            </ChartCard>
          </div>

          <div className="grid gap-6 xl:grid-cols-3">
            <ChartCard
              title="Distribucion de estados de negocio (SUCCESS, FAILED, PENDING, UNKNOWN)"
              className="xl:col-span-2"
              chartClassName="h-[280px] min-h-[260px]"
              isLoading={query.isPending}
              hasData={summaryBars.some((item) => item.value > 0)}
              emptyState={<EmptyState title="Sin ordenes" description="No hay suficientes datos para comparar estados." />}
            >
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={summaryBars}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                  <XAxis dataKey="name" stroke="#94a3b8" />
                  <YAxis stroke="#94a3b8" />
                  <Tooltip contentStyle={TOOLTIP_STYLE} labelStyle={TOOLTIP_LABEL_STYLE} itemStyle={TOOLTIP_ITEM_STYLE} />
                  <Bar dataKey="value" fill="#22d3ee" radius={[8, 8, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </ChartCard>
            <Card>
              <CardHeader>
                <CardTitle>Leyenda de estados de negocio</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-sm text-muted">SUCCESS</span>
                  <StatusChip status="SUCCESS" />
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm text-muted">FAILED</span>
                  <StatusChip status="FAILED" />
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm text-muted">PENDING</span>
                  <StatusChip status="PENDING" />
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm text-muted">UNKNOWN</span>
                  <StatusChip status="UNKNOWN" />
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      ) : null}
    </section>
  )
}

function resolveStatusColor(status: string) {
  return STATUS_COLOR_MAP[status.toUpperCase()] ?? '#60a5fa'
}

function StatusPieTooltip({ active, payload }: TooltipContentProps<ValueType, NameType>) {
  if (!active || !payload || payload.length === 0) {
    return null
  }

  const row = payload[0]
  const label = typeof row.name === 'string' ? row.name.toUpperCase() : String(row.name ?? 'UNKNOWN').toUpperCase()
  const total = typeof row.value === 'number' ? row.value : Number(row.value ?? 0)

  return (
    <div className="rounded-md border border-slate-600 bg-slate-950/95 px-3 py-2 shadow-xl shadow-black/40">
      <p className="text-xs font-semibold tracking-wide text-slate-300">{label}</p>
      <p className="text-sm font-bold text-white">{Number.isNaN(total) ? 0 : total}</p>
    </div>
  )
}

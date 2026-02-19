import { useMemo, useState } from 'react'
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

import { KpiCard } from '@/admin/components/dashboard/KpiCard'
import { ChartCard } from '@/admin/components/dashboard/ChartCard'
import { EmptyState } from '@/admin/components/common/EmptyState'
import { ErrorAlert } from '@/admin/components/common/ErrorAlert'
import { KpiSkeletonGrid } from '@/admin/components/common/Skeletons'
import { StatusChip } from '@/admin/components/common/StatusChip'
import { DateRangeFilter } from '@/admin/components/common/DateRangeFilter'
import { PageHeader } from '@/admin/components/layout/PageHeader'
import { Card, CardContent, CardHeader, CardTitle } from '@/admin/components/ui/card'
import { useDebouncedValue } from '@/admin/hooks/useDebouncedValue'
import { useDashboardStats } from '@/admin/hooks/useDashboardStats'
import { normalizeHttpError } from '@/admin/services/apiClient'
import { formatCurrency } from '@/lib/utils'

const PIE_COLORS = ['#10b981', '#ef4444', '#3b82f6', '#f59e0b', '#8b97b5']

function toIso(value: string) {
  if (!value) {
    return undefined
  }
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? undefined : parsed.toISOString()
}

export function DashboardPage() {
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const debouncedFrom = useDebouncedValue(from, 450)
  const debouncedTo = useDebouncedValue(to, 450)

  const query = useDashboardStats({
    from: toIso(debouncedFrom),
    to: toIso(debouncedTo),
  })

  const chartData = query.data?.ordersByStatus ?? []
  const revenueData = query.data?.revenueByDay ?? []
  const summaryBars = [
    { name: 'SUCCESS', value: query.data?.successOrders ?? 0 },
    { name: 'FAILED', value: query.data?.failedOrders ?? 0 },
    { name: 'UNKNOWN', value: query.data?.unknownSessions ?? 0 },
  ]

  const error = useMemo(() => (query.error ? normalizeHttpError(query.error) : null), [query.error])

  return (
    <section>
      <PageHeader
        title="Admin Dashboard"
        description="Monitoreo operativo de funnel, ordenes y correlacion por eventId"
        actions={<DateRangeFilter from={from} to={to} onFromChange={setFrom} onToChange={setTo} />}
      />

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
                  <Tooltip contentStyle={{ backgroundColor: '#0b1020', border: '1px solid #23324f' }} />
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
                    {chartData.map((entry, index) => (
                      <Cell key={entry.status} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip contentStyle={{ backgroundColor: '#0b1020', border: '1px solid #23324f' }} />
                </PieChart>
              </ResponsiveContainer>
            </ChartCard>
          </div>

          <div className="grid gap-6 xl:grid-cols-3">
            <ChartCard
              title="SUCCESS vs FAILED vs UNKNOWN"
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
                  <Tooltip contentStyle={{ backgroundColor: '#0b1020', border: '1px solid #23324f' }} />
                  <Bar dataKey="value" fill="#22d3ee" radius={[8, 8, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </ChartCard>
            <Card>
              <CardHeader>
                <CardTitle>Leyenda de estados</CardTitle>
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
                  <span className="text-sm text-muted">SENT</span>
                  <StatusChip status="SENT" />
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm text-muted">ERROR</span>
                  <StatusChip status="ERROR" />
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

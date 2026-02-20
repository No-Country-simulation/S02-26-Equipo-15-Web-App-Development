import { useQuery } from '@tanstack/react-query'

import { metricsService } from '@/admin/services/metricsService'
import { sessionsService } from '@/admin/services/sessionsService'
import type { DashboardStats, DateRangeParams, IntegrationLogDto, OrderDto, SessionSummary } from '@/admin/types/api'
import { mapWithConcurrency } from '@/lib/utils'

const DASHBOARD_SESSIONS_PAGE_SIZE = 100
const DASHBOARD_MAX_SESSIONS = 1000
const DASHBOARD_DETAIL_CONCURRENCY = 6
const SUCCESS_ORDER_STATES = new Set(['SUCCESS', 'SUCCEEDED', 'PAID'])
const FAILED_ORDER_STATES = new Set(['FAILED', 'ERROR', 'CANCELED'])
const SUCCESS_INTEGRATION_STATES = new Set(['SENT', 'SENT_WITH_WARNINGS'])

export function useDashboardStats(filters: DateRangeParams) {
  return useQuery({
    queryKey: ['dashboard', filters.from ?? '', filters.to ?? ''],
    queryFn: async (): Promise<DashboardStats> => {
      const [metrics, sessions] = await Promise.all([
        metricsService.getMetrics(filters),
        loadSessions(filters),
      ])
      const sessionItems = sessions

      const details = await mapWithConcurrency(
        sessionItems,
        DASHBOARD_DETAIL_CONCURRENCY,
        async (session) => sessionsService.getSessionDetail(session.eventId).catch(() => null),
      )

      const orders = details.flatMap((detail) => detail?.orders ?? [])
      const integrations = details.flatMap((detail) => detail?.integrations ?? [])
      const unknownSessions = details.filter((detail) => !detail || (detail.orders?.length ?? 0) === 0).length
      const totalEvents = metrics.landingView + metrics.clickCta + metrics.beginCheckout + metrics.purchase
      const successOrders = orders.filter((order) => SUCCESS_ORDER_STATES.has(resolveBusinessStatus(order))).length
      const failedOrders = orders.filter((order) => FAILED_ORDER_STATES.has(resolveBusinessStatus(order))).length
      const revenue = orders.reduce((acc, order) => {
        if (!SUCCESS_ORDER_STATES.has(resolveBusinessStatus(order))) {
          return acc
        }
        return acc + Number(order.amount ?? 0)
      }, 0)

      const groupedStatus = new Map<string, number>()
      const groupedRevenue = new Map<string, number>()

      for (const order of orders) {
        const status = resolveBusinessStatus(order)
        groupedStatus.set(status, (groupedStatus.get(status) ?? 0) + 1)

        if (SUCCESS_ORDER_STATES.has(status)) {
          const day = order.createdAt.slice(0, 10)
          groupedRevenue.set(day, (groupedRevenue.get(day) ?? 0) + Number(order.amount ?? 0))
        }
      }

      if (unknownSessions > 0) {
        groupedStatus.set('UNKNOWN', (groupedStatus.get('UNKNOWN') ?? 0) + unknownSessions)
      }

      return {
        totalSessions: sessionItems.length,
        totalEvents,
        totalOrders: orders.length,
        unknownSessions,
        conversionRate: metrics.conversionRate,
        successOrders,
        failedOrders,
        revenue,
        ga4Health: resolveIntegrationHealth(integrations, 'GA4_MP'),
        metaHealth: resolveIntegrationHealth(integrations, 'META_CAPI'),
        pipedriveHealth: resolveIntegrationHealth(integrations, 'PIPEDRIVE'),
        ordersByStatus: Array.from(groupedStatus.entries()).map(([status, total]) => ({ status, total })),
        revenueByDay: Array.from(groupedRevenue.entries())
          .sort(([a], [b]) => a.localeCompare(b))
          .map(([date, value]) => ({
            date,
            revenue: value,
          })),
      }
    },
    staleTime: 60_000,
  })
}

async function loadSessions(filters: DateRangeParams): Promise<SessionSummary[]> {
  const rows: SessionSummary[] = []
  let offset = 0

  while (rows.length < DASHBOARD_MAX_SESSIONS) {
    const limit = Math.min(DASHBOARD_SESSIONS_PAGE_SIZE, DASHBOARD_MAX_SESSIONS - rows.length)
    const page = await sessionsService.getSessions({ ...filters, limit, offset })
    const items = page.items ?? []

    if (items.length === 0) {
      break
    }

    rows.push(...items)

    if (items.length < limit) {
      break
    }
    offset += limit
  }

  return rows
}

function resolveBusinessStatus(order: OrderDto) {
  return (order.businessStatus || order.status || 'UNKNOWN').toUpperCase()
}

function resolveIntegrationHealth(rows: IntegrationLogDto[], integration: string) {
  const attemptedRows = rows.filter((row) => {
    return row.integration === integration && row.status.toUpperCase() !== 'SKIPPED'
  })
  if (attemptedRows.length === 0) {
    return null
  }
  const successCount = attemptedRows.filter((row) => SUCCESS_INTEGRATION_STATES.has(row.status.toUpperCase())).length
  return (successCount / attemptedRows.length) * 100
}

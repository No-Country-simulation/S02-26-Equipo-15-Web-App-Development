import { useQuery } from '@tanstack/react-query'

import { metricsService } from '@/admin/services/metricsService'
import { sessionsService } from '@/admin/services/sessionsService'
import type { DashboardStats, DateRangeParams, OrderDto } from '@/admin/types/api'
import { mapWithConcurrency } from '@/lib/utils'

const DASHBOARD_SESSION_SAMPLE = 40
const DASHBOARD_DETAIL_CONCURRENCY = 5
const SUCCESS_ORDER_STATES = new Set(['SUCCESS', 'SUCCEEDED', 'PAID'])
const FAILED_ORDER_STATES = new Set(['FAILED', 'ERROR', 'CANCELED'])

export function useDashboardStats(filters: DateRangeParams) {
  return useQuery({
    queryKey: ['dashboard', filters.from ?? '', filters.to ?? ''],
    queryFn: async (): Promise<DashboardStats> => {
      const [metrics, sessions] = await Promise.all([
        metricsService.getMetrics(filters),
        sessionsService.getSessions({ ...filters, limit: DASHBOARD_SESSION_SAMPLE, offset: 0 }),
      ])
      const sessionItems = sessions.items ?? []

      const details = await mapWithConcurrency(
        sessionItems,
        DASHBOARD_DETAIL_CONCURRENCY,
        async (session) => sessionsService.getSessionDetail(session.eventId).catch(() => null),
      )

      const orders = details.flatMap((detail) => detail?.orders ?? [])
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

      return {
        totalSessions: sessionItems.length,
        totalEvents,
        totalOrders: orders.length,
        conversionRate: metrics.conversionRate,
        successOrders,
        failedOrders,
        revenue,
        ga4Health: null,
        metaHealth: null,
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

function resolveBusinessStatus(order: OrderDto) {
  return (order.businessStatus || order.status || 'UNKNOWN').toUpperCase()
}

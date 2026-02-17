import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'

import { sessionsService } from '@/admin/services/sessionsService'
import type { SessionTableRow, SessionsParams } from '@/admin/types/api'
import { mapWithConcurrency } from '@/lib/utils'

const SESSION_DETAILS_CONCURRENCY = 4

export function useSessionsTableData(params: SessionsParams) {
  const sessionsQuery = useQuery({
    queryKey: ['sessions', params],
    queryFn: () => sessionsService.getSessions(params),
    staleTime: 30_000,
  })
  const sessionItems = sessionsQuery.data?.items ?? []
  const sessionEventIds = useMemo(() => sessionItems.map((session) => session.eventId), [sessionItems])

  const detailLookupQuery = useQuery({
    queryKey: ['session-details-for-list', sessionEventIds],
    queryFn: async () => {
      const details = await mapWithConcurrency(
        sessionEventIds,
        SESSION_DETAILS_CONCURRENCY,
        async (eventId) => sessionsService.getSessionDetail(eventId).catch(() => null),
      )
      return details
    },
    enabled: sessionEventIds.length > 0,
    staleTime: 30_000,
  })

  const rows = useMemo<SessionTableRow[]>(() => {
    const details = detailLookupQuery.data ?? []
    return sessionItems.map((session, index) => {
      const detail = details[index]
      const latestOrder = detail?.orders?.[0]
      const businessStatus = (latestOrder?.businessStatus || latestOrder?.status || 'UNKNOWN').toUpperCase()
      return {
        ...session,
        amount: latestOrder?.amount ?? null,
        currency: latestOrder?.currency ?? null,
        businessStatus,
        ga4Status: 'N/A',
        metaStatus: 'N/A',
      }
    })
  }, [detailLookupQuery.data, sessionItems])

  const isLoadingDetails = detailLookupQuery.isPending

  return {
    sessionsQuery,
    rows,
    isLoadingDetails,
  }
}

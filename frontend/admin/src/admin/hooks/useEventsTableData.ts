import { useMemo } from 'react'
import { useQueries, useQuery } from '@tanstack/react-query'

import { eventsService } from '@/admin/services/eventsService'
import { sessionsService } from '@/admin/services/sessionsService'
import type { EventsParams, EventsTableRow, SessionSummary } from '@/admin/types/api'

const SESSION_LOOKUP_LIMIT = 500

export function useEventsTableData(params: EventsParams) {
  const eventsQuery = useQuery({
    queryKey: ['events', params],
    queryFn: () => eventsService.getEvents(params),
    staleTime: 30_000,
  })

  const sessionsQuery = useQuery({
    queryKey: ['events-sessions-lookup', params.from ?? '', params.to ?? ''],
    queryFn: () =>
      sessionsService.getSessions({
        from: params.from,
        to: params.to,
        limit: SESSION_LOOKUP_LIMIT,
        offset: 0,
      }),
    staleTime: 60_000,
  })

  const eventIds = useMemo(() => {
    return [...new Set((eventsQuery.data?.items ?? []).map((item) => item.eventId))]
  }, [eventsQuery.data?.items])

  const detailQueries = useQueries({
    queries: eventIds.map((eventId) => ({
      queryKey: ['event-session-detail-for-events', eventId],
      queryFn: () => sessionsService.getSessionDetail(eventId),
      staleTime: 30_000,
    })),
  })

  const rows = useMemo<EventsTableRow[]>(() => {
    const sessionLookup = new Map<string, SessionSummary>()
    for (const session of sessionsQuery.data?.items ?? []) {
      sessionLookup.set(session.eventId, session)
    }

    const orderLookup = new Map<string, string | null>()
    for (let index = 0; index < eventIds.length; index += 1) {
      const detail = detailQueries[index]?.data
      orderLookup.set(eventIds[index], detail?.orders?.[0]?.id ?? null)
    }

    return (eventsQuery.data?.items ?? []).map((event) => {
      const session = sessionLookup.get(event.eventId)
      return {
        ...event,
        landingPath: session?.landingPath ?? null,
        utmSource: session?.utmSource ?? null,
        utmMedium: session?.utmMedium ?? null,
        utmCampaign: session?.utmCampaign ?? null,
        gclid: session?.gclid ?? null,
        fbclid: session?.fbclid ?? null,
        orderId: orderLookup.get(event.eventId) ?? null,
      }
    })
  }, [detailQueries, eventIds, eventsQuery.data?.items, sessionsQuery.data?.items])

  const isLoadingDetails = detailQueries.some((query) => query.isPending)

  return {
    eventsQuery,
    rows,
    sessionsQuery,
    isLoadingDetails,
  }
}

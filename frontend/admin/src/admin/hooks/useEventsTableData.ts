import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'

import { eventsService } from '@/admin/services/eventsService'
import { sessionsService } from '@/admin/services/sessionsService'
import type { EventsParams, EventsTableRow, SessionSummary } from '@/admin/types/api'
import { mapWithConcurrency } from '@/lib/utils'

const SESSION_LOOKUP_LIMIT = 500
const EVENT_DETAILS_CONCURRENCY = 4

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

  const detailLookupQuery = useQuery({
    queryKey: ['event-session-detail-for-events', eventIds],
    queryFn: async () =>
      mapWithConcurrency(
        eventIds,
        EVENT_DETAILS_CONCURRENCY,
        async (eventId) => sessionsService.getSessionDetail(eventId).catch(() => null),
      ),
    enabled: eventIds.length > 0,
    staleTime: 30_000,
  })

  const rows = useMemo<EventsTableRow[]>(() => {
    const sessionLookup = new Map<string, SessionSummary>()
    for (const session of sessionsQuery.data?.items ?? []) {
      sessionLookup.set(session.eventId, session)
    }

    const details = detailLookupQuery.data ?? []
    const orderLookup = new Map<string, string | null>()
    for (let index = 0; index < eventIds.length; index += 1) {
      const detail = details[index]
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
  }, [detailLookupQuery.data, eventIds, eventsQuery.data?.items, sessionsQuery.data?.items])

  const isLoadingDetails = detailLookupQuery.isPending

  return {
    eventsQuery,
    rows,
    sessionsQuery,
    isLoadingDetails,
  }
}

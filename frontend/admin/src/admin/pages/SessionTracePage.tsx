import { useMemo } from 'react'
import { Link, useParams } from 'react-router-dom'
import { CheckCircle2, Circle, CreditCard, GitMerge, Link2, TimerReset, TriangleAlert } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'

import { EmptyState } from '@/admin/components/common/EmptyState'
import { ErrorAlert } from '@/admin/components/common/ErrorAlert'
import { JsonViewerModal } from '@/admin/components/common/JsonViewerModal'
import { StatusChip } from '@/admin/components/common/StatusChip'
import { TableSkeleton } from '@/admin/components/common/Skeletons'
import { PageHeader } from '@/admin/components/layout/PageHeader'
import { Button } from '@/admin/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/admin/components/ui/card'
import { sessionsService } from '@/admin/services/sessionsService'
import { normalizeHttpError } from '@/admin/services/apiClient'
import type { EventDto, IntegrationLogDto, SessionDetail } from '@/admin/types/api'
import { formatCurrency, formatDateTime, safeJsonParse } from '@/lib/utils'

interface TimelineItem {
  id: string
  title: string
  subtitle: string
  timestamp: string
  status: string
  payload: unknown
}

export function SessionTracePage() {
  const { eventId } = useParams<{ eventId: string }>()

  const detailQuery = useQuery({
    queryKey: ['session-detail', eventId],
    queryFn: async () => {
      if (!eventId) {
        throw new Error('eventId requerido')
      }
      return sessionsService.getSessionDetail(eventId)
    },
    enabled: Boolean(eventId),
  })

  const timeline = useMemo(() => {
    if (!detailQuery.data) {
      return []
    }
    return buildTimeline(detailQuery.data)
  }, [detailQuery.data])

  const correlation = useMemo(() => {
    const order = detailQuery.data?.orders?.[0]
    return {
      eventId: detailQuery.data?.session.eventId ?? '-',
      orderId: order?.id ?? '-',
      paymentIntentId: order?.paymentIntentId ?? '-',
      stripeSessionId: order?.stripeSessionId ?? '-',
      transactionId: order?.stripeSessionId ?? '-',
      fbtraceId: resolveFbtraceId(detailQuery.data),
    }
  }, [detailQuery.data])

  const error = detailQuery.error ? normalizeHttpError(detailQuery.error) : null

  return (
    <section className="space-y-6">
      <PageHeader
        title="Trace View"
        description={`Trazabilidad end-to-end para eventId ${eventId ?? ''}`}
        actions={
          <Button asChild variant="outline">
            <Link to="/admin/sessions">Volver</Link>
          </Button>
        }
      />

      {detailQuery.isPending ? <TableSkeleton rows={6} /> : null}
      {error ? <ErrorAlert error={error} /> : null}

      {detailQuery.data ? (
        <>
          <div className="grid gap-6 xl:grid-cols-3">
            <Card className="xl:col-span-2">
              <CardHeader>
                <CardTitle>1) Session info</CardTitle>
              </CardHeader>
              <CardContent className="grid gap-2 text-sm md:grid-cols-2">
                <div>
                  <p className="text-muted">eventId</p>
                  <p className="font-mono">{detailQuery.data.session.eventId}</p>
                </div>
                <div>
                  <p className="text-muted">first seen</p>
                  <p>{formatDateTime(detailQuery.data.session.createdAt)}</p>
                </div>
                <div>
                  <p className="text-muted">last seen</p>
                  <p>{formatDateTime(detailQuery.data.session.lastSeenAt)}</p>
                </div>
                <div>
                  <p className="text-muted">landing_path</p>
                  <p>{detailQuery.data.session.landingPath || '-'}</p>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>3) Correlation IDs</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3 text-sm">
                <KeyValue label="eventId" value={correlation.eventId} mono />
                <KeyValue label="orderId" value={correlation.orderId} mono />
                <KeyValue label="payment_intent_id" value={correlation.paymentIntentId} mono />
                <KeyValue label="stripe_session_id" value={correlation.stripeSessionId} mono />
                <KeyValue label="transaction_id" value={correlation.transactionId} mono />
                <KeyValue label="fbtrace_id" value={correlation.fbtraceId} mono />
              </CardContent>
            </Card>
          </div>

          <Card>
            <CardHeader>
              <CardTitle>2) Timeline</CardTitle>
            </CardHeader>
            <CardContent>
              {timeline.length === 0 ? (
                <EmptyState title="Sin pasos de trazabilidad" description="No hay eventos ni ordenes registradas para esta sesion." />
              ) : (
                <ol className="relative ml-3 border-l border-border">
                  {timeline.map((item) => (
                    <li key={item.id} className="mb-8 ml-6">
                      <span className="absolute -left-3 flex h-6 w-6 items-center justify-center rounded-full border border-border bg-surface">
                        <TimelineIcon status={item.status} title={item.title} />
                      </span>
                      <div className="rounded-2xl border border-border bg-slate-900/50 p-4">
                        <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
                          <div>
                            <h3 className="font-semibold text-slate-100">{item.title}</h3>
                            <p className="text-xs text-muted">{item.subtitle}</p>
                          </div>
                          <div className="flex items-center gap-2">
                            <StatusChip status={item.status} />
                            <span className="text-xs text-muted">{formatDateTime(item.timestamp)}</span>
                          </div>
                        </div>
                        <JsonViewerModal title={`${item.title} payload`} payload={item.payload} />
                      </div>
                    </li>
                  ))}
                </ol>
              )}
            </CardContent>
          </Card>
        </>
      ) : null}
    </section>
  )
}

function buildTimeline(detail: SessionDetail): TimelineItem[] {
  const rows: TimelineItem[] = [
    {
      id: `session-${detail.session.eventId}`,
      title: 'tracking_session',
      subtitle: 'Session creada',
      timestamp: detail.session.createdAt,
      status: 'PROCESSED',
      payload: detail.session,
    },
  ]

  const sortedEvents = [...detail.events].sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
  sortedEvents.forEach((event) => {
    rows.push({
      id: `event-${event.id}`,
      title: `tracking_event: ${event.eventType}`,
      subtitle: `eventId=${event.eventId}`,
      timestamp: event.createdAt,
      status: event.eventType === 'purchase' ? 'SUCCESS' : 'SENT',
      payload: event,
    })

    if (event.eventType === 'purchase') {
      const inferred = inferStripeWebhookStep(event)
      if (inferred) {
        rows.push(inferred)
      }
    }
  })

  detail.orders
    .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
    .forEach((order) => {
      rows.push({
        id: `order-${order.id}`,
        title: 'order persisted',
        subtitle: `amount=${formatCurrency(order.amount, order.currency)}`,
        timestamp: order.createdAt,
        status: order.businessStatus || order.status,
        payload: order,
      })
    })

  detail.integrations
    .filter((integration) => integration.integration !== 'PIPEDRIVE')
    .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
    .forEach((integration) => {
      rows.push({
        id: `integration-${integration.id}`,
        title: integrationLabel(integration.integration),
        subtitle: integrationSubtitle(integration),
        timestamp: integration.createdAt,
        status: integration.status || 'N/A',
        payload: integrationPayload(integration),
      })
    })

  return rows.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime())
}

function inferStripeWebhookStep(event: EventDto): TimelineItem | null {
  const payload = safeJsonParse(event.payloadJson) as Record<string, unknown> | null
  if (!payload) {
    return null
  }

  const stripeEventId = typeof payload.id === 'string' ? payload.id : 'unknown'
  const stripeType = typeof payload.type === 'string' ? payload.type : 'checkout.session.completed'
  return {
    id: `stripe-${event.id}`,
    title: 'stripe_webhook_event (inferido)',
    subtitle: `${stripeType} - ${stripeEventId}`,
    timestamp: event.createdAt,
    status: 'PROCESSED',
    payload,
  }
}

function integrationLabel(integration: string) {
  if (integration === 'GA4_MP') {
    return 'integration: GA4 MP'
  }
  if (integration === 'META_CAPI') {
    return 'integration: Meta CAPI'
  }
  if (integration === 'PIPEDRIVE') {
    return 'integration: Pipedrive'
  }
  return `integration: ${integration}`
}

function integrationSubtitle(integration: IntegrationLogDto) {
  const chunks: string[] = []
  if (integration.httpStatus != null) {
    chunks.push(`http=${integration.httpStatus}`)
  }
  if (integration.latencyMs != null) {
    chunks.push(`latency=${integration.latencyMs}ms`)
  }
  if (integration.errorMessage) {
    chunks.push(integration.errorMessage)
  }
  if (chunks.length === 0) {
    return `status=${integration.status}`
  }
  return chunks.join(' | ')
}

function integrationPayload(integration: IntegrationLogDto) {
  return {
    ...integration,
    requestPayload: safeJsonParse(integration.requestPayload) ?? integration.requestPayload,
    responsePayload: safeJsonParse(integration.responsePayload) ?? integration.responsePayload,
  }
}

function resolveFbtraceId(detail: SessionDetail | undefined) {
  if (!detail?.integrations?.length) {
    return 'N/A'
  }
  const meta = detail.integrations.find((row) => row.integration === 'META_CAPI')
  if (!meta?.responsePayload) {
    return 'N/A'
  }
  const parsed = safeJsonParse(meta.responsePayload) as Record<string, unknown> | null
  const fbtraceId = parsed?.fbtrace_id
  return typeof fbtraceId === 'string' && fbtraceId.length > 0 ? fbtraceId : 'N/A'
}

function KeyValue({ label, value, mono = false }: { label: string; value: string; mono?: boolean }) {
  return (
    <div>
      <p className="text-xs text-muted">{label}</p>
      <p className={mono ? 'break-all font-mono text-xs text-slate-200' : 'break-all text-slate-200'}>{value}</p>
    </div>
  )
}

function TimelineIcon({ status, title }: { status: string; title: string }) {
  const normalized = status.toUpperCase()
  if (title.includes('order')) {
    return <CreditCard className="h-3.5 w-3.5 text-cyan-300" />
  }
  if (title.includes('stripe')) {
    return <GitMerge className="h-3.5 w-3.5 text-blue-300" />
  }
  if (normalized === 'SUCCESS' || normalized === 'SUCCEEDED' || normalized === 'PROCESSED') {
    return <CheckCircle2 className="h-3.5 w-3.5 text-emerald-300" />
  }
  if (normalized === 'FAILED' || normalized === 'ERROR') {
    return <TriangleAlert className="h-3.5 w-3.5 text-red-300" />
  }
  if (normalized === 'N/A') {
    return <Link2 className="h-3.5 w-3.5 text-muted" />
  }
  if (title.includes('tracking_session')) {
    return <TimerReset className="h-3.5 w-3.5 text-slate-300" />
  }
  return <Circle className="h-3.5 w-3.5 text-slate-400" />
}

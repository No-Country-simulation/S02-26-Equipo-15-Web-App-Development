export interface ApiError {
  error: string
  message: string
  details: string[]
}

export interface PagedResponse<T> {
  items: T[]
  limit: number
  offset: number
}

export interface SessionSummary {
  eventId: string
  createdAt: string
  lastSeenAt: string
  utmSource: string | null
  utmMedium: string | null
  utmCampaign: string | null
  utmTerm: string | null
  utmContent: string | null
  gclid: string | null
  fbclid: string | null
  landingPath: string | null
  userAgent: string | null
  ipHash: string | null
}

export interface EventDto {
  id: string
  eventId: string
  eventType: string
  createdAt: string
  currency: string | null
  value: number | null
  payloadJson: string | null
}

export interface OrderDto {
  id: string
  eventId: string | null
  stripeSessionId: string
  paymentIntentId: string | null
  amount: number
  currency: string
  status: string
  businessStatus: string | null
  createdAt: string
}

export interface SessionDetail {
  session: SessionSummary
  events: EventDto[]
  orders: OrderDto[]
  integrations: IntegrationLogDto[]
}

export interface IntegrationLogDto {
  id: string
  integration: string
  referenceId: string | null
  status: string
  httpStatus: number | null
  latencyMs: number | null
  requestPayload: string | null
  responsePayload: string | null
  errorMessage: string | null
  createdAt: string
}

export interface MetricsDto {
  landingView: number
  clickCta: number
  beginCheckout: number
  purchase: number
  conversionRate: number
  orphanFailedOrders: number
}

export interface DateRangeParams {
  from?: string
  to?: string
}

export interface SessionsParams extends DateRangeParams {
  utm_source?: string
  limit?: number
  offset?: number
}

export interface EventsParams extends DateRangeParams {
  eventType?: string
  limit?: number
  offset?: number
}

export interface SessionTableRow extends SessionSummary {
  amount: number | null
  currency: string | null
  businessStatus: string
  ga4Status: string
  metaStatus: string
  pipedriveStatus: string
}

export interface DashboardOrderStat {
  status: string
  total: number
}

export interface DashboardRevenuePoint {
  date: string
  revenue: number
}

export interface DashboardStats {
  totalSessions: number
  totalEvents: number
  totalOrders: number
  unknownSessions: number
  conversionRate: number
  successOrders: number
  failedOrders: number
  revenue: number
  ga4Health: number | null
  metaHealth: number | null
  pipedriveHealth: number | null
  ordersByStatus: DashboardOrderStat[]
  revenueByDay: DashboardRevenuePoint[]
}

export interface EventsTableRow extends EventDto {
  landingPath: string | null
  utmSource: string | null
  utmMedium: string | null
  utmCampaign: string | null
  gclid: string | null
  fbclid: string | null
  orderId: string | null
}

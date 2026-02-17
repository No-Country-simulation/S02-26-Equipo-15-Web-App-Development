import { apiClient } from '@/admin/services/apiClient'
import { ensurePagedResponse } from '@/admin/services/responseGuards'
import type { EventDto, EventsParams } from '@/admin/types/api'

export const eventsService = {
  async getEvents(params?: EventsParams) {
    const response = await apiClient.get<unknown>('/api/admin/events', { params })
    return ensurePagedResponse<EventDto>(response.data, '/api/admin/events')
  },
}

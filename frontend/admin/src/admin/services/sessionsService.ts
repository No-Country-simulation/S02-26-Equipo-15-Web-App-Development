import { apiClient } from '@/admin/services/apiClient'
import { ensurePagedResponse, ensureSessionDetail } from '@/admin/services/responseGuards'
import type { SessionSummary, SessionsParams } from '@/admin/types/api'

export const sessionsService = {
  async getSessions(params?: SessionsParams) {
    const response = await apiClient.get<unknown>('/api/admin/sessions', { params })
    return ensurePagedResponse<SessionSummary>(response.data, '/api/admin/sessions')
  },

  async getSessionDetail(eventId: string) {
    const response = await apiClient.get<unknown>(`/api/admin/sessions/${eventId}`)
    return ensureSessionDetail(response.data, `/api/admin/sessions/${eventId}`)
  },
}

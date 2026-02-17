import { apiClient } from '@/admin/services/apiClient'
import type { DateRangeParams, MetricsDto } from '@/admin/types/api'

export const metricsService = {
  async getMetrics(params?: DateRangeParams) {
    const response = await apiClient.get<MetricsDto>('/api/admin/metrics', { params })
    return response.data
  },
}

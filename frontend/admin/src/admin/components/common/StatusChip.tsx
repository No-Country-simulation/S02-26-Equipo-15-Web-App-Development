import { Badge } from '@/admin/components/ui/badge'

interface StatusChipProps {
  status: string | null | undefined
}

const STATUS_VARIANT_MAP: Record<string, 'success' | 'danger' | 'warning' | 'info' | 'muted' | 'default'> = {
  SUCCESS: 'success',
  SUCCEEDED: 'success',
  PAID: 'success',
  PROCESSED: 'success',
  SENT: 'info',
  FAILED: 'danger',
  ERROR: 'danger',
  RATE_LIMITED: 'warning',
  SKIPPED: 'muted',
  PENDING: 'warning',
  UNKNOWN: 'muted',
  N_A: 'muted',
}

export function StatusChip({ status }: StatusChipProps) {
  const normalized = (status || 'UNKNOWN').toUpperCase().replace('/', '_').replace(/\s+/g, '_')
  const variant = STATUS_VARIANT_MAP[normalized] ?? 'default'
  const label = normalized === 'N_A' ? 'N/A' : normalized

  return <Badge variant={variant}>{label}</Badge>
}

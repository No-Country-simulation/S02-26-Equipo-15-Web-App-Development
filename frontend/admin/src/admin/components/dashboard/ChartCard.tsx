import type { ReactNode } from 'react'

import { Card, CardContent, CardHeader, CardTitle } from '@/admin/components/ui/card'
import { cn } from '@/lib/utils'

interface ChartCardProps {
  title: string
  children: ReactNode
  className?: string
  chartClassName?: string
  isLoading?: boolean
  hasData?: boolean
  emptyState?: ReactNode
}

export function ChartCard({
  title,
  children,
  className,
  chartClassName,
  isLoading = false,
  hasData = true,
  emptyState = null,
}: ChartCardProps) {
  return (
    <Card className={className}>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="h-[320px] min-h-[280px] w-full animate-pulse rounded-xl bg-slate-800/80" />
        ) : !hasData ? (
          emptyState
        ) : (
          <div className={cn('h-[320px] min-h-[280px] w-full', chartClassName)}>{children}</div>
        )}
      </CardContent>
    </Card>
  )
}

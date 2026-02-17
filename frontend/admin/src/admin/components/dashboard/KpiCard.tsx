import { Card, CardContent, CardHeader, CardTitle } from '@/admin/components/ui/card'

interface KpiCardProps {
  title: string
  value: string | number
  subtext?: string
}

export function KpiCard({ title, value, subtext }: KpiCardProps) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-sm text-muted">{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-2xl font-bold text-slate-100">{value}</p>
        {subtext ? <p className="mt-1 text-xs text-muted">{subtext}</p> : null}
      </CardContent>
    </Card>
  )
}

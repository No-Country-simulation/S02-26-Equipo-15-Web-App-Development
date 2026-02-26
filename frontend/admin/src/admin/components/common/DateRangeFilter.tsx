import { CalendarRange } from 'lucide-react'

import { Input } from '@/admin/components/ui/input'

interface DateRangeFilterProps {
  from: string
  to: string
  onFromChange: (value: string) => void
  onToChange: (value: string) => void
}

export function DateRangeFilter({ from, to, onFromChange, onToChange }: DateRangeFilterProps) {
  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2 text-sm text-muted">
        <CalendarRange className="h-4 w-4" />
        <span>Rango</span>
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <Input type="date" value={from} onChange={(event) => onFromChange(event.target.value)} />
        <Input type="date" value={to} onChange={(event) => onToChange(event.target.value)} />
      </div>
    </div>
  )
}

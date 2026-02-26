import { CalendarRange } from 'lucide-react'

import { Input } from '@/admin/components/ui/input'

interface DateRangeFilterProps {
  from: string
  to: string
  onFromChange: (value: string) => void
  onToChange: (value: string) => void
}

function splitDateTime(value: string) {
  if (!value) {
    return { date: '', time: '' }
  }
  const [datePart = '', timePart = ''] = value.split('T')
  return {
    date: datePart,
    time: timePart.slice(0, 5),
  }
}

function mergeDateTime(date: string, time: string, fallbackTime: string) {
  if (!date) {
    return ''
  }
  return `${date}T${time || fallbackTime}`
}

export function DateRangeFilter({ from, to, onFromChange, onToChange }: DateRangeFilterProps) {
  const fromParts = splitDateTime(from)
  const toParts = splitDateTime(to)

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2 text-sm text-muted">
        <CalendarRange className="h-4 w-4" />
        <span>Rango</span>
      </div>
      <div className="grid gap-3 lg:grid-cols-2">
        <div className="grid min-w-0 grid-cols-[minmax(0,1fr)_110px] gap-2">
          <Input
            type="date"
            value={fromParts.date}
            onChange={(event) => onFromChange(mergeDateTime(event.target.value, fromParts.time, '00:00'))}
          />
          <Input
            type="time"
            step={60}
            value={fromParts.time}
            onChange={(event) => onFromChange(mergeDateTime(fromParts.date, event.target.value, '00:00'))}
            disabled={!fromParts.date}
          />
        </div>

        <div className="grid min-w-0 grid-cols-[minmax(0,1fr)_110px] gap-2">
          <Input
            type="date"
            value={toParts.date}
            onChange={(event) => onToChange(mergeDateTime(event.target.value, toParts.time, '23:59'))}
          />
          <Input
            type="time"
            step={60}
            value={toParts.time}
            onChange={(event) => onToChange(mergeDateTime(toParts.date, event.target.value, '23:59'))}
            disabled={!toParts.date}
          />
        </div>
      </div>
    </div>
  )
}

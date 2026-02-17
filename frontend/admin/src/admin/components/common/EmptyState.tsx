import { Inbox } from 'lucide-react'

interface EmptyStateProps {
  title: string
  description: string
}

export function EmptyState({ title, description }: EmptyStateProps) {
  return (
    <div className="rounded-2xl border border-dashed border-border bg-slate-900/40 p-10 text-center">
      <Inbox className="mx-auto mb-3 h-8 w-8 text-muted" />
      <h3 className="mb-2 text-lg font-semibold text-slate-100">{title}</h3>
      <p className="text-sm text-muted">{description}</p>
    </div>
  )
}

import { Button } from '@/admin/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/admin/components/ui/dialog'

interface JsonViewerModalProps {
  title: string
  payload: unknown
  triggerLabel?: string
}

export function JsonViewerModal({ title, payload, triggerLabel = 'Ver JSON' }: JsonViewerModalProps) {
  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button size="sm" variant="outline">
          {triggerLabel}
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>Payload capturado para trazabilidad.</DialogDescription>
        </DialogHeader>
        <pre className="max-h-[60vh] overflow-auto rounded-xl border border-border bg-slate-950/70 p-4 text-xs text-cyan-200">
          {JSON.stringify(payload, null, 2)}
        </pre>
      </DialogContent>
    </Dialog>
  )
}

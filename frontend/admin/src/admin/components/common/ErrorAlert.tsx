import { AlertTriangle } from 'lucide-react'

import type { HttpClientError } from '@/admin/services/apiClient'

interface ErrorAlertProps {
  title?: string
  error: HttpClientError
}

export function ErrorAlert({ title = 'No fue posible cargar la informacion', error }: ErrorAlertProps) {
  return (
    <div className="rounded-2xl border border-danger/40 bg-danger/10 p-4 text-sm text-red-200">
      <div className="mb-1 flex items-center gap-2 font-semibold text-red-100">
        <AlertTriangle className="h-4 w-4" />
        <span>{title}</span>
      </div>
      <p className="mb-1 text-red-200">{error.message}</p>
      {import.meta.env.DEV && error.requestUrl ? (
        <p className="mb-1 break-all font-mono text-xs text-red-200/90">request: {error.requestUrl}</p>
      ) : null}
      {error.details.length > 0 ? (
        <ul className="list-disc space-y-1 pl-5 text-xs text-red-200/90">
          {error.details.map((detail) => (
            <li key={detail}>{detail}</li>
          ))}
        </ul>
      ) : null}
    </div>
  )
}

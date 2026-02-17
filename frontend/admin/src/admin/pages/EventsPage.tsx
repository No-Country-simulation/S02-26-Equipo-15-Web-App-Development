import { useMemo, useState } from 'react'
import { type ColumnDef, flexRender, getCoreRowModel, useReactTable } from '@tanstack/react-table'

import { DateRangeFilter } from '@/admin/components/common/DateRangeFilter'
import { EmptyState } from '@/admin/components/common/EmptyState'
import { ErrorAlert } from '@/admin/components/common/ErrorAlert'
import { TableSkeleton } from '@/admin/components/common/Skeletons'
import { PageHeader } from '@/admin/components/layout/PageHeader'
import { Button } from '@/admin/components/ui/button'
import { Input } from '@/admin/components/ui/input'
import { Select } from '@/admin/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/admin/components/ui/table'
import { useEventsTableData } from '@/admin/hooks/useEventsTableData'
import { normalizeHttpError } from '@/admin/services/apiClient'
import type { EventsTableRow } from '@/admin/types/api'
import { formatDateTime } from '@/lib/utils'

const PAGE_SIZE = 30

const columns: ColumnDef<EventsTableRow>[] = [
  { accessorKey: 'eventType', header: 'eventType' },
  { accessorKey: 'landingPath', header: 'landing_path' },
  { accessorKey: 'utmSource', header: 'utm_source' },
  { accessorKey: 'utmMedium', header: 'utm_medium' },
  { accessorKey: 'utmCampaign', header: 'utm_campaign' },
  { accessorKey: 'gclid', header: 'gclid' },
  { accessorKey: 'fbclid', header: 'fbclid' },
  {
    accessorKey: 'eventId',
    header: 'eventId',
    cell: ({ row }) => <span className="font-mono text-xs">{row.original.eventId}</span>,
  },
  {
    accessorKey: 'createdAt',
    header: 'timestamp',
    cell: ({ row }) => formatDateTime(row.original.createdAt),
  },
  {
    accessorKey: 'orderId',
    header: 'orderId',
    cell: ({ row }) => <span className="font-mono text-xs">{row.original.orderId ?? '-'}</span>,
  },
]

function toIso(value: string) {
  if (!value) {
    return undefined
  }
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? undefined : parsed.toISOString()
}

export function EventsPage() {
  const [search, setSearch] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [eventType, setEventType] = useState('')
  const [offset, setOffset] = useState(0)

  const { eventsQuery, rows, isLoadingDetails } = useEventsTableData({
    from: toIso(from),
    to: toIso(to),
    eventType: eventType || undefined,
    limit: PAGE_SIZE,
    offset,
  })

  const filteredRows = useMemo(
    () => rows.filter((row) => row.eventId.toLowerCase().includes(search.toLowerCase())),
    [rows, search],
  )

  const table = useReactTable({
    data: filteredRows,
    columns,
    getCoreRowModel: getCoreRowModel(),
  })

  const hasNext = (eventsQuery.data?.items?.length ?? 0) === PAGE_SIZE
  const error = eventsQuery.error ? normalizeHttpError(eventsQuery.error) : null

  return (
    <section className="space-y-4">
      <PageHeader
        title="Events"
        description="Eventos de tracking con enrichment de atribucion por eventId"
        actions={
          <div className="flex items-center gap-2">
            <Button variant="outline" onClick={() => setOffset((current) => Math.max(current - PAGE_SIZE, 0))} disabled={offset === 0}>
              Anterior
            </Button>
            <Button variant="outline" onClick={() => setOffset((current) => current + PAGE_SIZE)} disabled={!hasNext}>
              Siguiente
            </Button>
          </div>
        }
      />

      <div className="grid gap-3 rounded-2xl border border-border bg-card p-4 lg:grid-cols-4">
        <Input
          placeholder="Buscar eventId..."
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
        <DateRangeFilter from={from} to={to} onFromChange={setFrom} onToChange={setTo} />
        <Select value={eventType} onChange={(event) => setEventType(event.target.value)}>
          <option value="">Todos los tipos</option>
          <option value="landing_view">landing_view</option>
          <option value="click_cta">click_cta</option>
          <option value="begin_checkout">begin_checkout</option>
          <option value="purchase">purchase</option>
        </Select>
        <Button
          variant="secondary"
          onClick={() => {
            setSearch('')
            setFrom('')
            setTo('')
            setEventType('')
            setOffset(0)
          }}
        >
          Limpiar filtros
        </Button>
      </div>

      {eventsQuery.isPending ? <TableSkeleton /> : null}
      {error ? <ErrorAlert error={error} /> : null}

      {!eventsQuery.isPending && !error ? (
        <div className="overflow-hidden rounded-2xl border border-border bg-card">
          {filteredRows.length === 0 ? (
            <div className="p-6">
              <EmptyState
                title="No hay eventos para mostrar"
                description="Ajusta los filtros de fecha o tipo de evento."
              />
            </div>
          ) : (
            <Table>
              <TableHeader className="sticky top-0 z-10 bg-surface">
                {table.getHeaderGroups().map((headerGroup) => (
                  <TableRow key={headerGroup.id}>
                    {headerGroup.headers.map((header) => (
                      <TableHead key={header.id}>
                        {header.isPlaceholder
                          ? null
                          : flexRender(header.column.columnDef.header, header.getContext())}
                      </TableHead>
                    ))}
                  </TableRow>
                ))}
              </TableHeader>
              <TableBody>
                {table.getRowModel().rows.map((row) => (
                  <TableRow key={row.id}>
                    {row.getVisibleCells().map((cell) => (
                      <TableCell key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </div>
      ) : null}

      {isLoadingDetails ? (
        <p className="text-xs text-muted">Resolviendo enrichment de sesiones y ordenes para eventos visibles...</p>
      ) : null}
    </section>
  )
}

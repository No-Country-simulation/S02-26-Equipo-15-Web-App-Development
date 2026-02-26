import { useCallback, useDeferredValue, useMemo, useState, type ChangeEvent } from 'react'
import { type ColumnDef, flexRender, getCoreRowModel, useReactTable } from '@tanstack/react-table'

import { DateRangeFilter } from '@/admin/components/common/DateRangeFilter'
import { EmptyState } from '@/admin/components/common/EmptyState'
import { ErrorAlert } from '@/admin/components/common/ErrorAlert'
import { StickyFiltersPanel } from '@/admin/components/common/StickyFiltersPanel'
import { TableSkeleton } from '@/admin/components/common/Skeletons'
import { PageHeader } from '@/admin/components/layout/PageHeader'
import { Button } from '@/admin/components/ui/button'
import { Input } from '@/admin/components/ui/input'
import { Select } from '@/admin/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/admin/components/ui/table'
import { useDebouncedValue } from '@/admin/hooks/useDebouncedValue'
import { useEventsTableData } from '@/admin/hooks/useEventsTableData'
import { normalizeHttpError } from '@/admin/services/apiClient'
import type { EventsTableRow } from '@/admin/types/api'
import { formatDateTime } from '@/lib/utils'

const PAGE_SIZE = 30

const columns: ColumnDef<EventsTableRow>[] = [
  { accessorKey: 'eventType', header: 'eventType' },
  { accessorKey: 'landingPath', header: 'landing_path' },
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

function toIso(value: string, endOfDay = false) {
  if (!value) {
    return undefined
  }
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return undefined
  }

  if (endOfDay) {
    parsed.setHours(23, 59, 59, 999)
  } else {
    parsed.setHours(0, 0, 0, 0)
  }
  return parsed.toISOString()
}

export function EventsPage() {
  const [searchInput, setSearchInput] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [eventType, setEventType] = useState('')
  const [offset, setOffset] = useState(0)
  const debouncedSearch = useDebouncedValue(searchInput, 350)
  const deferredSearch = useDeferredValue(debouncedSearch)
  const debouncedFrom = useDebouncedValue(from, 450)
  const debouncedTo = useDebouncedValue(to, 450)
  const normalizedSearch = deferredSearch.trim().toLowerCase()

  const { eventsQuery, rows, isLoadingDetails } = useEventsTableData({
    from: toIso(debouncedFrom),
    to: toIso(debouncedTo, true),
    eventType: eventType || undefined,
    limit: PAGE_SIZE,
    offset,
  })

  const filteredRows = useMemo(
    () => rows.filter((row) => row.eventId.toLowerCase().includes(normalizedSearch)),
    [normalizedSearch, rows],
  )

  const handleSearchChange = useCallback((event: ChangeEvent<HTMLInputElement>) => {
    setSearchInput(event.target.value)
  }, [])

  const handleEventTypeChange = useCallback((event: ChangeEvent<HTMLSelectElement>) => {
    setEventType(event.target.value)
  }, [])

  const handlePreviousPage = useCallback(() => {
    setOffset((current) => Math.max(current - PAGE_SIZE, 0))
  }, [])

  const handleNextPage = useCallback(() => {
    setOffset((current) => current + PAGE_SIZE)
  }, [])

  const handleClearFilters = useCallback(() => {
    setSearchInput('')
    setFrom('')
    setTo('')
    setEventType('')
    setOffset(0)
  }, [])

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
            <Button variant="outline" onClick={handlePreviousPage} disabled={offset === 0}>
              Anterior
            </Button>
            <Button variant="outline" onClick={handleNextPage} disabled={!hasNext}>
              Siguiente
            </Button>
          </div>
        }
      />

      <StickyFiltersPanel>
        <div className="grid gap-3 lg:grid-cols-4">
          <Input
            placeholder="Buscar eventId..."
            value={searchInput}
            onChange={handleSearchChange}
          />
          <DateRangeFilter from={from} to={to} onFromChange={setFrom} onToChange={setTo} />
          <Select value={eventType} onChange={handleEventTypeChange}>
            <option value="">Todos los tipos</option>
            <option value="landing_view">landing_view</option>
            <option value="click_cta">click_cta</option>
            <option value="begin_checkout">begin_checkout</option>
            <option value="purchase">purchase</option>
          </Select>
          <Button variant="secondary" onClick={handleClearFilters}>
            Limpiar filtros
          </Button>
        </div>
      </StickyFiltersPanel>

      {eventsQuery.isPending ? <TableSkeleton /> : null}
      {error ? <ErrorAlert error={error} /> : null}

      {!eventsQuery.isPending && !error ? (
        <>
          {filteredRows.length === 0 ? (
            <div className="rounded-2xl border border-border bg-card p-6">
              <EmptyState
                title="No hay eventos para mostrar"
                description="Ajusta los filtros de fecha o tipo de evento."
              />
            </div>
          ) : (
            <>
              <div className="space-y-3 md:hidden">
                {filteredRows.map((row) => (
                  <article key={row.id} className="rounded-2xl border border-border bg-card p-4">
                    <div className="flex items-center justify-between gap-3">
                      <p className="text-sm font-semibold text-slate-100">{row.eventType}</p>
                      <p className="text-xs text-muted">{formatDateTime(row.createdAt)}</p>
                    </div>
                    <div className="mt-3 space-y-1">
                      <p className="text-[11px] uppercase tracking-wide text-muted">eventId</p>
                      <p className="break-all font-mono text-xs text-slate-200">{row.eventId}</p>
                    </div>
                    <div className="mt-3 grid grid-cols-2 gap-3">
                      <div>
                        <p className="text-[11px] uppercase tracking-wide text-muted">landing_path</p>
                        <p className="break-all text-xs text-slate-100">{row.landingPath ?? '-'}</p>
                      </div>
                      <div>
                        <p className="text-[11px] uppercase tracking-wide text-muted">orderId</p>
                        <p className="break-all font-mono text-xs text-slate-100">{row.orderId ?? '-'}</p>
                      </div>
                    </div>
                  </article>
                ))}
              </div>

              <div className="hidden overflow-hidden rounded-2xl border border-border bg-card md:block">
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
              </div>
            </>
          )}
        </>
      ) : null}

      {isLoadingDetails ? (
        <p className="text-xs text-muted">Resolviendo enrichment de sesiones y ordenes para eventos visibles...</p>
      ) : null}
    </section>
  )
}

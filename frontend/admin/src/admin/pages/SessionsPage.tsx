import { useCallback, useDeferredValue, useMemo, useState, type ChangeEvent } from 'react'
import { Link } from 'react-router-dom'
import { type ColumnDef, flexRender, getCoreRowModel, useReactTable } from '@tanstack/react-table'

import { DateRangeFilter } from '@/admin/components/common/DateRangeFilter'
import { EmptyState } from '@/admin/components/common/EmptyState'
import { ErrorAlert } from '@/admin/components/common/ErrorAlert'
import { StatusChip } from '@/admin/components/common/StatusChip'
import { StickyFiltersPanel } from '@/admin/components/common/StickyFiltersPanel'
import { TableSkeleton } from '@/admin/components/common/Skeletons'
import { PageHeader } from '@/admin/components/layout/PageHeader'
import { Button } from '@/admin/components/ui/button'
import { Input } from '@/admin/components/ui/input'
import { Select } from '@/admin/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/admin/components/ui/table'
import { useDebouncedValue } from '@/admin/hooks/useDebouncedValue'
import { useSessionsTableData } from '@/admin/hooks/useSessionsTableData'
import { normalizeHttpError } from '@/admin/services/apiClient'
import type { SessionTableRow } from '@/admin/types/api'
import { formatCurrency, formatDateTime } from '@/lib/utils'

const PAGE_SIZE = 25

const columns: ColumnDef<SessionTableRow>[] = [
  {
    accessorKey: 'eventId',
    header: 'eventId',
    cell: ({ row }) => <span className="font-mono text-xs">{row.original.eventId}</span>,
  },
  {
    accessorKey: 'createdAt',
    header: 'Fecha',
    cell: ({ row }) => <span>{formatDateTime(row.original.createdAt)}</span>,
  },
  {
    accessorKey: 'businessStatus',
    header: 'business_status',
    cell: ({ row }) => <StatusChip status={row.original.businessStatus} />,
  },
  {
    accessorKey: 'amount',
    header: 'amount',
    cell: ({ row }) => formatCurrency(row.original.amount, row.original.currency ?? 'USD'),
  },
  {
    accessorKey: 'ga4Status',
    header: 'GA4 status',
    cell: ({ row }) => <StatusChip status={row.original.ga4Status} />,
  },
  {
    accessorKey: 'metaStatus',
    header: 'Meta status',
    cell: ({ row }) => <StatusChip status={row.original.metaStatus} />,
  },
  {
    id: 'actions',
    header: 'Accion',
    cell: ({ row }) => (
      <Button asChild size="sm" variant="outline">
        <Link to={`/admin/sessions/${row.original.eventId}`}>Ver trazabilidad</Link>
      </Button>
    ),
  },
]

function toIso(value: string) {
  if (!value) {
    return undefined
  }
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? undefined : parsed.toISOString()
}

export function SessionsPage() {
  const [searchInput, setSearchInput] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [businessStatus, setBusinessStatus] = useState('ALL')
  const [offset, setOffset] = useState(0)
  const debouncedSearch = useDebouncedValue(searchInput, 350)
  const deferredSearch = useDeferredValue(debouncedSearch)
  const debouncedFrom = useDebouncedValue(from, 450)
  const debouncedTo = useDebouncedValue(to, 450)
  const normalizedSearch = deferredSearch.trim().toLowerCase()

  const { sessionsQuery, rows, isLoadingDetails } = useSessionsTableData({
    from: toIso(debouncedFrom),
    to: toIso(debouncedTo),
    limit: PAGE_SIZE,
    offset,
  })

  const filteredRows = useMemo(() => {
    return rows.filter((row) => {
      const matchesSearch = row.eventId.toLowerCase().includes(normalizedSearch)
      const matchesStatus = businessStatus === 'ALL' ? true : row.businessStatus === businessStatus
      return matchesSearch && matchesStatus
    })
  }, [businessStatus, normalizedSearch, rows])

  const handlePreviousPage = useCallback(() => {
    setOffset((current) => Math.max(current - PAGE_SIZE, 0))
  }, [])

  const handleNextPage = useCallback(() => {
    setOffset((current) => current + PAGE_SIZE)
  }, [])

  const handleSearchChange = useCallback((event: ChangeEvent<HTMLInputElement>) => {
    setSearchInput(event.target.value)
  }, [])

  const handleBusinessStatusChange = useCallback((event: ChangeEvent<HTMLSelectElement>) => {
    setBusinessStatus(event.target.value)
  }, [])

  const handleClearFilters = useCallback(() => {
    setSearchInput('')
    setFrom('')
    setTo('')
    setBusinessStatus('ALL')
    setOffset(0)
  }, [])

  const table = useReactTable({
    data: filteredRows,
    columns,
    getCoreRowModel: getCoreRowModel(),
  })

  const hasNext = (sessionsQuery.data?.items?.length ?? 0) === PAGE_SIZE
  const error = sessionsQuery.error ? normalizeHttpError(sessionsQuery.error) : null

  return (
    <section className="space-y-4">
      <PageHeader
        title="Sessions"
        description="Busqueda por eventId, filtros por fecha y estado de negocio"
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              onClick={handlePreviousPage}
              disabled={offset === 0}
            >
              Anterior
            </Button>
            <Button
              variant="outline"
              onClick={handleNextPage}
              disabled={!hasNext}
            >
              Siguiente
            </Button>
          </div>
        }
      />

      <StickyFiltersPanel>
        <div className="grid gap-3 lg:grid-cols-4">
          <Input
            placeholder="Buscar por eventId..."
            value={searchInput}
            onChange={handleSearchChange}
          />
          <DateRangeFilter from={from} to={to} onFromChange={setFrom} onToChange={setTo} />
          <Select value={businessStatus} onChange={handleBusinessStatusChange}>
            <option value="ALL">Todos los estados</option>
            <option value="SUCCESS">SUCCESS</option>
            <option value="FAILED">FAILED</option>
            <option value="PENDING">PENDING</option>
            <option value="UNKNOWN">UNKNOWN</option>
          </Select>
          <Button variant="secondary" onClick={handleClearFilters}>
            Limpiar filtros
          </Button>
        </div>
      </StickyFiltersPanel>

      {sessionsQuery.isPending ? <TableSkeleton /> : null}
      {error ? <ErrorAlert error={error} /> : null}

      {!sessionsQuery.isPending && !error ? (
        <>
          {filteredRows.length === 0 ? (
            <div className="rounded-2xl border border-border bg-card p-6">
              <EmptyState
                title="No se encontraron sesiones"
                description="Ajusta los filtros o prueba con un rango de fechas mas amplio."
              />
            </div>
          ) : (
            <>
              <div className="space-y-3 md:hidden">
                {filteredRows.map((row) => (
                  <article key={row.eventId} className="rounded-2xl border border-border bg-card p-4">
                    <div className="space-y-1">
                      <p className="text-[11px] uppercase tracking-wide text-muted">eventId</p>
                      <p className="break-all font-mono text-xs text-slate-200">{row.eventId}</p>
                    </div>

                    <div className="mt-4 grid grid-cols-2 gap-3">
                      <div>
                        <p className="text-[11px] uppercase tracking-wide text-muted">Fecha</p>
                        <p className="text-sm text-slate-100">{formatDateTime(row.createdAt)}</p>
                      </div>
                      <div>
                        <p className="text-[11px] uppercase tracking-wide text-muted">Monto</p>
                        <p className="text-sm text-slate-100">{formatCurrency(row.amount, row.currency ?? 'USD')}</p>
                      </div>
                      <div>
                        <p className="text-[11px] uppercase tracking-wide text-muted">Business</p>
                        <StatusChip status={row.businessStatus} />
                      </div>
                      <div>
                        <p className="text-[11px] uppercase tracking-wide text-muted">GA4 / Meta</p>
                        <div className="flex items-center gap-2">
                          <StatusChip status={row.ga4Status} />
                          <StatusChip status={row.metaStatus} />
                        </div>
                      </div>
                    </div>

                    <div className="mt-4">
                      <Button asChild size="sm" variant="outline" className="w-full">
                        <Link to={`/admin/sessions/${row.eventId}`}>Ver trazabilidad</Link>
                      </Button>
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

      {isLoadingDetails ? <p className="text-xs text-muted">Resolviendo business_status para sesiones visibles...</p> : null}
    </section>
  )
}

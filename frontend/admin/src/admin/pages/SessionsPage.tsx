import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { type ColumnDef, flexRender, getCoreRowModel, useReactTable } from '@tanstack/react-table'

import { DateRangeFilter } from '@/admin/components/common/DateRangeFilter'
import { EmptyState } from '@/admin/components/common/EmptyState'
import { ErrorAlert } from '@/admin/components/common/ErrorAlert'
import { StatusChip } from '@/admin/components/common/StatusChip'
import { TableSkeleton } from '@/admin/components/common/Skeletons'
import { PageHeader } from '@/admin/components/layout/PageHeader'
import { Button } from '@/admin/components/ui/button'
import { Input } from '@/admin/components/ui/input'
import { Select } from '@/admin/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/admin/components/ui/table'
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
  { accessorKey: 'utmSource', header: 'utm_source' },
  { accessorKey: 'utmMedium', header: 'utm_medium' },
  { accessorKey: 'utmCampaign', header: 'utm_campaign' },
  { accessorKey: 'gclid', header: 'gclid' },
  { accessorKey: 'fbclid', header: 'fbclid' },
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
  const [search, setSearch] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [businessStatus, setBusinessStatus] = useState('ALL')
  const [offset, setOffset] = useState(0)

  const { sessionsQuery, rows, isLoadingDetails } = useSessionsTableData({
    from: toIso(from),
    to: toIso(to),
    limit: PAGE_SIZE,
    offset,
  })

  const filteredRows = useMemo(() => {
    return rows.filter((row) => {
      const matchesSearch = row.eventId.toLowerCase().includes(search.toLowerCase())
      const matchesStatus = businessStatus === 'ALL' ? true : row.businessStatus === businessStatus
      return matchesSearch && matchesStatus
    })
  }, [businessStatus, rows, search])

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
              onClick={() => {
                setOffset((current) => Math.max(current - PAGE_SIZE, 0))
              }}
              disabled={offset === 0}
            >
              Anterior
            </Button>
            <Button
              variant="outline"
              onClick={() => setOffset((current) => current + PAGE_SIZE)}
              disabled={!hasNext}
            >
              Siguiente
            </Button>
          </div>
        }
      />

      <div className="grid gap-3 rounded-2xl border border-border bg-card p-4 lg:grid-cols-4">
        <Input
          placeholder="Buscar por eventId..."
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
        <DateRangeFilter from={from} to={to} onFromChange={setFrom} onToChange={setTo} />
        <Select value={businessStatus} onChange={(event) => setBusinessStatus(event.target.value)}>
          <option value="ALL">Todos los estados</option>
          <option value="SUCCESS">SUCCESS</option>
          <option value="FAILED">FAILED</option>
          <option value="PENDING">PENDING</option>
          <option value="UNKNOWN">UNKNOWN</option>
        </Select>
        <Button
          variant="secondary"
          onClick={() => {
            setSearch('')
            setFrom('')
            setTo('')
            setBusinessStatus('ALL')
            setOffset(0)
          }}
        >
          Limpiar filtros
        </Button>
      </div>

      {sessionsQuery.isPending ? <TableSkeleton /> : null}
      {error ? <ErrorAlert error={error} /> : null}

      {!sessionsQuery.isPending && !error ? (
        <div className="overflow-hidden rounded-2xl border border-border bg-card">
          {filteredRows.length === 0 ? (
            <div className="p-6">
              <EmptyState
                title="No se encontraron sesiones"
                description="Ajusta los filtros o prueba con un rango de fechas mas amplio."
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

      {isLoadingDetails ? <p className="text-xs text-muted">Resolviendo business_status para sesiones visibles...</p> : null}
    </section>
  )
}

import { useState } from 'react'
import { ChevronDown, ChevronUp, SlidersHorizontal } from 'lucide-react'

import { Button } from '@/admin/components/ui/button'
import { cn } from '@/lib/utils'

interface StickyFiltersPanelProps {
  children: React.ReactNode
  className?: string
  defaultExpanded?: boolean
}

export function StickyFiltersPanel({
  children,
  className,
  defaultExpanded = true,
}: StickyFiltersPanelProps) {
  const [isExpanded, setIsExpanded] = useState(defaultExpanded)

  return (
    <section
      className={cn(
        'sticky top-3 z-20 rounded-2xl border border-border/90 bg-[linear-gradient(180deg,rgba(17,31,74,0.92)_0%,rgba(10,20,56,0.92)_100%)] shadow-[0_12px_28px_-20px_rgba(45,140,255,0.6)] backdrop-blur lg:top-4',
        className,
      )}
    >
      <div className="flex items-center justify-between gap-3 border-b border-border px-3 py-2">
        <div className="flex items-center gap-2 text-sm font-medium text-slate-200">
          <SlidersHorizontal className="h-4 w-4 text-accent" />
          <span>Filtros</span>
        </div>
        <Button
          type="button"
          size="sm"
          variant="ghost"
          className="h-8 px-2 text-muted"
          onClick={() => setIsExpanded((prev) => !prev)}
          aria-expanded={isExpanded}
        >
          {isExpanded ? (
            <>
              Ocultar
              <ChevronUp className="ml-1 h-4 w-4" />
            </>
          ) : (
            <>
              Mostrar
              <ChevronDown className="ml-1 h-4 w-4" />
            </>
          )}
        </Button>
      </div>

      <div
        className={cn(
          'grid transition-all duration-200',
          isExpanded ? 'grid-rows-[1fr] overflow-visible opacity-100' : 'grid-rows-[0fr] overflow-hidden opacity-0',
        )}
      >
        <div className="min-h-0">
          <div className="p-4">{children}</div>
        </div>
      </div>
    </section>
  )
}

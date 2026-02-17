import * as React from 'react'
import { cva, type VariantProps } from 'class-variance-authority'

import { cn } from '@/lib/utils'

const badgeVariants = cva(
  'inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold uppercase tracking-wide',
  {
    variants: {
      variant: {
        default: 'border-border bg-slate-900 text-slate-100',
        success: 'border-emerald-600/40 bg-emerald-500/20 text-emerald-300',
        danger: 'border-red-500/40 bg-red-500/20 text-red-300',
        warning: 'border-amber-500/40 bg-amber-500/20 text-amber-300',
        info: 'border-blue-500/40 bg-blue-500/20 text-blue-300',
        muted: 'border-slate-700 bg-slate-800 text-slate-300',
      },
    },
    defaultVariants: {
      variant: 'default',
    },
  },
)

export interface BadgeProps extends React.HTMLAttributes<HTMLDivElement>, VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, ...props }: BadgeProps) {
  return <div className={cn(badgeVariants({ variant }), className)} {...props} />
}

export { Badge, badgeVariants }

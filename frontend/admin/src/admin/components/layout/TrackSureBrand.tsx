import { cn } from '@/lib/utils'

export function TrackSureBrand({ className }: { className?: string }) {
  return (
    <div
      className={cn(
        'flex items-center gap-2 rounded-xl border border-[#2D8CFF]/30 bg-[#070F2B]/90 px-3 py-2 shadow-[0_0_14px_rgba(45,140,255,0.22)]',
        className,
      )}
    >
      <span className="h-2.5 w-2.5 rounded-full bg-[linear-gradient(90deg,#FF1FB3_0%,#2D8CFF_100%)] shadow-[0_0_8px_rgba(255,31,179,0.55)]" />
      <div className="leading-none">
        <p className="bg-[linear-gradient(90deg,#FF1FB3_0%,#2D8CFF_100%)] bg-clip-text text-sm font-extrabold tracking-[0.02em] text-transparent">
          TrackSure
        </p>
        <p className="mt-1 text-[10px] font-medium tracking-wide text-[#7CB6FF]">Observability</p>
      </div>
    </div>
  )
}

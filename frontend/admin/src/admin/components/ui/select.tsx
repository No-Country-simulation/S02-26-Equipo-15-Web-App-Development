import {
  Children,
  isValidElement,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ChangeEvent,
  type OptionHTMLAttributes,
  type ReactElement,
  type ReactNode,
  type SelectHTMLAttributes,
} from 'react'
import { ChevronDown } from 'lucide-react'

import { cn } from '@/lib/utils'

interface SelectProps extends Omit<SelectHTMLAttributes<HTMLSelectElement>, 'children'> {
  children: ReactNode
  onValueChange?: (value: string) => void
}

interface ParsedOption {
  value: string
  label: string
  disabled: boolean
}

function parseOptions(children: ReactNode): ParsedOption[] {
  return Children.toArray(children).flatMap((child) => {
    if (!isValidElement(child) || child.type !== 'option') {
      return []
    }

    const option = child as ReactElement<OptionHTMLAttributes<HTMLOptionElement>>
    return [
      {
        value: String(option.props.value ?? ''),
        label: String(option.props.children ?? option.props.value ?? ''),
        disabled: Boolean(option.props.disabled),
      },
    ]
  })
}

export function Select({
  className,
  value,
  disabled,
  onChange,
  onValueChange,
  name,
  id,
  children,
}: SelectProps) {
  const rootRef = useRef<HTMLDivElement | null>(null)
  const [isOpen, setIsOpen] = useState(false)
  const options = useMemo(() => parseOptions(children), [children])
  const selectedValue = String(value ?? '')
  const selected = options.find((option) => option.value === selectedValue) ?? options[0]

  useEffect(() => {
    if (!isOpen) {
      return
    }

    const onDocumentClick = (event: MouseEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) {
        setIsOpen(false)
      }
    }

    const onEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsOpen(false)
      }
    }

    document.addEventListener('mousedown', onDocumentClick)
    document.addEventListener('keydown', onEscape)
    return () => {
      document.removeEventListener('mousedown', onDocumentClick)
      document.removeEventListener('keydown', onEscape)
    }
  }, [isOpen])

  const handleSelect = (nextValue: string) => {
    if (disabled) {
      return
    }
    onValueChange?.(nextValue)
    if (onChange) {
      const syntheticEvent = {
        target: {
          value: nextValue,
          name,
          id,
        },
      } as unknown as ChangeEvent<HTMLSelectElement>
      onChange(syntheticEvent)
    }
    setIsOpen(false)
  }

  return (
    <div ref={rootRef} className="relative">
      <button
        type="button"
        id={id}
        name={name}
        disabled={disabled}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        onClick={() => setIsOpen((open) => !open)}
        className={cn(
          'flex h-10 w-full items-center justify-between rounded-xl border border-border bg-slate-950/40 px-3 py-2 text-left text-sm text-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent disabled:cursor-not-allowed disabled:opacity-50',
          className,
        )}
      >
        <span className="truncate">{selected?.label ?? ''}</span>
        <ChevronDown className={cn('h-4 w-4 shrink-0 text-muted transition-transform', isOpen ? 'rotate-180' : '')} />
      </button>

      {isOpen ? (
        <div
          role="listbox"
          className="absolute left-0 right-0 z-50 mt-2 max-h-60 overflow-y-auto rounded-xl border border-border bg-[#111F4A] p-1 shadow-[0_12px_24px_-14px_rgba(45,140,255,0.65)]"
        >
          {options.map((option) => (
            <button
              key={option.value}
              type="button"
              role="option"
              aria-selected={selectedValue === option.value}
              disabled={option.disabled}
              onClick={() => handleSelect(option.value)}
              className={cn(
                'w-full rounded-lg px-3 py-2 text-left text-sm transition-colors',
                selectedValue === option.value
                  ? 'bg-[linear-gradient(90deg,rgba(255,31,179,0.2)_0%,rgba(45,140,255,0.2)_100%)] text-white'
                  : 'text-slate-200 hover:bg-slate-800/70',
                option.disabled ? 'cursor-not-allowed opacity-50' : '',
              )}
            >
              {option.label}
            </button>
          ))}
        </div>
      ) : null}
    </div>
  )
}

import { cn } from '../lib/utils';
import type { StatusStyle } from '../features/_shared/statusStyles';

interface StatusBadgeProps {
  style: StatusStyle;
  size?: 'sm' | 'md';
  className?: string;
}

export function StatusBadge({ style, size = 'md', className }: StatusBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full font-medium whitespace-nowrap',
        size === 'sm' ? 'px-2 py-0.5 text-[11px]' : 'px-2.5 py-1 text-xs',
        className,
      )}
      style={{ color: style.fg, backgroundColor: style.bg }}
    >
      {style.dot && (
        <span
          className="h-1.5 w-1.5 rounded-full"
          style={{ backgroundColor: style.dot }}
          aria-hidden
        />
      )}
      {style.label}
    </span>
  );
}

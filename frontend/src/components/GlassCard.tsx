import type { HTMLAttributes } from 'react';
import { cn } from '../lib/utils';

type Variant = 'default' | 'strong' | 'subtle';

interface GlassCardProps extends HTMLAttributes<HTMLDivElement> {
  variant?: Variant;
  hover?: boolean;
}

const variantCls: Record<Variant, string> = {
  default: 'glass',
  strong: 'glass-strong',
  subtle: 'glass-subtle',
};

export function GlassCard({
  className,
  variant = 'default',
  hover = false,
  children,
  ...rest
}: GlassCardProps) {
  return (
    <div
      className={cn(
        variantCls[variant],
        hover && 'glass-hover',
        'rounded-xl',
        className,
      )}
      {...rest}
    >
      {children}
    </div>
  );
}

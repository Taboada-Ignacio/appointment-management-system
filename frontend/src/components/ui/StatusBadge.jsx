import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { normalizeStatus } from '@/utils/status';

const variants = {
  ACTIVO: {
    dot: 'bg-success',
    style: 'border-success/20 bg-success/10 text-success',
    label: 'Activo',
  },
  INACTIVO: {
    dot: 'bg-muted-foreground',
    style: 'border-border bg-muted text-muted-foreground',
    label: 'Inactivo',
  },
  EN_TRANSCURSO: {
    dot: 'bg-info',
    style: 'border-info/20 bg-info/10 text-info',
    label: 'En transcurso',
  },
  FINALIZADO: {
    dot: 'bg-muted-foreground',
    style: 'border-border bg-background text-muted-foreground',
    label: 'Finalizado',
  },
};

export function StatusBadge({ status, className = '' }) {
  const config = variants[normalizeStatus(status)] || variants.INACTIVO;

  return (
    <Badge
      variant="outline"
      className={cn('h-6 gap-1.5 px-2.5 font-semibold', config.style, className)}
      aria-label={`Estado: ${config.label}`}
    >
      <span className={cn('size-1.5 rounded-full', config.dot)} aria-hidden="true" />
      {config.label}
    </Badge>
  );
}

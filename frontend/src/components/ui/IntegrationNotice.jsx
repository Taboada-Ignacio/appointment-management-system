import { AlertTriangle, Info } from 'lucide-react';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { cn } from '@/lib/utils';

export function IntegrationNotice({ title, children, type = 'info', className = '' }) {
  const warning = type === 'warning';
  const Icon = warning ? AlertTriangle : Info;

  return (
    <Alert
      className={cn(
        'gap-x-3 px-4 py-3.5 shadow-none',
        warning
          ? 'border-warning/25 bg-warning/8 text-warning'
          : 'border-info/20 bg-info/7 text-info',
        className
      )}
    >
      <Icon className="mt-0.5 size-4" aria-hidden="true" />
      <AlertTitle className="font-semibold text-foreground">{title}</AlertTitle>
      <AlertDescription className="mt-0.5 leading-5 text-muted-foreground">
        {children}
      </AlertDescription>
    </Alert>
  );
}

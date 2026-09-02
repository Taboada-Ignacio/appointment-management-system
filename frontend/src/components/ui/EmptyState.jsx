import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { cn } from '@/lib/utils';

export function EmptyState({ icon: Icon, title, description, action = null, className = '' }) {
  return (
    <Card className={cn('border-dashed bg-card/80 shadow-none', className)}>
      <CardContent className="flex min-h-64 flex-col items-center justify-center px-6 py-10 text-center">
        <div className="mb-5 grid size-12 place-items-center rounded-xl border border-accent bg-accent/65 text-accent-foreground">
          {Icon && <Icon className="size-5" aria-hidden="true" />}
        </div>
        <h3 className="font-heading text-lg font-semibold tracking-tight">{title}</h3>
        <p className="mt-2 max-w-md text-sm leading-6 text-muted-foreground">{description}</p>
        {action && (
          <Button type="button" onClick={action.onClick} className="mt-6">
            {action.label}
          </Button>
        )}
      </CardContent>
    </Card>
  );
}

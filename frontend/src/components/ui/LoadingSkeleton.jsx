import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { cn } from '@/lib/utils';

export { Skeleton };

export function SkeletonCard({ className = '' }) {
  return (
    <Card className={cn('shadow-none', className)} aria-busy="true">
      <CardContent className="flex flex-col gap-4 p-5">
        <Skeleton className="h-5 w-1/3" />
        <Skeleton className="h-4 w-1/2" />
        <Skeleton className="mt-4 h-10 w-full" />
      </CardContent>
    </Card>
  );
}

export function SkeletonTimeline({ className = '' }) {
  return (
    <div className={cn('flex flex-col gap-4', className)} aria-busy="true">
      {Array.from({ length: 5 }).map((_, index) => (
        <div key={index} className="flex gap-4">
          <Skeleton className="h-5 w-14 shrink-0" />
          <Skeleton className="h-12 flex-1 rounded-lg" />
        </div>
      ))}
    </div>
  );
}

export function SkeletonCalendar({ className = '' }) {
  return (
    <div className={cn('grid grid-cols-7 gap-1.5', className)} aria-busy="true">
      {Array.from({ length: 7 }).map((_, index) => (
        <Skeleton key={`header-${index}`} className="h-7" />
      ))}
      {Array.from({ length: 35 }).map((_, index) => (
        <Skeleton key={`cell-${index}`} className="h-20 rounded-lg sm:h-24" />
      ))}
    </div>
  );
}

export function SkeletonText({ lines = 3, className = '' }) {
  return (
    <div className={cn('flex flex-col gap-2', className)} aria-busy="true">
      {Array.from({ length: lines }).map((_, index) => (
        <Skeleton key={index} className={cn('h-4', index === lines - 1 ? 'w-2/3' : 'w-full')} />
      ))}
    </div>
  );
}

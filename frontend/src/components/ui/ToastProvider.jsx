import { createContext, useCallback, useContext, useState } from 'react';
import { CheckCircle2, Info, X, XCircle } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Toaster } from '@/components/ui/sonner';
import { cn } from '@/lib/utils';

const ToastContext = createContext(null);
const APP_TOAST_ID = 'turnos-app-feedback';

const toastIcons = {
  success: CheckCircle2,
  error: XCircle,
  info: Info,
};

function ToastContent({ id, type, title, message }) {
  const [visible, setVisible] = useState(true);
  const Icon = toastIcons[type] || Info;

  if (!visible) return null;

  return (
    <div
      role={type === 'error' ? 'alert' : 'status'}
      className="flex w-[min(24rem,calc(100vw-2rem))] items-start gap-3 rounded-xl border bg-popover p-4 text-popover-foreground shadow-lg"
    >
      <div
        className={cn(
          'mt-0.5 grid size-8 shrink-0 place-items-center rounded-lg',
          type === 'success' && 'bg-success/10 text-success',
          type === 'error' && 'bg-destructive/10 text-destructive',
          type === 'info' && 'bg-info/10 text-info'
        )}
      >
        <Icon className="size-4" aria-hidden="true" />
      </div>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-semibold">{title}</p>
        {message && <p className="mt-1 text-sm leading-5 text-muted-foreground">{message}</p>}
      </div>
      <Button
        type="button"
        variant="ghost"
        size="icon-xs"
        className="-mr-1 -mt-1 text-muted-foreground"
        onPointerDown={(event) => event.stopPropagation()}
        onClick={() => {
          setVisible(false);
          toast.dismiss(id);
        }}
        aria-label="Cerrar notificación"
      >
        <X />
      </Button>
    </div>
  );
}

export function ToastProvider({ children }) {
  const addToast = useCallback((type, title, message) => {
    toast.custom(
      (id) => <ToastContent id={id} type={type} title={title} message={message} />,
      { id: APP_TOAST_ID, duration: 5000 }
    );
  }, []);

  const success = useCallback((title, message) => addToast('success', title, message), [addToast]);
  const error = useCallback((title, message) => addToast('error', title, message), [addToast]);
  const info = useCallback((title, message) => addToast('info', title, message), [addToast]);
  const dismiss = useCallback((id) => toast.dismiss(id), []);

  return (
    <ToastContext.Provider value={{ success, error, info, dismiss }}>
      {children}
      <Toaster position="bottom-right" visibleToasts={3} gap={8} />
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) throw new Error('useToast must be used within a ToastProvider');
  return context;
}

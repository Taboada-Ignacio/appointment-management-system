import { Outlet } from 'react-router-dom';
import { Badge } from '@/components/ui/badge';

export function RootLayout() {
  return (
    <div className="flex min-h-screen flex-col">
      <header className="border-b bg-card px-6 py-4 shadow-xs">
        <div className="mx-auto flex max-w-7xl items-center justify-between">
          <h1 className="font-heading text-lg font-semibold tracking-tight">
            Sistema de Gestión de Turnos
          </h1>
          <Badge variant="secondary">
            API Turnos Frontend
          </Badge>
        </div>
      </header>

      <main className="mx-auto w-full max-w-7xl flex-1 p-6">
        <Outlet />
      </main>

      <footer className="border-t bg-card px-6 py-3 text-center text-xs text-muted-foreground">
        &copy; {new Date().getFullYear()} Sistema de Gestión de Turnos. Esqueleto Base.
      </footer>
    </div>
  );
}


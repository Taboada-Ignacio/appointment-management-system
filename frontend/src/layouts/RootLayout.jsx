import { Outlet } from 'react-router-dom';

export function RootLayout() {
  return (
    <div className="flex min-h-screen flex-col">
      <header className="border-b border-neutral-200 bg-white px-6 py-4 shadow-xs">
        <div className="mx-auto flex max-w-7xl items-center justify-between">
          <h1 className="text-lg font-semibold text-neutral-900">
            Sistema de Gestión de Turnos
          </h1>
          <span className="text-xs font-medium rounded-full bg-blue-50 text-blue-700 px-2.5 py-1 border border-blue-200">
            API Turnos Frontend
          </span>
        </div>
      </header>

      <main className="mx-auto w-full max-w-7xl flex-1 p-6">
        <Outlet />
      </main>

      <footer className="border-t border-neutral-200 bg-white px-6 py-3 text-center text-xs text-neutral-500">
        &copy; {new Date().getFullYear()} Sistema de Gestión de Turnos. Esqueleto Base.
      </footer>
    </div>
  );
}


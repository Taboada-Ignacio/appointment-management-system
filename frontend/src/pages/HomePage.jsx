import { Calendar, Clock, Users, Bell } from 'lucide-react';

export function HomePage() {
  const modules = [
    { name: 'Turnos', icon: Calendar, description: 'Gestión y reserva de turnos' },
    { name: 'Agenda', icon: Clock, description: 'Administración de agendas y calendarios' },
    { name: 'Disponibilidad', icon: Clock, description: 'Cálculo de franjas horarias libres' },
    { name: 'Clientes', icon: Users, description: 'Perfiles e información de clientes' },
  ];

  return (
    <div className="space-y-6">
      <div className="rounded-xl border border-neutral-200 bg-white p-6 shadow-xs">
        <h2 className="text-xl font-bold text-neutral-900">
          Bienvenido al Frontend de Gestión de Turnos
        </h2>
        <p className="mt-1 text-sm text-neutral-600">
          Estructura base inicial lista para el desarrollo de módulos.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {modules.map((mod) => {
          const Icon = mod.icon;
          return (
            <div
              key={mod.name}
              className="rounded-lg border border-neutral-200 bg-white p-5 shadow-xs transition-shadow hover:shadow-sm"
            >
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-50 text-blue-600">
                  <Icon className="h-5 w-5" />
                </div>
                <div>
                  <h3 className="font-semibold text-neutral-900">{mod.name}</h3>
                  <p className="text-xs text-neutral-500">{mod.description}</p>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}


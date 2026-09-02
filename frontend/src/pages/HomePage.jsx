import { Calendar, Clock, Users } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

export function HomePage() {
  const modules = [
    { name: 'Turnos', icon: Calendar, description: 'Gestión y reserva de turnos' },
    { name: 'Agenda', icon: Clock, description: 'Administración de agendas y calendarios' },
    { name: 'Disponibilidad', icon: Clock, description: 'Cálculo de franjas horarias libres' },
    { name: 'Clientes', icon: Users, description: 'Perfiles e información de clientes' },
  ];

  return (
    <div className="space-y-6">
      <Card className="shadow-none">
        <CardHeader>
        <CardTitle>
          Bienvenido al Frontend de Gestión de Turnos
        </CardTitle>
        <CardDescription>
          Estructura base inicial lista para el desarrollo de módulos.
        </CardDescription>
        </CardHeader>
      </Card>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {modules.map((mod) => {
          const Icon = mod.icon;
          return (
            <Card
              key={mod.name}
              className="shadow-none transition hover:border-ring/40 hover:shadow-sm"
            >
              <CardContent className="flex items-center gap-3 p-5">
                <div className="flex size-10 items-center justify-center rounded-lg bg-accent text-accent-foreground">
                  <Icon className="h-5 w-5" />
                </div>
                <div>
                  <h3 className="font-heading font-semibold">{mod.name}</h3>
                  <p className="text-xs text-muted-foreground">{mod.description}</p>
                </div>
              </CardContent>
            </Card>
          );
        })}
      </div>
    </div>
  );
}


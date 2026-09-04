import { Link, useLocation } from 'react-router-dom';
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from '@/components/ui/breadcrumb';

const routeLabels = {
  '/profesional/mi-dia': 'Mi día',
  '/profesional/mi-semana': 'Cambiar mi semana',
  '/profesional/mi-mes': 'Mi mes',
  '/profesional/mi-anio': 'Mi año',
  '/profesional/ausencias': 'Manejo de ausencias',
  '/profesional/ausencias/registrar': 'Registrar excepción',
  '/profesional/ausencias/excepciones': 'Consultar excepciones',
  '/profesional/turnos-afectados': 'Turnos afectados',
  '/profesional/configuracion': 'Configuración',
};

export function PageHeader({ eyebrow, title, description, status = null, actions = null }) {
  const { pathname } = useLocation();
  const currentLabel = routeLabels[pathname] || title;
  const isAbsenceChild = pathname.startsWith('/profesional/ausencias/');

  return (
    <header className="flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">
      <div className="min-w-0">
        <Breadcrumb className="mb-4">
          <BreadcrumbList>
            <BreadcrumbItem>
              <BreadcrumbLink asChild>
                <Link to="/profesional/mi-dia">Agenda</Link>
              </BreadcrumbLink>
            </BreadcrumbItem>
            <BreadcrumbSeparator />
            {isAbsenceChild && <>
              <BreadcrumbItem><BreadcrumbLink asChild><Link to="/profesional/ausencias/registrar">Ausencias y excepciones</Link></BreadcrumbLink></BreadcrumbItem>
              <BreadcrumbSeparator />
            </>}
            <BreadcrumbItem>
              <BreadcrumbPage role="presentation">{currentLabel}</BreadcrumbPage>
            </BreadcrumbItem>
          </BreadcrumbList>
        </Breadcrumb>

        <div className="flex gap-4">
          <div className="relative hidden w-1 shrink-0 overflow-hidden rounded-full bg-border sm:block" aria-hidden="true">
            <span className="absolute inset-x-0 top-0 h-8 rounded-full bg-ring" />
          </div>
          <div className="min-w-0">
            {eyebrow && <p className="text-[11px] font-bold uppercase tracking-[0.15em] text-info">{eyebrow}</p>}
            <div className="mt-1.5 flex flex-wrap items-center gap-3">
              <h1 className="font-heading text-3xl font-semibold tracking-[-0.045em] text-foreground sm:text-[2.35rem] sm:leading-none">{title}</h1>
              {status}
            </div>
            {description && <p className="mt-3 max-w-3xl text-sm leading-6 text-muted-foreground">{description}</p>}
          </div>
        </div>
      </div>
      {actions && <div className="flex shrink-0 flex-wrap items-center gap-2">{actions}</div>}
    </header>
  );
}

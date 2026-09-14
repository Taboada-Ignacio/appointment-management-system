import {
  CalendarDays,
  CalendarRange,
  CalendarX2,
  ChevronRight,
  Clock3,
  PanelLeftClose,
  PanelLeftOpen,
  Settings2,
  SlidersHorizontal,
  UserRound,
  X,
} from 'lucide-react';
import { NavLink, useLocation } from 'react-router-dom';
import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetDescription,
  SheetTitle,
} from '@/components/ui/sheet';
import {
  Sidebar,
  SidebarContent as SidebarScrollContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
} from '@/components/ui/sidebar';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { professionalContext } from '@/config/professional';
import { cn } from '@/lib/utils';

const navigation = [
  { to: '/profesional/turnos/nuevo', label: 'Nuevo turno', icon: UserRound },
  { to: '/profesional/mi-dia', label: 'Mi día', icon: Clock3 },
  { to: '/profesional/mi-mes', label: 'Mi mes', icon: CalendarDays },
  { to: '/profesional/mi-anio', label: 'Mi año', icon: CalendarRange },
  { type: 'absences', label: 'Ausencias y Modificaciones Excepcionales', icon: CalendarX2 },
  { to: '/profesional/turnos-afectados', label: 'Turnos afectados', icon: UserRound },
  { to: '/profesional/configuracion', label: 'Configuración', icon: Settings2 },
  { to: '/profesional/mi-semana', label: 'Cambiar mi semana', icon: SlidersHorizontal },
];

function ProductMark({ isCollapsed = false }) {
  if (isCollapsed) {
    return (
      <Tooltip>
        <TooltipTrigger asChild>
          <div className="flex justify-center" aria-label="Turnos profesional">
            <div
              className="relative grid size-10 shrink-0 grid-cols-3 gap-0.5 rounded-xl border border-sidebar-border bg-sidebar-accent p-2 cursor-default"
              aria-hidden="true"
            >
              {[0, 1, 2, 3, 4, 5].map((item) => (
                <span
                  key={item}
                  className={cn('rounded-[2px]', item === 1 || item === 4 ? 'bg-sidebar-primary' : 'bg-sidebar-foreground/55')}
                />
              ))}
            </div>
            <span className="sr-only">Turnos · Agenda profesional</span>
          </div>
        </TooltipTrigger>
        <TooltipContent side="right">Turnos · Agenda profesional</TooltipContent>
      </Tooltip>
    );
  }

  return (
    <div className="flex items-center gap-3" aria-label="Turnos profesional">
      <div
        className="relative grid size-10 shrink-0 grid-cols-3 gap-0.5 rounded-xl border border-sidebar-border bg-sidebar-accent p-2"
        aria-hidden="true"
      >
        {[0, 1, 2, 3, 4, 5].map((item) => (
          <span
            key={item}
            className={cn('rounded-[2px]', item === 1 || item === 4 ? 'bg-sidebar-primary' : 'bg-sidebar-foreground/55')}
          />
        ))}
      </div>
      <div>
        <p className="font-heading text-base font-semibold tracking-[-0.03em] text-sidebar-foreground">Turnos</p>
        <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-sidebar-primary">Agenda profesional</p>
      </div>
    </div>
  );
}

function ProfileSummary({ isCollapsed = false }) {
  const initials = professionalContext.name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('') || 'PR';

  if (isCollapsed) {
    return (
      <Tooltip>
        <TooltipTrigger asChild>
          <div className="flex justify-center">
            <div className="grid size-9 shrink-0 place-items-center rounded-lg bg-sidebar-primary font-heading text-xs font-bold text-sidebar-primary-foreground cursor-default">
              {initials}
            </div>
            <span className="sr-only">{professionalContext.name}</span>
          </div>
        </TooltipTrigger>
        <TooltipContent side="right">
          <p className="font-semibold">{professionalContext.name}</p>
          <p className="text-[11px] text-muted-foreground">
            {professionalContext.specialty || `Profesional #${professionalContext.id}`}
          </p>
        </TooltipContent>
      </Tooltip>
    );
  }

  return (
    <div className="rounded-xl border border-sidebar-border bg-sidebar-accent/65 p-3">
      <div className="flex items-center gap-3">
        <div className="grid size-9 shrink-0 place-items-center rounded-lg bg-sidebar-primary font-heading text-xs font-bold text-sidebar-primary-foreground">
          {initials}
        </div>
        <div className="min-w-0">
          <p className="truncate text-sm font-semibold text-sidebar-foreground">{professionalContext.name}</p>
          <p className="truncate text-[11px] text-sidebar-foreground/60">
            {professionalContext.specialty || `Profesional #${professionalContext.id}`}
          </p>
        </div>
      </div>
    </div>
  );
}

function NavigationContent({
  hasAnnualAgenda,
  onNavigate,
  pendingAffectedCount,
  isCollapsed = false,
  onToggleCollapse = null,
}) {
  const { pathname } = useLocation();
  const inAbsences = pathname.startsWith('/profesional/ausencias');
  const [collapsedPath, setCollapsedPath] = useState(null);
  const [manuallyOpen, setManuallyOpen] = useState(false);
  const absencesOpen = inAbsences ? collapsedPath !== pathname : manuallyOpen;
  const pendingCount = pendingAffectedCount;

  return (
    <>
      <SidebarHeader className={cn('gap-4 p-4 pb-2', isCollapsed && 'px-2 items-center')}>
        <ProductMark isCollapsed={isCollapsed} />
        <ProfileSummary isCollapsed={isCollapsed} />
        {!hasAnnualAgenda && (
          isCollapsed ? (
            <Tooltip>
              <TooltipTrigger asChild>
                <Button asChild size="icon" className="size-9 bg-sidebar-primary text-sidebar-primary-foreground hover:bg-sidebar-primary/90">
                  <NavLink to="/profesional/configuracion" onClick={onNavigate} aria-label="Configurar agenda">
                    <ChevronRight className="size-4" />
                  </NavLink>
                </Button>
              </TooltipTrigger>
              <TooltipContent side="right">Configurar agenda</TooltipContent>
            </Tooltip>
          ) : (
            <Button asChild className="w-full justify-between bg-sidebar-primary text-sidebar-primary-foreground hover:bg-sidebar-primary/90">
              <NavLink to="/profesional/configuracion" onClick={onNavigate}>
                Configurar agenda
                <ChevronRight data-icon="inline-end" />
              </NavLink>
            </Button>
          )
        )}
      </SidebarHeader>

      <SidebarScrollContent>
        <SidebarGroup className={cn('py-4', isCollapsed ? 'px-1 items-center' : 'px-3')}>
          {!isCollapsed && (
            <SidebarGroupLabel className="px-2 text-[10px] font-bold uppercase tracking-[0.16em] text-sidebar-foreground/45">
              Agenda
            </SidebarGroupLabel>
          )}
          <SidebarGroupContent>
            <SidebarMenu className={cn('gap-1.5', isCollapsed && 'items-center')}>
              {navigation.map((item) => {
                const { to, label, icon: Icon } = item;
                if (item.type === 'absences') {
                  if (isCollapsed) {
                    return (
                      <SidebarMenuItem key="absences" className="flex justify-center">
                        <DropdownMenu>
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <DropdownMenuTrigger asChild>
                                <SidebarMenuButton
                                  type="button"
                                  isActive={inAbsences}
                                  aria-label={label}
                                  className={cn(
                                    'size-10 justify-center rounded-lg p-0 font-semibold text-sidebar-foreground/72 hover:bg-sidebar-accent hover:text-sidebar-accent-foreground data-active:bg-sidebar-primary data-active:text-sidebar-primary-foreground'
                                  )}
                                >
                                  <Icon
                                    className={cn(
                                      'size-5 shrink-0',
                                      inAbsences ? 'text-sidebar-primary-foreground' : 'text-sidebar-primary'
                                    )}
                                    aria-hidden="true"
                                  />
                                  <span className="sr-only">{label}</span>
                                </SidebarMenuButton>
                              </DropdownMenuTrigger>
                            </TooltipTrigger>
                            <TooltipContent side="right">{label}</TooltipContent>
                          </Tooltip>
                          <DropdownMenuContent side="right" align="start" className="w-64 bg-sidebar text-sidebar-foreground border-sidebar-border p-1.5 shadow-xl">
                            <DropdownMenuLabel className="text-[11px] font-bold uppercase tracking-wider text-sidebar-foreground/50 px-2 py-1">
                              Ausencias y Excepciones
                            </DropdownMenuLabel>
                            <DropdownMenuSeparator className="bg-sidebar-border" />
                            {[
                              ['/profesional/ausencias/registrar', 'Registrar Ausencias'],
                              ['/profesional/ausencias/habilitaciones', 'Registrar Habilitaciones Extraordinarias'],
                              ['/profesional/ausencias/modificaciones', 'Registrar Modificaciones Extraordinarias'],
                              ['/profesional/ausencias/excepciones', 'Consultar excepciones'],
                            ].map(([childTo, childLabel]) => (
                              <DropdownMenuItem key={childTo} asChild className="focus:bg-sidebar-accent focus:text-sidebar-accent-foreground">
                                <NavLink
                                  to={childTo}
                                  onClick={onNavigate}
                                  className={({ isActive }) =>
                                    cn(
                                      'flex w-full items-center rounded-md px-2.5 py-1.5 text-xs font-semibold cursor-pointer',
                                      isActive ? 'bg-sidebar-accent text-sidebar-primary font-bold' : 'text-sidebar-foreground/80'
                                    )
                                  }
                                >
                                  {childLabel}
                                </NavLink>
                              </DropdownMenuItem>
                            ))}
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </SidebarMenuItem>
                    );
                  }

                  return (
                    <SidebarMenuItem key="absences">
                      <SidebarMenuButton
                        type="button"
                        isActive={inAbsences}
                        aria-expanded={absencesOpen}
                        aria-controls="submenu-ausencias"
                        onClick={() => inAbsences
                          ? setCollapsedPath(absencesOpen ? pathname : null)
                          : setManuallyOpen((open) => !open)}
                        className={cn(
                          'min-h-11 h-auto py-2.5 rounded-lg px-3 font-semibold text-sidebar-foreground/72 hover:bg-sidebar-accent hover:text-sidebar-accent-foreground data-active:bg-sidebar-primary data-active:text-sidebar-primary-foreground items-start gap-2.5 overflow-visible'
                        )}
                      >
                        <Icon className={cn('size-4 shrink-0 mt-0.5', inAbsences ? 'text-sidebar-primary-foreground' : 'text-sidebar-primary')} aria-hidden="true" />
                        <span className="flex-1 text-left text-xs sm:text-sm font-semibold leading-snug whitespace-normal break-words overflow-visible">
                          {label}
                        </span>
                        <ChevronRight className={cn('size-4 shrink-0 mt-0.5 transition-transform duration-200', absencesOpen && 'rotate-90')} aria-hidden="true" />
                      </SidebarMenuButton>
                      {absencesOpen && (
                        <div id="submenu-ausencias" className="grid animate-in fade-in-0 slide-in-from-top-1 duration-200">
                          <div>
                            <div className="ml-5 mt-1 space-y-1 border-l border-sidebar-border pl-3">
                              {[
                                ['/profesional/ausencias/registrar', 'Registrar Ausencias'],
                                ['/profesional/ausencias/habilitaciones', 'Registrar Habilitaciones Extraordinarias'],
                                ['/profesional/ausencias/modificaciones', 'Registrar Modificaciones Extraordinarias'],
                                ['/profesional/ausencias/excepciones', 'Consultar excepciones'],
                              ].map(([childTo, childLabel]) => (
                                <NavLink
                                  key={childTo}
                                  to={childTo}
                                  onClick={onNavigate}
                                  className={({ isActive }) =>
                                    cn(
                                      'flex min-h-9 items-center rounded-lg px-3 py-1.5 text-xs font-semibold text-sidebar-foreground/65 transition hover:bg-sidebar-accent hover:text-sidebar-accent-foreground leading-snug',
                                      isActive && 'bg-sidebar-accent text-sidebar-primary'
                                    )
                                  }
                                >
                                  <span className="flex-1 whitespace-normal break-words">{childLabel}</span>
                                </NavLink>
                              ))}
                            </div>
                          </div>
                        </div>
                      )}
                    </SidebarMenuItem>
                  );
                }

                const active = pathname === to;
                const affectedAlert = to === '/profesional/turnos-afectados' && pendingCount > 0;

                if (isCollapsed) {
                  return (
                    <SidebarMenuItem key={to} className="flex justify-center">
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <SidebarMenuButton
                            asChild
                            isActive={active}
                            className={cn(
                              'size-10 justify-center rounded-lg p-0 font-semibold text-sidebar-foreground/72 hover:bg-sidebar-accent hover:text-sidebar-accent-foreground data-active:bg-sidebar-primary data-active:text-sidebar-primary-foreground relative',
                              affectedAlert && !active && 'bg-orange-500/10 text-orange-700'
                            )}
                          >
                            <NavLink
                              to={to}
                              onClick={() => {
                                setManuallyOpen(false);
                                onNavigate?.();
                              }}
                              aria-label={label}
                            >
                              <Icon
                                className={cn(
                                  'size-5 shrink-0',
                                  active
                                    ? 'text-sidebar-primary-foreground'
                                    : affectedAlert
                                    ? 'text-orange-600'
                                    : 'text-sidebar-primary'
                                )}
                                aria-hidden="true"
                              />
                              <span className="sr-only">{label}</span>
                              {affectedAlert && (
                                <span
                                  className="absolute -top-1 -right-1 flex size-4 items-center justify-center rounded-full bg-orange-500 text-[9px] font-black text-white shadow-xs"
                                  aria-label={`${pendingCount} ${pendingCount === 1 ? 'turno pendiente' : 'turnos pendientes'}`}
                                >
                                  {pendingCount}
                                </span>
                              )}
                            </NavLink>
                          </SidebarMenuButton>
                        </TooltipTrigger>
                        <TooltipContent side="right">
                          {label}
                          {affectedAlert && ` (${pendingCount} pendiente${pendingCount === 1 ? '' : 's'})`}
                        </TooltipContent>
                      </Tooltip>
                    </SidebarMenuItem>
                  );
                }

                return (
                  <SidebarMenuItem key={to}>
                    <SidebarMenuButton
                      asChild
                      isActive={active}
                      className={cn(
                        'h-10 rounded-lg px-3 font-semibold text-sidebar-foreground/72 hover:bg-sidebar-accent hover:text-sidebar-accent-foreground data-active:bg-sidebar-primary data-active:text-sidebar-primary-foreground',
                        affectedAlert && !active && 'bg-orange-500/10 text-orange-700'
                      )}
                    >
                      <NavLink
                        to={to}
                        onClick={() => {
                          setManuallyOpen(false);
                          onNavigate?.();
                        }}
                      >
                        <Icon
                          className={cn(
                            active
                              ? 'text-sidebar-primary-foreground'
                              : affectedAlert
                              ? 'text-orange-600'
                              : 'text-sidebar-primary'
                          )}
                          aria-hidden="true"
                        />
                        <span className="flex-1">{label}</span>
                        {affectedAlert && (
                          <span
                            className={cn(
                              'min-w-5 rounded-full bg-orange-500 px-1.5 py-0.5 text-center text-[10px] font-black text-white',
                              active && 'bg-white text-orange-600'
                            )}
                            aria-label={`${pendingCount} ${pendingCount === 1 ? 'turno pendiente' : 'turnos pendientes'}`}
                          >
                            {pendingCount}
                          </span>
                        )}
                      </NavLink>
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                );
              })}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarScrollContent>

      <SidebarFooter className={cn('p-4 pt-2', isCollapsed && 'px-2 items-center')}>
        <Separator className="mb-2 bg-sidebar-border" />
        {isCollapsed ? (
          <Tooltip>
            <TooltipTrigger asChild>
              <NavLink
                to="/profesional/configuracion#perfil"
                onClick={onNavigate}
                aria-label="Perfil y zona horaria"
                className="flex size-10 items-center justify-center rounded-lg text-sidebar-foreground/65 outline-none transition hover:bg-sidebar-accent hover:text-sidebar-accent-foreground focus-visible:ring-2 focus-visible:ring-sidebar-ring"
              >
                <UserRound className="size-5 text-sidebar-primary" aria-hidden="true" />
                <span className="sr-only">Perfil y zona horaria</span>
              </NavLink>
            </TooltipTrigger>
            <TooltipContent side="right">
              <p className="font-semibold">Perfil y zona horaria</p>
              <p className="text-[10px] text-muted-foreground">{professionalContext.timezone}</p>
            </TooltipContent>
          </Tooltip>
        ) : (
          <NavLink
            to="/profesional/configuracion#perfil"
            onClick={onNavigate}
            className="flex items-center gap-3 rounded-lg p-2.5 text-xs font-semibold text-sidebar-foreground/65 outline-none transition hover:bg-sidebar-accent hover:text-sidebar-accent-foreground focus-visible:ring-2 focus-visible:ring-sidebar-ring"
          >
            <UserRound className="size-4 text-sidebar-primary" aria-hidden="true" />
            <span className="min-w-0 flex-1">
              <span className="block">Perfil y zona horaria</span>
              <span className="mt-0.5 block truncate text-[10px] font-normal text-sidebar-foreground/40">{professionalContext.timezone}</span>
            </span>
          </NavLink>
        )}

        {onToggleCollapse && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={onToggleCollapse}
            className={cn(
              'w-full text-sidebar-foreground/60 hover:bg-sidebar-accent hover:text-sidebar-accent-foreground mt-2',
              isCollapsed ? 'h-9 p-0 justify-center' : 'justify-between px-2.5'
            )}
            aria-label={isCollapsed ? 'Expandir menú lateral' : 'Contraer menú lateral'}
            title={isCollapsed ? 'Expandir menú lateral' : 'Contraer menú lateral'}
          >
            {!isCollapsed && <span className="text-xs font-medium">Contraer menú</span>}
            {isCollapsed ? (
              <PanelLeftOpen className="size-4" />
            ) : (
              <PanelLeftClose className="size-4" />
            )}
          </Button>
        )}
      </SidebarFooter>
    </>
  );
}

export function ProfessionalSidebar({
  open = false,
  onClose = null,
  hasAnnualAgenda = true,
  returnFocusRef = null,
  pendingAffectedCount,
  isCollapsed = false,
  onToggleCollapse = null,
  onNavigate = null,
}) {
  return (
    <TooltipProvider delayDuration={150}>
      <SidebarProvider className="contents">
        <Sidebar
          collapsible="none"
          className={cn(
            'fixed inset-y-0 left-0 z-40 hidden border-r border-sidebar-border bg-sidebar text-sidebar-foreground lg:flex transition-[width] duration-200 ease-in-out',
            isCollapsed ? 'w-20' : 'w-72'
          )}
          aria-label="Navegación profesional"
        >
          <NavigationContent
            hasAnnualAgenda={hasAnnualAgenda}
            pendingAffectedCount={pendingAffectedCount}
            isCollapsed={isCollapsed}
            onToggleCollapse={onToggleCollapse}
            onNavigate={onNavigate}
          />
        </Sidebar>
      </SidebarProvider>

      <Sheet open={open} onOpenChange={(nextOpen) => !nextOpen && onClose?.()}>
        <SheetContent
          id="navegacion-profesional-movil"
          side="left"
          showCloseButton={false}
          className="w-[min(19rem,88vw)] gap-0 border-sidebar-border bg-sidebar p-0 text-sidebar-foreground sm:max-w-[19rem] lg:hidden"
          onCloseAutoFocus={(event) => {
            if (!returnFocusRef?.current) return;
            event.preventDefault();
            returnFocusRef.current.focus();
          }}
        >
          <SheetTitle className="sr-only">Navegación profesional</SheetTitle>
          <SheetDescription className="sr-only">
            Accesos a las vistas y la configuración de la agenda profesional.
          </SheetDescription>
          <div className="absolute right-3 top-3 z-10">
            <SheetClose asChild>
              <Button
                type="button"
                variant="ghost"
                size="icon"
                className="text-sidebar-foreground/70 hover:bg-sidebar-accent hover:text-sidebar-foreground"
                aria-label="Cerrar menú"
              >
                <X />
              </Button>
            </SheetClose>
          </div>
          <SidebarProvider className="contents">
            <div className="flex min-h-0 flex-1 flex-col overflow-y-auto">
              <NavigationContent
                hasAnnualAgenda={hasAnnualAgenda}
                onNavigate={onClose}
                pendingAffectedCount={pendingAffectedCount}
                isCollapsed={false}
              />
            </div>
          </SidebarProvider>
        </SheetContent>
      </Sheet>
    </TooltipProvider>
  );
}

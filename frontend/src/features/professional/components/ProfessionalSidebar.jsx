import {
  CalendarDays,
  CalendarRange,
  ChevronRight,
  Clock3,
  Settings2,
  SlidersHorizontal,
  UserRound,
  X,
} from 'lucide-react';
import { NavLink, useLocation } from 'react-router-dom';
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
import { professionalContext } from '@/config/professional';
import { cn } from '@/lib/utils';

const navigation = [
  { to: '/profesional/mi-dia', label: 'Mi día', icon: Clock3 },
  { to: '/profesional/mi-mes', label: 'Mi mes', icon: CalendarDays },
  { to: '/profesional/mi-anio', label: 'Mi año', icon: CalendarRange },
  { to: '/profesional/configuracion', label: 'Configuración', icon: Settings2 },
  { to: '/profesional/mi-semana', label: 'Cambiar mi semana', icon: SlidersHorizontal },
];

function ProductMark() {
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

function ProfileSummary() {
  const initials = professionalContext.name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('') || 'PR';

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

function NavigationContent({ hasAnnualAgenda, onNavigate }) {
  const { pathname } = useLocation();

  return (
    <>
      <SidebarHeader className="gap-5 p-4 pb-2">
        <ProductMark />
        <ProfileSummary />
        {!hasAnnualAgenda && (
          <Button asChild className="w-full justify-between bg-sidebar-primary text-sidebar-primary-foreground hover:bg-sidebar-primary/90">
            <NavLink to="/profesional/configuracion" onClick={onNavigate}>
              Configurar agenda
              <ChevronRight data-icon="inline-end" />
            </NavLink>
          </Button>
        )}
      </SidebarHeader>

      <SidebarScrollContent>
        <SidebarGroup className="px-3 py-4">
          <SidebarGroupLabel className="px-2 text-[10px] font-bold uppercase tracking-[0.16em] text-sidebar-foreground/45">
            Agenda
          </SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu className="gap-1">
              {navigation.map(({ to, label, icon: Icon }) => (
                <SidebarMenuItem key={to}>
                  <SidebarMenuButton
                    asChild
                    isActive={pathname === to}
                    className="h-10 rounded-lg px-3 font-semibold text-sidebar-foreground/72 hover:bg-sidebar-accent hover:text-sidebar-accent-foreground data-active:bg-sidebar-primary data-active:text-sidebar-primary-foreground"
                  >
                    <NavLink to={to} onClick={onNavigate}>
                      <Icon className={cn(pathname === to ? 'text-sidebar-primary-foreground' : 'text-sidebar-primary')} aria-hidden="true" />
                      <span>{label}</span>
                    </NavLink>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarScrollContent>

      <SidebarFooter className="p-4 pt-2">
        <Separator className="mb-2 bg-sidebar-border" />
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
      </SidebarFooter>
    </>
  );
}

export function ProfessionalSidebar({
  open = false,
  onClose = null,
  hasAnnualAgenda = true,
  returnFocusRef = null,
}) {
  return (
    <>
      <SidebarProvider className="contents">
        <Sidebar
          collapsible="none"
          className="fixed inset-y-0 left-0 z-40 hidden w-64 border-r-0 bg-sidebar text-sidebar-foreground lg:flex"
          aria-label="Navegación profesional"
        >
          <NavigationContent hasAnnualAgenda={hasAnnualAgenda} />
        </Sidebar>
      </SidebarProvider>

      <Sheet open={open} onOpenChange={(nextOpen) => !nextOpen && onClose?.()}>
        <SheetContent
          id="navegacion-profesional-movil"
          side="left"
          showCloseButton={false}
          className="w-[min(17rem,88vw)] gap-0 border-sidebar-border bg-sidebar p-0 text-sidebar-foreground sm:max-w-[17rem] lg:hidden"
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
              <NavigationContent hasAnnualAgenda={hasAnnualAgenda} onNavigate={onClose} />
            </div>
          </SidebarProvider>
        </SheetContent>
      </Sheet>
    </>
  );
}

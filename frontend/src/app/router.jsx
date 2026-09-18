import { ProfessionalDirectoryPage } from '../features/professional/pages/ProfessionalDirectoryPage';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import { ProfessionalLayout } from '../features/professional/components/ProfessionalLayout';
import { MyDayPage } from '../features/professional/pages/MyDayPage';
import { MyWeekPage } from '../features/professional/pages/MyWeekPage';
import { MyMonthPage } from '../features/professional/pages/MyMonthPage';
import { MyYearPage } from '../features/professional/pages/MyYearPage';
import { SettingsPage } from '../features/professional/pages/SettingsPage';
import { AbsenceManagementPage } from '../features/professional/pages/AbsenceManagementPage';
import { NewAppointmentPage } from '../features/professional/pages/NewAppointmentPage';
import { RescheduleAppointmentPage } from '../features/professional/pages/RescheduleAppointmentPage';
import { CompleteRescheduleAppointmentPage } from '../features/professional/pages/CompleteRescheduleAppointmentPage';
import { NotFoundPage } from '../features/professional/pages/NotFoundPage';
import { BookingPage } from '../features/selfService/BookingPage';
import { SelfServicePage } from '../features/professional/pages/SelfServicePage';

/**
 * Route configuration factory.
 * Exported so tests can use createMemoryRouter(createRoutes()).
 */
export function createRoutes() {
  return [
    { path: '/reservar', element: <BookingPage /> },
    {
      path: '/',
      element: <Navigate to="/profesional/mi-dia" replace />,
    },
    {
      path: '/profesional',
      element: <ProfessionalLayout />,
      children: [
        { index: true, element: <Navigate to="/profesional/mi-dia" replace /> },
        { path: 'turnos-pendientes', element: <ProfessionalDirectoryPage key="pending" /> },
        { path: 'clientes-pendientes', element: <ProfessionalDirectoryPage key="pending-clients" clients pendingClients /> },
        { path: 'clientes', element: <ProfessionalDirectoryPage key="clients" clients /> },
        { path: 'mi-dia', element: <MyDayPage /> },
        { path: 'turnos/nuevo', element: <NewAppointmentPage /> },
        { path: 'turnos/:appointmentId/cambiar-dia', element: <RescheduleAppointmentPage /> },
        { path: 'turnos/:appointmentId/reprogramar', element: <CompleteRescheduleAppointmentPage /> },
        { path: 'mi-semana', element: <MyWeekPage /> },
        { path: 'mi-mes', element: <MyMonthPage /> },
        { path: 'mi-anio', element: <MyYearPage /> },
        { path: 'ausencias', element: <Navigate to="/profesional/ausencias/registrar" replace /> },
        { path: 'ausencias/registrar', element: <AbsenceManagementPage key="register" section="register" /> },
        { path: 'ausencias/habilitaciones', element: <AbsenceManagementPage key="habilitaciones" section="habilitaciones" /> },
        { path: 'ausencias/modificaciones', element: <AbsenceManagementPage key="modificaciones" section="modificaciones" /> },
        { path: 'ausencias/excepciones', element: <AbsenceManagementPage key="exceptions" section="exceptions" /> },
        { path: 'turnos-afectados', element: <AbsenceManagementPage section="affected" /> },
        { path: 'configuracion', element: <SettingsPage /> },
        { path: 'autogestion', element: <SelfServicePage /> },
      ],
    },
    {
      path: '*',
      element: <NotFoundPage />,
    },
  ];
}

export const router = createBrowserRouter(createRoutes());

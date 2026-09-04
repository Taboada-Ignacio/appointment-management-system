import { createBrowserRouter, Navigate } from 'react-router-dom';
import { ProfessionalLayout } from '../features/professional/components/ProfessionalLayout';
import { MyDayPage } from '../features/professional/pages/MyDayPage';
import { MyWeekPage } from '../features/professional/pages/MyWeekPage';
import { MyMonthPage } from '../features/professional/pages/MyMonthPage';
import { MyYearPage } from '../features/professional/pages/MyYearPage';
import { SettingsPage } from '../features/professional/pages/SettingsPage';
import { AbsenceManagementPage } from '../features/professional/pages/AbsenceManagementPage';
import { NotFoundPage } from '../features/professional/pages/NotFoundPage';

/**
 * Route configuration factory.
 * Exported so tests can use createMemoryRouter(createRoutes()).
 */
export function createRoutes() {
  return [
    {
      path: '/',
      element: <Navigate to="/profesional/mi-dia" replace />,
    },
    {
      path: '/profesional',
      element: <ProfessionalLayout />,
      children: [
        { index: true, element: <Navigate to="/profesional/mi-dia" replace /> },
        { path: 'mi-dia', element: <MyDayPage /> },
        { path: 'mi-semana', element: <MyWeekPage /> },
        { path: 'mi-mes', element: <MyMonthPage /> },
        { path: 'mi-anio', element: <MyYearPage /> },
        { path: 'ausencias', element: <Navigate to="/profesional/ausencias/registrar" replace /> },
        { path: 'ausencias/registrar', element: <AbsenceManagementPage section="register" /> },
        { path: 'ausencias/excepciones', element: <AbsenceManagementPage section="exceptions" /> },
        { path: 'turnos-afectados', element: <AbsenceManagementPage section="affected" /> },
        { path: 'configuracion', element: <SettingsPage /> },
      ],
    },
    {
      path: '*',
      element: <NotFoundPage />,
    },
  ];
}

export const router = createBrowserRouter(createRoutes());

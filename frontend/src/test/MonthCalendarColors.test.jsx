import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { MonthCalendar } from '../features/professional/components/MonthCalendar';
import * as dateUtils from '../utils/dates';

describe('MonthCalendar color states', () => {
  it('applies the correct background colors for inactivo, activo, hoy, and excepcion', () => {
    // Mock today to 2027-02-10
    const todayStr = '2027-02-10';
    vi.spyOn(dateUtils, 'isToday').mockImplementation((dateStr) => dateStr === todayStr);
    vi.spyOn(dateUtils, 'isPast').mockImplementation((dateStr) => dateStr < todayStr);

    const days = [
      // Día inactivo (gris)
      {
        id: 1,
        fecha: '2027-02-05',
        estadoActual: 'INACTIVO',
        cantidadBrechas: 0,
        cantidadTurnosAsignados: 0,
        tiposExcepcion: [],
      },
      // Día activo (celeste)
      {
        id: 2,
        fecha: '2027-02-06',
        estadoActual: 'ACTIVO',
        cantidadBrechas: 4,
        cantidadTurnosAsignados: 2,
        tiposExcepcion: [],
      },
      // Día actual (verde)
      {
        id: 3,
        fecha: '2027-02-10',
        estadoActual: 'ACTIVO',
        cantidadBrechas: 4,
        cantidadTurnosAsignados: 1,
        tiposExcepcion: [],
      },
      // Día con excepción (naranja)
      {
        id: 4,
        fecha: '2027-02-15',
        estadoActual: 'ACTIVO',
        cantidadBrechas: 0,
        cantidadTurnosAsignados: 0,
        tiposExcepcion: ['VACACIONES'],
      },
      // Día con habilitación extraordinaria (verde agua / emerald)
      {
        id: 5,
        fecha: '2027-02-18',
        estadoActual: 'ACTIVO',
        cantidadBrechas: 3,
        cantidadTurnosAsignados: 1,
        tiposExcepcion: ['HABILITACION_EXTRAORDINARIA'],
      },
    ];

    render(
      <MonthCalendar
        year={2027}
        month={2}
        days={days}
      />
    );

    const inactiveButton = screen.getByRole('gridcell', { name: /^5 de Febrero de 2027/i });
    const activeButton = screen.getByRole('gridcell', { name: /^6 de Febrero de 2027/i });
    const todayButton = screen.getByRole('gridcell', { name: /^10 de Febrero de 2027/i });
    const exceptionButton = screen.getByRole('gridcell', { name: /^15 de Febrero de 2027/i });
    const extraordinaryButton = screen.getByRole('gridcell', { name: /^18 de Febrero de 2027/i });

    // 1. Inactivo -> fondo gris (mismo gris que casillas fuera del mes)
    expect(inactiveButton.className).toContain('bg-[#dbe3eb]/85');
    expect(inactiveButton.getAttribute('data-status')).toBe('INACTIVO');

    // 2. Activo -> fondo blanco
    expect(activeButton.className).toContain('bg-white');
    expect(activeButton.getAttribute('data-status')).toBe('ACTIVO');

    // 3. Día actual -> fondo blanco con indicador de Hoy
    expect(todayButton.className).toContain('bg-white');
    expect(todayButton.getAttribute('data-today')).toBe('true');
    expect(todayButton).toHaveTextContent('Hoy');

    // 4. Con excepción de ausencia -> fondo naranja (warning)
    expect(exceptionButton.className).toContain('bg-warning/25');
    expect(exceptionButton.getAttribute('data-has-exception')).toBe('true');
    expect(exceptionButton).toHaveTextContent('Vacaciones');

    // 5. Con habilitación extraordinaria -> fondo verde agua (emerald)
    expect(extraordinaryButton.className).toContain('bg-emerald-500/20');
    expect(extraordinaryButton.getAttribute('data-has-extraordinary')).toBe('true');
    expect(extraordinaryButton).toHaveTextContent('Habilitación extra');
    expect(extraordinaryButton.querySelector('.font-heading').className).toContain('text-emerald-700');

    // 6. Referencias
    expect(screen.getByText('Referencias:')).toBeInTheDocument();
    expect(screen.getByText('Día inactivo')).toBeInTheDocument();
    expect(screen.getByText('Día activo')).toBeInTheDocument();
    expect(screen.getByText('Día actual')).toBeInTheDocument();
    expect(screen.getByText('Ausencia / No laborable')).toBeInTheDocument();
    expect(screen.getByText('Habilitación extraordinaria')).toBeInTheDocument();

    vi.restoreAllMocks();
  });

  it('prioritizes emerald background when a day is INACTIVO but has HABILITACION_EXTRAORDINARIA', () => {
    const days = [
      {
        id: 20,
        fecha: '2027-02-21', // Domingo normalmente inactivo
        estadoActual: 'INACTIVO',
        cantidadBrechas: 2,
        cantidadTurnosAsignados: 0,
        tiposExcepcion: ['HABILITACION_EXTRAORDINARIA'],
      },
    ];

    render(<MonthCalendar year={2027} month={2} days={days} />);

    const sundayExtraordinary = screen.getByRole('gridcell', { name: /^21 de Febrero de 2027/i });
    expect(sundayExtraordinary.className).toContain('bg-emerald-500/20');
    expect(sundayExtraordinary.className).not.toContain('bg-[#dbe3eb]');
    expect(sundayExtraordinary).toHaveTextContent('Habilitación extra');
  });

  it('prioritizes emerald background when HABILITACION_EXTRAORDINARIA coexists with an absence exception', () => {
    const days = [
      {
        id: 22,
        fecha: '2027-02-22',
        estadoActual: 'ACTIVO',
        cantidadBrechas: 4,
        cantidadTurnosAsignados: 1,
        tiposExcepcion: ['VACACIONES', 'HABILITACION_EXTRAORDINARIA'],
      },
    ];

    render(<MonthCalendar year={2027} month={2} days={days} />);

    const mixedExceptionButton = screen.getByRole('gridcell', { name: /^22 de Febrero de 2027/i });
    // Emerald background has priority
    expect(mixedExceptionButton.className).toContain('bg-emerald-500/20');
    // Both badges are visible
    expect(mixedExceptionButton).toHaveTextContent('Vacaciones');
    expect(mixedExceptionButton).toHaveTextContent('Habilitación extra');
  });

  it('prioritizes orange background when today has an exception while keeping the Hoy badge', () => {
    const todayStr = '2027-02-10';
    vi.spyOn(dateUtils, 'isToday').mockImplementation((dateStr) => dateStr === todayStr);
    vi.spyOn(dateUtils, 'isPast').mockImplementation((dateStr) => dateStr < todayStr);

    const days = [
      {
        id: 10,
        fecha: '2027-02-10',
        estadoActual: 'ACTIVO',
        cantidadBrechas: 0,
        cantidadTurnosAsignados: 0,
        tiposExcepcion: ['DIA_NO_LABORABLE'],
      },
    ];

    render(
      <MonthCalendar
        year={2027}
        month={2}
        days={days}
      />
    );

    const todayWithException = screen.getByRole('gridcell', { name: /^10 de Febrero de 2027/i });

    // Background is orange due to exception
    expect(todayWithException.className).toContain('bg-warning/25');
    // But it still shows the "Hoy" badge
    expect(todayWithException).toHaveTextContent('Hoy');
    expect(todayWithException).toHaveTextContent('No laborable');

    vi.restoreAllMocks();
  });

  it('keeps the Hoy badge and renders emerald background when today has HABILITACION_EXTRAORDINARIA', () => {
    const todayStr = '2027-02-10';
    vi.spyOn(dateUtils, 'isToday').mockImplementation((dateStr) => dateStr === todayStr);
    vi.spyOn(dateUtils, 'isPast').mockImplementation((dateStr) => dateStr < todayStr);

    const days = [
      {
        id: 10,
        fecha: '2027-02-10',
        estadoActual: 'ACTIVO',
        cantidadBrechas: 4,
        cantidadTurnosAsignados: 2,
        tiposExcepcion: ['HABILITACION_EXTRAORDINARIA'],
      },
    ];

    render(
      <MonthCalendar
        year={2027}
        month={2}
        days={days}
      />
    );

    const todayWithExtraordinary = screen.getByRole('gridcell', { name: /^10 de Febrero de 2027/i });

    // Background is emerald due to extraordinary enablement
    expect(todayWithExtraordinary.className).toContain('bg-emerald-500/20');
    // Keeps Hoy badge and shows exception label
    expect(todayWithExtraordinary).toHaveTextContent('Hoy');
    expect(todayWithExtraordinary).toHaveTextContent('Habilitación extra');

    vi.restoreAllMocks();
  });

  it('renders column headers ordered from Monday to Sunday (LUN. to DOM.)', () => {
    render(<MonthCalendar year={2027} month={2} days={[]} />);
    const headers = screen.getAllByRole('columnheader').map((h) => h.textContent);
    expect(headers).toEqual(['LUN.', 'MAR.', 'MIÉ.', 'JUE.', 'VIE.', 'SÁB.', 'DOM.']);
  });

  it('renders only Jornada reducida for BLOQUEO_HORARIO without duplicate Bloqueo de Horario badge', () => {
    const days = [
      {
        id: 30,
        fecha: '2027-02-25',
        estadoActual: 'ACTIVO',
        cantidadBrechas: 4,
        cantidadTurnosAsignados: 2,
        tiposExcepcion: ['BLOQUEO_HORARIO'],
      },
    ];

    render(<MonthCalendar year={2027} month={2} days={days} />);

    const reducedDayCell = screen.getByRole('gridcell', { name: /^25 de Febrero de 2027/i });
    expect(reducedDayCell).toHaveTextContent('Jornada reducida');
    expect(reducedDayCell).not.toHaveTextContent('Bloqueo de Horario');
  });

  it('renders Horario Modificado badge with white background and hides bottom Horario excepcional for MODIFICACION_HORARIO', () => {
    const days = [
      {
        id: 31,
        fecha: '2027-02-26',
        estadoActual: 'ACTIVO',
        cantidadBrechas: 2,
        cantidadTurnosAsignados: 0,
        tiposExcepcion: ['MODIFICACION_HORARIO'],
      },
    ];

    render(<MonthCalendar year={2027} month={2} days={days} />);

    const modifiedDayCell = screen.getByRole('gridcell', { name: /^26 de Febrero de 2027/i });
    // Background is white, not warning
    expect(modifiedDayCell.className).toContain('bg-white');
    expect(modifiedDayCell.className).not.toContain('bg-warning');
    // Number is not warning
    expect(modifiedDayCell.querySelector('.font-heading').className).not.toContain('text-warning');
    // Renders Horario Modificado badge in orange-500
    const badge = screen.getByText('Horario Modificado');
    expect(badge).toBeInTheDocument();
    expect(badge.className).toContain('bg-orange-500');
    // Does not render bottom exception label "Horario excepcional" or "Horario modificado"
    expect(modifiedDayCell).not.toHaveTextContent('Horario excepcional');
  });
});

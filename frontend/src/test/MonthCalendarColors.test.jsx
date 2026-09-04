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

    // 4. Con excepción -> fondo naranja (warning)
    expect(exceptionButton.className).toContain('bg-warning/25');
    expect(exceptionButton.getAttribute('data-has-exception')).toBe('true');
    expect(exceptionButton).toHaveTextContent('Vacaciones');

    // 5. Referencias
    expect(screen.getByText('Referencias:')).toBeInTheDocument();
    expect(screen.getByText('Día inactivo')).toBeInTheDocument();
    expect(screen.getByText('Día activo')).toBeInTheDocument();
    expect(screen.getByText('Día actual')).toBeInTheDocument();
    expect(screen.getByText('Con excepción')).toBeInTheDocument();

    vi.restoreAllMocks();
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

  it('renders column headers ordered from Monday to Sunday (LUN. to DOM.)', () => {
    render(<MonthCalendar year={2027} month={2} days={[]} />);
    const headers = screen.getAllByRole('columnheader').map((h) => h.textContent);
    expect(headers).toEqual(['LUN.', 'MAR.', 'MIÉ.', 'JUE.', 'VIE.', 'SÁB.', 'DOM.']);
  });
});

import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { DailyTimeline } from '../features/professional/components/DailyTimeline';

describe('DailyTimeline Component', () => {
  it('renders time range starting one hour before the first gap and ending one hour after the last gap', () => {
    const dayWithGaps = {
      fecha: '2026-09-15',
      estadoActual: 'ACTIVO',
      brechas: [
        { horaInicio: '09:00', horaFin: '13:00' },
        { horaInicio: '14:00', horaFin: '18:00' },
      ],
    };

    render(
      <DailyTimeline
        day={dayWithGaps}
        timezone="America/Argentina/Buenos_Aires"
      />
    );

    // Starts at 08:00 (one hour before first gap 09:00)
    expect(screen.getAllByText('08:00').length).toBeGreaterThanOrEqual(1);

    // Intermediate hours
    expect(screen.getAllByText('09:00').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('10:00').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('13:00').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('18:00').length).toBeGreaterThanOrEqual(1);

    // Ends at 19:00 (one hour after last gap 18:00)
    expect(screen.getAllByText('19:00').length).toBeGreaterThanOrEqual(1);

    // Availability signal displays dynamic range
    expect(screen.getByText('08:00—19:00')).toBeInTheDocument();

    // Earlier hours before 08:00 are not rendered
    expect(screen.queryByText('06:00')).not.toBeInTheDocument();
    expect(screen.queryByText('07:00')).not.toBeInTheDocument();

    // Later hours after 19:00 are not rendered
    expect(screen.queryByText('20:00')).not.toBeInTheDocument();
    expect(screen.queryByText('21:00')).not.toBeInTheDocument();
    expect(screen.queryByText('22:00')).not.toBeInTheDocument();
  });

  it('renders single gap starting one hour before and ending one hour after', () => {
    const dayWithSingleGap = {
      fecha: '2026-09-15',
      estadoActual: 'ACTIVO',
      brechas: [
        { horaInicio: '10:00', horaFin: '14:00' },
      ],
    };

    render(
      <DailyTimeline
        day={dayWithSingleGap}
        timezone="America/Argentina/Buenos_Aires"
      />
    );

    // 10:00 - 1h = 09:00
    expect(screen.getAllByText('09:00').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('10:00').length).toBeGreaterThanOrEqual(1);
    // 14:00 + 1h = 15:00
    expect(screen.getAllByText('15:00').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('09:00—15:00')).toBeInTheDocument();
    expect(screen.queryByText('08:00')).not.toBeInTheDocument();
    expect(screen.queryByText('16:00')).not.toBeInTheDocument();
  });

  it('shows empty state when day has no brechas', () => {
    const dayWithoutGaps = {
      fecha: '2026-09-15',
      estadoActual: 'ACTIVO',
      brechas: [],
    };

    render(
      <DailyTimeline
        day={dayWithoutGaps}
        timezone="America/Argentina/Buenos_Aires"
      />
    );

    expect(screen.getByText('Sin brechas de atención')).toBeInTheDocument();
  });
});

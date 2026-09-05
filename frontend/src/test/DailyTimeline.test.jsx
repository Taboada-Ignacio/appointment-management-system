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

  it('renders only feasible appointment gaps and reflects schedule blocks when bloqueosHorario is present', () => {
    const dayWithBlocks = {
      fecha: '2026-09-15',
      estadoActual: 'ACTIVO',
      brechas: [
        { horaInicio: '09:00', horaFin: '13:00' },
      ],
      bloqueosHorario: [
        { horaInicio: '10:00', horaFin: '11:00' },
      ],
    };

    render(
      <DailyTimeline
        day={dayWithBlocks}
        timezone="America/Argentina/Buenos_Aires"
      />
    );

    // Feasible available gaps displayed in timeline (difference: 09:00-10:00 and 11:00-13:00)
    expect(screen.getByText('09:00 – 10:00')).toBeInTheDocument();
    expect(screen.getByText('11:00 – 13:00')).toBeInTheDocument();
    // Original uninterrupted range 09:00 – 13:00 should not be rendered as an available gap
    expect(screen.queryByText('09:00 – 13:00')).not.toBeInTheDocument();

    // Blocked interval displayed in timeline
    expect(screen.getByText('10:00 – 11:00')).toBeInTheDocument();
    expect(screen.getByText('· Horario bloqueado')).toBeInTheDocument();

    // Legend entries
    expect(screen.getByText('Disponible para turnos')).toBeInTheDocument();
    expect(screen.getByText('Bloqueo de horario')).toBeInTheDocument();
    expect(screen.getByText('Bloqueado 10:00 – 11:00')).toBeInTheDocument();
  });

  it('renders contextual banner when day has HABILITACION_EXTRAORDINARIA', () => {
    const dayWithExtraordinary = {
      fecha: '2026-09-18',
      estadoActual: 'ACTIVO',
      brechas: [
        { horaInicio: '09:00', horaFin: '12:00' },
      ],
      tiposExcepcion: ['HABILITACION_EXTRAORDINARIA'],
    };

    render(
      <DailyTimeline
        day={dayWithExtraordinary}
        timezone="America/Argentina/Buenos_Aires"
      />
    );

    expect(
      screen.getByText('Día con habilitación extraordinaria de atención')
    ).toBeInTheDocument();
  });

  it('renders extraordinary gaps with emerald styling and availability signal legend', () => {
    const dayWithHabilitacion = {
      fecha: '2026-09-18',
      estadoActual: 'ACTIVO',
      brechas: [],
      habilitacionesExtraordinarias: [
        { horaInicio: '09:00', horaFin: '13:00' },
      ],
      tiposExcepcion: ['HABILITACION_EXTRAORDINARIA'],
    };

    const { container } = render(
      <DailyTimeline
        day={dayWithHabilitacion}
        timezone="America/Argentina/Buenos_Aires"
      />
    );

    // Gaps in timeline
    expect(screen.getByText('09:00 – 13:00')).toBeInTheDocument();
    expect(screen.getByText('· Habilitación extraordinaria disponible')).toBeInTheDocument();

    // Availability Signal legend
    expect(screen.getByText('Habilitación extraordinaria')).toBeInTheDocument();
    expect(screen.getByText('Habilitado 09:00 – 13:00')).toBeInTheDocument();

    // Availability signal segment
    const segment = container.querySelector('[title="Habilitación extraordinaria: 09:00 - 13:00"]');
    expect(segment).toBeInTheDocument();
    expect(segment.className).toContain('bg-emerald-500');
  });

  it('renders only the modification intervals with regular gap styling when day has MODIFICACION_HORARIO', () => {
    const dayWithModification = {
      fecha: '2026-09-19',
      estadoActual: 'ACTIVO',
      brechas: [
        { horaInicio: '08:00', horaFin: '12:00' },
        { horaInicio: '14:00', horaFin: '18:00' },
      ],
      modificacionesHorarias: [
        { horaInicio: '10:00', horaFin: '15:00' },
      ],
      tiposExcepcion: ['MODIFICACION_HORARIO'],
    };

    const { container } = render(
      <DailyTimeline
        day={dayWithModification}
        timezone="America/Argentina/Buenos_Aires"
      />
    );

    // Only 10:00 - 15:00 is rendered, not the base gaps 08:00-12:00 or 14:00-18:00
    expect(screen.getByText('10:00 – 15:00')).toBeInTheDocument();
    expect(screen.queryByText('08:00 – 12:00')).not.toBeInTheDocument();
    expect(screen.queryByText('14:00 – 18:00')).not.toBeInTheDocument();

    // Renders with regular gap styling "Franja de atención disponible"
    expect(screen.getByText('· Franja de atención disponible')).toBeInTheDocument();

    // Availability signal shows standard available segment (bg-ring)
    const segment = container.querySelector('[title="Disponible: 10:00 - 15:00"]');
    expect(segment).toBeInTheDocument();
    expect(segment.className).toContain('bg-ring');
    expect(segment.className).not.toContain('bg-amber');
    expect(segment.className).not.toContain('bg-emerald');

    // No extraordinary or warning banners
    expect(screen.queryByText('Día con habilitación extraordinaria de atención')).not.toBeInTheDocument();
  });
});

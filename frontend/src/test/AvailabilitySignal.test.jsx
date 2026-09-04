import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { AvailabilitySignal } from '../components/ui/AvailabilitySignal';

describe('AvailabilitySignal Component', () => {
  it('renders clinical day signal with aria label', () => {
    const brechas = [{ horaInicio: '09:00', horaFin: '13:00' }];
    render(<AvailabilitySignal brechas={brechas} dayStart="08:00" dayEnd="18:00" />);
    const signal = screen.getByRole('img', { name: /Escala de disponibilidad/i });
    expect(signal).toBeInTheDocument();
  });

  it('renders detailed variant with hour bounds visible', () => {
    const brechas = [{ horaInicio: '09:00', horaFin: '13:00' }];
    render(
      <AvailabilitySignal
        brechas={brechas}
        dayStart="08:00"
        dayEnd="18:00"
        variant="detailed"
      />
    );
    expect(screen.getByText('08:00')).toBeInTheDocument();
    expect(screen.getByText('18:00')).toBeInTheDocument();
  });

  it('renders summary variant with percentage and day counts', () => {
    render(
      <AvailabilitySignal
        totalMinutes={480}
        maxMinutes={2400}
        dayCount={5}
        configuredDayCount={20}
      />
    );
    const summary = screen.getByRole('img', { name: /Disponibilidad: 20%/i });
    expect(summary).toBeInTheDocument();
  });

  it('renders blocked segments in orange and reflects feasible availability difference', () => {
    const brechas = [{ horaInicio: '09:00', horaFin: '13:00' }];
    const bloqueos = [{ horaInicio: '10:00', horaFin: '11:00' }];

    const { container } = render(
      <AvailabilitySignal
        brechas={brechas}
        bloqueos={bloqueos}
        dayStart="08:00"
        dayEnd="14:00"
      />
    );

    // Blocked segment
    const blockedSegment = container.querySelector('[title="Bloqueado: 10:00 - 11:00"]');
    expect(blockedSegment).toBeInTheDocument();
    expect(blockedSegment.className).toContain('bg-orange-500');

    // Feasible available segments
    const firstAvailable = container.querySelector('[title="Disponible: 09:00 - 10:00"]');
    const secondAvailable = container.querySelector('[title="Disponible: 11:00 - 13:00"]');
    expect(firstAvailable).toBeInTheDocument();
    expect(firstAvailable.className).toContain('bg-ring');
    expect(secondAvailable).toBeInTheDocument();
    expect(secondAvailable.className).toContain('bg-ring');
  });
});


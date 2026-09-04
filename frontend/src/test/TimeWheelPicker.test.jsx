import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { TimeWheelPicker } from '../components/ui/TimeWheelPicker';

describe('TimeWheelPicker Component', () => {
  it('renders trigger with placeholder when value is empty', () => {
    render(<TimeWheelPicker value="" placeholder="--:--" aria-label="Hora de inicio" />);
    const trigger = screen.getByRole('button', { name: /Hora de inicio/i });
    expect(trigger).toBeInTheDocument();
    expect(trigger).toHaveTextContent('--:--');
  });

  it('renders trigger with formatted time when value is provided', () => {
    render(<TimeWheelPicker value="14:30" aria-label="Hora de atención" />);
    const trigger = screen.getByRole('button', { name: /Hora de atención/i });
    expect(trigger).toBeInTheDocument();
    expect(trigger).toHaveTextContent('14:30');
  });

  it('opens popover when trigger is clicked', async () => {
    render(<TimeWheelPicker value="09:00" aria-label="Hora" />);
    const trigger = screen.getByRole('button', { name: /Hora/i });
    expect(screen.queryByText('Hora y Minutos')).not.toBeInTheDocument();

    fireEvent.click(trigger);

    expect(screen.getByText('Hora y Minutos')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Listo/i })).toBeInTheDocument();
    expect(screen.getAllByText('09').length).toBeGreaterThan(0);
  });

  it('updates value when synthetic change event is fired on the accessible input', () => {
    const handleChange = vi.fn();
    render(<TimeWheelPicker value="09:00" onChange={handleChange} aria-label="Hora" />);
    fireEvent.change(screen.getByLabelText('Hora'), { target: { value: '14:00' } });
    expect(handleChange).toHaveBeenCalledWith('14:00');
  });

  it('respects disabled state', () => {
    render(<TimeWheelPicker value="10:00" disabled aria-label="Hora deshabilitada" />);
    const trigger = screen.getByRole('button', { name: /Hora deshabilitada/i });
    expect(trigger).toBeDisabled();
  });

  it('renders in inline mode directly without popover button', () => {
    render(<TimeWheelPicker value="08:15" inline aria-label="Hora inline" />);
    expect(screen.getByText('Hora y Minutos')).toBeInTheDocument();
    expect(screen.getByText('08:15')).toBeInTheDocument();
  });

  it('clears value when Limpiar button is clicked', () => {
    const handleChange = vi.fn();
    render(<TimeWheelPicker value="11:45" onChange={handleChange} />);
    const trigger = screen.getByRole('button', { name: /Seleccionar hora/i });
    fireEvent.click(trigger);

    const clearBtn = screen.getByRole('button', { name: /Limpiar/i });
    fireEvent.click(clearBtn);

    expect(handleChange).toHaveBeenCalledWith('');
  });

  it('allows clicking Listo to confirm default time if value was empty', () => {
    const handleChange = vi.fn();
    render(<TimeWheelPicker value="" onChange={handleChange} />);
    const trigger = screen.getByRole('button', { name: /Seleccionar hora/i });
    fireEvent.click(trigger);

    const doneBtn = screen.getByRole('button', { name: /Listo/i });
    fireEvent.click(doneBtn);

    expect(handleChange).toHaveBeenCalledWith('09:00');
  });

  it('handles custom minuteStep and minTime default', () => {
    const handleChange = vi.fn();
    render(<TimeWheelPicker value="" minTime="10:00" minuteStep={30} onChange={handleChange} />);
    const trigger = screen.getByRole('button', { name: /Seleccionar hora/i });
    fireEvent.click(trigger);

    const doneBtn = screen.getByRole('button', { name: /Listo/i });
    fireEvent.click(doneBtn);

    expect(handleChange).toHaveBeenCalledWith('10:00');
  });

  it('updates time to 20:00 when typing 2, 0, 0, 0 sequentially on trigger', () => {
    const handleChange = vi.fn();
    render(<TimeWheelPicker value="" onChange={handleChange} aria-label="Hora" />);
    const trigger = screen.getByRole('button', { name: /Hora/i });

    fireEvent.keyDown(trigger, { key: '2' });
    expect(trigger).toHaveTextContent('2_:--');

    fireEvent.keyDown(trigger, { key: '0' });
    expect(trigger).toHaveTextContent('20:--');

    fireEvent.keyDown(trigger, { key: '0' });
    expect(trigger).toHaveTextContent('20:0_');

    fireEvent.keyDown(trigger, { key: '0' });
    expect(handleChange).toHaveBeenCalledWith('20:00');
  });

  it('updates time when typing digits inside open popover', () => {
    const handleChange = vi.fn();
    render(<TimeWheelPicker value="08:00" onChange={handleChange} aria-label="Hora" />);
    const trigger = screen.getByRole('button', { name: /Hora/i });
    fireEvent.click(trigger);

    const dialog = screen.getByText('Hora y Minutos').closest('div');
    fireEvent.keyDown(dialog, { key: '1' });
    fireEvent.keyDown(dialog, { key: '5' });
    fireEvent.keyDown(dialog, { key: '4' });
    fireEvent.keyDown(dialog, { key: '5' });

    expect(handleChange).toHaveBeenCalledWith('15:45');
  });

  it('handles smart single-digit hour shortcut (8, 3, 0 -> 08:30)', () => {
    const handleChange = vi.fn();
    render(<TimeWheelPicker value="" onChange={handleChange} aria-label="Hora" />);
    const trigger = screen.getByRole('button', { name: /Hora/i });

    fireEvent.keyDown(trigger, { key: '8' });
    expect(trigger).toHaveTextContent('08:--');

    fireEvent.keyDown(trigger, { key: '3' });
    expect(trigger).toHaveTextContent('08:3_');

    fireEvent.keyDown(trigger, { key: '0' });
    expect(handleChange).toHaveBeenCalledWith('08:30');
  });

  it('handles colon separator when typing', () => {
    const handleChange = vi.fn();
    render(<TimeWheelPicker value="" onChange={handleChange} aria-label="Hora" />);
    const trigger = screen.getByRole('button', { name: /Hora/i });

    fireEvent.keyDown(trigger, { key: '1' });
    fireEvent.keyDown(trigger, { key: '4' });
    fireEvent.keyDown(trigger, { key: ':' });
    fireEvent.keyDown(trigger, { key: '1' });
    fireEvent.keyDown(trigger, { key: '5' });

    expect(handleChange).toHaveBeenCalledWith('14:15');
  });

  it('handles backspace while typing digits', () => {
    const handleChange = vi.fn();
    render(<TimeWheelPicker value="" onChange={handleChange} aria-label="Hora" />);
    const trigger = screen.getByRole('button', { name: /Hora/i });

    fireEvent.keyDown(trigger, { key: '2' });
    fireEvent.keyDown(trigger, { key: '1' });
    expect(trigger).toHaveTextContent('21:--');

    fireEvent.keyDown(trigger, { key: 'Backspace' });
    expect(trigger).toHaveTextContent('2_:--');

    fireEvent.keyDown(trigger, { key: '0' });
    fireEvent.keyDown(trigger, { key: '0' });
    fireEvent.keyDown(trigger, { key: '0' });

    expect(handleChange).toHaveBeenCalledWith('20:00');
  });

  it('commits partial hour on Enter', () => {
    const handleChange = vi.fn();
    render(<TimeWheelPicker value="" onChange={handleChange} aria-label="Hora" />);
    const trigger = screen.getByRole('button', { name: /Hora/i });

    fireEvent.keyDown(trigger, { key: '2' });
    fireEvent.keyDown(trigger, { key: '0' });
    fireEvent.keyDown(trigger, { key: 'Enter' });

    expect(handleChange).toHaveBeenCalledWith('20:00');
  });

  it('respects minTime and maxTime clamping on keyboard input', () => {
    const handleChange = vi.fn();
    render(<TimeWheelPicker value="" minTime="09:00" maxTime="18:00" onChange={handleChange} aria-label="Hora" />);
    const trigger = screen.getByRole('button', { name: /Hora/i });

    // Try typing 07:00 (less than minTime 09:00)
    fireEvent.keyDown(trigger, { key: '0' });
    fireEvent.keyDown(trigger, { key: '7' });
    fireEvent.keyDown(trigger, { key: '0' });
    fireEvent.keyDown(trigger, { key: '0' });
    expect(handleChange).toHaveBeenCalledWith('09:00');

    // Try typing 22:00 (greater than maxTime 18:00)
    fireEvent.keyDown(trigger, { key: '2' });
    fireEvent.keyDown(trigger, { key: '2' });
    fireEvent.keyDown(trigger, { key: '0' });
    fireEvent.keyDown(trigger, { key: '0' });
    expect(handleChange).toHaveBeenCalledWith('18:00');
  });

  it('supports keyboard input in inline mode', () => {
    const handleChange = vi.fn();
    render(<TimeWheelPicker value="" inline onChange={handleChange} aria-label="Hora inline" />);
    const container = screen.getByLabelText('Hora inline');

    fireEvent.keyDown(container, { key: '1' });
    fireEvent.keyDown(container, { key: '6' });
    fireEvent.keyDown(container, { key: '3' });
    fireEvent.keyDown(container, { key: '0' });

    expect(handleChange).toHaveBeenCalledWith('16:30');
  });
});


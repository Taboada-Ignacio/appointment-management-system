import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { MonthWheelPicker } from '@/components/ui/MonthWheelPicker';

describe('MonthWheelPicker', () => {
  it('usa meses 1 a 12 y permite seleccionar con click', () => {
    const onChange = vi.fn();
    render(<MonthWheelPicker value={8} onChange={onChange} aria-label="Mes" />);

    expect(screen.queryByRole('listbox', { name: 'Mes' })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Mes' }));
    expect(screen.getAllByRole('option')).toHaveLength(12);
    expect(screen.getByRole('option', { name: 'Agosto' })).toHaveAttribute('aria-selected', 'true');
    fireEvent.click(screen.getByRole('option', { name: 'Octubre' }));
    expect(onChange).toHaveBeenCalledWith(10);
  });

  it('selecciona el mes más cercano al terminar el scroll', async () => {
    const onChange = vi.fn();
    render(<MonthWheelPicker value={1} onChange={onChange} aria-label="Mes" />);
    fireEvent.click(screen.getByRole('button', { name: 'Mes' }));
    const wheel = screen.getByRole('listbox', { name: 'Mes' });

    Object.defineProperty(wheel, 'scrollTop', { configurable: true, writable: true, value: 7 * 40 });
    fireEvent.scroll(wheel);

    await waitFor(() => expect(onChange).toHaveBeenCalledWith(8));
  });

  it('abre centrado en el valor actual y confirma el mes visible al hacer click afuera', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(<><MonthWheelPicker value={8} onChange={onChange} aria-label="Mes" /><button type="button">Afuera</button></>);
    fireEvent.click(screen.getByRole('button', { name: 'Mes' }));
    const wheel = screen.getByRole('listbox', { name: 'Mes' });
    expect(wheel.scrollTop).toBe(7 * 40);

    wheel.scrollTop = 9 * 40;
    fireEvent.scroll(wheel);
    expect(wheel.scrollTop).toBe(9 * 40);
    await user.click(screen.getByRole('button', { name: 'Afuera' }));

    await waitFor(() => expect(onChange).toHaveBeenCalledWith(10));
    await waitFor(() => expect(screen.queryByRole('listbox', { name: 'Mes' })).not.toBeInTheDocument());
  });

  it('mueve un solo mes por cada gesto de rueda', () => {
    render(<MonthWheelPicker value={8} onChange={vi.fn()} aria-label="Mes" />);
    fireEvent.click(screen.getByRole('button', { name: 'Mes' }));
    const wheel = screen.getByRole('listbox', { name: 'Mes' });

    fireEvent.wheel(wheel, { deltaY: 100 });
    fireEvent.wheel(wheel, { deltaY: 80 });
    fireEvent.wheel(wheel, { deltaY: 40 });

    expect(wheel.scrollTop).toBe(8 * 40);
  });

  it('admite teclado y respeta disabled', () => {
    const onChange = vi.fn();
    const { rerender } = render(<MonthWheelPicker value={8} onChange={onChange} aria-label="Mes" />);
    fireEvent.click(screen.getByRole('button', { name: 'Mes' }));
    fireEvent.keyDown(screen.getByRole('listbox', { name: 'Mes' }), { key: 'ArrowDown' });
    expect(onChange).toHaveBeenCalledWith(9);

    rerender(<MonthWheelPicker value={8} onChange={onChange} disabled aria-label="Mes" />);
    expect(screen.getByRole('button', { name: 'Mes' })).toBeDisabled();
    expect(screen.queryByRole('listbox', { name: 'Mes' })).not.toBeInTheDocument();
    expect(onChange).toHaveBeenCalledTimes(1);
  });
});

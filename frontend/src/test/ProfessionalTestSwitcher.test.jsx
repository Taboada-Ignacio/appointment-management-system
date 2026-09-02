import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ProfessionalTestSwitcher } from '@/features/professional/components/ProfessionalTestSwitcher';
import { PROFESSIONAL_STORAGE_KEY } from '@/config/professional';

afterEach(() => {
  window.localStorage.removeItem(PROFESSIONAL_STORAGE_KEY);
  vi.restoreAllMocks();
});

describe('ProfessionalTestSwitcher', () => {
  it('rechaza IDs inválidos sin consultar la API', async () => {
    const user = userEvent.setup();
    const fetchSpy = vi.spyOn(window, 'fetch');

    render(<ProfessionalTestSwitcher onContextChanged={vi.fn()} />);
    await user.click(screen.getByRole('button', { name: /Cambiar profesional de prueba/i }));
    const input = screen.getByLabelText('ID del profesional');
    await user.clear(input);
    await user.click(screen.getByRole('button', { name: 'Ingresar como profesional' }));

    expect(screen.getByRole('alert')).toHaveTextContent('ID entero mayor que cero');
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it('valida el profesional, persiste el contexto y solicita recargar', async () => {
    const user = userEvent.setup();
    const onContextChanged = vi.fn();
    vi.spyOn(window, 'fetch').mockResolvedValue({
      ok: true,
      status: 200,
      text: async () => JSON.stringify({
        id: 27,
        nombre: 'Ana',
        apellido: 'Pérez',
        especialidad: 'Clínica',
      }),
    });

    render(<ProfessionalTestSwitcher onContextChanged={onContextChanged} />);
    await user.click(screen.getByRole('button', { name: /Cambiar profesional de prueba/i }));
    const input = screen.getByLabelText('ID del profesional');
    await user.clear(input);
    await user.type(input, '27');
    await user.click(screen.getByRole('button', { name: 'Ingresar como profesional' }));

    expect(JSON.parse(window.localStorage.getItem(PROFESSIONAL_STORAGE_KEY))).toEqual({
      id: 27,
      name: 'Ana Pérez',
      specialty: 'Clínica',
    });
    expect(onContextChanged).toHaveBeenCalledWith(expect.objectContaining({ id: 27 }));
  });
});

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { WeeklyEditor, WEEKLY_TEMPLATE_KEY } from '../features/professional/components/WeeklyEditor';

describe('WeeklyEditor Component', () => {
  it('renders days of week and allows saving draft to localStorage', async () => {
    const user = userEvent.setup();
    const handleSave = vi.fn();

    render(
      <WeeklyEditor
        onSave={handleSave}
        canApply={false}
        applyTargetLabel="Septiembre 2026"
      />
    );

    expect(screen.getByText('Plantilla Semanal de Atención')).toBeInTheDocument();
    expect(screen.getByText('Lunes')).toBeInTheDocument();
    expect(screen.getByText('Viernes')).toBeInTheDocument();
    expect(screen.getByText('Domingo')).toBeInTheDocument();

    const saveBtn = screen.getByRole('button', { name: /Guardar borrador/i });
    expect(saveBtn).toBeEnabled();
    await user.click(saveBtn);

    expect(handleSave).toHaveBeenCalled();
    expect(localStorage.getItem(WEEKLY_TEMPLATE_KEY)).toBeTruthy();
  });
});


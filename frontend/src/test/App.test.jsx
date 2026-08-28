import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { App } from '../App';

describe('App Component', () => {
  it('renders the main layout header without crashing', () => {
    render(<App />);
    expect(
      screen.getByRole('heading', { level: 1, name: /Sistema de Gestión de Turnos/i })
    ).toBeInTheDocument();
  });

  it('renders the initial module cards', () => {
    render(<App />);
    expect(screen.getByText('Turnos')).toBeInTheDocument();
    expect(screen.getByText('Agenda')).toBeInTheDocument();
    expect(screen.getByText('Disponibilidad')).toBeInTheDocument();
    expect(screen.getByText('Clientes')).toBeInTheDocument();
  });
});

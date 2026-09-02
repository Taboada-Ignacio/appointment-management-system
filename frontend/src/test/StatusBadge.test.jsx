import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { StatusBadge } from '../components/ui/StatusBadge';

describe('StatusBadge Component', () => {
  it('renders ACTIVO badge with correct text and dot', () => {
    render(<StatusBadge status="ACTIVO" />);
    const badge = screen.getByLabelText(/Estado: Activo/i);
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveTextContent('Activo');
  });

  it('renders INACTIVO badge', () => {
    render(<StatusBadge status="INACTIVO" />);
    const badge = screen.getByLabelText(/Estado: Inactivo/i);
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveTextContent('Inactivo');
  });

  it('renders EN_TRANSCURSO badge', () => {
    render(<StatusBadge status="EN_TRANSCURSO" />);
    const badge = screen.getByLabelText(/Estado: En transcurso/i);
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveTextContent('En transcurso');
  });

  it('renders FINALIZADO badge', () => {
    render(<StatusBadge status="FINALIZADO" />);
    const badge = screen.getByLabelText(/Estado: Finalizado/i);
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveTextContent('Finalizado');
  });

  it('normalizes null or undefined status to Inactivo', () => {
    render(<StatusBadge status={null} />);
    expect(screen.getByText('Inactivo')).toBeInTheDocument();
  });
});


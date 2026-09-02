import { describe, it, expect } from 'vitest';
import {
  STATUS,
  STATUS_LABELS,
  normalizeStatus,
  deriveTemporalStatus,
  isMonthConfigured,
  canActivate,
  canInactivate,
  isTerminal,
} from '../utils/status';

describe('Status Utilities (status.js)', () => {
  const timezone = 'America/Argentina/Buenos_Aires';

  it('normalizes null or undefined to INACTIVO', () => {
    expect(normalizeStatus(null)).toBe(STATUS.INACTIVO);
    expect(normalizeStatus(undefined)).toBe(STATUS.INACTIVO);
    expect(normalizeStatus('')).toBe(STATUS.INACTIVO);
  });

  it('normalizes uppercase and lowercase status strings', () => {
    expect(normalizeStatus('ACTIVO')).toBe(STATUS.ACTIVO);
    expect(normalizeStatus('activo')).toBe(STATUS.ACTIVO);
    expect(normalizeStatus('en_transcurso')).toBe(STATUS.EN_TRANSCURSO);
    expect(normalizeStatus('finalizado')).toBe(STATUS.FINALIZADO);
  });

  it('provides readable human labels', () => {
    expect(STATUS_LABELS.ACTIVO).toBe('Activo');
    expect(STATUS_LABELS.INACTIVO).toBe('Inactivo');
    expect(STATUS_LABELS.EN_TRANSCURSO).toBe('En transcurso');
    expect(STATUS_LABELS.FINALIZADO).toBe('Finalizado');
  });

  it('preserves non-null backend status in deriveTemporalStatus', () => {
    expect(deriveTemporalStatus('ACTIVO', '2020-01-01', timezone)).toBe(STATUS.ACTIVO);
    expect(deriveTemporalStatus('FINALIZADO', '2099-01-01', timezone)).toBe(STATUS.FINALIZADO);
  });

  it('always presents a null backend status as INACTIVO', () => {
    expect(deriveTemporalStatus(null, '2020-05-10', timezone)).toBe(STATUS.INACTIVO);
    expect(deriveTemporalStatus(null, '2099-12-31', timezone)).toBe(STATUS.INACTIVO);
  });

  it('does not override an explicit backend status', () => {
    const today = '2026-09-01';
    expect(deriveTemporalStatus(null, today, timezone)).toBe(STATUS.INACTIVO);
    expect(deriveTemporalStatus('INACTIVO', today, timezone)).toBe(STATUS.INACTIVO);
    expect(deriveTemporalStatus('EN_TRANSCURSO', today, timezone)).toBe(STATUS.EN_TRANSCURSO);
  });

  it('normalizes null year-month states to INACTIVO', () => {
    expect(deriveTemporalStatus(null, '2020-05', timezone)).toBe(STATUS.INACTIVO);
    expect(deriveTemporalStatus(null, '2099-12', timezone)).toBe(STATUS.INACTIVO);
  });

  it('checks if month is configured based on dias and cantidadBrechas', () => {
    expect(isMonthConfigured(null)).toBe(false);
    expect(isMonthConfigured({})).toBe(false);
    expect(isMonthConfigured({ dias: [] })).toBe(false);
    expect(isMonthConfigured({ dias: [{ cantidadBrechas: 0 }, { cantidadBrechas: 0 }] })).toBe(false);
    expect(isMonthConfigured({ dias: [{ cantidadBrechas: 0 }, { cantidadBrechas: 2 }] })).toBe(true);
  });

  it('checks canActivate and canInactivate properly', () => {
    expect(canActivate('INACTIVO', true)).toBe(true);
    expect(canActivate('INACTIVO', false)).toBe(false);
    expect(canActivate(null, true)).toBe(true);
    expect(canActivate('ACTIVO', true)).toBe(false);

    expect(canInactivate('ACTIVO')).toBe(true);
    expect(canInactivate('INACTIVO')).toBe(false);
    expect(canInactivate(null)).toBe(false);
  });

  it('identifies terminal states', () => {
    expect(isTerminal('FINALIZADO')).toBe(true);
    expect(isTerminal('ACTIVO')).toBe(false);
    expect(isTerminal('INACTIVO')).toBe(false);
  });
});

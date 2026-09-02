import { useState } from 'react';
import { FlaskConical, LogIn, RotateCcw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { api } from '@/services/api';
import {
  clearTestProfessional,
  professionalContext,
  saveTestProfessional,
} from '@/config/professional';

export function ProfessionalTestSwitcher({ onContextChanged = () => window.location.reload() }) {
  const [open, setOpen] = useState(false);
  const [professionalId, setProfessionalId] = useState(String(professionalContext.id));
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    const id = Number(professionalId);

    if (!Number.isSafeInteger(id) || id <= 0) {
      setError('Ingresá un ID entero mayor que cero.');
      return;
    }

    setError('');
    setIsLoading(true);
    try {
      const professional = await api.get(`/api/profesionales/${encodeURIComponent(id)}`);
      saveTestProfessional(professional);
      onContextChanged(professional);
    } catch (requestError) {
      setError(requestError.status === 404
        ? `No existe un profesional con ID ${id}.`
        : requestError.message || 'No se pudo cambiar el profesional.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleReset = () => {
    clearTestProfessional();
    onContextChanged(null);
  };

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant={professionalContext.testMode ? 'secondary' : 'outline'}
          size="sm"
          className="gap-2"
          aria-label={`Cambiar profesional de prueba. Profesional actual: ${professionalContext.id}`}
        >
          <FlaskConical className="size-4 text-info" />
          <span className="hidden sm:inline">Profesional</span> #{professionalContext.id}
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-[min(22rem,calc(100vw-2rem))]">
        <form className="space-y-4" onSubmit={handleSubmit}>
          <div className="space-y-1">
            <h2 className="font-heading text-sm font-semibold">Simular sesión profesional</h2>
            <p className="text-xs leading-relaxed text-muted-foreground">
              Ingresá un ID existente. La agenda se recargará usando ese profesional.
            </p>
          </div>
          <div className="space-y-2">
            <Label htmlFor="test-professional-id">ID del profesional</Label>
            <Input
              id="test-professional-id"
              type="number"
              min="1"
              step="1"
              inputMode="numeric"
              value={professionalId}
              onChange={(event) => setProfessionalId(event.target.value)}
              disabled={isLoading}
              aria-invalid={Boolean(error)}
              aria-describedby={error ? 'test-professional-error' : undefined}
            />
            {error && (
              <p id="test-professional-error" className="text-xs font-medium text-destructive" role="alert">
                {error}
              </p>
            )}
          </div>
          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-between">
            {professionalContext.testMode ? (
              <Button type="button" variant="ghost" size="sm" onClick={handleReset} disabled={isLoading}>
                <RotateCcw /> Usar valor del entorno
              </Button>
            ) : <span />}
            <Button type="submit" size="sm" disabled={isLoading}>
              <LogIn /> {isLoading ? 'Verificando…' : 'Ingresar como profesional'}
            </Button>
          </div>
        </form>
      </PopoverContent>
    </Popover>
  );
}

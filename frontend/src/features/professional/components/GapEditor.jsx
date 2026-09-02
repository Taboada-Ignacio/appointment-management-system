import { useState } from 'react';
import { Trash2, Plus, AlertCircle } from 'lucide-react';
import { validateGaps, sortGaps, emptyGap } from '../../../utils/gaps';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

export function GapEditor({ gaps = [], onChange, readOnly = false, maxGaps = 8 }) {
  const [localErrors, setLocalErrors] = useState([]);

  const handleFieldChange = (index, field, value) => {
    if (readOnly) return;
    const updated = gaps.map((gap, i) => (i === index ? { ...gap, [field]: value } : gap));
    const validation = validateGaps(updated);
    setLocalErrors(validation.errors);
    onChange?.(updated);
  };

  const handleAdd = () => {
    if (readOnly || gaps.length >= maxGaps) return;
    const updated = [...gaps, emptyGap()];
    const validation = validateGaps(updated);
    setLocalErrors(validation.errors);
    onChange?.(updated);
  };

  const handleRemove = (index) => {
    if (readOnly) return;
    const updated = gaps.filter((_, i) => i !== index);
    const sorted = sortGaps(updated);
    const validation = validateGaps(sorted);
    setLocalErrors(validation.errors);
    onChange?.(sorted);
  };

  return (
    <div className="flex flex-col gap-3">
      {gaps.length === 0 ? (
        <p className="py-1 text-xs italic text-muted-foreground">Sin franjas configuradas.</p>
      ) : (
        <div className="flex flex-col gap-2">
          {gaps.map((gap, index) => (
            <div key={index} className="flex items-center gap-2">
              <div className="flex items-center gap-1.5 flex-1 min-w-0">
                <Input
                  type="time"
                  step="900"
                  value={gap.horaInicio || ''}
                  onChange={(e) => handleFieldChange(index, 'horaInicio', e.target.value)}
                  disabled={readOnly}
                  className="w-28 font-medium tabular-nums"
                  aria-label={`Brecha ${index + 1}: Hora de inicio`}
                />
                <span className="text-xs font-bold text-muted-foreground" aria-hidden="true">
                  –
                </span>
                <Input
                  type="time"
                  step="900"
                  value={gap.horaFin || ''}
                  onChange={(e) => handleFieldChange(index, 'horaFin', e.target.value)}
                  disabled={readOnly}
                  className="w-28 font-medium tabular-nums"
                  aria-label={`Brecha ${index + 1}: Hora de fin`}
                />
              </div>

              {!readOnly && (
                <Button
                  type="button"
                  onClick={() => handleRemove(index)}
                  variant="ghost"
                  size="icon"
                  className="shrink-0 text-destructive hover:bg-destructive/10 hover:text-destructive"
                  aria-label={`Eliminar brecha ${index + 1}`}
                  title="Eliminar brecha"
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              )}
            </div>
          ))}
        </div>
      )}

      {localErrors.length > 0 && (
        <Alert variant="destructive" className="px-3 py-2.5">
          <AlertCircle aria-hidden="true" />
          <AlertDescription className="space-y-1">
            {localErrors.map((err, i) => <p key={i}>{err}</p>)}
          </AlertDescription>
        </Alert>
      )}

      {!readOnly && gaps.length < maxGaps && (
        <Button
          type="button"
          onClick={handleAdd}
          variant="outline"
          size="sm"
          className="self-start"
        >
          <Plus className="h-3.5 w-3.5" />
          <span>Agregar franja</span>
        </Button>
      )}
    </div>
  );
}

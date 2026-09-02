import { useState } from 'react';
import { ConfirmDialog } from '../../../components/ui/ConfirmDialog';
import { IntegrationNotice } from '../../../components/ui/IntegrationNotice';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';

export function AffectedAppointmentsDialog({
  open = false,
  onOpenChange = null,
  onConfirm = null,
  loading = false,
}) {
  const [reason, setReason] = useState('');

  const handleConfirm = () => {
    if (!reason.trim()) return;
    onConfirm?.(reason.trim());
    setReason('');
  };

  const handleOpenChange = (nextOpen) => {
    if (!nextOpen) setReason('');
    onOpenChange?.(nextOpen);
  };

  return (
    <ConfirmDialog
      open={open}
      onOpenChange={handleOpenChange}
      title="Turnos afectados por modificación"
      description="Este flujo queda preparado para registrar las bajas cuando el backend pueda informar qué turnos resultan afectados."
      confirmLabel="Registrar bajas con motivo"
      cancelLabel="Cancelar"
      onConfirm={handleConfirm}
      variant="danger"
      loading={loading}
      confirmDisabled={!reason.trim()}
    >
      <div className="flex flex-col gap-4 py-2">
        <IntegrationNotice
          type="warning"
          title="No se puede consultar el impacto"
        >
          El backend todavía no permite listar los turnos afectados antes de modificar la agenda. Este componente no supone cantidades ni muestra turnos ficticios; solo prepara el motivo que exige el flujo oficial de baja.
        </IntegrationNotice>

        <div className="mt-2 flex flex-col gap-2">
          <Label htmlFor="motivo-baja-input">
            Motivo de baja administrativa (obligatorio)
          </Label>
          <Textarea
            id="motivo-baja-input"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            className="min-h-24 resize-none"
            rows={3}
            placeholder="Ej.: Modificación de agenda por guardia o capacitación"
            required
            disabled={loading}
          />
        </div>
      </div>
    </ConfirmDialog>
  );
}

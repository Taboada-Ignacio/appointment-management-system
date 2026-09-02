import { useNavigate } from 'react-router-dom';
import { EmptyState } from '../../../components/ui/EmptyState';
import { Compass } from 'lucide-react';

export function NotFoundPage() {
  const navigate = useNavigate();

  return (
    <div className="flex min-h-screen items-center justify-center bg-background p-4">
      <div className="w-full max-w-md">
        <EmptyState
          icon={Compass}
          title="Página no encontrada"
          description="La ruta solicitada no existe o no se encuentra disponible dentro del panel profesional."
          action={{
            label: 'Volver a Mi Día',
            onClick: () => navigate('/profesional/mi-dia'),
          }}
        />
      </div>
    </div>
  );
}

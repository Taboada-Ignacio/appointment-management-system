#!/usr/bin/env bash
# ==============================================================================
# Script para detener todo el proyecto en Bash (PostgreSQL + Backend + Frontend)
# ==============================================================================
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

echo ""
echo "=========================================================="
echo " 🛑 Deteniendo Sistema de Gestión de Turnos (api-turnos)"
echo "=========================================================="
echo ""

# 1. Detener procesos por PID
if [ -f .backend.pid ]; then
    PID=$(cat .backend.pid)
    kill $PID 2>/dev/null || true
    rm -f .backend.pid
    echo "  ✔ Backend detenido (PID $PID)."
fi

if [ -f .frontend.pid ]; then
    PID=$(cat .frontend.pid)
    kill $PID 2>/dev/null || true
    rm -f .frontend.pid
    echo "  ✔ Frontend detenido (PID $PID)."
fi

# 2. Detener procesos por puerto (fallback)
if command -v fuser > /dev/null 2>&1; then
    fuser -k 8080/tcp 2>/dev/null || true
    fuser -k 5173/tcp 2>/dev/null || true
fi

# 3. Detener Docker
echo "Deteniendo contenedores Docker..."
docker compose down

echo ""
echo "=========================================================="
echo " ✔ Todos los servicios han sido detenidos."
echo "=========================================================="
echo ""


#!/usr/bin/env bash
# ==============================================================================
# Script para levantar todo el proyecto con Docker (PostgreSQL + Backend + Frontend + Swagger)
# ==============================================================================
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

echo ""
echo "=========================================================="
echo " 🚀 Levantando Sistema de Gestión de Turnos con Docker"
echo "=========================================================="
echo ""

# 1. Verificar Docker
echo "[1/3] Verificando Docker..."
if ! docker info > /dev/null 2>&1; then
    echo "❌ Error: Docker no está en ejecución. Por favor inicia Docker Desktop y vuelve a intentar."
    exit 1
fi
echo "  ✔ Docker está activo."

if [ "$1" == "--local" ]; then
    echo "Modo Local: Levantando PostgreSQL con Docker y backend/frontend locales..."
    docker compose up -d postgres
    echo "  Esperando a que PostgreSQL esté listo..."
    until docker exec postgres-turnos pg_isready -U postgres -d turnos_db > /dev/null 2>&1; do
        sleep 1
    done
    mvn spring-boot:run > backend.log 2>&1 &
    echo $! > .backend.pid
    if [ -d "frontend" ]; then
        cd frontend
        [ ! -d "node_modules" ] && npm install
        npm run dev > ../frontend.log 2>&1 &
        echo $! > ../.frontend.pid
        cd ..
    fi
    echo "  ✔ Servicios locales iniciados."
    exit 0
fi

# 2. Levantar todos los servicios con Docker Compose
echo "[2/3] Levantando contenedores (Postgres, Backend, Frontend, Swagger)..."
docker compose up --build -d

# 3. Esperar a que el backend reporte salud
echo "[3/3] Esperando inicialización del Backend y Swagger UI..."
retries=35
until [ "$retries" -le 0 ]; do
    status=$(docker inspect -f '{{.State.Health.Status}}' api-turnos 2>/dev/null || echo "")
    if [ "$status" == "healthy" ]; then
        break
    fi
    sleep 2
    retries=$((retries-1))
done

echo ""
echo "=========================================================="
echo " 🎉 Todos los servicios se han iniciado correctamente!"
echo "=========================================================="
echo ""
echo "  📖 Swagger UI:  http://localhost:8080/swagger-ui.html"
echo "  ⚙️ Backend API: http://localhost:8080"
echo "  🖥️ Frontend:    http://localhost:5173"
echo "  🗄️ PostgreSQL:  localhost:5432 (turnos_db)"
echo ""
echo "Para detener todo, ejecuta: ./stop.sh"
echo ""

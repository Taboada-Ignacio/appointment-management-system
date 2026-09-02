#!/usr/bin/env bash
# ==============================================================================
# Script para levantar todo el proyecto en Bash (PostgreSQL + Backend + Frontend)
# ==============================================================================
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

echo ""
echo "=========================================================="
echo " 🚀 Levantando Sistema de Gestión de Turnos (api-turnos)"
echo "=========================================================="
echo ""

# 1. Verificar Docker
echo "[1/4] Verificando Docker..."
if ! docker info > /dev/null 2>&1; then
    echo "❌ Error: Docker no está en ejecución."
    exit 1
fi
echo "  ✔ Docker está activo."

# 2. Levantar PostgreSQL
echo "[2/4] Iniciando contenedor PostgreSQL..."
docker compose up -d postgres

echo "  Esperando a que PostgreSQL esté listo..."
until docker exec postgres-turnos pg_isready -U postgres -d turnos_db > /dev/null 2>&1; do
    sleep 1
done
echo "  ✔ PostgreSQL 17 listo en localhost:5432."

# 3. Levantar Backend
echo "[3/4] Iniciando Backend Spring Boot..."
mvn spring-boot:run > backend.log 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID > .backend.pid
echo "  ✔ Backend iniciado (PID: $BACKEND_PID, logs: backend.log)."

# 4. Levantar Frontend
if [ -d "frontend" ]; then
    echo "[4/4] Iniciando Frontend React (Vite)..."
    cd frontend
    if [ ! -d "node_modules" ]; then
        npm install
    fi
    npm run dev > ../frontend.log 2>&1 &
    FRONTEND_PID=$!
    echo $FRONTEND_PID > ../.frontend.pid
    cd ..
    echo "  ✔ Frontend iniciado (PID: $FRONTEND_PID, logs: frontend.log)."
fi

echo ""
echo "=========================================================="
echo " 🎉 Todos los servicios se han iniciado correctamente!"
echo "=========================================================="
echo ""
echo "  📖 Swagger UI:  http://localhost:8080/swagger-ui.html"
echo "  ⚙️ Backend API: http://localhost:8080"
echo "  🖥️ Frontend:    http://localhost:5173"
echo "  🗄️ PostgreSQL:  localhost:5432"
echo ""
echo "Para detener todo, ejecuta: ./stop.sh"
echo ""


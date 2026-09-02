# ==============================================================================
# Script para levantar todo el proyecto (PostgreSQL + Backend + Frontend)
# ==============================================================================

param (
    [switch]$DockerAll, # Levantar todo con Docker Compose en lugar de procesos locales
    [switch]$NoFrontend, # Levantar solo base de datos y backend
    [switch]$NoBrowser # No abrir el navegador automáticamente
)

$ErrorActionPreference = "Stop"
$rootDir = $PSScriptRoot

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " 🚀 Levantando Sistema de Gestión de Turnos (api-turnos)" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Verificar Docker
Write-Host "[1/4] Verificando Docker..." -ForegroundColor Yellow
try {
    docker info > $null 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Error: Docker no está en ejecución. Por favor inicia Docker Desktop y vuelve a intentar." -ForegroundColor Red
        exit 1
    }
    Write-Host "  ✔ Docker está activo." -ForegroundColor Green
} catch {
    Write-Host "❌ Error: No se pudo verificar Docker." -ForegroundColor Red
    exit 1
}

# Opción Docker All
if ($DockerAll) {
    Write-Host ""
    Write-Host "Levantando todos los servicios mediante Docker Compose..." -ForegroundColor Yellow
    Set-Location $rootDir
    docker compose up --build -d
    Write-Host ""
    Write-Host "✔ Contenedores levantados con Docker Compose." -ForegroundColor Green
    Write-Host "  - Swagger UI: http://localhost:8080/swagger-ui.html" -ForegroundColor Cyan
    Write-Host "  - Backend API: http://localhost:8080" -ForegroundColor Cyan
    exit 0
}

# 2. Levantar Base de Datos PostgreSQL
Write-Host "[2/4] Iniciando contenedor PostgreSQL (postgres-turnos)..." -ForegroundColor Yellow
Set-Location $rootDir
docker compose up -d postgres

# Esperar a que la base de datos esté lista
Write-Host "  Esperando a que PostgreSQL esté listo para aceptar conexiones..." -ForegroundColor Gray
$retries = 15
$ready = $false
while ($retries -gt 0) {
    $status = docker inspect -f '{{.State.Health.Status}}' postgres-turnos 2>$null
    if ($status -eq "healthy" -or $status -eq "") {
        # Probar con pg_isready si está disponible
        $isReady = docker exec postgres-turnos pg_isready -U postgres -d turnos_db 2>$null
        if ($LASTEXITCODE -eq 0) {
            $ready = $true
            break
        }
    }
    Start-Sleep -Seconds 2
    $retries--
}

if ($ready) {
    Write-Host "  ✔ PostgreSQL 17 listo en localhost:5432." -ForegroundColor Green
} else {
    Write-Host "  ⚠ Advertencia: PostgreSQL tardó en reportar salud, continuando..." -ForegroundColor Yellow
}

# 3. Levantar Backend Spring Boot
Write-Host "[3/4] Iniciando Backend Spring Boot en nueva ventana..." -ForegroundColor Yellow
$backendCmd = "cd '$rootDir'; Write-Host '🚀 Iniciando Backend Spring Boot...' -ForegroundColor Cyan; mvn spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", $backendCmd

# 4. Levantar Frontend Vite
if (-not $NoFrontend) {
    Write-Host "[4/4] Iniciando Frontend React (Vite) en nueva ventana..." -ForegroundColor Yellow
    $frontendDir = Join-Path $rootDir "frontend"
    if (Test-Path $frontendDir) {
        $frontendCmd = "cd '$frontendDir'; Write-Host '🚀 Iniciando Frontend React (Vite)...' -ForegroundColor Cyan; if (-not (Test-Path 'node_modules')) { npm install }; npm run dev"
        Start-Process powershell -ArgumentList "-NoExit", "-Command", $frontendCmd
        Write-Host "  ✔ Frontend iniciado." -ForegroundColor Green
    } else {
        Write-Host "  ⚠ Directorio frontend no encontrado." -ForegroundColor Yellow
    }
} else {
    Write-Host "[4/4] Frontend omitido (-NoFrontend)." -ForegroundColor Gray
}

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host " 🎉 Todos los servicios se han iniciado correctamente!" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  📖 Swagger UI:  http://localhost:8080/swagger-ui.html" -ForegroundColor Cyan
Write-Host "  ⚙️ Backend API: http://localhost:8080" -ForegroundColor Cyan
Write-Host "  🖥️ Frontend:    http://localhost:5173" -ForegroundColor Cyan
Write-Host "  🗄️ PostgreSQL:  localhost:5432 (turnos_db)" -ForegroundColor Cyan
Write-Host ""
Write-Host "Para detener todos los servicios, ejecuta: .\stop.ps1 o double-click en stop.bat" -ForegroundColor Yellow
Write-Host ""

if (-not $NoBrowser) {
    Start-Sleep -Seconds 3
    Start-Process "http://localhost:8080/swagger-ui.html"
    if (-not $NoFrontend) {
        Start-Process "http://localhost:5173"
    }
}


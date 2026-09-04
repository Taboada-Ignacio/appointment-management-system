# ==============================================================================
# Script para levantar todo el proyecto con Docker (PostgreSQL + Backend + Frontend + Swagger)
# ==============================================================================

param (
    [switch]$Local,      # Levantar mediante procesos locales en lugar de Docker Compose
    [switch]$NoBrowser,  # No abrir el navegador automáticamente
    [switch]$NoBuild     # No reconstruir imágenes de Docker si ya existen
)

$ErrorActionPreference = "Stop"
$rootDir = $PSScriptRoot

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " 🚀 Levantando Sistema de Gestion de Turnos con Docker" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Verificar Docker
Write-Host "[1/3] Verificando Docker..." -ForegroundColor Yellow
$dockerCheck = docker info 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host " Error: Docker no esta en ejecucion. Por favor inicia Docker Desktop y vuelve a intentar." -ForegroundColor Red
    exit 1
}
Write-Host "   Docker esta activo." -ForegroundColor Green

# Deteccion de IP LAN
$lanIp = (Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
    Where-Object { $_.InterfaceAlias -notmatch "vEthernet|Loopback|Docker|WSL" -and $_.IPAddress -notmatch "^127\.|^169\.254\." } |
    Sort-Object -Property InterfaceMetric |
    Select-Object -First 1).IPAddress

if ($Local) {
    Write-Host ""
    Write-Host "Modo Local: Levantando PostgreSQL con Docker y backend/frontend en procesos locales..." -ForegroundColor Yellow
    Set-Location $rootDir
    docker compose up -d postgres

    Write-Host "Iniciando Backend Spring Boot en nueva ventana..." -ForegroundColor Yellow
    $backendCmd = "cd '$rootDir'; Write-Host 'Iniciando Backend Spring Boot...' -ForegroundColor Cyan; mvn spring-boot:run"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $backendCmd

    Write-Host "Iniciando Frontend React (Vite) en nueva ventana..." -ForegroundColor Yellow
    $frontendDir = Join-Path $rootDir "frontend"
    $frontendCmd = "cd '$frontendDir'; Write-Host 'Iniciando Frontend React (Vite)...' -ForegroundColor Cyan; if (-not (Test-Path 'node_modules')) { npm install }; npm run dev"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $frontendCmd

    Write-Host ""
    Write-Host " Servicios locales iniciados." -ForegroundColor Green
    Write-Host "  Frontend (Local):   http://localhost:5173" -ForegroundColor Cyan
    if ($lanIp) {
        Write-Host "  Frontend (Red LAN): http://${lanIp}:5173" -ForegroundColor Yellow
    }
    Write-Host "  Swagger UI:         http://localhost:8080/swagger-ui.html" -ForegroundColor Cyan
    if ($lanIp) {
        Write-Host "  Swagger UI (LAN):   http://${lanIp}:5173/swagger-ui.html" -ForegroundColor Yellow
    }
    Write-Host ""
    if (-not $NoBrowser) {
        Start-Sleep -Seconds 4
        Start-Process "http://localhost:8080/swagger-ui.html"
        Start-Process "http://localhost:5173"
    }
    exit 0
}

# 2. Levantar todos los servicios con Docker Compose
Write-Host "[2/3] Levantando todos los servicios con Docker Compose (Postgres, Backend, Frontend, Swagger)..." -ForegroundColor Yellow
Set-Location $rootDir

if ($NoBuild) {
    docker compose up -d
} else {
    docker compose up --build -d
}

if ($LASTEXITCODE -ne 0) {
    Write-Host " Error al levantar los contenedores con Docker Compose." -ForegroundColor Red
    exit 1
}

# 3. Esperar disponibilidad de los servicios
Write-Host "[3/3] Esperando inicializacion del Backend y Swagger UI..." -ForegroundColor Yellow
$retries = 35
$ready = $false
while ($retries -gt 0) {
    $status = docker inspect -f '{{.State.Health.Status}}' api-turnos 2>$null
    if ($status -eq "healthy") {
        $ready = $true
        break
    }
    Start-Sleep -Seconds 2
    $retries--
}

if ($ready) {
    Write-Host "   Backend y Swagger UI listos y saludables." -ForegroundColor Green
} else {
    Write-Host "   Advertencia: El backend tardo mas de lo esperado en reportar salud, continuando..." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "  Todos los servicios se han levantado correctamente!" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Frontend (Local):   http://localhost:5173" -ForegroundColor Cyan
if ($lanIp) {
    Write-Host "  Frontend (Red LAN): http://${lanIp}:5173" -ForegroundColor Yellow
}
Write-Host "  Swagger UI:         http://localhost:8080/swagger-ui.html" -ForegroundColor Cyan
if ($lanIp) {
    Write-Host "  Swagger UI (LAN):   http://${lanIp}:5173/swagger-ui.html" -ForegroundColor Yellow
}
Write-Host "  Backend API:        http://localhost:8080" -ForegroundColor Cyan
Write-Host "  PostgreSQL:         127.0.0.1:5432 (turnos_db)" -ForegroundColor Cyan
Write-Host ""
Write-Host "Para detener todos los servicios, ejecuta: .\stop.ps1 o doble clic en stop.bat" -ForegroundColor Yellow
Write-Host ""

if (-not $NoBrowser) {
    Start-Sleep -Seconds 1
    Start-Process "http://localhost:8080/swagger-ui.html"
    Start-Process "http://localhost:5173"
}

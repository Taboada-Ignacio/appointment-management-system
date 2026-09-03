# ==============================================================================
# Script para bajar / detener todo el proyecto (Docker Compose)
# ==============================================================================

param (
    [switch]$KeepDatabase # Mantener PostgreSQL corriendo en Docker
)

$rootDir = $PSScriptRoot

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Red
Write-Host " Deteniendo Sistema de Gestion de Turnos (api-turnos)" -ForegroundColor Red
Write-Host "==========================================================" -ForegroundColor Red
Write-Host ""

# 1. Detener Contenedores Docker
Set-Location $rootDir
if ($KeepDatabase) {
    Write-Host "Deteniendo contenedores de Backend y Frontend..." -ForegroundColor Yellow
    docker compose stop api-turnos frontend
} else {
    Write-Host "Deteniendo todos los contenedores Docker..." -ForegroundColor Yellow
    docker compose down
}

# 2. Detener procesos locales residuales en puertos si existen
function Stop-ProcessOnPort {
    param ([int]$Port, [string]$ServiceName)
    try {
        $connections = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
        if ($connections) {
            $pids = $connections | Select-Object -ExpandProperty OwningProcess -Unique
            foreach ($p in $pids) {
                if ($p -and $p -ne 0) {
                    try {
                        Stop-Process -Id $p -Force -ErrorAction SilentlyContinue
                    } catch {
                    }
                }
            }
            Write-Host "   $ServiceName detenido (puerto $Port liberado)." -ForegroundColor Green
        }
    } catch {
    }
}

Stop-ProcessOnPort -Port 8080 -ServiceName "Backend"
Stop-ProcessOnPort -Port 5173 -ServiceName "Frontend"

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "  Todos los servicios del proyecto han sido detenidos." -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host ""

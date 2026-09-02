# ==============================================================================
# Script para bajar / detener todo el proyecto (PostgreSQL + Backend + Frontend)
# ==============================================================================

param (
    [switch]$KeepDatabase # Mantener PostgreSQL corriendo en Docker
)

$rootDir = $PSScriptRoot

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Red
Write-Host " 🛑 Deteniendo Sistema de Gestión de Turnos (api-turnos)" -ForegroundColor Red
Write-Host "==========================================================" -ForegroundColor Red
Write-Host ""

# Función auxiliar para matar procesos por puerto
function Stop-ProcessOnPort {
    param ([int]$Port, [string]$ServiceName)
    Write-Host "Buscando procesos en el puerto $Port ($ServiceName)..." -ForegroundColor Yellow
    try {
        $connections = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
        if ($connections) {
            $pids = $connections | Select-Object -ExpandProperty OwningProcess -Unique
            foreach ($p in $pids) {
                if ($p -and $p -ne 0) {
                    try {
                        $proc = Get-Process -Id $p -ErrorAction SilentlyContinue
                        if ($proc) {
                            Write-Host "  Deteniendo proceso $($proc.ProcessName) (PID: $p)..." -ForegroundColor Gray
                            Stop-Process -Id $p -Force -ErrorAction SilentlyContinue
                        }
                    } catch {
                        # Ignorar si ya se cerró
                    }
                }
            }
            Write-Host "  ✔ $ServiceName detenido (puerto $Port liberado)." -ForegroundColor Green
        } else {
            Write-Host "  - No hay procesos escuchando en el puerto $Port." -ForegroundColor Gray
        }
    } catch {
        # Fallback con netstat
        $lines = netstat -ano | findstr ":$Port"
        if ($lines) {
            foreach ($line in $lines) {
                $parts = ($line -split '\s+') | Where-Object { $_ -ne '' }
                if ($parts.Length -ge 5) {
                    $procId = $parts[-1]
                    if ($procId -match '^\d+$' -and [int]$procId -ne 0) {
                        taskkill /PID $procId /F > $null 2>&1
                    }
                }
            }
            Write-Host "  ✔ $ServiceName detenido mediante taskkill." -ForegroundColor Green
        } else {
            Write-Host "  - No hay procesos escuchando en el puerto $Port." -ForegroundColor Gray
        }
    }
}

# 1. Detener Backend (Puerto 8080)
Stop-ProcessOnPort -Port 8080 -ServiceName "Backend Spring Boot"

# 2. Detener Frontend (Puerto 5173)
Stop-ProcessOnPort -Port 5173 -ServiceName "Frontend Vite React"

# 3. Detener Contenedores Docker
if (-not $KeepDatabase) {
    Write-Host ""
    Write-Host "Deteniendo contenedores Docker..." -ForegroundColor Yellow
    Set-Location $rootDir
    try {
        docker compose down
        Write-Host "  ✔ Contenedores Docker detenidos." -ForegroundColor Green
    } catch {
        Write-Host "  ⚠ Error al detener contenedores docker compose." -ForegroundColor Yellow
    }
} else {
    Write-Host ""
    Write-Host "- Base de datos conservada en ejecución (-KeepDatabase)." -ForegroundColor Gray
}

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host " ✔ Todos los servicios del proyecto han sido detenidos." -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host ""


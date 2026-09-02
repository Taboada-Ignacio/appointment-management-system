@echo off
title Deteniendo Proyecto - Sistema de Gestion de Turnos
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop.ps1" %*
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Ocurrio un error al ejecutar el script de detencion.
    pause
)


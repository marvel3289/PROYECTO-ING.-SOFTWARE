@echo off
setlocal
cd /d "%~dp0"
title Biblioteca San Lucas - PostgreSQL
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0INICIAR_POSTGRESQL.ps1"
if errorlevel 1 (
  echo.
  echo No se pudo iniciar la biblioteca. Revise el error superior.
)
pause

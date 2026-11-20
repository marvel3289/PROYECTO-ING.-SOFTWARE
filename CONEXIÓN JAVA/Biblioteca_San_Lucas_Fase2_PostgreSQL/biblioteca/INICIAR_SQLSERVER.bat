@echo off
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0INICIAR_SQLSERVER.ps1"
if errorlevel 1 (
 echo.
 echo No se pudo iniciar en modo SQL Server. Revisa el error anterior.
 pause
)

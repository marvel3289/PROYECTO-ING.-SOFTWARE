@echo off
setlocal
cd /d "%~dp0"
if not exist "biblioteca-web.jar" (echo Falta biblioteca-web.jar & pause & exit /b 1)
java -cp biblioteca-web.jar biblioteca.PruebasBasicas
if errorlevel 1 (echo PRUEBAS FALLIDAS & pause & exit /b 1)
echo Pruebas aprobadas.
pause

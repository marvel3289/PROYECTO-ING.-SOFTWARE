@echo off
setlocal
cd /d "%~dp0"
title Biblioteca Web - servidor local
echo.
echo =============================================
echo         BIBLIOTECA WEB - INICIO
 echo =============================================
echo.
where java >nul 2>&1
if errorlevel 1 (
  echo ERROR: No se encontro Java. Instale Java 17 o superior.
  goto fallo
)
java -version 2>&1
echo.
if not exist "biblioteca-web.jar" (
  echo ERROR: Falta biblioteca-web.jar. Extraiga el ZIP completo.
  goto fallo
)
if not exist "web\index.html" (
  echo ERROR: Falta web\index.html. Extraiga el ZIP completo.
  goto fallo
)
echo.
echo Cuando aparezca "Biblioteca Web disponible", abre:
echo   http://127.0.0.1:8081/
echo.
echo Para terminar, presiona Ctrl+C.
echo.
java -Dfile.encoding=UTF-8 -jar "biblioteca-web.jar"
if errorlevel 1 goto fallo
pause
exit /b 0
:fallo
echo.
echo No se pudo iniciar la pagina. Copia aqui el texto del error.
pause
exit /b 1

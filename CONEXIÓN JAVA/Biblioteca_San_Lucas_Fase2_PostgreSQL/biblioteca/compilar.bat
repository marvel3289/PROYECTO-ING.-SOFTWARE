@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>&1 || (echo Instale un JDK 17 o superior & pause & exit /b 1)
if exist "out" rmdir /s /q "out"
mkdir "out"
javac -encoding UTF-8 --release 17 -sourcepath "src" -d "out" "src\biblioteca\Main.java" "src\biblioteca\ConsolaMain.java" "src\biblioteca\PruebasBasicas.java"
if errorlevel 1 (echo ERROR de compilacion & pause & exit /b 1)
jar --create --file biblioteca-web.jar --main-class biblioteca.Main -C out .
echo Compilacion correcta. Ejecuta iniciar.bat.
pause

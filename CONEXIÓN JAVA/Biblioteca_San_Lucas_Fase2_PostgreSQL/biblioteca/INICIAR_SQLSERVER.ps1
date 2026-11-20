# Inicio seguro de la aplicacion con SQL Server (credenciales solo en el proceso actual).
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if (-not (Test-Path '.\biblioteca-web.jar')) { throw 'Falta biblioteca-web.jar. Compile el proyecto primero.' }
if (-not (Test-Path '.\lib\mssql-jdbc-*.jar')) { throw 'Falta el driver Microsoft JDBC en la carpeta lib (mssql-jdbc-*.jar).' }
$env:BIBLIOTECA_MODO = 'sqlserver'
if (-not $env:BIBLIOTECA_DB_URL) {
    $env:BIBLIOTECA_DB_URL = 'jdbc:sqlserver://localhost:1433;databaseName=BibliotecaSanLucas;encrypt=true;trustServerCertificate=true'
}
if (-not $env:BIBLIOTECA_DB_USER) { $env:BIBLIOTECA_DB_USER = Read-Host 'Usuario SQL Server (autenticacion SQL)' }

function PedirClave([string]$mensaje) {
    $seguro = Read-Host $mensaje -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($seguro)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
}
if (-not $env:BIBLIOTECA_DB_PASSWORD) { $env:BIBLIOTECA_DB_PASSWORD = PedirClave 'Clave del usuario SQL Server' }
if (-not $env:BIBLIOTECA_ADMIN_PASSWORD) {
    Write-Host 'Si la BD esta vacia, se usara la siguiente clave para crear admin@biblioteca.gt.'
    $env:BIBLIOTECA_ADMIN_PASSWORD = PedirClave 'Clave inicial del administrador (minimo 12 caracteres)'
}
Write-Host "`nAbre http://127.0.0.1:8081/ cuando aparezca Biblioteca Web disponible."
Write-Host 'Para detener el servidor presiona Ctrl+C.'
& java '-Dfile.encoding=UTF-8' -cp 'biblioteca-web.jar;lib/*' biblioteca.Main
if ($LASTEXITCODE -ne 0) { throw "El servidor termino con error ($LASTEXITCODE). Revisa el mensaje superior." }

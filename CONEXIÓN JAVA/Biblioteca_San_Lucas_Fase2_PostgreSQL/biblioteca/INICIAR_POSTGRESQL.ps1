# Biblioteca San Lucas - Inicio local con PostgreSQL, sin credenciales en archivos.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'No se encuentra Java. Instale Java 17 o superior.'
}
if (-not (Test-Path '.\biblioteca-web.jar')) {
    throw 'Falta biblioteca-web.jar. Extraiga el ZIP completo.'
}
$drivers = @(Get-ChildItem '.\lib\postgresql-*.jar' -ErrorAction SilentlyContinue)
if ($drivers.Count -lt 1) {
    throw 'Falta el controlador JDBC: descargue postgresql-42.7.14.jar de https://jdbc.postgresql.org/download/ y copielo en biblioteca\lib.'
}
$env:BIBLIOTECA_MODO = 'postgresql'
$env:BIBLIOTECA_HOST = '127.0.0.1'
$env:BIBLIOTECA_PUERTO = '8081'
$env:BIBLIOTECA_DB_URL = 'jdbc:postgresql://127.0.0.1:5440/biblioteca_san_lucas'
$env:BIBLIOTECA_DB_USER = 'postgres'

function PedirClave([string]$mensaje) {
    $seguro = Read-Host $mensaje -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($seguro)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
}
$env:BIBLIOTECA_DB_PASSWORD = PedirClave 'Contrasena PostgreSQL (la de tu Docker Compose)'
$env:BIBLIOTECA_ADMIN_PASSWORD = PedirClave 'Clave inicial para admin@biblioteca.gt (minimo 12 caracteres; se utiliza si la base esta vacia)'
Write-Host "`nSolo conexion LOCAL: jdbc:postgresql://127.0.0.1:5440/biblioteca_san_lucas"
Write-Host 'Cuando aparezca Biblioteca Web disponible, abra http://127.0.0.1:8081/'
Write-Host 'Para detener el servidor presione Ctrl+C.'
& java '-Dfile.encoding=UTF-8' -cp 'biblioteca-web.jar;lib/*' biblioteca.Main
if ($LASTEXITCODE -ne 0) { throw "El servidor termino con error ($LASTEXITCODE). Revise el texto superior." }

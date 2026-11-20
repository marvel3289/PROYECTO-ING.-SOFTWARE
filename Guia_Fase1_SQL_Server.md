# Fase 1 - Integración de SQL Server (Proyecto Integrador)

## Qué se entregó
Se añadieron cuatro tablas relacionales, repositorios JDBC reales, una opción de arranque `sqlserver` y transacciones para préstamos y devoluciones. Se conserva el modo anterior `archivo` para proteger la demostración y permitir comparar resultados. **El modo SQL Server no usa el archivo `biblioteca.dat`.** No hay migración automática de datos antiguos.

## Antes de empezar (Windows)
1. Tenga instalado Java JDK/JRE 17+ (se verificó el proyecto con Java 21 en la revisión).
2. Instale o identifique **SQL Server** (Express/Developer también sirve) y una herramienta para ejecutar scripts SQL, como **SQL Server Management Studio (SSMS)**.
3. Confirme que la instancia acepte conexiones TCP/IP y autenticación de SQL Server (usuario y contraseña). El puerto habitual es 1433; en `SQLEXPRESS` puede variar.
4. Descargue el **Microsoft JDBC Driver for SQL Server** desde https://learn.microsoft.com/es-es/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server y coloque el `.jar` adecuado para Java 17+ dentro de `biblioteca/lib/`. El nombre debe comenzar con `mssql-jdbc-` y terminar en `.jar`.
5. Abra `biblioteca/sql/01_crear_base_de_datos.sql` en SSMS y ejecute el script completo. Requiere privilegios para crear BD; puede pedir a un administrador que ejecute esa parte. Si la BD ya existe, el script no borra los datos.
6. Abra `biblioteca/INICIAR_SQLSERVER.bat`. Se pedirán usuario SQL, contraseña SQL y clave inicial del administrador. La clave inicial solo se utiliza si la tabla `usuarios` está vacía.
7. Abra **http://127.0.0.1:8081/** en el navegador e inicie sesión con `admin@biblioteca.gt` y la clave inicial elegida.

## ¿Su SQL Server no está en localhost:1433?
Configure `BIBLIOTECA_DB_URL` **en la ventana de PowerShell antes de ejecutar** `INICIAR_SQLSERVER.ps1`. Ejemplos:

```powershell
# Instancia SQL Server con puerto TCP fijo
$env:BIBLIOTECA_DB_URL = 'jdbc:sqlserver://localhost:1433;databaseName=BibliotecaSanLucas;encrypt=true;trustServerCertificate=true'

# Instancia nombrada local; SQL Server Browser/UDP 1434 debe resolverla
$env:BIBLIOTECA_DB_URL = 'jdbc:sqlserver://localhost;instanceName=SQLEXPRESS;databaseName=BibliotecaSanLucas;encrypt=true;trustServerCertificate=true'

.\INICIAR_SQLSERVER.ps1
```

`trustServerCertificate=true` se utiliza **solo para pruebas locales** cuando no se dispone de un certificado confiable. Antes de desplegar en la nube deberá emplearse un certificado válido y `trustServerCertificate=false`, más políticas de contraseñas, secretos y despliegue HTTPS.

## Comprobar en SQL Server
Tras registrar un libro, crear un ejemplar y realizar una operación desde la web, ejecute:

```sql
USE BibliotecaSanLucas;
SELECT id, nombre, correo, rol, activo FROM dbo.usuarios;
SELECT id, titulo, autor, isbn FROM dbo.libros;
SELECT id, libro_id, codigo, disponible FROM dbo.ejemplares;
SELECT id, usuario_id, ejemplar_id, fecha_prestamo, fecha_vencimiento, fecha_devolucion FROM dbo.prestamos;
```

Compruebe que cada acción aparece en SQL y que los cambios se conservan después de cerrar y abrir la aplicación. Haga una copia de seguridad antes de cargas importantes.

## Evidencia para entregar al docente
- Captura del script ejecutado con éxito en SSMS.
- Captura de la BD y las cuatro tablas (incluidas relaciones).
- Captura del inicio del servidor indicando **Persistencia activa: SQL Server**.
- Captura de usuarios/libros/préstamos creados desde la web y consultados con SQL.
- Verificación de que un ejemplar no puede prestarse dos veces simultáneamente.
- Captura de registros conservados tras reiniciar el servidor.

## Qué no está terminado
- No se ha realizado una conexión con su instancia particular de SQL Server durante esta revisión: necesita ejecutar el script y configurar las credenciales en su equipo.
- No se incluyen datos del archivo `biblioteca.dat` en la BD ni migración automática.
- Quedan por completar CRUD de edición, reportes, HTTPS de producción, Docker, GitHub/CI, pruebas de rendimiento, despliegue en nube y documentación académica restante.

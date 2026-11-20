# Sistema de Gestión Bibliotecaria — San Lucas Sacatepéquez

Versión actual: modo PostgreSQL añadido (Java + JDBC), modo demostración conservado, modo SQL Server previo conservado.

**Comience por `docs/03_PASOS_POSTGRESQL.md`** para conectar a PostgreSQL 16 en Docker (`127.0.0.1:5440`, base `biblioteca_san_lucas`).

**Lanzadores**
- `INICIAR_POSTGRESQL.bat`: PostgreSQL relacional (requiere driver JDBC descargado por separado y contraseñas que se solicitan de forma segura).
- `iniciar.bat`: datos de demostración en archivo `.dat`.
- `INICIAR_SQLSERVER.bat`: modo SQL Server previo (opcional).

La interfaz abre en `http://127.0.0.1:8081/`. Solo pruebas locales; no exponer a Internet sin implementar HTTPS, usuarios de BD restringidos y refuerzos de seguridad.

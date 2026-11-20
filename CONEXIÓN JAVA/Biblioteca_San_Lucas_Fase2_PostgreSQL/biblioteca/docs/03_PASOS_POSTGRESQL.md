# Biblioteca San Lucas — Fase 1: conexión PostgreSQL

## Estado
Las tablas ya se crearon en DBeaver y se confirmaron las 4 claves primarias y 3 foráneas.
Esta entrega incorpora un repositorio JDBC PostgreSQL. NO cambia ni migra los proyectos existentes.

## Preparar el equipo (Windows)

1. Descargue el driver JDBC PostgreSQL de https://jdbc.postgresql.org/download/ (recomendado: postgresql-42.7.14.jar, Java 8 o superior).
2. Coloque el archivo `postgresql-42.7.14.jar` dentro de `biblioteca/lib/`. **No lo abra ni descomprima.**
3. Compruebe que Java 17 o superior está instalado (en Símbolo del sistema: `java -version`).
4. Confirme en DBeaver que la base `biblioteca_san_lucas` existe en PostgreSQL, host `127.0.0.1`, puerto Windows `5440` y tiene las tablas `usuarios`, `libros`, `ejemplares` y `prestamos`.
5. Haga doble clic en `biblioteca/INICIAR_POSTGRESQL.bat`.
6. Ingrese la contraseña de PostgreSQL cuando la solicite. El valor no se guarda en el código ni en el ZIP.
7. Ingrese una contraseña nueva para el administrador inicial (12 caracteres o más; solamente se usará si `usuarios` está vacío).
8. Espere el mensaje `Persistencia activa: PostgreSQL` y `Biblioteca Web disponible en http://127.0.0.1:8081/`.
9. Abra `http://127.0.0.1:8081/` e inicie sesión con `admin@biblioteca.gt` y la contraseña de administrador que definió.
10. Registre un libro y un ejemplar; compruebe en DBeaver que aparecen en las tablas, cierre la aplicación y vuelva a iniciarla para comprobar la persistencia real.

## Importante
- Esta versión funciona **solamente en local** y no incorpora HTTPS para Internet.
- Si ya hay usuarios registrados, NO crea ni cambia el administrador; use sus credenciales existentes.
- El archivo `biblioteca.dat` del modo anterior no se importará automáticamente. Los datos nuevos se crean en PostgreSQL.
- No use `INICIAR_AQUI.bat` para probar PostgreSQL: ese ejecuta la versión de archivo local. Use `INICIAR_POSTGRESQL.bat`.
- Para comprobar la base seleccionada en DBeaver: `SELECT current_database();`
- No introduzca contraseñas en capturas o mensajes.

## Restricción importante
Para impedir que una configuración errónea escriba sobre otras bases, el código verifica que la conexión corresponda exactamente a `biblioteca_san_lucas` y que las cuatro tablas existan antes de iniciar el servidor. No se incluyeron comandos destructivos ni se cambió Docker Compose.

## Comprobaciones ya realizadas sobre este ZIP
- Compilación de fuentes Java sin errores de sintaxis.
- Ejecución de pruebas básicas del modo archivo (13 pruebas) y de API HTTP (29 comprobaciones).
- **Pendiente de comprobar en Windows:** enlace JDBC PostgreSQL real y operaciones contra PostgreSQL 16, porque aquí no tenemos acceso a su servidor Docker.

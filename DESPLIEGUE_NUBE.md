# Biblioteca San Lucas: despliegue web con PostgreSQL

Esta guía conserva la instalación local de Docker/DBeaver y crea **otra base** en la nube.
El sistema público es un prototipo académico: no usar con datos personales reales sin una auditoría de seguridad adicional.

## Arquitectura

- **Netlify**: interfaz web desde la carpeta `CONEXIÓN JAVA/Biblioteca_San_Lucas_Fase2_PostgreSQL/biblioteca/web`.
- **Render, Web Service con runtime Docker**: servidor Java de la API; usa el `Dockerfile` de la raíz de este repositorio.
- **Neon PostgreSQL**: base de datos independiente de `127.0.0.1:5440`.

## Paso 1: crear PostgreSQL independiente en Neon

1. Crear un proyecto nuevo de PostgreSQL en Neon. No conectar al Docker local.
2. Crear una base de datos **exactamente** `biblioteca_san_lucas`. Neon también puede tener `neondb`; no usarla para este proyecto.
3. Conectarse a `biblioteca_san_lucas` desde el SQL Editor de Neon o DBeaver con las credenciales de Neon.
4. Verificar `SELECT current_database();` (debe devolver `biblioteca_san_lucas`).
5. Ejecutar **solo en esa base** el archivo existente `CONEXIÓN JAVA/Biblioteca_San_Lucas_Fase2_PostgreSQL/biblioteca/sql/02_crear_tablas_postgresql.sql`.
6. Verificar que existan `public.usuarios`, `public.libros`, `public.ejemplares` y `public.prestamos`.

## Paso 2: desplegar Java en Render

1. Crear **New → Web Service**, conectar el repositorio GitHub `marvel3289/PROYECTO-ING.-SOFTWARE`.
2. Runtime **Docker**; dejar **Root Directory vacío**, para que Render lea el `Dockerfile` de la raíz.
3. Antes de activar la aplicación, establecer las variables **solo en el panel de Render**:

   | Variable | Valor |
   | --- | --- |
   | `BIBLIOTECA_MODO` | `postgresql` |
   | `BIBLIOTECA_HOST` | `0.0.0.0` |
   | `BIBLIOTECA_PUBLICO` | `true` |
   | `BIBLIOTECA_DB_URL` | `jdbc:postgresql://HOST_DE_NEON:5432/biblioteca_san_lucas?sslmode=require` |
   | `BIBLIOTECA_DB_USER` | Usuario de conexión proporcionado por Neon |
   | `BIBLIOTECA_DB_PASSWORD` | Contraseña proporcionada por Neon, **secreta** |
   | `BIBLIOTECA_ADMIN_PASSWORD` | Contraseña **nueva y fuerte** de al menos 12 caracteres para el primer administrador |

   Render proporciona `PORT` automáticamente (el Dockerfile da 10000 como valor por defecto).
   Nunca agregar contraseñas ni URLs con credenciales al repositorio GitHub o a `netlify.toml`.

4. Desplegar. El servicio debe iniciar, mostrar `Persistencia activa: PostgreSQL` y publicar una URL HTTPS de Render.
5. En esa URL, `GET /api/me` debe devolver JSON con error `Inicia sesión para continuar.` y código **401**, porque todavía no hay sesión. Eso confirma que la API está viva.
6. No abrir el puerto de PostgreSQL local ni copiar sus contraseñas a la nube.

**Limitaciones del plan gratuito**: Render puede detener el servidor cuando no hay tráfico; al reactivarse puede tardar cerca de un minuto, y se perderán las sesiones guardadas en memoria (se requiere volver a iniciar sesión). La base Neon guarda los registros de manera separada. Verifica los límites vigentes de cada proveedor.

## Paso 3: conectar Netlify con la API

**Esperar a tener la URL real de Render.** No publicar un ejemplo con URL ficticia.

En el directorio `CONEXIÓN JAVA/Biblioteca_San_Lucas_Fase2_PostgreSQL/biblioteca/web` crear el archivo `_redirects` con esta única línea, **sustituyendo** `TU-SERVICIO` por el subdominio real asignado en Render:

```text
/api/* https://TU-SERVICIO.onrender.com/api/:splat 200
```

Subir ese archivo a GitHub, esperar la publicación automática de Netlify y abrir `https://biblioteca-san-lucas.netlify.app/`.

Esto permite que el navegador acceda a `/api/...` en su propio dominio mientras Netlify redirige internamente esas peticiones a Java, evitando contraseñas en el JavaScript.

## Paso 4: pruebas de extremo a extremo

1. Iniciar sesión con `admin@biblioteca.gt` y la contraseña inicial elegida en Render.
2. Registrar un libro y un ejemplar de prueba.
3. Registrar un lector, hacer un préstamo y devolverlo.
4. Recargar la página y comprobar los datos. Al reiniciarse el servicio Java puede pedir iniciar sesión otra vez, pero los datos deben seguir en Neon.
5. Confirmar en DBeaver o Neon que los registros existen solo en la **base en la nube**, no en los proyectos locales.

## Seguridad antes de un uso real

Este código usa PBKDF2 para las contraseñas y cookies HttpOnly/SameSite con `Secure` en modo público. Aun así, el servidor fue creado como prototipo académico y requiere revisión de seguridad adicional (controles anti-fuerza-bruta, protección de peticiones, monitoreo, copias de respaldo, políticas de privacidad y gestión de sesiones) antes de manejar datos personales en producción.

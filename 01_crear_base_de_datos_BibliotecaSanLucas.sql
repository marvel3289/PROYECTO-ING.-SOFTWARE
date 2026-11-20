/* PROYECTO INTEGRADOR: Sistema de Gestión Bibliotecaria – San Lucas Sacatepéquez.
   SQL Server. Ejecutar en SSMS/Azure Data Studio con cuenta que pueda crear BD.
   Reejecutable: no elimina tablas ni registros existentes.
   El comando GO es del cliente SSMS/sqlcmd, NO es SQL JDBC. */
IF DB_ID(N'BibliotecaSanLucas') IS NULL
BEGIN
    CREATE DATABASE BibliotecaSanLucas;
END;
GO
USE BibliotecaSanLucas;
GO
IF OBJECT_ID(N'dbo.usuarios', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.usuarios (
        id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_usuarios PRIMARY KEY,
        nombre NVARCHAR(150) NOT NULL,
        correo NVARCHAR(254) NOT NULL,
        sal VARCHAR(64) NOT NULL,
        hash_contrasena VARCHAR(128) NOT NULL,
        rol VARCHAR(20) NOT NULL,
        activo BIT NOT NULL CONSTRAINT DF_usuarios_activo DEFAULT (1),
        creado_en DATETIME2 NOT NULL CONSTRAINT DF_usuarios_creado DEFAULT SYSUTCDATETIME(),
        CONSTRAINT UQ_usuarios_correo UNIQUE (correo),
        CONSTRAINT CK_usuarios_rol CHECK (rol IN ('ADMINISTRADOR','OPERADOR','LECTOR')),
        CONSTRAINT CK_usuarios_nombre CHECK (LEN(LTRIM(RTRIM(nombre))) > 0)
    );
END;
GO
IF OBJECT_ID(N'dbo.libros', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.libros (
        id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_libros PRIMARY KEY,
        titulo NVARCHAR(300) NOT NULL,
        autor NVARCHAR(200) NOT NULL,
        isbn NVARCHAR(32) NOT NULL,
        CONSTRAINT UQ_libros_isbn UNIQUE (isbn)
    );
END;
GO
IF OBJECT_ID(N'dbo.ejemplares', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ejemplares (
        id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ejemplares PRIMARY KEY,
        libro_id INT NOT NULL,
        codigo NVARCHAR(60) NOT NULL,
        disponible BIT NOT NULL CONSTRAINT DF_ejemplares_disponible DEFAULT (1),
        CONSTRAINT FK_ejemplares_libros FOREIGN KEY (libro_id) REFERENCES dbo.libros(id),
        CONSTRAINT UQ_ejemplares_codigo UNIQUE (codigo)
    );
END;
GO
IF OBJECT_ID(N'dbo.prestamos', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.prestamos (
        id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_prestamos PRIMARY KEY,
        usuario_id INT NOT NULL,
        ejemplar_id INT NOT NULL,
        fecha_prestamo DATE NOT NULL,
        fecha_vencimiento DATE NOT NULL,
        fecha_devolucion DATE NULL,
        CONSTRAINT FK_prestamos_usuarios FOREIGN KEY (usuario_id) REFERENCES dbo.usuarios(id),
        CONSTRAINT FK_prestamos_ejemplares FOREIGN KEY (ejemplar_id) REFERENCES dbo.ejemplares(id),
        CONSTRAINT CK_prestamos_vencimiento CHECK (fecha_vencimiento >= fecha_prestamo),
        CONSTRAINT CK_prestamos_devolucion CHECK (fecha_devolucion IS NULL OR fecha_devolucion >= fecha_prestamo)
    );
END;
GO
/* Dos préstamos activos sobre el mismo ejemplar están prohibidos por el motor. */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'UX_prestamos_ejemplar_activo' AND object_id=OBJECT_ID(N'dbo.prestamos'))
    CREATE UNIQUE INDEX UX_prestamos_ejemplar_activo ON dbo.prestamos(ejemplar_id) WHERE fecha_devolucion IS NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_prestamos_usuario' AND object_id=OBJECT_ID(N'dbo.prestamos'))
    CREATE INDEX IX_prestamos_usuario ON dbo.prestamos(usuario_id);
GO
/* Verificación rápida de tablas y contenido */
SELECT N'usuarios' AS tabla, COUNT(*) AS registros FROM dbo.usuarios
UNION ALL SELECT N'libros', COUNT(*) FROM dbo.libros
UNION ALL SELECT N'ejemplares', COUNT(*) FROM dbo.ejemplares
UNION ALL SELECT N'prestamos', COUNT(*) FROM dbo.prestamos;
GO

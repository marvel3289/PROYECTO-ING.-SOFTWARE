package biblioteca.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;

/** JDBC SQL Server; sin dependencias de compilación. El driver debe estar en lib/ al EJECUTAR. */
public final class SqlServer implements UnidadTrabajo {
    @FunctionalInterface public interface Consulta<T> { T ejecutar(Connection conexion) throws SQLException; }

    private final String url, usuario, clave;
    private final ThreadLocal<Connection> transaccionActual = new ThreadLocal<>();

    public SqlServer(String url, String usuario, String clave) {
        if (url == null || !url.startsWith("jdbc:sqlserver://"))
            throw new IllegalArgumentException("BIBLIOTECA_DB_URL debe ser una URL JDBC de SQL Server.");
        if (usuario == null || usuario.isBlank() || clave == null || clave.isBlank())
            throw new IllegalArgumentException("Defina BIBLIOTECA_DB_USER y BIBLIOTECA_DB_PASSWORD.");
        this.url = url; this.usuario = usuario; this.clave = clave;
        try { Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver"); }
        catch (ClassNotFoundException e) {
            throw new IllegalStateException("Falta el driver Microsoft JDBC. Copie mssql-jdbc-*.jar a biblioteca/lib y use iniciar_sqlserver.bat.", e);
        }
    }

    public static SqlServer desdeEntorno() {
        String url = System.getenv().getOrDefault("BIBLIOTECA_DB_URL", 
                "jdbc:sqlserver://localhost:1433;databaseName=BibliotecaSanLucas;encrypt=true;trustServerCertificate=true");
        return new SqlServer(url, System.getenv("BIBLIOTECA_DB_USER"), System.getenv("BIBLIOTECA_DB_PASSWORD"));
    }

    private Connection abrir() throws SQLException { return DriverManager.getConnection(url, usuario, clave); }

    /** Las consultas dentro de una transacción comparten la misma conexión. */
    public <T> T consultar(Consulta<T> trabajo) {
        Connection actual = transaccionActual.get();
        if (actual != null) {
            try { return trabajo.ejecutar(actual); }
            catch (SQLException e) { throw new IllegalStateException("Error SQL en transacción: " + e.getMessage(), e); }
        }
        try (Connection conexion = abrir()) { return trabajo.ejecutar(conexion); }
        catch (SQLException e) { throw new IllegalStateException("Error de conexión u operación SQL: " + e.getMessage(), e); }
    }

    @Override public <T> T ejecutar(Supplier<T> operacion) {
        // Una transacción anidada se incorpora a la exterior.
        if (transaccionActual.get() != null) return operacion.get();
        try (Connection conexion = abrir()) {
            conexion.setAutoCommit(false);
            transaccionActual.set(conexion);
            try {
                T resultado = operacion.get();
                conexion.commit();
                return resultado;
            } catch (RuntimeException | Error e) {
                try { conexion.rollback(); } catch (SQLException falloRollback) { e.addSuppressed(falloRollback); }
                throw e;
            } catch (SQLException e) {
                try { conexion.rollback(); } catch (SQLException falloRollback) { e.addSuppressed(falloRollback); }
                throw new IllegalStateException("No se confirmó la operación SQL.", e);
            } finally { transaccionActual.remove(); }
        } catch (SQLException e) { throw new IllegalStateException("No se pudo iniciar la transacción SQL: " + e.getMessage(), e); }
    }

    public void comprobarEsquema() {
        consultar(conexion -> {
            try (Statement st = conexion.createStatement()) {
                for (String tabla : new String[]{"usuarios", "libros", "ejemplares", "prestamos"}) {
                    try (var rs = st.executeQuery("SELECT TOP (1) id FROM dbo." + tabla)) { /* comprueba la tabla */ }
                }
            }
            return null;
        });
    }
}

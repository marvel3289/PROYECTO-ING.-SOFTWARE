package biblioteca.sql;

import biblioteca.modelo.*;
import biblioteca.repositorio.Repositorio;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Repositorio JDBC de entidades con INSERT/UPDATE mediante parámetros preparados. */
public final class RepositorioPostgreSql<T extends Entidad> implements Repositorio<T> {
    @FunctionalInterface private interface Lector<E> { E leer(ResultSet rs) throws SQLException; }
    @FunctionalInterface private interface Escritor<E> { void escribir(PreparedStatement ps, E e) throws SQLException; }

    private final PostgreSql sql;
    private final String tabla, columnas, columnasUpdate, placeholders;
    private final Lector<T> lector;
    private final Escritor<T> escritor;

    private RepositorioPostgreSql(PostgreSql sql, String tabla, String columnas,
                                String columnasUpdate, String placeholders,
                                Lector<T> lector, Escritor<T> escritor) {
        this.sql=sql; this.tabla=tabla; this.columnas=columnas;
        this.columnasUpdate=columnasUpdate; this.placeholders=placeholders;
        this.lector=lector; this.escritor=escritor;
    }

    public static RepositorioPostgreSql<Usuario> usuarios(PostgreSql sql) {
        return new RepositorioPostgreSql<>(sql, "usuarios", "nombre, correo, sal, hash_contrasena, rol, activo",
            "nombre=?, correo=?, sal=?, hash_contrasena=?, rol=?, activo=?", "?, ?, ?, ?, ?, ?",
            rs -> {
                Usuario u = new Usuario(rs.getString("nombre"), rs.getString("correo"), rs.getString("sal"),
                    rs.getString("hash_contrasena"), Rol.valueOf(rs.getString("rol")));
                u.setId(rs.getInt("id")); u.setActivo(rs.getBoolean("activo")); return u;
            },
            (ps,u) -> { ps.setString(1,u.getNombre()); ps.setString(2,u.getCorreo());
                ps.setString(3,u.getSal()); ps.setString(4,u.getHash());
                ps.setString(5,u.getRol().name()); ps.setBoolean(6,u.isActivo()); });
    }
    public static RepositorioPostgreSql<Libro> libros(PostgreSql sql) {
        return new RepositorioPostgreSql<>(sql, "libros", "titulo, autor, isbn",
            "titulo=?, autor=?, isbn=?", "?, ?, ?",
            rs -> { Libro l = new Libro(rs.getString("titulo"),rs.getString("autor"),rs.getString("isbn"));
                l.setId(rs.getInt("id")); return l; },
            (ps,l) -> { ps.setString(1,l.getTitulo()); ps.setString(2,l.getAutor()); ps.setString(3,l.getIsbn()); });
    }
    public static RepositorioPostgreSql<Ejemplar> ejemplares(PostgreSql sql) {
        return new RepositorioPostgreSql<>(sql, "ejemplares", "libro_id, codigo, disponible",
            "libro_id=?, codigo=?, disponible=?", "?, ?, ?",
            rs -> { Ejemplar e = new Ejemplar(rs.getInt("libro_id"),rs.getString("codigo"));
                e.setId(rs.getInt("id")); e.setDisponible(rs.getBoolean("disponible")); return e; },
            (ps,e) -> { ps.setInt(1,e.getLibroId()); ps.setString(2,e.getCodigo()); ps.setBoolean(3,e.isDisponible()); });
    }
    public static RepositorioPostgreSql<Prestamo> prestamos(PostgreSql sql) {
        return new RepositorioPostgreSql<>(sql, "prestamos",
            "usuario_id, ejemplar_id, fecha_prestamo, fecha_vencimiento, fecha_devolucion",
            "usuario_id=?, ejemplar_id=?, fecha_prestamo=?, fecha_vencimiento=?, fecha_devolucion=?",
            "?, ?, ?, ?, ?",
            rs -> {
                Prestamo p = new Prestamo(rs.getInt("usuario_id"),rs.getInt("ejemplar_id"),
                    rs.getDate("fecha_prestamo").toLocalDate(),rs.getDate("fecha_vencimiento").toLocalDate());
                p.setId(rs.getInt("id"));
                Date retorno = rs.getDate("fecha_devolucion");
                if (retorno != null) p.setFechaDevolucion(retorno.toLocalDate());
                return p;
            },
            (ps,p) -> { ps.setInt(1,p.getUsuarioId()); ps.setInt(2,p.getEjemplarId());
                ps.setDate(3,Date.valueOf(p.getFechaPrestamo())); ps.setDate(4,Date.valueOf(p.getFechaVencimiento()));
                if (p.getFechaDevolucion()==null) ps.setNull(5,Types.DATE);
                else ps.setDate(5,Date.valueOf(p.getFechaDevolucion())); });
    }

    @Override public T guardar(T entidad) {
        if (entidad == null) throw new IllegalArgumentException("Entidad nula");
        return sql.consultar(con -> {
            if (entidad.getId() == 0) {
                String sentencia = "INSERT INTO public." + tabla + " (" + columnas + ") VALUES (" + placeholders + ") RETURNING id";
                try (PreparedStatement ps = con.prepareStatement(sentencia)) {
                    escritor.escribir(ps, entidad);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("INSERT sin id generado en " + tabla);
                        entidad.setId(rs.getInt(1));
                    }
                }
            } else {
                String sentencia = "UPDATE public." + tabla + " SET " + columnasUpdate + " WHERE id=?";
                try (PreparedStatement ps = con.prepareStatement(sentencia)) {
                    escritor.escribir(ps,entidad);
                    ps.setInt(cantidadColumnas()+1, entidad.getId());
                    if (ps.executeUpdate()!=1) throw new SQLException("No existe la entidad " + tabla + " #" + entidad.getId());
                }
            }
            return entidad;
        });
    }
    private int cantidadColumnas() { return (int) columnas.chars().filter(ch -> ch==',').count()+1; }

    @Override public Optional<T> buscarPorId(int id) {
        return sql.consultar(con -> {
            try (PreparedStatement ps = con.prepareStatement("SELECT id, " + columnas + " FROM public." + tabla + " WHERE id=?")) {
                ps.setInt(1,id);
                try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(lector.leer(rs)) : Optional.empty(); }
            }
        });
    }
    @Override public List<T> todos() {
        return sql.consultar(con -> {
            List<T> resultado=new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement("SELECT id, " + columnas + " FROM public." + tabla + " ORDER BY id")) {
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) resultado.add(lector.leer(rs)); }
            }
            return resultado;
        });
    }
}

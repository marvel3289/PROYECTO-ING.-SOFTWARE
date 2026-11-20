package biblioteca.web;

import biblioteca.modelo.*;
import biblioteca.servicio.*;
import biblioteca.repositorio.Repositorio;
import biblioteca.sql.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** API HTTP + interfaz estática. Detrás de un proxy HTTPS puede servir un despliegue de pruebas en la nube. */
public final class WebServidor {
    private static final boolean PUBLICO = "true".equalsIgnoreCase(
            System.getenv().getOrDefault("BIBLIOTECA_PUBLICO", "false"));
    private final UsuarioServicio usuarios;
    private final CatalogoServicio catalogo;
    private final PrestamoServicio prestamos;
    private final Almacen almacen;
    public record Repositorios(Repositorio<Usuario> usuarios, Repositorio<Libro> libros,
                               Repositorio<Ejemplar> ejemplares, Repositorio<Prestamo> prestamos) {}
    private final Repositorios estado;
    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();
    private final SecureRandom aleatorio = new SecureRandom();
    private final Object candado = new Object();
    private final Path web;
    private record Sesion(int usuarioId, Instant expira) {}

    public WebServidor(Almacen almacen, Repositorios estado, UnidadTrabajo unidadTrabajo, Path web) {
        this.almacen = almacen;
        this.estado = estado;
        this.web = web.toAbsolutePath().normalize();
        this.usuarios = new UsuarioServicio(estado.usuarios());
        this.catalogo = new CatalogoServicio(estado.libros(), estado.ejemplares());
        this.prestamos = new PrestamoServicio(estado.prestamos(), catalogo, usuarios,
                                             Clock.systemDefaultZone(), unidadTrabajo);
    }

    public static void main(String[] args) throws Exception {
        int puerto = Integer.parseInt(System.getenv().getOrDefault("BIBLIOTECA_PUERTO",
                System.getenv().getOrDefault("PORT", "8081")));
        String host = System.getenv().getOrDefault("BIBLIOTECA_HOST", "127.0.0.1");
        Path base = localizarProyecto();
        String modo = System.getenv().getOrDefault("BIBLIOTECA_MODO", "archivo").toLowerCase(Locale.ROOT);
        if (PUBLICO && !modo.equals("postgresql"))
            throw new IllegalStateException("El despliegue público exige BIBLIOTECA_MODO=postgresql; no se permiten datos de demostración.");
        if (PUBLICO && (System.getenv("BIBLIOTECA_DB_URL") == null
                || System.getenv("BIBLIOTECA_DB_URL").isBlank()))
            throw new IllegalStateException("Configure BIBLIOTECA_DB_URL para la base PostgreSQL remota.");
        WebServidor app;
        if (modo.equals("postgresql")) {
            PostgreSql sql = PostgreSql.desdeEntorno();
            sql.comprobarEsquema();
            Repositorios r = new Repositorios(RepositorioPostgreSql.usuarios(sql),
                    RepositorioPostgreSql.libros(sql), RepositorioPostgreSql.ejemplares(sql),
                    RepositorioPostgreSql.prestamos(sql));
            if (r.usuarios().todos().isEmpty()) {
                String claveInicial = System.getenv("BIBLIOTECA_ADMIN_PASSWORD");
                if (claveInicial == null || claveInicial.length() < 12)
                    throw new IllegalStateException("La base no tiene usuarios. Establezca BIBLIOTECA_ADMIN_PASSWORD (12 caracteres como mínimo) para crear el administrador inicial.");
                new UsuarioServicio(r.usuarios()).crearAdministradorInicial(
                        "Administrador", "admin@biblioteca.gt", claveInicial);
                System.out.println("Administrador inicial creado en PostgreSQL.");
            }
            app = new WebServidor(null, r, sql, base.resolve("web"));
            System.out.println("Persistencia activa: PostgreSQL (no se usa biblioteca.dat).");
        } else if (modo.equals("sqlserver")) {
            SqlServer sql = SqlServer.desdeEntorno();
            sql.comprobarEsquema();
            Repositorios r = new Repositorios(RepositorioSqlServer.usuarios(sql),
                    RepositorioSqlServer.libros(sql), RepositorioSqlServer.ejemplares(sql),
                    RepositorioSqlServer.prestamos(sql));
            if (r.usuarios().todos().isEmpty()) {
                String claveInicial = System.getenv("BIBLIOTECA_ADMIN_PASSWORD");
                if (claveInicial == null || claveInicial.length() < 12)
                    throw new IllegalStateException("La BD no tiene usuarios. Establezca BIBLIOTECA_ADMIN_PASSWORD (al menos 12 caracteres) para crear el administrador inicial.");
                new UsuarioServicio(r.usuarios()).crearAdministradorInicial(
                        "Administrador", "admin@biblioteca.gt", claveInicial);
                System.out.println("Administrador inicial creado en SQL Server. Cambie sus credenciales después del primer ingreso.");
            }
            app = new WebServidor(null, r, sql, base.resolve("web"));
            System.out.println("Persistencia activa: SQL Server (no se utiliza biblioteca.dat).");
        } else if (modo.equals("archivo")) {
            Path datos = Path.of(System.getenv().getOrDefault("BIBLIOTECA_DATOS", base.resolve("datos/biblioteca.dat").toString()));
            Almacen almacen = new Almacen(datos);
            Almacen.Estado estado = almacen.cargar();
            if (estado.usuarios().todos().isEmpty()) {
                UsuarioServicio us = new UsuarioServicio(estado.usuarios());
                var admin = us.crearAdministradorInicial("Administrador", "admin@biblioteca.gt", "Admin12345");
                us.registrar(admin, "Lector de ejemplo", "lector@biblioteca.gt", "Lector12345", Rol.LECTOR);
                var cs = new CatalogoServicio(estado.libros(), estado.ejemplares());
                Libro libro = cs.registrarLibro(admin, "Cien años de soledad", "Gabriel García Márquez", "978-0307474728");
                cs.agregarEjemplar(admin, libro.getId(), "CAS-001");
                almacen.guardar(estado);
                System.out.println("Datos de demostración inicializados. Cambie las contraseñas antes de cualquier despliegue real.");
            }
            Repositorios r = new Repositorios(estado.usuarios(), estado.libros(), estado.ejemplares(), estado.prestamos());
            app = new WebServidor(almacen, r, UnidadTrabajo.SIN_TRANSACCION, base.resolve("web"));
        } else throw new IllegalArgumentException("BIBLIOTECA_MODO debe ser archivo, postgresql o sqlserver.");
        HttpServer server = HttpServer.create(new InetSocketAddress(host, puerto), 0);
        server.createContext("/", app::atender);
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("Biblioteca Web disponible en http://" + host + ":" + puerto + "/");
        if (!host.equals("127.0.0.1") && !host.equals("localhost"))
            System.err.println("IMPORTANTE: publique este servicio únicamente detrás de un proxy HTTPS y con contraseñas seguras.");
    }

    /** Encuentra la carpeta web tanto al ejecutar desde biblioteca como desde su carpeta padre (VS Code). */
    private static Path localizarProyecto() {
        Path actual = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        List<Path> opciones = new ArrayList<>();
        opciones.add(actual);
        opciones.add(actual.resolve("biblioteca"));
        try {
            Path codigo = Path.of(WebServidor.class.getProtectionDomain().getCodeSource()
                    .getLocation().toURI()).toAbsolutePath().normalize();
            Path carpeta = Files.isDirectory(codigo) ? codigo : codigo.getParent();
            if (carpeta != null) {
                opciones.add(carpeta);
                opciones.add(carpeta.resolve("biblioteca"));
                if (carpeta.getParent() != null) opciones.add(carpeta.getParent());
            }
        } catch (Exception ignorado) {
            // Se usaran las rutas relativas al directorio de ejecucion.
        }
        for (Path opcion : opciones) {
            if (Files.isRegularFile(opcion.resolve("web").resolve("index.html"))) {
                System.out.println("Archivos web: " + opcion.resolve("web").toAbsolutePath().normalize());
                return opcion;
            }
        }
        throw new IllegalStateException("No se encuentra web/index.html. "
            + "Extraiga el ZIP completo y abra la carpeta biblioteca en VS Code. "
            + "Directorio actual: " + actual);
    }

    private void atender(HttpExchange ex) throws IOException {
        try {
            ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            ex.getResponseHeaders().set("Referrer-Policy", "no-referrer");
            ex.getResponseHeaders().set("X-Frame-Options", "DENY");
            ex.getResponseHeaders().set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; object-src 'none'; base-uri 'none'; frame-ancestors 'none'");
            ex.getResponseHeaders().set("Cache-Control", "no-store");
            if (ex.getRequestURI().getPath().startsWith("/api/")) {
                synchronized (candado) { api(ex); }
            } else if (ex.getRequestMethod().equals("GET") || ex.getRequestMethod().equals("HEAD")) {
                estatico(ex);
            } else responder(ex, 405, "text/plain", "Método no permitido");
        } catch (BibliotecaException | IllegalArgumentException e) {
            responder(ex, 400, "application/json", json(Map.of("error", e.getMessage() == null ? "Solicitud inválida" : e.getMessage())));
        } catch (AccesoDenegado e) {
            responder(ex, e.codigo, "application/json", json(Map.of("error", e.getMessage())));
        } catch (Exception e) {
            e.printStackTrace();
            responder(ex, 500, "application/json", "{\"error\":\"Error interno del servidor.\"}");
        } finally { ex.close(); }
    }

    private static class AccesoDenegado extends RuntimeException {
        final int codigo;
        AccesoDenegado(int codigo, String mensaje) { super(mensaje); this.codigo = codigo; }
    }
    private static void exigirMetodo(HttpExchange ex, String metodo) {
        if (!ex.getRequestMethod().equals(metodo)) throw new AccesoDenegado(405, "Método no permitido.");
    }
    private static String campo(Map<String, String> campos, String nombre) {
        return campos.getOrDefault(nombre, "").trim();
    }
    private static int entero(Map<String, String> campos, String nombre) {
        try { return Integer.parseInt(campo(campos, nombre)); }
        catch (NumberFormatException e) { throw new BibliotecaException("Dato numérico inválido: " + nombre); }
    }
    private static Rol rol(Map<String, String> f) {
        try { return Rol.valueOf(campo(f, "rol")); }
        catch (IllegalArgumentException e) { throw new BibliotecaException("Rol inválido."); }
    }
    private Map<String, String> formulario(HttpExchange ex) throws IOException {
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("application/x-www-form-urlencoded"))
            throw new AccesoDenegado(415, "Se requiere formulario URL encoded.");
        byte[] datos = ex.getRequestBody().readNBytes(16_385);
        if (datos.length > 16_384) throw new AccesoDenegado(413, "Formulario demasiado grande.");
        Map<String, String> f = new HashMap<>();
        for (String parte : new String(datos, StandardCharsets.UTF_8).split("&")) {
            if (parte.isEmpty()) continue;
            String[] par = parte.split("=", 2);
            f.put(URLDecoder.decode(par[0], StandardCharsets.UTF_8),
                  URLDecoder.decode(par.length > 1 ? par[1] : "", StandardCharsets.UTF_8));
        }
        return f;
    }
    private Usuario actor(HttpExchange ex) {
        String cookie = ex.getRequestHeaders().getFirst("Cookie");
        if (cookie == null) throw new AccesoDenegado(401, "Inicia sesión para continuar.");
        String token = null;
        for (String segmento : cookie.split(";")) {
            String c = segmento.trim();
            if (c.startsWith("BIBSESION=")) token = c.substring("BIBSESION=".length());
        }
        Sesion sesion = token == null ? null : sesiones.get(token);
        if (sesion == null || Instant.now().isAfter(sesion.expira())) {
            if (token != null) sesiones.remove(token);
            throw new AccesoDenegado(401, "Tu sesión ha vencido.");
        }
        Usuario u = usuarios.buscarPorId(sesion.usuarioId());
        if (!u.isActivo()) throw new AccesoDenegado(401, "Usuario inactivo.");
        return u;
    }
    private static void personal(Usuario actor) {
        if (actor.getRol() == Rol.LECTOR) throw new AccesoDenegado(403, "No tienes permiso para esta acción.");
    }
    private static void admin(Usuario actor) {
        if (actor.getRol() != Rol.ADMINISTRADOR) throw new AccesoDenegado(403, "Solo administradores.");
    }
    /** Modo JDBC persiste en la base de datos; modo archivo conserva guardado de demo. */
    private void modificar() throws IOException {
        if (almacen != null) {
            // Los repositorios en memoria son exactamente los del estado cargado.
            almacen.guardar(new Almacen.Estado(
                (biblioteca.repositorio.RepositorioMemoria<Usuario>) estado.usuarios(),
                (biblioteca.repositorio.RepositorioMemoria<Libro>) estado.libros(),
                (biblioteca.repositorio.RepositorioMemoria<Ejemplar>) estado.ejemplares(),
                (biblioteca.repositorio.RepositorioMemoria<Prestamo>) estado.prestamos()));
        }
    }

    private void api(HttpExchange ex) throws IOException {
        String ruta = ex.getRequestURI().getPath();
        Object respuesta;
        int codigo = 200;
        if (ruta.equals("/api/login")) {
            exigirMetodo(ex, "POST");
            var f = formulario(ex);
            Usuario u = usuarios.autenticar(campo(f, "correo"), f.getOrDefault("contrasena", ""));
            byte[] bytes = new byte[32]; aleatorio.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            sesiones.put(token, new Sesion(u.getId(), Instant.now().plusSeconds(8 * 3600)));
            ex.getResponseHeaders().add("Set-Cookie", "BIBSESION=" + token + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=28800" + (PUBLICO ? "; Secure" : ""));
            respuesta = usuario(u);
        } else if (ruta.equals("/api/logout")) {
            exigirMetodo(ex, "POST");
            String cookie = ex.getRequestHeaders().getFirst("Cookie");
            if (cookie != null) for (String c : cookie.split(";")) {
                c = c.trim(); if (c.startsWith("BIBSESION=")) sesiones.remove(c.substring(10));
            }
            ex.getResponseHeaders().add("Set-Cookie", "BIBSESION=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0" + (PUBLICO ? "; Secure" : ""));
            respuesta = Map.of("ok", true);
        } else {
            Usuario u = actor(ex);
            switch (ruta) {
                case "/api/me" -> { exigirMetodo(ex, "GET"); respuesta = usuario(u); }
                case "/api/libros" -> {
                    exigirMetodo(ex, "GET");
                    String query = parametro(ex, "q");
                    respuesta = catalogo.buscar(query).stream().map(r -> Map.of(
                        "id", r.libro().getId(), "titulo", r.libro().getTitulo(),
                        "autor", r.libro().getAutor(), "isbn", r.libro().getIsbn(),
                        "disponibles", r.disponibles(), "total", r.total())).toList();
                }
                case "/api/libros/crear" -> {
                    exigirMetodo(ex, "POST"); personal(u);
                    var f = formulario(ex);
                    Libro l = catalogo.registrarLibro(u, campo(f,"titulo"), campo(f,"autor"), campo(f,"isbn"));
                    modificar(); respuesta = Map.of("id", l.getId()); codigo = 201;
                }
                case "/api/ejemplares" -> {
                    exigirMetodo(ex, "GET");
                    personal(u);
                    respuesta = estado.ejemplares().todos().stream().map(e -> Map.of("id",e.getId(),
                        "libroId",e.getLibroId(), "codigo",e.getCodigo(), "disponible",e.isDisponible())).toList();
                }
                case "/api/ejemplares/crear" -> {
                    exigirMetodo(ex, "POST"); personal(u);
                    var f = formulario(ex);
                    Ejemplar e = catalogo.agregarEjemplar(u, entero(f,"libroId"), campo(f,"codigo"));
                    modificar(); respuesta = Map.of("id",e.getId()); codigo = 201;
                }
                case "/api/usuarios" -> {
                    exigirMetodo(ex, "GET"); admin(u);
                    respuesta = usuarios.listar(u).stream().map(WebServidor::usuario).toList();
                }
                case "/api/usuarios/crear" -> {
                    exigirMetodo(ex, "POST"); admin(u);
                    var f = formulario(ex);
                    Usuario nuevo = usuarios.registrar(u, campo(f,"nombre"),campo(f,"correo"),
                            f.get("contrasena"),rol(f));
                    modificar(); respuesta = usuario(nuevo); codigo = 201;
                }
                case "/api/prestamos" -> {
                    exigirMetodo(ex, "GET");
                    String filtro = parametro(ex, "usuarioId");
                    if (u.getRol() == Rol.LECTOR || (filtro != null && !filtro.isBlank())) {
                        int id = u.getRol() == Rol.LECTOR ? u.getId() : idOUsuario(u, filtro);
                        respuesta = prestamos.historial(u, id).stream().map(this::prestamo).toList();
                    } else {
                        respuesta = estado.prestamos().todos().stream().map(this::prestamo).toList();
                    }
                }
                case "/api/prestamos/crear" -> {
                    exigirMetodo(ex, "POST"); personal(u);
                    var f = formulario(ex);
                    Prestamo p = prestamos.prestar(u, entero(f,"usuarioId"),campo(f,"codigo"));
                    modificar(); respuesta = prestamo(p); codigo = 201;
                }
                case "/api/prestamos/devolver" -> {
                    exigirMetodo(ex, "POST"); personal(u);
                    Prestamo p = prestamos.devolver(u, entero(formulario(ex),"prestamoId"));
                    modificar(); respuesta = prestamo(p);
                }
                case "/api/alertas" -> {
                    exigirMetodo(ex, "GET"); personal(u);
                    respuesta = prestamos.alertas(u).stream().map(a -> {
                        Map<String,Object> m = new LinkedHashMap<>(prestamo(a.prestamo()));
                        m.put("vencido", a.vencido()); return m;
                    }).toList();
                }
                case "/api/resumen" -> {
                    exigirMetodo(ex, "GET");
                    var ls = catalogo.buscar("");
                    long total = ls.stream().mapToLong(r -> r.total()).sum();
                    long libres = ls.stream().mapToLong(r -> r.disponibles()).sum();
                    long activos = estado.prestamos().todos().stream().filter(p -> p.getFechaDevolucion() == null).count();
                    respuesta = Map.of("titulos",ls.size(),"ejemplares",total,"disponibles",libres,
                        "prestamosActivos",activos);
                }
                default -> throw new AccesoDenegado(404,"Ruta no encontrada.");
            }
        }
        responder(ex, codigo, "application/json", json(respuesta));
    }
    private static int idOUsuario(Usuario u, String id) {
        if (id == null || id.isBlank()) return u.getId();
        try { return Integer.parseInt(id); } catch (NumberFormatException e) { throw new BibliotecaException("ID de usuario inválido."); }
    }
    private static String parametro(HttpExchange ex, String nombre) {
        String q = ex.getRequestURI().getRawQuery();
        if (q == null) return "";
        for (String p : q.split("&")) {
            String[] par = p.split("=",2);
            if (URLDecoder.decode(par[0],StandardCharsets.UTF_8).equals(nombre))
                return URLDecoder.decode(par.length > 1 ? par[1] : "", StandardCharsets.UTF_8);
        }
        return "";
    }
    private static Map<String,Object> usuario(Usuario u) {
        return Map.of("id",u.getId(),"nombre",u.getNombre(),"correo",u.getCorreo(),
                      "rol",u.getRol().toString(),"activo",u.isActivo());
    }
    private Map<String,Object> prestamo(Prestamo p) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",p.getId()); m.put("usuarioId",p.getUsuarioId());
        m.put("ejemplarId",p.getEjemplarId()); m.put("libro",prestamos.describir(p));
        m.put("fechaPrestamo",p.getFechaPrestamo().toString());
        m.put("fechaVencimiento",p.getFechaVencimiento().toString());
        m.put("fechaDevolucion",p.getFechaDevolucion()==null ? null : p.getFechaDevolucion().toString());
        m.put("estado",prestamos.estado(p).toString()); return m;
    }
    private void estatico(HttpExchange ex) throws IOException {
        String ruta = ex.getRequestURI().getPath();
        if (ruta.equals("/")) ruta = "/index.html";
        Path archivo = web.resolve(ruta.substring(1)).normalize();
        if (!archivo.startsWith(web) || !Files.isRegularFile(archivo)) {
            responder(ex,404,"text/plain","No encontrado"); return;
        }
        String tipo = ruta.endsWith(".css") ? "text/css" : ruta.endsWith(".js") ? "text/javascript" : "text/html";
        byte[] cuerpo = Files.readAllBytes(archivo);
        ex.getResponseHeaders().set("Content-Type",tipo+"; charset=utf-8");
        ex.sendResponseHeaders(200, ex.getRequestMethod().equals("HEAD") ? -1 : cuerpo.length);
        if (!ex.getRequestMethod().equals("HEAD")) ex.getResponseBody().write(cuerpo);
    }
    private static void responder(HttpExchange ex, int codigo, String tipo, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type",tipo+"; charset=utf-8");
        ex.sendResponseHeaders(codigo, bytes.length);
        ex.getResponseBody().write(bytes);
    }
    /** Generación JSON mínima: valores de nuestra aplicación, nunca inserción sin escapar. */
    private static String json(Object o) {
        if (o==null) return "null";
        if (o instanceof Number || o instanceof Boolean) return o.toString();
        if (o instanceof String s) {
            StringBuilder b = new StringBuilder("\"");
            for (char c : s.toCharArray()) {
                switch(c) {
                    case '"' -> b.append("\\\""); case '\\' -> b.append("\\\\");
                    case '\n' -> b.append("\\n"); case '\r' -> b.append("\\r");
                    case '\t' -> b.append("\\t");
                    default -> { if (c < 32) b.append(String.format("\\u%04x",(int)c)); else b.append(c); }
                }
            }
            return b.append('"').toString();
        }
        if (o instanceof Map<?,?> mapa) {
            List<String> items = new ArrayList<>();
            mapa.forEach((k,v) -> items.add(json(k.toString())+":"+json(v)));
            return "{"+String.join(",",items)+"}";
        }
        if (o instanceof Iterable<?> lista) {
            List<String> items = new ArrayList<>();
            for (Object v : lista) items.add(json(v));
            return "["+String.join(",",items)+"]";
        }
        throw new IllegalArgumentException("Tipo JSON no admitido");
    }
}

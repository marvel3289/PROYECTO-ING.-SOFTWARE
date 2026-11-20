package biblioteca;

import biblioteca.modelo.*;
import biblioteca.repositorio.RepositorioMemoria;
import biblioteca.servicio.*;
import java.time.*;

/** Pruebas sin dependencias externas. Ejecutar: java -cp out biblioteca.PruebasBasicas */
public class PruebasBasicas {
    private static int fallos = 0;

    private static void esperarError(String nombre, Runnable accion) {
        try { accion.run(); fallos++; System.out.println("FALLÓ  " + nombre + " (no lanzó error)"); }
        catch (BibliotecaException e) { System.out.println("OK     " + nombre + " → " + e.getMessage()); }
    }
    private static void verificar(String nombre, boolean condicion) {
        if (!condicion) fallos++;
        System.out.println((condicion ? "OK     " : "FALLÓ  ") + nombre);
    }

    public static void main(String[] args) {
        var us = new UsuarioServicio(new RepositorioMemoria<Usuario>());
        var cs = new CatalogoServicio(new RepositorioMemoria<Libro>(), new RepositorioMemoria<Ejemplar>());
        var ps = new PrestamoServicio(new RepositorioMemoria<Prestamo>(), cs, us, Clock.systemDefaultZone());

        Usuario admin = us.crearAdministradorInicial("Admin", "admin@x.gt", "Admin12345");
        Usuario ana = us.registrar(admin, "Ana", "ana@x.gt", "Clave12345", Rol.LECTOR);
        Usuario luis = us.registrar(admin, "Luis", "luis@x.gt", "Clave12345", Rol.LECTOR);
        Libro libro = cs.registrarLibro(admin, "Libro", "Autor", "111");
        cs.agregarEjemplar(admin, libro.getId(), "E-1");

        verificar("P-01 login válido", us.autenticar("admin@x.gt", "Admin12345").getRol() == Rol.ADMINISTRADOR);
        esperarError("P-02 contraseña incorrecta", () -> us.autenticar("admin@x.gt", "mala"));
        esperarError("P-03 lector intenta función administrativa", () -> cs.registrarLibro(ana, "T", "A", "222"));
        esperarError("P-05 usuario duplicado", () -> us.registrar(admin, "Ana 2", "ANA@x.gt", "Clave12345", Rol.LECTOR));
        esperarError("P-09 libro sin datos obligatorios", () -> cs.registrarLibro(admin, "", "A", "333"));
        verificar("P-11 búsqueda sin resultados", cs.buscar("zzz").isEmpty());

        Prestamo p = ps.prestar(admin, ana.getId(), "E-1");
        verificar("P-12 préstamo deja el ejemplar no disponible", cs.buscar("Libro").get(0).disponibles() == 0);
        esperarError("P-13 prestar ejemplar ya prestado", () -> ps.prestar(admin, luis.getId(), "E-1"));
        esperarError("P-16 historial ajeno", () -> ps.historial(luis, ana.getId()));
        verificar("P-15 historial propio", ps.historial(ana, ana.getId()).size() == 1);

        ps.devolver(admin, p.getId());
        verificar("P-17 devolución libera el ejemplar", cs.buscar("Libro").get(0).disponibles() == 1);
        esperarError("P-18 devolver dos veces", () -> ps.devolver(admin, p.getId()));

        // Vencimiento: reloj 10 días en el futuro (P-19 / P-20)
        var futuro = Clock.offset(Clock.systemDefaultZone(), Duration.ofDays(10));
        var ps2 = new PrestamoServicio(new RepositorioMemoria<Prestamo>(), cs, us, Clock.systemDefaultZone());
        Prestamo p2 = ps2.prestar(admin, luis.getId(), "E-1");
        var psFuturo = new PrestamoServicio(new RepositorioMemoria<Prestamo>() {
            @Override public java.util.List<Prestamo> todos() { return java.util.List.of(p2); }
        }, cs, us, futuro);
        verificar("P-19 préstamo vencido genera alerta", psFuturo.alertas(admin).get(0).vencido());

        System.out.println(fallos == 0 ? "\nTodas las pruebas pasaron." : "\nPruebas fallidas: " + fallos);
        System.exit(fallos == 0 ? 0 : 1);
    }
}

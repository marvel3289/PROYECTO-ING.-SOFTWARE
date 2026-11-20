package biblioteca;

import biblioteca.modelo.*;
import biblioteca.servicio.*;
import java.util.Scanner;

/** Interfaz de consola provisional; se reemplazará por la interfaz web (HTML/CSS/JS) del proyecto. */
public class Consola {
    private final Scanner in = new Scanner(System.in);
    private final UsuarioServicio usuarios;
    private final CatalogoServicio catalogo;
    private final PrestamoServicio prestamos;

    public Consola(UsuarioServicio u, CatalogoServicio c, PrestamoServicio p) {
        this.usuarios = u; this.catalogo = c; this.prestamos = p;
    }

    private String leer(String msg) {
        System.out.print(msg);
        return in.hasNextLine() ? in.nextLine().trim() : null;
    }

    public void iniciar() {
        System.out.println("=== Sistema de Gestión Bibliotecaria ===");
        while (true) {
            String correo = leer("\nCorreo (vacío para salir): ");
            if (correo == null || correo.isEmpty()) return;
            String clave = leer("Contraseña: ");
            try {
                Usuario u = usuarios.autenticar(correo, clave);
                System.out.println("Bienvenido, " + u.getNombre() + " [" + u.getRol() + "]");
                menu(u);
            } catch (BibliotecaException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void menu(Usuario u) {
        while (true) {
            System.out.println("\n1) Buscar libros   2) Mi historial");
            if (u.getRol() != Rol.LECTOR)
                System.out.println("3) Registrar libro  4) Agregar ejemplar  5) Prestar  6) Devolver  7) Alertas");
            if (u.getRol() == Rol.ADMINISTRADOR) System.out.println("8) Registrar usuario  9) Listar usuarios");
            System.out.println("0) Cerrar sesión");
            String op = leer("> ");
            if (op == null || op.equals("0")) return;
            try {
                switch (op) {
                    case "1" -> buscar();
                    case "2" -> historial(u, u.getId());
                    case "3" -> {
                        var l = catalogo.registrarLibro(u, leer("Título: "), leer("Autor: "), leer("ISBN: "));
                        System.out.println("Libro registrado con ID " + l.getId());
                    }
                    case "4" -> {
                        int libroId = Integer.parseInt(leer("ID del libro: "));
                        var e = catalogo.agregarEjemplar(u, libroId, leer("Código del ejemplar: "));
                        System.out.println("Ejemplar " + e.getCodigo() + " agregado.");
                    }
                    case "5" -> {
                        int lector = Integer.parseInt(leer("ID del lector: "));
                        var p = prestamos.prestar(u, lector, leer("Código del ejemplar: "));
                        System.out.println("Préstamo " + p.getId() + " registrado. Vence: " + p.getFechaVencimiento());
                    }
                    case "6" -> {
                        var p = prestamos.devolver(u, Integer.parseInt(leer("ID del préstamo: ")));
                        System.out.println("Devolución registrada (" + p.getFechaDevolucion() + ").");
                    }
                    case "7" -> alertas(u);
                    case "8" -> {
                        String nombre = leer("Nombre: "), correo = leer("Correo: "), clave = leer("Contraseña: ");
                        Rol rol = Rol.valueOf(leer("Rol (ADMINISTRADOR/OPERADOR/LECTOR): ").toUpperCase());
                        var nuevo = usuarios.registrar(u, nombre, correo, clave, rol);
                        System.out.println("Usuario registrado con ID " + nuevo.getId());
                    }
                    case "9" -> usuarios.listar(u).forEach(x ->
                            System.out.println(x.getId() + " | " + x.getNombre() + " | " + x.getCorreo() + " | " + x.getRol()));
                    default -> System.out.println("Opción no válida.");
                }
            } catch (BibliotecaException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (NullPointerException | IllegalArgumentException e) {
                System.out.println("Error: dato inválido.");
            }
        }
    }

    private void buscar() {
        var r = catalogo.buscar(leer("Buscar (vacío = todo): "));
        if (r.isEmpty()) { System.out.println("Sin resultados."); return; }
        r.forEach(x -> System.out.printf("%d | %s — %s | ISBN %s | disponibles %d de %d%n",
                x.libro().getId(), x.libro().getTitulo(), x.libro().getAutor(), x.libro().getIsbn(),
                x.disponibles(), x.total()));
    }

    private void historial(Usuario actor, int usuarioId) {
        var lista = prestamos.historial(actor, usuarioId);
        if (lista.isEmpty()) { System.out.println("No hay préstamos registrados."); return; }
        lista.forEach(p -> System.out.printf("#%d | %s | prestado %s | vence %s | %s%n",
                p.getId(), prestamos.describir(p), p.getFechaPrestamo(), p.getFechaVencimiento(), prestamos.estado(p)));
    }

    private void alertas(Usuario u) {
        var lista = prestamos.alertas(u);
        if (lista.isEmpty()) { System.out.println("Sin alertas."); return; }
        lista.forEach(a -> System.out.printf("%s | préstamo #%d | %s | lector %d | vence %s%n",
                a.vencido() ? "VENCIDO" : "POR VENCER", a.prestamo().getId(), prestamos.describir(a.prestamo()),
                a.prestamo().getUsuarioId(), a.prestamo().getFechaVencimiento()));
    }
}

package biblioteca.servicio;

import biblioteca.modelo.*;
import biblioteca.repositorio.Repositorio;
import biblioteca.sql.UnidadTrabajo;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** HU-05 (préstamo), HU-07 (devolución), HU-06 (historial) y HU-08 (alertas). */
public class PrestamoServicio {
    /** RN-10 pendiente: el plazo oficial y el margen de aviso deben confirmarse con la biblioteca. */
    public static final int DIAS_PLAZO = 7;
    public static final int DIAS_AVISO = 2;

    public record Alerta(Prestamo prestamo, boolean vencido) {}

    private final Repositorio<Prestamo> repo;
    private final CatalogoServicio catalogo;
    private final UsuarioServicio usuarios;
    private final Clock reloj;
    private final UnidadTrabajo unidadTrabajo;

    public PrestamoServicio(Repositorio<Prestamo> repo, CatalogoServicio catalogo, UsuarioServicio usuarios, Clock reloj) {
        this(repo, catalogo, usuarios, reloj, UnidadTrabajo.SIN_TRANSACCION);
    }
    public PrestamoServicio(Repositorio<Prestamo> repo, CatalogoServicio catalogo,
                            UsuarioServicio usuarios, Clock reloj, UnidadTrabajo unidadTrabajo) {
        this.repo = repo; this.catalogo = catalogo; this.usuarios = usuarios; this.reloj = reloj;
        this.unidadTrabajo = unidadTrabajo;
    }

    public Prestamo prestar(Usuario actor, int lectorId, String codigoEjemplar) {
        return unidadTrabajo.ejecutar(() -> prestarDentroDeTransaccion(actor, lectorId, codigoEjemplar));
    }
    private Prestamo prestarDentroDeTransaccion(Usuario actor, int lectorId, String codigoEjemplar) {
        Autorizacion.exigir(actor, Rol.ADMINISTRADOR, Rol.OPERADOR);
        Usuario lector = usuarios.buscarPorId(lectorId);
        Ejemplar ej = catalogo.ejemplarPorCodigo(codigoEjemplar);
        if (!ej.isDisponible()) throw new BibliotecaException("El ejemplar no está disponible."); // RN-03
        LocalDate hoy = LocalDate.now(reloj);
        var p = new Prestamo(lector.getId(), ej.getId(), hoy, hoy.plusDays(DIAS_PLAZO));          // RN-04
        // El gestor JDBC ejecuta cambio de disponibilidad + préstamo en una transacción.
        catalogo.cambiarDisponibilidad(ej.getId(), false);
        return repo.guardar(p);
    }

    public Prestamo devolver(Usuario actor, int prestamoId) {
        return unidadTrabajo.ejecutar(() -> devolverDentroDeTransaccion(actor, prestamoId));
    }
    private Prestamo devolverDentroDeTransaccion(Usuario actor, int prestamoId) {
        Autorizacion.exigir(actor, Rol.ADMINISTRADOR, Rol.OPERADOR);
        Prestamo p = repo.buscarPorId(prestamoId)
                .orElseThrow(() -> new BibliotecaException("Préstamo no encontrado."));
        if (p.getFechaDevolucion() != null)
            throw new BibliotecaException("Este préstamo ya fue devuelto.");                       // RN-06
        p.setFechaDevolucion(LocalDate.now(reloj));
        repo.guardar(p);
        catalogo.cambiarDisponibilidad(p.getEjemplarId(), true);
        return p;
    }

    /** Un lector solo ve su propio historial (RN-07); el personal puede consultar el de cualquiera. */
    public List<Prestamo> historial(Usuario actor, int usuarioId) {
        if (actor.getRol() == Rol.LECTOR && actor.getId() != usuarioId)
            throw new BibliotecaException("Solo puede consultar su propio historial.");
        return repo.todos().stream().filter(p -> p.getUsuarioId() == usuarioId).toList();
    }

    /** Préstamos sin devolver que ya vencieron o vencen dentro del margen de aviso (RN-08). */
    public List<Alerta> alertas(Usuario actor) {
        Autorizacion.exigir(actor, Rol.ADMINISTRADOR, Rol.OPERADOR);
        LocalDate hoy = LocalDate.now(reloj);
        return repo.todos().stream()
                .filter(p -> p.getFechaDevolucion() == null)
                .filter(p -> !p.getFechaVencimiento().isAfter(hoy.plusDays(DIAS_AVISO)))
                .map(p -> new Alerta(p, p.estado(hoy) == EstadoPrestamo.VENCIDO)).toList();
    }

    public EstadoPrestamo estado(Prestamo p) { return p.estado(LocalDate.now(reloj)); }
    public String describir(Prestamo p) { return catalogo.describir(p.getEjemplarId()); }
}

package biblioteca.servicio;

import biblioteca.modelo.*;
import biblioteca.repositorio.Repositorio;
import java.util.List;

/** HU-03 (libros e inventario) y HU-04 (consulta de disponibilidad). */
public class CatalogoServicio {
    public record ResultadoBusqueda(Libro libro, long disponibles, long total) {}

    private final Repositorio<Libro> libros;
    private final Repositorio<Ejemplar> ejemplares;
    public CatalogoServicio(Repositorio<Libro> libros, Repositorio<Ejemplar> ejemplares) {
        this.libros = libros; this.ejemplares = ejemplares;
    }

    public Libro registrarLibro(Usuario actor, String titulo, String autor, String isbn) {
        Autorizacion.exigir(actor, Rol.ADMINISTRADOR, Rol.OPERADOR);
        if (titulo == null || titulo.isBlank() || autor == null || autor.isBlank() || isbn == null || isbn.isBlank())
            throw new BibliotecaException("Título, autor e ISBN son obligatorios.");
        if (libros.todos().stream().anyMatch(l -> l.getIsbn().equalsIgnoreCase(isbn.trim())))
            throw new BibliotecaException("Ya existe un libro con ese ISBN.");
        return libros.guardar(new Libro(titulo.trim(), autor.trim(), isbn.trim()));
    }

    public Ejemplar agregarEjemplar(Usuario actor, int libroId, String codigo) {
        Autorizacion.exigir(actor, Rol.ADMINISTRADOR, Rol.OPERADOR);
        if (libros.buscarPorId(libroId).isEmpty()) throw new BibliotecaException("Libro no encontrado.");
        if (codigo == null || codigo.isBlank()) throw new BibliotecaException("El código del ejemplar es obligatorio.");
        if (ejemplares.todos().stream().anyMatch(e -> e.getCodigo().equalsIgnoreCase(codigo.trim())))
            throw new BibliotecaException("Ya existe un ejemplar con ese código.");
        return ejemplares.guardar(new Ejemplar(libroId, codigo.trim()));
    }

    /** Búsqueda por título, autor o ISBN; texto vacío lista todo el catálogo. */
    public List<ResultadoBusqueda> buscar(String texto) {
        String t = texto == null ? "" : texto.trim().toLowerCase();
        return libros.todos().stream()
                .filter(l -> l.getTitulo().toLowerCase().contains(t) || l.getAutor().toLowerCase().contains(t)
                        || l.getIsbn().toLowerCase().contains(t))
                .map(l -> {
                    var ex = ejemplares.todos().stream().filter(e -> e.getLibroId() == l.getId()).toList();
                    return new ResultadoBusqueda(l, ex.stream().filter(Ejemplar::isDisponible).count(), ex.size());
                }).toList();
    }

    public Ejemplar ejemplarPorCodigo(String codigo) {
        return ejemplares.todos().stream()
                .filter(e -> e.getCodigo().equalsIgnoreCase(codigo == null ? "" : codigo.trim()))
                .findFirst().orElseThrow(() -> new BibliotecaException("Ejemplar no encontrado."));
    }

    public void cambiarDisponibilidad(int ejemplarId, boolean disponible) {
        Ejemplar e = ejemplares.buscarPorId(ejemplarId)
                .orElseThrow(() -> new BibliotecaException("Ejemplar no encontrado."));
        e.setDisponible(disponible);
        ejemplares.guardar(e);
    }

    public String describir(int ejemplarId) {
        Ejemplar e = ejemplares.buscarPorId(ejemplarId).orElseThrow();
        return libros.buscarPorId(e.getLibroId()).orElseThrow().getTitulo() + " [" + e.getCodigo() + "]";
    }
}

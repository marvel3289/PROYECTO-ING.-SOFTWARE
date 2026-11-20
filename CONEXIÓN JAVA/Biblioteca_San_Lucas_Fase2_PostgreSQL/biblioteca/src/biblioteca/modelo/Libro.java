package biblioteca.modelo;

/** Ficha bibliográfica (el título). Las unidades físicas son {@link Ejemplar}. */
public class Libro extends Entidad {
    private final String titulo, autor, isbn;
    public Libro(String titulo, String autor, String isbn) {
        this.titulo = titulo; this.autor = autor; this.isbn = isbn;
    }
    public String getTitulo() { return titulo; }
    public String getAutor() { return autor; }
    public String getIsbn() { return isbn; }
}

package biblioteca.modelo;

/** Unidad física de un libro; la disponibilidad se controla por ejemplar. */
public class Ejemplar extends Entidad {
    private final int libroId;
    private final String codigo;
    private boolean disponible = true;
    public Ejemplar(int libroId, String codigo) { this.libroId = libroId; this.codigo = codigo; }
    public int getLibroId() { return libroId; }
    public String getCodigo() { return codigo; }
    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }
}

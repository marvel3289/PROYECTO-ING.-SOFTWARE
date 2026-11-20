package biblioteca.modelo;

/** Base de todas las entidades: identificador asignado por el repositorio. */
public abstract class Entidad implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
}

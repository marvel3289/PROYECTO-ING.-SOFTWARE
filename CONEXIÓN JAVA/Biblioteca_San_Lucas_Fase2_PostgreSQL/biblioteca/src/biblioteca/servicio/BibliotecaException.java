package biblioteca.servicio;

/** Error de regla de negocio o validación; el mensaje es apto para mostrar al usuario. */
public class BibliotecaException extends RuntimeException {
    public BibliotecaException(String mensaje) { super(mensaje); }
}

package biblioteca.servicio;

import biblioteca.modelo.Rol;
import biblioteca.modelo.Usuario;
import java.util.Arrays;

/** Control de acceso por rol (RNF-03, RN-02). */
public final class Autorizacion {
    private Autorizacion() {}
    public static void exigir(Usuario actor, Rol... permitidos) {
        if (actor == null || !Arrays.asList(permitidos).contains(actor.getRol()))
            throw new BibliotecaException("No tiene permisos para realizar esta operación.");
    }
}

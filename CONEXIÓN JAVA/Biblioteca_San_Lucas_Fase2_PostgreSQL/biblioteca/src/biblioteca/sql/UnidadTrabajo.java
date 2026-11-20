package biblioteca.sql;

import java.util.function.Supplier;

/** Ejecuta una operación lógica de manera atómica cuando existe gestor de transacciones. */
@FunctionalInterface
public interface UnidadTrabajo {
    <T> T ejecutar(Supplier<T> operacion);

    UnidadTrabajo SIN_TRANSACCION = new UnidadTrabajo() {
        @Override public <T> T ejecutar(Supplier<T> operacion) { return operacion.get(); }
    };
}

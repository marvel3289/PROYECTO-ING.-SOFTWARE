package biblioteca.repositorio;

import biblioteca.modelo.Entidad;
import java.util.List;
import java.util.Optional;

/** Contrato de persistencia. Para SQL Server se implementa con JDBC sin tocar los servicios. */
public interface Repositorio<T extends Entidad> {
    T guardar(T entidad);               // inserta (id == 0) o actualiza
    Optional<T> buscarPorId(int id);
    List<T> todos();
}

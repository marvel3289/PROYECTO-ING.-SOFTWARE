package biblioteca.repositorio;

import biblioteca.modelo.Entidad;
import java.util.*;

/** Implementación temporal en memoria, útil para pruebas y demostraciones. */
public class RepositorioMemoria<T extends Entidad> implements Repositorio<T>, java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<Integer, T> datos = new LinkedHashMap<>();
    private int siguiente = 1;

    @Override public T guardar(T e) {
        if (e.getId() == 0) e.setId(siguiente++);
        datos.put(e.getId(), e);
        return e;
    }
    @Override public Optional<T> buscarPorId(int id) { return Optional.ofNullable(datos.get(id)); }
    @Override public List<T> todos() { return new ArrayList<>(datos.values()); }
}

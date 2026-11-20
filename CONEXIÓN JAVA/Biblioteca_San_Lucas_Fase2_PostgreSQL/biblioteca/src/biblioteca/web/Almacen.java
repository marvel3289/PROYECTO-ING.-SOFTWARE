package biblioteca.web;

import biblioteca.modelo.*;
import biblioteca.repositorio.RepositorioMemoria;
import java.io.*;
import java.nio.file.*;

/** Almacenamiento local para la DEMOSTRACIÓN, no destinado a producción. */
public final class Almacen {
    public record Estado(RepositorioMemoria<Usuario> usuarios,
                         RepositorioMemoria<Libro> libros,
                         RepositorioMemoria<Ejemplar> ejemplares,
                         RepositorioMemoria<Prestamo> prestamos) implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    private final Path archivo;
    public Almacen(Path archivo) { this.archivo = archivo.toAbsolutePath(); }

    public Estado cargar() throws IOException, ClassNotFoundException {
        if (!Files.exists(archivo)) return new Estado(new RepositorioMemoria<>(),
                new RepositorioMemoria<>(), new RepositorioMemoria<>(), new RepositorioMemoria<>());
        // Solo cargar archivos creados por esta misma aplicación en un directorio de confianza.
        try (ObjectInputStream entrada = new ObjectInputStream(Files.newInputStream(archivo))) {
            entrada.setObjectInputFilter(info -> {
                if (info.depth() > 18 || info.references() > 100_000 || info.arrayLength() > 100_000) {
                    return ObjectInputFilter.Status.REJECTED;
                }
                Class<?> type = info.serialClass();
                if (type == null) return ObjectInputFilter.Status.UNDECIDED;
                while (type.isArray()) type = type.getComponentType();
                String n = type.getName();
                if (type.isPrimitive() || n.startsWith("biblioteca.modelo.") ||
                        n.startsWith("biblioteca.repositorio.") || n.equals("biblioteca.web.Almacen$Estado") ||
                        n.startsWith("java.lang.") || n.startsWith("java.util.") || n.startsWith("java.time."))
                    return ObjectInputFilter.Status.ALLOWED;
                return ObjectInputFilter.Status.REJECTED;
            });
            Object obj = entrada.readObject();
            if (!(obj instanceof Estado estado)) throw new IOException("Formato de datos inválido.");
            return estado;
        }
    }

    public void guardar(Estado estado) throws IOException {
        Path dir = archivo.getParent();
        Files.createDirectories(dir);
        Path temporal = Files.createTempFile(dir, "biblioteca-", ".tmp");
        try {
            try (ObjectOutputStream salida = new ObjectOutputStream(Files.newOutputStream(temporal))) {
                salida.writeObject(estado);
            }
            try { Files.move(temporal, archivo, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporal); }
    }
}

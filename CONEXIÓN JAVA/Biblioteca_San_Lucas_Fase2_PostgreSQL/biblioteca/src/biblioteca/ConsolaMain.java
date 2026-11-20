package biblioteca;

import biblioteca.modelo.*;
import biblioteca.repositorio.RepositorioMemoria;
import biblioteca.servicio.*;
import java.time.Clock;

public class ConsolaMain {
    public static void main(String[] args) {
        var usuarios = new UsuarioServicio(new RepositorioMemoria<Usuario>());
        var catalogo = new CatalogoServicio(new RepositorioMemoria<Libro>(), new RepositorioMemoria<Ejemplar>());
        var prestamos = new PrestamoServicio(new RepositorioMemoria<Prestamo>(), catalogo, usuarios, Clock.systemDefaultZone());

        // Datos de demostración (se perderán al cerrar: aún no hay persistencia en SQL Server).
        Usuario admin = usuarios.crearAdministradorInicial("Administrador", "admin@biblioteca.gt", "Admin12345");
        usuarios.registrar(admin, "Lector de ejemplo", "lector@biblioteca.gt", "Lector12345", Rol.LECTOR);
        Libro libro = catalogo.registrarLibro(admin, "Cien años de soledad", "Gabriel García Márquez", "978-0307474728");
        catalogo.agregarEjemplar(admin, libro.getId(), "CAS-001");
        System.out.println("Demo → admin@biblioteca.gt / Admin12345   |   lector@biblioteca.gt / Lector12345");

        new Consola(usuarios, catalogo, prestamos).iniciar();
    }
}

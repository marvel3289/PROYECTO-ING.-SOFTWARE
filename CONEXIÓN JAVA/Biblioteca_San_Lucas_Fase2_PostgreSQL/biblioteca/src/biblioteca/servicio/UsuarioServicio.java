package biblioteca.servicio;

import biblioteca.modelo.Rol;
import biblioteca.modelo.Usuario;
import biblioteca.repositorio.Repositorio;
import java.util.List;

/** HU-01 (inicio de sesión) y HU-02 (gestión de usuarios). */
public class UsuarioServicio {
    private final Repositorio<Usuario> repo;
    public UsuarioServicio(Repositorio<Usuario> repo) { this.repo = repo; }

    public Usuario autenticar(String correo, String contrasena) {
        return repo.todos().stream()
                .filter(u -> u.isActivo() && u.getCorreo().equalsIgnoreCase(correo == null ? "" : correo.trim()))
                .filter(u -> Seguridad.verificar(contrasena == null ? "" : contrasena, u.getSal(), u.getHash()))
                .findFirst()
                .orElseThrow(() -> new BibliotecaException("Credenciales incorrectas."));
    }

    public Usuario registrar(Usuario actor, String nombre, String correo, String contrasena, Rol rol) {
        Autorizacion.exigir(actor, Rol.ADMINISTRADOR);
        return crear(nombre, correo, contrasena, rol);
    }

    /** Solo funciona mientras no exista ningún usuario (arranque del sistema). */
    public Usuario crearAdministradorInicial(String nombre, String correo, String contrasena) {
        if (!repo.todos().isEmpty()) throw new BibliotecaException("El sistema ya tiene usuarios.");
        return crear(nombre, correo, contrasena, Rol.ADMINISTRADOR);
    }

    private Usuario crear(String nombre, String correo, String contrasena, Rol rol) {
        if (nombre == null || nombre.isBlank()) throw new BibliotecaException("El nombre es obligatorio.");
        if (correo == null || !correo.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))
            throw new BibliotecaException("El correo no es válido.");
        if (contrasena == null || contrasena.length() < 8)
            throw new BibliotecaException("La contraseña debe tener al menos 8 caracteres.");
        if (repo.todos().stream().anyMatch(u -> u.getCorreo().equalsIgnoreCase(correo.trim())))
            throw new BibliotecaException("Ya existe un usuario con ese correo."); // RN-01
        String sal = Seguridad.nuevaSal();
        return repo.guardar(new Usuario(nombre.trim(), correo.trim(), sal, Seguridad.hash(contrasena, sal), rol));
    }

    public Usuario modificar(Usuario actor, int id, String nombre, Rol rol) {
        Autorizacion.exigir(actor, Rol.ADMINISTRADOR);
        if (nombre == null || nombre.isBlank()) throw new BibliotecaException("El nombre es obligatorio.");
        Usuario u = buscarPorId(id);
        u.setNombre(nombre.trim());
        u.setRol(rol);
        return repo.guardar(u);
    }

    public List<Usuario> listar(Usuario actor) {
        Autorizacion.exigir(actor, Rol.ADMINISTRADOR);
        return repo.todos();
    }

    public Usuario buscarPorId(int id) {
        return repo.buscarPorId(id).orElseThrow(() -> new BibliotecaException("Usuario no encontrado."));
    }
}

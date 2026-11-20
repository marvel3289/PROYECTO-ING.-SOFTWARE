package biblioteca.modelo;

public class Usuario extends Entidad {
    private String nombre;
    private final String correo;
    private final String sal;
    private final String hash;
    private Rol rol;
    private boolean activo = true;

    public Usuario(String nombre, String correo, String sal, String hash, Rol rol) {
        this.nombre = nombre; this.correo = correo; this.sal = sal; this.hash = hash; this.rol = rol;
    }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getSal() { return sal; }
    public String getHash() { return hash; }
    public Rol getRol() { return rol; }
    public boolean isActivo() { return activo; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setRol(Rol rol) { this.rol = rol; }
    public void setActivo(boolean activo) { this.activo = activo; }
}

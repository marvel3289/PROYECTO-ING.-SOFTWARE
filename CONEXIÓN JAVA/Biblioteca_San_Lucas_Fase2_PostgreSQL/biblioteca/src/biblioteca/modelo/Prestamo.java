package biblioteca.modelo;

import java.time.LocalDate;

public class Prestamo extends Entidad {
    private final int usuarioId, ejemplarId;
    private final LocalDate fechaPrestamo, fechaVencimiento;
    private LocalDate fechaDevolucion;

    public Prestamo(int usuarioId, int ejemplarId, LocalDate fechaPrestamo, LocalDate fechaVencimiento) {
        this.usuarioId = usuarioId; this.ejemplarId = ejemplarId;
        this.fechaPrestamo = fechaPrestamo; this.fechaVencimiento = fechaVencimiento;
    }
    public int getUsuarioId() { return usuarioId; }
    public int getEjemplarId() { return ejemplarId; }
    public LocalDate getFechaPrestamo() { return fechaPrestamo; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public LocalDate getFechaDevolucion() { return fechaDevolucion; }
    public void setFechaDevolucion(LocalDate f) { this.fechaDevolucion = f; }

    public EstadoPrestamo estado(LocalDate hoy) {
        if (fechaDevolucion != null) return EstadoPrestamo.DEVUELTO;
        return hoy.isAfter(fechaVencimiento) ? EstadoPrestamo.VENCIDO : EstadoPrestamo.ACTIVO;
    }
}

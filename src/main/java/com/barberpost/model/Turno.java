package com.barberpost.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Modelo: Turno
 *
 * Representa una reserva en el sistema.
 * Relaciona un Cliente con un Servicio en una fecha y hora específicas.
 * El campo 'precioCobrado' guarda un snapshot del precio al momento de
 * la reserva, así los cambios futuros de precio no afectan estadísticas.
 *
 * Estados posibles:
 *   pendiente  → la reserva fue creada, aún no fue confirmada
 *   confirmado → el negocio la confirmó
 *   completado → el servicio fue brindado (para estadísticas)
 *   cancelado  → fue cancelada por el cliente o barbería
 *
 * @version 1.0.0
 */
public class Turno {

    private int id;

    /** FK → Cliente */
    private int clienteId;

    /** FK → Servicio */
    private int servicioId;

    /** Fecha del turno */
    private LocalDate fecha;

    /** Hora de inicio del turno */
    private LocalTime hora;

    /**
     * Estado del ciclo de vida del turno.
     * Valores válidos: "pendiente", "confirmado", "completado", "cancelado"
     */
    private String estado;

    /**
     * Precio al momento de la reserva.
     * Evita que cambios futuros en la tabla servicios afecten el historial.
     */
    private BigDecimal precioCobrado;

    /** Notas del empleado (observaciones, pedidos especiales, etc.) */
    private String notas;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Objetos embebidos para respuestas enriquecidas (no se persisten)
    private Cliente  cliente;
    private Servicio servicio;

    // ============================================================
    // Constructores
    // ============================================================

    public Turno() {}

    public Turno(int clienteId, int servicioId, LocalDate fecha, LocalTime hora, BigDecimal precio) {
        this.clienteId     = clienteId;
        this.servicioId    = servicioId;
        this.fecha         = fecha;
        this.hora          = hora;
        this.precioCobrado = precio;
        this.estado        = "pendiente";
    }

    // ============================================================
    // Métodos utilitarios
    // ============================================================

    /** ¿El turno está activo (pendiente o confirmado)? */
    public boolean isActivo() {
        return "pendiente".equals(estado) || "confirmado".equals(estado);
    }

    /** ¿El turno fue completado? */
    public boolean isCompletado() {
        return "completado".equals(estado);
    }

    /** ¿El turno fue cancelado? */
    public boolean isCancelado() {
        return "cancelado".equals(estado);
    }

    /**
     * Retorna la fecha formateada para mostrar al cliente.
     * Ejemplo: "Lunes 15 de enero de 2025"
     */
    public String getFechaFormateada() {
        if (fecha == null) return "";
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE dd 'de' MMMM 'de' yyyy",
                new java.util.Locale("es", "AR"));
        String f = fecha.format(fmt);
        // Capitalizar primera letra
        return f.substring(0, 1).toUpperCase() + f.substring(1);
    }

    /**
     * Retorna la hora formateada en formato HH:mm.
     * Ejemplo: "14:30"
     */
    public String getHoraFormateada() {
        if (hora == null) return "";
        return hora.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    // ============================================================
    // Getters y Setters
    // ============================================================

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public int getClienteId()                   { return clienteId; }
    public void setClienteId(int clienteId)     { this.clienteId = clienteId; }

    public int getServicioId()                  { return servicioId; }
    public void setServicioId(int servicioId)   { this.servicioId = servicioId; }

    public LocalDate getFecha()                 { return fecha; }
    public void setFecha(LocalDate fecha)       { this.fecha = fecha; }

    public LocalTime getHora()                  { return hora; }
    public void setHora(LocalTime hora)         { this.hora = hora; }

    public String getEstado()                   { return estado; }
    public void setEstado(String estado)        { this.estado = estado; }

    public BigDecimal getPrecioCobrado()        { return precioCobrado; }
    public void setPrecioCobrado(BigDecimal p)  { this.precioCobrado = p; }

    public String getNotas()                    { return notas; }
    public void setNotas(String notas)          { this.notas = notas; }

    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime t)   { this.createdAt = t; }

    public LocalDateTime getUpdatedAt()         { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)   { this.updatedAt = t; }

    public Cliente getCliente()                 { return cliente; }
    public void setCliente(Cliente cliente)     { this.cliente = cliente; }

    public Servicio getServicio()               { return servicio; }
    public void setServicio(Servicio servicio)  { this.servicio = servicio; }

    @Override
    public String toString() {
        return "Turno{id=" + id + ", fecha=" + fecha + ", hora=" + hora
                + ", estado='" + estado + "', clienteId=" + clienteId + "}";
    }
}

package com.barberpost.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Modelo: Servicio
 *
 * Representa un servicio del catálogo de la barbería.
 * Incluye precio, descripción y duración estimada.
 * Los precios pueden ser modificados desde el dashboard del dueño sin
 * necesidad de tocar código ni base de datos manualmente.
 *
 * @version 1.0.0
 */
public class Servicio {

    private int id;

    /** Nombre visible del servicio (ej: "Corte + Barba") */
    private String nombre;

    /** Descripción detallada que se muestra al cliente */
    private String descripcion;

    /**
     * Precio en pesos argentinos.
     * Se usa BigDecimal para evitar errores de punto flotante en moneda.
     */
    private BigDecimal precio;

    /**
     * Duración estimada del servicio en minutos.
     * Se usa para calcular los slots disponibles: si un turno dura 45 min,
     * los siguientes slots se muestran con ese intervalo de gracia.
     */
    private int duracionMinutos;

    /**
     * Flag de visibilidad.
     * true  = servicio activo (visible en el sitio y disponible para reserva)
     * false = servicio desactivado (no aparece en la lista pública)
     */
    private boolean activo;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ============================================================
    // Constructores
    // ============================================================

    public Servicio() {}

    public Servicio(String nombre, String descripcion, BigDecimal precio, int duracionMinutos) {
        this.nombre           = nombre;
        this.descripcion      = descripcion;
        this.precio           = precio;
        this.duracionMinutos  = duracionMinutos;
        this.activo           = true;
    }

    // ============================================================
    // Métodos utilitarios
    // ============================================================

    /**
     * Retorna el precio formateado como string con símbolo de pesos.
     * Ejemplo: "$1.500,00"
     */
    public String getPrecioFormateado() {
        if (precio == null) return "$0,00";
        return "$" + String.format("%,.2f", precio).replace(',', 'X').replace('.', ',').replace('X', '.');
    }

    /**
     * Retorna la duración en formato legible.
     * Ejemplo: 90 min → "1h 30min"
     */
    public String getDuracionFormateada() {
        if (duracionMinutos < 60) {
            return duracionMinutos + " min";
        }
        int horas   = duracionMinutos / 60;
        int minutos = duracionMinutos % 60;
        return minutos == 0 ? horas + "h" : horas + "h " + minutos + "min";
    }

    // ============================================================
    // Getters y Setters
    // ============================================================

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public String getNombre()                   { return nombre; }
    public void setNombre(String nombre)        { this.nombre = nombre; }

    public String getDescripcion()              { return descripcion; }
    public void setDescripcion(String d)        { this.descripcion = d; }

    public BigDecimal getPrecio()               { return precio; }
    public void setPrecio(BigDecimal precio)    { this.precio = precio; }

    public int getDuracionMinutos()             { return duracionMinutos; }
    public void setDuracionMinutos(int d)       { this.duracionMinutos = d; }

    public boolean isActivo()                   { return activo; }
    public void setActivo(boolean activo)       { this.activo = activo; }

    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime t)   { this.createdAt = t; }

    public LocalDateTime getUpdatedAt()         { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)   { this.updatedAt = t; }

    @Override
    public String toString() {
        return "Servicio{id=" + id + ", nombre='" + nombre + "', precio=" + precio + "}";
    }
}

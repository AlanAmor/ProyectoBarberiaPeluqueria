package com.barberpost.model;

import java.time.LocalDateTime;

/**
 * Modelo: Cliente
 *
 * Representa a un cliente de la barbería.
 * El DNI es el identificador natural que se usa para detectar
 * clientes frecuentes en el dashboard del propietario.
 *
 * @version 1.0.0
 */
public class Cliente {

    private int id;

    /** Nombre de pila del cliente */
    private String nombre;

    /** Apellido del cliente */
    private String apellido;

    /**
     * Documento Nacional de Identidad.
     * Es ÚNICO en la tabla: si ya existe una reserva con el mismo DNI,
     * el cliente ya está registrado y sus datos se actualizan.
     */
    private String dni;

    /**
     * Teléfono del cliente con código de área (sin el 0 del inter-urbano).
     * Ejemplo: "1155667788" → Buenos Aires
     * Se usa para enviar el link de WhatsApp al confirmar el turno.
     */
    private String telefono;

    /** Email opcional del cliente */
    private String email;

    /** Fecha/hora de registro en el sistema */
    private LocalDateTime createdAt;

    /** Fecha/hora de última actualización */
    private LocalDateTime updatedAt;

    // ============================================================
    // Constructores
    // ============================================================

    public Cliente() {}

    public Cliente(String nombre, String apellido, String dni, String telefono, String email) {
        this.nombre    = nombre;
        this.apellido  = apellido;
        this.dni       = dni;
        this.telefono  = telefono;
        this.email     = email;
    }

    // ============================================================
    // Métodos utilitarios
    // ============================================================

    /**
     * Retorna el nombre completo formateado "Apellido, Nombre".
     */
    public String getNombreCompleto() {
        return apellido + ", " + nombre;
    }

    /**
     * Normaliza el número de teléfono para uso en WhatsApp.
     * Asume código de país Argentina (+54).
     * Elimina espacios, guiones y el prefijo "0" o "15".
     *
     * @return número listo para usar en wa.me (ej: "541155667788")
     */
    public String getTelefonoWhatsApp() {
        if (telefono == null) return "";
        String numero = telefono.replaceAll("[\\s\\-\\(\\)\\+]", "");
        // Si el número local empieza con 0, quitarlo
        if (numero.startsWith("0")) {
            numero = numero.substring(1);
        }
        // Si ya tiene el código de país, dejarlo como está
        if (numero.startsWith("54")) {
            return numero;
        }
        return "54" + numero;
    }

    // ============================================================
    // Getters y Setters
    // ============================================================

    public int getId()                       { return id; }
    public void setId(int id)                { this.id = id; }

    public String getNombre()                { return nombre; }
    public void setNombre(String nombre)     { this.nombre = nombre; }

    public String getApellido()              { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getDni()                   { return dni; }
    public void setDni(String dni)           { this.dni = dni; }

    public String getTelefono()              { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail()                 { return email; }
    public void setEmail(String email)       { this.email = email; }

    public LocalDateTime getCreatedAt()      { return createdAt; }
    public void setCreatedAt(LocalDateTime t){ this.createdAt = t; }

    public LocalDateTime getUpdatedAt()      { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t){ this.updatedAt = t; }

    @Override
    public String toString() {
        return "Cliente{id=" + id + ", dni='" + dni + "', nombre='" + getNombreCompleto() + "'}";
    }
}

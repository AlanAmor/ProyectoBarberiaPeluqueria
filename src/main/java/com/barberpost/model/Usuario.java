package com.barberpost.model;

import java.time.LocalDateTime;

/**
 * Modelo: Usuario
 *
 * Representa un usuario del panel de administración.
 * Hay dos roles:
 *   - owner    → dueño de la barbería, acceso total
 *   - employee → empleado, acceso limitado a agenda y estadísticas propias
 *
 * La contraseña NO se almacena aquí en texto plano.
 * Este modelo solo expone el hash (para comparar) y nunca
 * serializa el hash a las respuestas JSON del frontend.
 *
 * @version 1.0.0
 */
public class Usuario {

    private int id;

    /** Nombre de usuario para login (único en la tabla) */
    private String username;

    /**
     * Hash SHA-256 de la contraseña.
     * NUNCA se envía al frontend.
     * En producción, migrar a BCrypt con factor de trabajo ≥ 12.
     */
    private transient String passwordHash;

    /** Nombre completo visible en el dashboard */
    private String nombre;

    /**
     * Rol del usuario.
     * "owner"    → acceso completo (precios, estadísticas, usuarios)
     * "employee" → acceso a agenda y estadísticas de su propia actividad
     */
    private String rol;

    /** true = puede acceder al sistema, false = cuenta desactivada */
    private boolean activo;

    /** Última vez que el usuario inició sesión (para auditoría) */
    private LocalDateTime ultimoAcceso;

    private LocalDateTime createdAt;

    // ============================================================
    // Constructores
    // ============================================================

    public Usuario() {}

    public Usuario(String username, String nombre, String rol) {
        this.username = username;
        this.nombre   = nombre;
        this.rol      = rol;
        this.activo   = true;
    }

    // ============================================================
    // Métodos utilitarios
    // ============================================================

    /** ¿El usuario es propietario? */
    public boolean isOwner() {
        return "owner".equals(rol);
    }

    /** ¿El usuario es empleado? */
    public boolean isEmployee() {
        return "employee".equals(rol);
    }

    /**
     * Retorna un label legible del rol para mostrar en la UI.
     */
    public String getRolLabel() {
        if ("owner".equals(rol))    return "Propietario";
        if ("employee".equals(rol)) return "Empleado";
        return "Desconocido";
    }

    // ============================================================
    // Getters y Setters
    // ============================================================

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public String getUsername()                 { return username; }
    public void setUsername(String username)    { this.username = username; }

    public String getPasswordHash()             { return passwordHash; }
    public void setPasswordHash(String hash)    { this.passwordHash = hash; }

    public String getNombre()                   { return nombre; }
    public void setNombre(String nombre)        { this.nombre = nombre; }

    public String getRol()                      { return rol; }
    public void setRol(String rol)              { this.rol = rol; }

    public boolean isActivo()                   { return activo; }
    public void setActivo(boolean activo)       { this.activo = activo; }

    public LocalDateTime getUltimoAcceso()         { return ultimoAcceso; }
    public void setUltimoAcceso(LocalDateTime t)   { this.ultimoAcceso = t; }

    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime t)   { this.createdAt = t; }

    @Override
    public String toString() {
        return "Usuario{id=" + id + ", username='" + username + "', rol='" + rol + "'}";
    }
}

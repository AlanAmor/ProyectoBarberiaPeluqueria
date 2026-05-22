package com.barberpost.dao;

import com.barberpost.model.Usuario;
import com.barberpost.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * UsuarioDAO — Acceso a datos de la tabla 'usuarios'
 *
 * Maneja autenticación y gestión de usuarios del panel admin.
 * Las contraseñas se comparan siempre como hash SHA-256, nunca
 * en texto plano.
 *
 * @version 1.0.0
 */
public class UsuarioDAO {

    // ============================================================
    // Autenticación
    // ============================================================

    /**
     * Valida las credenciales de un usuario.
     * Hashea la contraseña ingresada y la compara con el hash almacenado.
     *
     * @param username nombre de usuario
     * @param password contraseña en texto plano (se hashea internamente)
     * @return el Usuario autenticado, o null si las credenciales son incorrectas
     */
    public Usuario autenticar(String username, String password) throws SQLException {
        String sql = "SELECT * FROM usuarios WHERE username = ? AND activo = 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hashGuardado  = rs.getString("password_hash");
                    String hashIngresado = PasswordUtil.hash(password);
                    if (hashGuardado.equals(hashIngresado)) {
                        Usuario u = mapear(rs);
                        // Actualizar timestamp de último acceso
                        actualizarUltimoAcceso(u.getId());
                        return u;
                    }
                }
            }
        }
        return null;
    }

    // ============================================================
    // CRUD
    // ============================================================

    /**
     * Crea un nuevo usuario en el sistema.
     * La contraseña se hashea antes de guardar.
     *
     * @param usuario datos del nuevo usuario
     * @param passwordPlano contraseña en texto plano
     * @return el usuario con el id generado
     */
    public Usuario crear(Usuario usuario, String passwordPlano) throws SQLException {
        String sql = "INSERT INTO usuarios (username, password_hash, nombre, rol, activo) VALUES (?,?,?,?,1)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, usuario.getUsername());
            ps.setString(2, PasswordUtil.hash(passwordPlano));
            ps.setString(3, usuario.getNombre());
            ps.setString(4, usuario.getRol());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) usuario.setId(keys.getInt(1));
            }
        }
        return usuario;
    }

    /**
     * Lista todos los usuarios del sistema (excluyendo el hash de contraseña).
     * Solo el owner puede acceder a este listado.
     */
    public List<Usuario> listarTodos() throws SQLException {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM usuarios ORDER BY nombre ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) usuarios.add(mapear(rs));
        }
        return usuarios;
    }

    /**
     * Activa o desactiva un usuario sin borrarlo.
     */
    public boolean cambiarEstado(int id, boolean activo) throws SQLException {
        String sql = "UPDATE usuarios SET activo = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, activo);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Verifica si ya hay usuarios registrados en el sistema.
     * Usado por SetupServlet para el bootstrap inicial.
     */
    public boolean hayUsuarios() throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuarios";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() && rs.getInt(1) > 0;
        }
    }

    // ============================================================
    // Métodos internos
    // ============================================================

    private void actualizarUltimoAcceso(int id) throws SQLException {
        String sql = "UPDATE usuarios SET ultimo_acceso = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ============================================================
    // Mapeo
    // ============================================================

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash")); // transient, no sale en JSON
        u.setNombre(rs.getString("nombre"));
        u.setRol(rs.getString("rol"));
        u.setActivo(rs.getBoolean("activo"));

        Timestamp ultimoAcceso = rs.getTimestamp("ultimo_acceso");
        if (ultimoAcceso != null) u.setUltimoAcceso(ultimoAcceso.toLocalDateTime());

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) u.setCreatedAt(created.toLocalDateTime());

        return u;
    }
}

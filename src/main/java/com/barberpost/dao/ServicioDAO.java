package com.barberpost.dao;

import com.barberpost.model.Servicio;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ServicioDAO — Acceso a datos de la tabla 'servicios'
 *
 * Expone el catálogo de servicios al frontend y permite al
 * propietario modificar precios y activar/desactivar servicios
 * desde el dashboard sin necesidad de acceso a la base de datos.
 *
 * @version 1.0.0
 */
public class ServicioDAO {

    // ============================================================
    // Consultas
    // ============================================================

    /**
     * Lista todos los servicios activos.
     * Este es el listado que ve el cliente en el formulario de reserva.
     *
     * @return lista de servicios activos ordenados por nombre
     */
    public List<Servicio> listarActivos() throws SQLException {
        List<Servicio> servicios = new ArrayList<>();
        String sql = "SELECT * FROM servicios WHERE activo = 1 ORDER BY nombre ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) servicios.add(mapear(rs));
        }
        return servicios;
    }

    /**
     * Lista todos los servicios (activos e inactivos).
     * Uso exclusivo del dashboard del propietario.
     */
    public List<Servicio> listarTodos() throws SQLException {
        List<Servicio> servicios = new ArrayList<>();
        String sql = "SELECT * FROM servicios ORDER BY nombre ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) servicios.add(mapear(rs));
        }
        return servicios;
    }

    /**
     * Busca un servicio por su ID.
     *
     * @param id ID del servicio
     * @return Servicio encontrado o null
     */
    public Servicio buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM servicios WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    // ============================================================
    // Modificaciones (solo propietario)
    // ============================================================

    /**
     * Actualiza el precio de un servicio.
     * El sistema registra la fecha/hora de la modificación.
     *
     * @param id        ID del servicio
     * @param nuevoPrecio nuevo precio en pesos
     * @return true si se modificó al menos una fila
     */
    public boolean actualizarPrecio(int id, BigDecimal nuevoPrecio) throws SQLException {
        String sql = "UPDATE servicios SET precio = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBigDecimal(1, nuevoPrecio);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Actualiza nombre, descripción, precio y duración de un servicio.
     *
     * @param servicio objeto con los datos actualizados
     * @return true si la actualización fue exitosa
     */
    public boolean actualizar(Servicio servicio) throws SQLException {
        String sql = "UPDATE servicios SET nombre=?, descripcion=?, precio=?, " +
                     "duracion_minutos=?, activo=?, updated_at=NOW() WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, servicio.getNombre());
            ps.setString(2, servicio.getDescripcion());
            ps.setBigDecimal(3, servicio.getPrecio());
            ps.setInt(4, servicio.getDuracionMinutos());
            ps.setBoolean(5, servicio.isActivo());
            ps.setInt(6, servicio.getId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Activa o desactiva un servicio (no lo borra, lo oculta).
     * Un servicio desactivado no aparece en el formulario de reserva.
     */
    public boolean cambiarEstado(int id, boolean activo) throws SQLException {
        String sql = "UPDATE servicios SET activo = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, activo);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ============================================================
    // Mapeo ResultSet → Objeto
    // ============================================================

    private Servicio mapear(ResultSet rs) throws SQLException {
        Servicio s = new Servicio();
        s.setId(rs.getInt("id"));
        s.setNombre(rs.getString("nombre"));
        s.setDescripcion(rs.getString("descripcion"));
        s.setPrecio(rs.getBigDecimal("precio"));
        s.setDuracionMinutos(rs.getInt("duracion_minutos"));
        s.setActivo(rs.getBoolean("activo"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) s.setCreatedAt(created.toLocalDateTime());

        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) s.setUpdatedAt(updated.toLocalDateTime());

        return s;
    }
}

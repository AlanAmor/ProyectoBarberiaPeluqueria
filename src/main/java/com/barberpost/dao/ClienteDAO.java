package com.barberpost.dao;

import com.barberpost.model.Cliente;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ClienteDAO — Acceso a datos de la tabla 'clientes'
 *
 * Implementa las operaciones CRUD básicas y consultas específicas
 * del negocio (clientes frecuentes, búsqueda por DNI, etc.).
 *
 * Patrón: cada método abre y cierra su propia conexión mediante
 * try-with-resources para liberar recursos correctamente.
 *
 * @version 1.0.0
 */
public class ClienteDAO {

    // ============================================================
    // Crear / Guardar
    // ============================================================

    /**
     * Intenta insertar un nuevo cliente.
     * Si el DNI ya existe, actualiza nombre, apellido, teléfono y email
     * (el cliente puede haber cambiado de número desde la última visita).
     *
     * @param cliente datos del cliente (sin id)
     * @return el cliente con el id asignado por la DB
     * @throws SQLException si falla la operación
     */
    public Cliente guardarOActualizar(Cliente cliente) throws SQLException {
        // Verificar si el DNI ya existe
        Cliente existente = buscarPorDni(cliente.getDni());

        if (existente != null) {
            // Actualizar datos del cliente existente
            String sql = "UPDATE clientes SET nombre=?, apellido=?, telefono=?, email=?, updated_at=NOW() " +
                         "WHERE dni=?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, cliente.getNombre());
                ps.setString(2, cliente.getApellido());
                ps.setString(3, cliente.getTelefono());
                ps.setString(4, cliente.getEmail());
                ps.setString(5, cliente.getDni());
                ps.executeUpdate();
            }
            cliente.setId(existente.getId());
        } else {
            // Insertar nuevo cliente
            String sql = "INSERT INTO clientes (nombre, apellido, dni, telefono, email) VALUES (?,?,?,?,?)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, cliente.getNombre());
                ps.setString(2, cliente.getApellido());
                ps.setString(3, cliente.getDni());
                ps.setString(4, cliente.getTelefono());
                ps.setString(5, cliente.getEmail());
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        cliente.setId(keys.getInt(1));
                    }
                }
            }
        }
        return cliente;
    }

    // ============================================================
    // Buscar
    // ============================================================

    /**
     * Busca un cliente por su ID.
     *
     * @param id ID del cliente
     * @return Cliente encontrado o null
     */
    public Cliente buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM clientes WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    /**
     * Busca un cliente por su DNI.
     * Se usa para detectar si el cliente ya existe antes de insertar.
     *
     * @param dni DNI sin puntos ni espacios
     * @return Cliente encontrado o null
     */
    public Cliente buscarPorDni(String dni) throws SQLException {
        String sql = "SELECT * FROM clientes WHERE dni = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    /**
     * Lista todos los clientes ordenados por apellido.
     */
    public List<Cliente> listarTodos() throws SQLException {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT * FROM clientes ORDER BY apellido ASC, nombre ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) clientes.add(mapear(rs));
        }
        return clientes;
    }

    /**
     * Retorna los N clientes más frecuentes, calculado por cantidad de turnos
     * con estado 'completado'. Se usa en el dashboard del propietario.
     *
     * @param limite cantidad máxima de resultados
     * @return lista de clientes con columna extra 'total_visitas'
     */
    public List<ClienteFrecuente> listarMasFrecuentes(int limite) throws SQLException {
        List<ClienteFrecuente> resultado = new ArrayList<>();
        String sql =
            "SELECT c.*, COUNT(t.id) AS total_visitas " +
            "FROM clientes c " +
            "JOIN turnos t ON t.cliente_id = c.id " +
            "WHERE t.estado IN ('completado', 'confirmado') " +
            "GROUP BY c.id " +
            "ORDER BY total_visitas DESC " +
            "LIMIT ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ClienteFrecuente cf = new ClienteFrecuente();
                    cf.cliente       = mapear(rs);
                    cf.totalVisitas  = rs.getInt("total_visitas");
                    resultado.add(cf);
                }
            }
        }
        return resultado;
    }

    /**
     * Busca clientes que coincidan con un término de búsqueda
     * (nombre, apellido o DNI). Se usa en el dashboard de empleados.
     *
     * @param termino texto parcial a buscar
     */
    public List<Cliente> buscar(String termino) throws SQLException {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT * FROM clientes " +
                     "WHERE nombre LIKE ? OR apellido LIKE ? OR dni LIKE ? " +
                     "ORDER BY apellido ASC LIMIT 20";
        String like = "%" + termino + "%";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) clientes.add(mapear(rs));
            }
        }
        return clientes;
    }

    // ============================================================
    // Mapeo ResultSet → Objeto
    // ============================================================

    /**
     * Convierte una fila del ResultSet en un objeto Cliente.
     * Método privado de uso interno.
     */
    private Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id"));
        c.setNombre(rs.getString("nombre"));
        c.setApellido(rs.getString("apellido"));
        c.setDni(rs.getString("dni"));
        c.setTelefono(rs.getString("telefono"));
        c.setEmail(rs.getString("email"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) c.setCreatedAt(created.toLocalDateTime());

        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) c.setUpdatedAt(updated.toLocalDateTime());

        return c;
    }

    // ============================================================
    // Clase auxiliar para resultados enriquecidos
    // ============================================================

    /**
     * Wrapper que agrega la cantidad de visitas al objeto Cliente.
     * Se usa exclusivamente en el reporte de clientes frecuentes.
     */
    public static class ClienteFrecuente {
        public Cliente cliente;
        public int totalVisitas;
    }
}

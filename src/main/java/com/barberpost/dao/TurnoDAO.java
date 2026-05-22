package com.barberpost.dao;

import com.barberpost.model.Turno;
import com.barberpost.model.Cliente;
import com.barberpost.model.Servicio;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * TurnoDAO — Acceso a datos de la tabla 'turnos'
 *
 * Gestiona el ciclo de vida completo de las reservas:
 *   - Crear una reserva nueva
 *   - Consultar slots disponibles para una fecha y servicio
 *   - Actualizar estado (confirmar, completar, cancelar)
 *   - Consultas para estadísticas del dashboard
 *
 * @version 1.0.0
 */
public class TurnoDAO {

    // ============================================================
    // Crear
    // ============================================================

    /**
     * Crea una nueva reserva en la base de datos.
     * También almacena el precio actual del servicio como snapshot.
     *
     * @param turno objeto con cliente_id, servicio_id, fecha, hora y precio
     * @return el turno con el id generado
     * @throws SQLException si falla o si ya existe un turno en esa fecha/hora
     */
    public Turno crear(Turno turno) throws SQLException {
        String sql = "INSERT INTO turnos (cliente_id, servicio_id, fecha, hora, estado, precio_cobrado, notas) " +
                     "VALUES (?, ?, ?, ?, 'pendiente', ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, turno.getClienteId());
            ps.setInt(2, turno.getServicioId());
            ps.setDate(3, Date.valueOf(turno.getFecha()));
            ps.setTime(4, Time.valueOf(turno.getHora()));
            ps.setBigDecimal(5, turno.getPrecioCobrado());
            ps.setString(6, turno.getNotas());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) turno.setId(keys.getInt(1));
            }
        }
        return turno;
    }

    // ============================================================
    // Consultas principales
    // ============================================================

    /**
     * Retorna los turnos activos (no cancelados) para una fecha específica.
     * Se usa para calcular los slots ya ocupados y mostrar sólo los disponibles.
     *
     * @param fecha la fecha a consultar
     * @return lista de turnos activos ese día
     */
    public List<Turno> listarPorFecha(LocalDate fecha) throws SQLException {
        List<Turno> turnos = new ArrayList<>();
        String sql = "SELECT t.*, " +
                     "c.nombre AS cli_nombre, c.apellido AS cli_apellido, c.dni AS cli_dni, " +
                     "c.telefono AS cli_telefono, " +
                     "s.nombre AS serv_nombre, s.precio AS serv_precio, s.duracion_minutos " +
                     "FROM turnos t " +
                     "JOIN clientes c ON c.id = t.cliente_id " +
                     "JOIN servicios s ON s.id = t.servicio_id " +
                     "WHERE t.fecha = ? AND t.estado != 'cancelado' " +
                     "ORDER BY t.hora ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) turnos.add(mapearCompleto(rs));
            }
        }
        return turnos;
    }

    /**
     * Lista todos los turnos con filtros opcionales.
     * Pensado para el dashboard: el owner ve todo, el employee puede ver su día.
     *
     * @param desde  fecha de inicio del rango (puede ser null)
     * @param hasta  fecha de fin del rango (puede ser null)
     * @param estado filtro de estado (null = todos)
     */
    public List<Turno> listar(LocalDate desde, LocalDate hasta, String estado) throws SQLException {
        List<Turno> turnos = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT t.*, " +
            "c.nombre AS cli_nombre, c.apellido AS cli_apellido, c.dni AS cli_dni, " +
            "c.telefono AS cli_telefono, " +
            "s.nombre AS serv_nombre, s.precio AS serv_precio, s.duracion_minutos " +
            "FROM turnos t " +
            "JOIN clientes c ON c.id = t.cliente_id " +
            "JOIN servicios s ON s.id = t.servicio_id " +
            "WHERE 1=1 "
        );

        if (desde  != null) sql.append("AND t.fecha >= ? ");
        if (hasta  != null) sql.append("AND t.fecha <= ? ");
        if (estado != null) sql.append("AND t.estado = ? ");
        sql.append("ORDER BY t.fecha DESC, t.hora ASC LIMIT 500");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            if (desde  != null) ps.setDate(idx++, Date.valueOf(desde));
            if (hasta  != null) ps.setDate(idx++, Date.valueOf(hasta));
            if (estado != null) ps.setString(idx, estado);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) turnos.add(mapearCompleto(rs));
            }
        }
        return turnos;
    }

    /**
     * Busca un turno específico por su ID.
     */
    public Turno buscarPorId(int id) throws SQLException {
        String sql = "SELECT t.*, " +
                     "c.nombre AS cli_nombre, c.apellido AS cli_apellido, c.dni AS cli_dni, " +
                     "c.telefono AS cli_telefono, " +
                     "s.nombre AS serv_nombre, s.precio AS serv_precio, s.duracion_minutos " +
                     "FROM turnos t " +
                     "JOIN clientes c ON c.id = t.cliente_id " +
                     "JOIN servicios s ON s.id = t.servicio_id " +
                     "WHERE t.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapearCompleto(rs);
            }
        }
        return null;
    }

    // ============================================================
    // Actualizar estado
    // ============================================================

    /**
     * Cambia el estado de un turno (confirmar, completar, cancelar).
     *
     * @param id     ID del turno
     * @param estado nuevo estado
     * @return true si se actualizó
     */
    public boolean actualizarEstado(int id, String estado) throws SQLException {
        String sql = "UPDATE turnos SET estado = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, estado);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Actualiza notas de un turno (uso de empleados durante la atención).
     */
    public boolean actualizarNotas(int id, String notas) throws SQLException {
        String sql = "UPDATE turnos SET notas = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, notas);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ============================================================
    // Estadísticas para dashboard
    // ============================================================

    /**
     * Cuenta los turnos del día de hoy por estado.
     * Responde: { pendientes: X, confirmados: Y, completados: Z }
     */
    public EstadisticasDia getEstadisticasHoy() throws SQLException {
        String sql = "SELECT estado, COUNT(*) AS total " +
                     "FROM turnos WHERE fecha = CURDATE() " +
                     "GROUP BY estado";
        EstadisticasDia est = new EstadisticasDia();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String estado = rs.getString("estado");
                int total     = rs.getInt("total");
                switch (estado) {
                    case "pendiente":  est.pendientes  = total; break;
                    case "confirmado": est.confirmados = total; break;
                    case "completado": est.completados = total; break;
                    case "cancelado":  est.cancelados  = total; break;
                }
            }
        }
        return est;
    }

    /**
     * Retorna la facturación y cantidad de turnos completados por mes
     * para el año en curso. Se usa para el gráfico del dashboard.
     *
     * @return lista de 12 filas (una por mes, incluye meses sin datos)
     */
    public List<FacturacionMensual> getFacturacionPorMes(int anio) throws SQLException {
        List<FacturacionMensual> resultado = new ArrayList<>();
        String sql =
            "SELECT MONTH(fecha) AS mes, " +
            "       COUNT(*) AS cantidad, " +
            "       SUM(precio_cobrado) AS total " +
            "FROM turnos " +
            "WHERE YEAR(fecha) = ? AND estado = 'completado' " +
            "GROUP BY MONTH(fecha) " +
            "ORDER BY mes ASC";

        // Inicializar los 12 meses en 0
        for (int m = 1; m <= 12; m++) {
            FacturacionMensual fm = new FacturacionMensual();
            fm.mes      = m;
            fm.cantidad = 0;
            fm.total    = 0.0;
            resultado.add(fm);
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, anio);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int mes   = rs.getInt("mes");
                    // Overwrite the zero-initialized slot
                    FacturacionMensual fm = resultado.get(mes - 1);
                    fm.cantidad = rs.getInt("cantidad");
                    // Puede ser null si no hay precio_cobrado
                    double total = rs.getDouble("total");
                    fm.total = rs.wasNull() ? 0.0 : total;
                }
            }
        }
        return resultado;
    }

    /**
     * Retorna estadísticas de uso por servicio (para el gráfico de torta).
     */
    public List<EstadisticaServicio> getEstadisticasPorServicio(LocalDate desde, LocalDate hasta) throws SQLException {
        List<EstadisticaServicio> resultado = new ArrayList<>();
        String sql =
            "SELECT s.nombre, COUNT(t.id) AS cantidad, SUM(t.precio_cobrado) AS total " +
            "FROM turnos t " +
            "JOIN servicios s ON s.id = t.servicio_id " +
            "WHERE t.estado = 'completado' " +
            "AND t.fecha BETWEEN ? AND ? " +
            "GROUP BY s.id, s.nombre " +
            "ORDER BY cantidad DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EstadisticaServicio es = new EstadisticaServicio();
                    es.servicio  = rs.getString("nombre");
                    es.cantidad  = rs.getInt("cantidad");
                    es.total     = rs.getDouble("total");
                    resultado.add(es);
                }
            }
        }
        return resultado;
    }

    /**
     * Total de ingresos del mes actual (para el resumen del dashboard).
     */
    public double getIngresosMesActual() throws SQLException {
        String sql = "SELECT COALESCE(SUM(precio_cobrado), 0) AS total " +
                     "FROM turnos " +
                     "WHERE YEAR(fecha) = YEAR(CURDATE()) " +
                     "AND MONTH(fecha) = MONTH(CURDATE()) " +
                     "AND estado IN ('completado', 'confirmado')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) return rs.getDouble("total");
        }
        return 0.0;
    }

    /**
     * Cuenta el total de clientes únicos.
     */
    public int getTotalClientes() throws SQLException {
        String sql = "SELECT COUNT(*) FROM clientes";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    // ============================================================
    // Slots disponibles
    // ============================================================

    /**
     * Calcula las horas ya ocupadas para una fecha dada.
     * Los slots se generan cada 30 minutos entre apertura y cierre.
     * Un slot está "ocupado" si hay un turno activo en esa hora.
     *
     * @param fecha fecha a consultar
     * @return lista de horas en formato "HH:mm" que ya están tomadas
     */
    public List<String> getHorasOcupadas(LocalDate fecha) throws SQLException {
        List<String> ocupadas = new ArrayList<>();
        String sql = "SELECT t.hora " +
                     "FROM turnos t " +
                     "WHERE t.fecha = ? AND t.estado != 'cancelado'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalTime hora = rs.getTime("hora").toLocalTime();
                    ocupadas.add(hora.toString().substring(0, 5)); // "HH:mm"
                }
            }
        }
        return ocupadas;
    }

    // ============================================================
    // Mapeo ResultSet → Objeto
    // ============================================================

    private Turno mapearCompleto(ResultSet rs) throws SQLException {
        Turno t = new Turno();
        t.setId(rs.getInt("id"));
        t.setClienteId(rs.getInt("cliente_id"));
        t.setServicioId(rs.getInt("servicio_id"));
        t.setFecha(rs.getDate("fecha").toLocalDate());
        t.setHora(rs.getTime("hora").toLocalTime());
        t.setEstado(rs.getString("estado"));
        t.setPrecioCobrado(rs.getBigDecimal("precio_cobrado"));
        t.setNotas(rs.getString("notas"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) t.setCreatedAt(created.toLocalDateTime());

        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) t.setUpdatedAt(updated.toLocalDateTime());

        // Datos del cliente (JOIN)
        try {
            Cliente c = new Cliente();
            c.setId(rs.getInt("cliente_id"));
            c.setNombre(rs.getString("cli_nombre"));
            c.setApellido(rs.getString("cli_apellido"));
            c.setDni(rs.getString("cli_dni"));
            c.setTelefono(rs.getString("cli_telefono"));
            t.setCliente(c);
        } catch (SQLException ignored) {}

        // Datos del servicio (JOIN)
        try {
            Servicio s = new Servicio();
            s.setId(rs.getInt("servicio_id"));
            s.setNombre(rs.getString("serv_nombre"));
            s.setPrecio(rs.getBigDecimal("serv_precio"));
            s.setDuracionMinutos(rs.getInt("duracion_minutos"));
            t.setServicio(s);
        } catch (SQLException ignored) {}

        return t;
    }

    // ============================================================
    // Clases auxiliares para estadísticas
    // ============================================================

    public static class EstadisticasDia {
        public int pendientes  = 0;
        public int confirmados = 0;
        public int completados = 0;
        public int cancelados  = 0;

        public int getTotal() { return pendientes + confirmados + completados + cancelados; }
    }

    public static class FacturacionMensual {
        public int    mes;
        public int    cantidad;
        public double total;
    }

    public static class EstadisticaServicio {
        public String servicio;
        public int    cantidad;
        public double total;
    }
}

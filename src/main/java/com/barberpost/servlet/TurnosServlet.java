package com.barberpost.servlet;

import com.barberpost.dao.TurnoDAO;
import com.barberpost.dao.ServicioDAO;
import com.barberpost.model.Servicio;
import com.barberpost.util.JsonUtil;

import javax.servlet.http.*;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * TurnosServlet — Controlador para gestión de turnos disponibles
 *
 * Endpoints:
 *   GET /api/turnos/disponibles?fecha=YYYY-MM-DD&servicioId=N
 *     → Retorna los horarios disponibles para reservar en esa fecha
 *
 *   GET /api/turnos/hoy
 *     → Retorna los turnos de hoy (requiere login)
 *
 *   PUT /api/turnos/{id}/estado
 *     → Cambia el estado de un turno (requiere login)
 *
 * Lógica de slots:
 *   - La barbería abre de 09:00 a 20:00 (configurable).
 *   - Los slots se generan cada 30 minutos.
 *   - Un slot está ocupado si hay algún turno activo en esa hora.
 *   - Los domingos no hay horario (la barbería está cerrada).
 *
 * @version 1.0.0
 */
public class TurnosServlet extends HttpServlet {

    private final TurnoDAO    turnoDAO    = new TurnoDAO();
    private final ServicioDAO servicioDAO = new ServicioDAO();

    // Configuración de horarios (se podría leer de la tabla 'horarios')
    private static final LocalTime APERTURA   = LocalTime.of(9,  0);
    private static final LocalTime CIERRE     = LocalTime.of(20, 0);
    private static final int       SLOT_MINS  = 30; // Intervalo entre turnos

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String pathInfo = req.getPathInfo() == null ? "" : req.getPathInfo();

        try {
            if (pathInfo.startsWith("/disponibles")) {
                manejarDisponibles(req, res);
            } else if (pathInfo.startsWith("/hoy")) {
                manejarTurnosHoy(req, res);
            } else {
                res.setStatus(404);
                res.getWriter().write(JsonUtil.error("Endpoint no encontrado"));
            }
        } catch (Exception e) {
            res.setStatus(500);
            res.getWriter().write(JsonUtil.error("Error al procesar turnos: " + e.getMessage()));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        // Requiere autenticación para cambiar estado de un turno
        if (!estaAutenticado(req)) {
            res.setStatus(401);
            res.getWriter().write(JsonUtil.error("Sesión no válida"));
            return;
        }

        try {
            // Path: /api/turnos/5/estado
            String pathInfo = req.getPathInfo();
            String[] partes = pathInfo.split("/");
            // partes[0]="" partes[1]=id partes[2]="estado"
            int    id      = Integer.parseInt(partes[1]);
            String nuevoEstado = JsonUtil.GSON.fromJson(req.getReader(),
                    Map.class).get("estado").toString();

            // Validar que el estado sea uno de los permitidos
            List<String> estadosValidos = Arrays.asList("pendiente","confirmado","completado","cancelado");
            if (!estadosValidos.contains(nuevoEstado)) {
                res.setStatus(400);
                res.getWriter().write(JsonUtil.error("Estado inválido: " + nuevoEstado));
                return;
            }

            boolean ok = turnoDAO.actualizarEstado(id, nuevoEstado);
            if (ok) {
                res.getWriter().write(JsonUtil.success(null, "Estado actualizado a: " + nuevoEstado));
            } else {
                res.setStatus(404);
                res.getWriter().write(JsonUtil.error("Turno no encontrado"));
            }

        } catch (Exception e) {
            res.setStatus(500);
            res.getWriter().write(JsonUtil.error("Error al actualizar estado: " + e.getMessage()));
        }
    }

    // ============================================================
    // Handlers internos
    // ============================================================

    /**
     * Calcula y retorna los slots disponibles para una fecha y servicio.
     * Solo retorna horas que aún no están tomadas.
     */
    private void manejarDisponibles(HttpServletRequest req, HttpServletResponse res)
            throws Exception {

        String fechaParam     = req.getParameter("fecha");
        String servicioParam  = req.getParameter("servicioId");

        if (fechaParam == null || fechaParam.isBlank()) {
            res.setStatus(400);
            res.getWriter().write(JsonUtil.error("Parámetro 'fecha' requerido (formato: YYYY-MM-DD)"));
            return;
        }

        LocalDate fecha = LocalDate.parse(fechaParam, DateTimeFormatter.ISO_LOCAL_DATE);

        // No se puede reservar en el pasado
        if (fecha.isBefore(LocalDate.now())) {
            res.setStatus(400);
            res.getWriter().write(JsonUtil.error("No se puede reservar para una fecha pasada"));
            return;
        }

        // Verificar si la barbería está abierta ese día (domingo = cerrado)
        DayOfWeek diaSemana = fecha.getDayOfWeek();
        if (diaSemana == DayOfWeek.SUNDAY) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("disponibles", new ArrayList<>());
            resp.put("mensaje", "La barbería no atiende los domingos");
            resp.put("cerrado", true);
            res.getWriter().write(JsonUtil.success(resp));
            return;
        }

        // Horario reducido los sábados: cierre a las 18:00
        LocalTime cierreDelDia = (diaSemana == DayOfWeek.SATURDAY)
                ? LocalTime.of(18, 0)
                : CIERRE;

        // Obtener horas ya ocupadas
        List<String> ocupadas = turnoDAO.getHorasOcupadas(fecha);

        // Generar todos los slots con su estado de disponibilidad
        List<Map<String, Object>> todosLosSlots = new ArrayList<>();
        for (String slot : generarSlots(APERTURA, cierreDelDia, SLOT_MINS)) {
            Map<String, Object> s = new HashMap<>();
            s.put("hora",       slot);
            s.put("disponible", !ocupadas.contains(slot));
            todosLosSlots.add(s);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("fecha",       fechaParam);
        resp.put("disponibles", todosLosSlots);
        resp.put("ocupados",    ocupadas.size());
        resp.put("cerrado",     false);
        res.getWriter().write(JsonUtil.success(resp));
    }

    /**
     * Retorna los turnos de hoy para el dashboard.
     */
    private void manejarTurnosHoy(HttpServletRequest req, HttpServletResponse res)
            throws Exception {

        if (!estaAutenticado(req)) {
            res.setStatus(401);
            res.getWriter().write(JsonUtil.error("Sesión no válida"));
            return;
        }

        var turnos = turnoDAO.listarPorFecha(LocalDate.now());
        res.getWriter().write(JsonUtil.success(turnos));
    }

    // ============================================================
    // Utilitarios
    // ============================================================

    /**
     * Genera una lista de strings "HH:mm" desde apertura hasta cierre
     * con el intervalo indicado.
     */
    private List<String> generarSlots(LocalTime desde, LocalTime hasta, int intervalMinutos) {
        List<String> slots = new ArrayList<>();
        LocalTime cursor   = desde;
        while (cursor.isBefore(hasta)) {
            slots.add(cursor.format(DateTimeFormatter.ofPattern("HH:mm")));
            cursor = cursor.plusMinutes(intervalMinutos);
        }
        return slots;
    }

    private boolean estaAutenticado(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && session.getAttribute("usuario") != null;
    }
}

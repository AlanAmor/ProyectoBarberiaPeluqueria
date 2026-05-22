package com.barberpost.servlet;

import com.barberpost.dao.ClienteDAO;
import com.barberpost.dao.ServicioDAO;
import com.barberpost.dao.TurnoDAO;
import com.barberpost.dao.UsuarioDAO;
import com.barberpost.model.Usuario;
import com.barberpost.util.JsonUtil;

import javax.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/**
 * DashboardServlet — Controlador de datos para el dashboard administrativo
 *
 * REQUIERE sesión activa (protegido por AuthFilter).
 *
 * Endpoints:
 *   GET /api/dashboard/resumen        → Tarjetas de resumen (hoy, mes, total)
 *   GET /api/dashboard/facturacion    → Facturación mensual del año actual
 *   GET /api/dashboard/servicios      → Estadísticas por servicio
 *   GET /api/dashboard/clientes-top   → Clientes más frecuentes
 *   GET /api/dashboard/usuarios       → Lista de usuarios (solo owner)
 *
 * @version 1.0.0
 */
public class DashboardServlet extends HttpServlet {

    private final TurnoDAO    turnoDAO    = new TurnoDAO();
    private final ClienteDAO  clienteDAO  = new ClienteDAO();
    private final ServicioDAO servicioDAO = new ServicioDAO();
    private final UsuarioDAO  usuarioDAO  = new UsuarioDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String pathInfo = req.getPathInfo() == null ? "" : req.getPathInfo();

        try {
            switch (pathInfo) {

                case "/resumen":
                    manejarResumen(req, res);
                    break;

                case "/facturacion":
                    manejarFacturacion(req, res);
                    break;

                case "/servicios":
                    manejarEstadisticasServicios(req, res);
                    break;

                case "/clientes-top":
                    manejarClientesTop(req, res);
                    break;

                case "/usuarios":
                    manejarUsuarios(req, res);
                    break;

                case "/turnos-hoy":
                    manejarTurnosHoy(req, res);
                    break;

                case "/turnos-semana":
                    manejarTurnosSemana(req, res);
                    break;

                default:
                    res.setStatus(404);
                    res.getWriter().write(JsonUtil.error("Endpoint no encontrado: " + pathInfo));
            }

        } catch (Exception e) {
            res.setStatus(500);
            res.getWriter().write(JsonUtil.error("Error en dashboard: " + e.getMessage()));
        }
    }

    // ============================================================
    // Handlers
    // ============================================================

    /**
     * Retorna las tarjetas resumen del dashboard:
     *   - Turnos de hoy (total y por estado)
     *   - Ingresos del mes
     *   - Total de clientes registrados
     *   - Próximos turnos (pendientes + confirmados)
     */
    private void manejarResumen(HttpServletRequest req, HttpServletResponse res)
            throws Exception {

        TurnoDAO.EstadisticasDia estadisticasHoy = turnoDAO.getEstadisticasHoy();
        double ingresosMes = turnoDAO.getIngresosMesActual();
        int totalClientes  = turnoDAO.getTotalClientes();

        // Turnos de los próximos 7 días
        var proximos = turnoDAO.listar(LocalDate.now(), LocalDate.now().plusDays(7), "pendiente");
        proximos.addAll(turnoDAO.listar(LocalDate.now(), LocalDate.now().plusDays(7), "confirmado"));
        proximos.sort(Comparator.comparing(t -> t.getFecha().toString() + t.getHora().toString()));

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("hoy",           estadisticasHoy);
        resumen.put("ingresosMes",   ingresosMes);
        resumen.put("totalClientes", totalClientes);
        resumen.put("proximos",      proximos.subList(0, Math.min(10, proximos.size())));

        res.getWriter().write(JsonUtil.success(resumen));
    }

    /**
     * Retorna la facturación mensual del año en curso para el gráfico
     * de barras del dashboard (12 meses, incluyendo meses en cero).
     */
    private void manejarFacturacion(HttpServletRequest req, HttpServletResponse res)
            throws Exception {

        String anioParam = req.getParameter("anio");
        int anio = (anioParam != null) ? Integer.parseInt(anioParam) : LocalDate.now().getYear();

        var facturacion = turnoDAO.getFacturacionPorMes(anio);

        Map<String, Object> data = new HashMap<>();
        data.put("anio",       anio);
        data.put("meses",      facturacion);

        res.getWriter().write(JsonUtil.success(data));
    }

    /**
     * Retorna las estadísticas por tipo de servicio para el período indicado
     * (gráfico de torta del dashboard).
     */
    private void manejarEstadisticasServicios(HttpServletRequest req, HttpServletResponse res)
            throws Exception {

        // Default: mes actual
        String desdeParam = req.getParameter("desde");
        String hastaParam = req.getParameter("hasta");

        LocalDate desde = (desdeParam != null)
            ? LocalDate.parse(desdeParam)
            : LocalDate.now().withDayOfMonth(1);
        LocalDate hasta = (hastaParam != null)
            ? LocalDate.parse(hastaParam)
            : LocalDate.now();

        var estadisticas = turnoDAO.getEstadisticasPorServicio(desde, hasta);
        res.getWriter().write(JsonUtil.success(estadisticas));
    }

    /**
     * Retorna el top de clientes más frecuentes por cantidad de visitas.
     * Solo accesible para el owner.
     */
    private void manejarClientesTop(HttpServletRequest req, HttpServletResponse res)
            throws Exception {

        if (!esOwner(req)) {
            res.setStatus(403);
            res.getWriter().write(JsonUtil.error("Solo el propietario puede ver esta sección"));
            return;
        }

        String limiteParam = req.getParameter("limite");
        int limite = (limiteParam != null) ? Integer.parseInt(limiteParam) : 10;

        var topClientes = clienteDAO.listarMasFrecuentes(limite);
        res.getWriter().write(JsonUtil.success(topClientes));
    }

    /**
     * Lista los usuarios del sistema. Solo para el owner.
     */
    private void manejarUsuarios(HttpServletRequest req, HttpServletResponse res)
            throws Exception {

        if (!esOwner(req)) {
            res.setStatus(403);
            res.getWriter().write(JsonUtil.error("Acceso restringido al propietario"));
            return;
        }

        var usuarios = usuarioDAO.listarTodos();
        // Eliminar los hashes antes de enviar
        usuarios.forEach(u -> u.setPasswordHash(null));
        res.getWriter().write(JsonUtil.success(usuarios));
    }

    /**
     * Retorna los turnos de hoy (para empleados).
     */
    private void manejarTurnosHoy(HttpServletRequest req, HttpServletResponse res)
            throws Exception {

        var turnos = turnoDAO.listarPorFecha(LocalDate.now());
        res.getWriter().write(JsonUtil.success(turnos));
    }

    /**
     * Retorna los turnos de la semana actual.
     */
    private void manejarTurnosSemana(HttpServletRequest req, HttpServletResponse res)
            throws Exception {

        LocalDate hoy   = LocalDate.now();
        LocalDate inicio = hoy.with(java.time.DayOfWeek.MONDAY);
        LocalDate fin    = hoy.with(java.time.DayOfWeek.SATURDAY);

        var turnos = turnoDAO.listar(inicio, fin, null);

        // --- Agrupar por día ---
        Map<String, List<Object>> porDia = new LinkedHashMap<>();
        LocalDate dia = inicio;
        while (!dia.isAfter(fin)) {
            porDia.put(dia.toString(), new ArrayList<>());
            dia = dia.plusDays(1);
        }
        for (var t : turnos) {
            String clave = t.getFecha().toString();
            if (porDia.containsKey(clave)) {
                porDia.get(clave).add(t);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("inicio",  inicio.toString());
        data.put("fin",     fin.toString());
        data.put("porDia",  porDia);
        data.put("total",   turnos.size());

        res.getWriter().write(JsonUtil.success(data));
    }

    // ============================================================
    // Helpers
    // ============================================================

    private boolean esOwner(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return false;
        Object attr = session.getAttribute("usuario");
        return (attr instanceof Usuario) && ((Usuario) attr).isOwner();
    }
}

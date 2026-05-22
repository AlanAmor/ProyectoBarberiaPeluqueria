package com.barberpost.servlet;

import com.barberpost.dao.ServicioDAO;
import com.barberpost.model.Servicio;
import com.barberpost.model.Usuario;
import com.barberpost.util.JsonUtil;

import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * ServiciosServlet — Controlador REST para el catálogo de servicios
 *
 * Endpoints:
 *   GET  /api/servicios          → Lista servicios activos (público)
 *   GET  /api/servicios/todos    → Lista todos (requiere login)
 *   PUT  /api/servicios/{id}     → Actualiza precio/datos (requiere owner)
 *   POST /api/servicios/{id}/toggle → Activa/desactiva (requiere owner)
 *
 * @version 1.0.0
 */
public class ServiciosServlet extends HttpServlet {

    private final ServicioDAO servicioDAO = new ServicioDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String pathInfo = req.getPathInfo(); // null o "/todos"

        try {
            if ("/todos".equals(pathInfo)) {
                // Solo usuarios autenticados pueden ver la lista completa
                if (!estaAutenticado(req)) {
                    res.setStatus(401);
                    res.getWriter().write(JsonUtil.error("Acceso no autorizado"));
                    return;
                }
                List<Servicio> todos = servicioDAO.listarTodos();
                res.getWriter().write(JsonUtil.success(todos));
            } else {
                // Lista pública: solo activos
                List<Servicio> activos = servicioDAO.listarActivos();
                res.getWriter().write(JsonUtil.success(activos));
            }

        } catch (Exception e) {
            res.setStatus(500);
            res.getWriter().write(JsonUtil.error("Error al obtener servicios: " + e.getMessage()));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        // Solo el propietario puede modificar precios
        if (!esOwner(req)) {
            res.setStatus(403);
            res.getWriter().write(JsonUtil.error("Solo el propietario puede modificar precios"));
            return;
        }

        try {
            // Extraer ID del path: /api/servicios/5
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                res.setStatus(400);
                res.getWriter().write(JsonUtil.error("ID de servicio requerido"));
                return;
            }
            int id = Integer.parseInt(pathInfo.substring(1));

            // Leer parámetros del body JSON
            Servicio data = JsonUtil.GSON.fromJson(req.getReader(), Servicio.class);

            if (data.getPrecio() != null) {
                servicioDAO.actualizarPrecio(id, data.getPrecio());
            }

            Servicio actualizado = servicioDAO.buscarPorId(id);
            res.getWriter().write(JsonUtil.success(actualizado, "Precio actualizado correctamente"));

        } catch (NumberFormatException e) {
            res.setStatus(400);
            res.getWriter().write(JsonUtil.error("ID de servicio inválido"));
        } catch (Exception e) {
            res.setStatus(500);
            res.getWriter().write(JsonUtil.error("Error al actualizar servicio: " + e.getMessage()));
        }
    }

    // ============================================================
    // Helpers de autenticación
    // ============================================================

    private boolean estaAutenticado(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && session.getAttribute("usuario") instanceof Usuario;
    }

    private boolean esOwner(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return false;
        Object attr = session.getAttribute("usuario");
        if (!(attr instanceof Usuario)) return false;
        return ((Usuario) attr).isOwner();
    }
}

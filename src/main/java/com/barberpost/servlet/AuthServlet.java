package com.barberpost.servlet;

import com.barberpost.dao.UsuarioDAO;
import com.barberpost.model.Usuario;
import com.barberpost.util.JsonUtil;

import javax.servlet.http.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * AuthServlet — Controlador de autenticación
 *
 * Endpoints:
 *   POST /api/auth/login   → Iniciar sesión (retorna datos del usuario)
 *   POST /api/auth/logout  → Cerrar sesión (invalida la sesión HTTP)
 *   GET  /api/auth/me      → Retorna datos del usuario autenticado actualmente
 *
 * La autenticación usa sesiones HTTP (cookies de sesión estándar).
 * No se implementan tokens JWT en el prototipo, pero la
 * arquitectura lo permite si se quiere migrar.
 *
 * @version 1.0.0
 */
public class AuthServlet extends HttpServlet {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    // ============================================================
    // POST — Login y Logout
    // ============================================================

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String pathInfo = req.getPathInfo() == null ? "" : req.getPathInfo();

        if (pathInfo.startsWith("/login")) {
            manejarLogin(req, res);
        } else if (pathInfo.startsWith("/logout")) {
            manejarLogout(req, res);
        } else {
            res.setStatus(404);
            res.getWriter().write(JsonUtil.error("Endpoint no encontrado"));
        }
    }

    // ============================================================
    // GET — Sesión actual
    // ============================================================

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String pathInfo = req.getPathInfo() == null ? "" : req.getPathInfo();

        if (pathInfo.startsWith("/me")) {
            HttpSession session = req.getSession(false);
            if (session == null || !(session.getAttribute("usuario") instanceof Usuario)) {
                res.setStatus(401);
                res.getWriter().write(JsonUtil.error("No hay sesión activa"));
                return;
            }
            Usuario u = (Usuario) session.getAttribute("usuario");
            res.getWriter().write(JsonUtil.success(buildSafeUserMap(u)));
        } else {
            res.setStatus(404);
            res.getWriter().write(JsonUtil.error("Endpoint no encontrado"));
        }
    }

    // ============================================================
    // Handlers internos
    // ============================================================

    /**
     * Procesa el login.
     * Recibe JSON: { "username": "...", "password": "..." }
     * Retorna los datos del usuario y su rol.
     */
    private void manejarLogin(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        try {
            @SuppressWarnings("unchecked")
            Map<String, String> body = JsonUtil.GSON.fromJson(req.getReader(), Map.class);

            if (body == null || !body.containsKey("username") || !body.containsKey("password")) {
                res.setStatus(400);
                res.getWriter().write(JsonUtil.error("Usuario y contraseña son requeridos"));
                return;
            }

            String username = body.get("username").trim();
            String password = body.get("password");

            // Intentar autenticar
            Usuario usuario = usuarioDAO.autenticar(username, password);

            if (usuario == null) {
                // No revelar si el usuario existe o la contraseña es incorrecta
                res.setStatus(401);
                res.getWriter().write(JsonUtil.error("Credenciales incorrectas"));
                return;
            }

            // Invalidar cualquier sesión anterior (previene session fixation)
            HttpSession sesionAnterior = req.getSession(false);
            if (sesionAnterior != null) sesionAnterior.invalidate();

            // Crear nueva sesión con el usuario autenticado
            HttpSession session = req.getSession(true);
            session.setAttribute("usuario", usuario);
            session.setMaxInactiveInterval(3600); // 60 minutos

            res.getWriter().write(JsonUtil.success(buildSafeUserMap(usuario), "Sesión iniciada correctamente"));

        } catch (Exception e) {
            res.setStatus(500);
            res.getWriter().write(JsonUtil.error("Error al iniciar sesión: " + e.getMessage()));
        }
    }

    /**
     * Cierra la sesión del usuario e invalida la cookie de sesión.
     */
    private void manejarLogout(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        res.getWriter().write(JsonUtil.success(null, "Sesión cerrada correctamente"));
    }

    // ============================================================
    // Helpers
    // ============================================================

    /**
     * Construye un Map con los datos del usuario sin exponer el hash de contraseña.
     * Este es el objeto que se envía al frontend.
     */
    private Map<String, Object> buildSafeUserMap(Usuario u) {
        Map<String, Object> map = new HashMap<>();
        map.put("id",       u.getId());
        map.put("username", u.getUsername());
        map.put("nombre",   u.getNombre());
        map.put("rol",      u.getRol());
        map.put("isOwner",  u.isOwner());
        return map;
    }
}

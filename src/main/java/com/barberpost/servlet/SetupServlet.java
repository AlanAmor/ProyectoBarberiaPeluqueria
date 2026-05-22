package com.barberpost.servlet;

import com.barberpost.dao.DatabaseConnection;
import com.barberpost.dao.UsuarioDAO;
import com.barberpost.model.Usuario;
import com.barberpost.util.JsonUtil;

import javax.servlet.http.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * SetupServlet — Inicialización del sistema (primer arranque)
 *
 * Endpoint:
 *   GET /api/setup/init
 *
 * Sólo actúa si NO hay usuarios en la base de datos.
 * Crea el administrador "admin" (contraseña: admin123) y un
 * empleado de prueba "empleado1" (contraseña: emp123).
 *
 * ⚠️ IMPORTANTE: En producción, deshabilitar o proteger este endpoint
 *    después del primer uso.
 *
 * @version 1.0.0
 */
public class SetupServlet extends HttpServlet {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        res.setContentType("application/json;charset=UTF-8");

        try {
            // Verificar conectividad con la DB
            if (!DatabaseConnection.isAvailable()) {
                res.setStatus(503);
                res.getWriter().write(JsonUtil.error(
                    "No se puede conectar a la base de datos. " +
                    "Verificá que MySQL esté corriendo y que la BD 'barberpost' exista."
                ));
                return;
            }

            // Solo crear usuarios si no hay ninguno
            if (usuarioDAO.hayUsuarios()) {
                Map<String, Object> data = new HashMap<>();
                data.put("mensaje", "El sistema ya fue inicializado. Este endpoint solo funciona la primera vez.");
                data.put("yaConfigurado", true);
                res.getWriter().write(JsonUtil.success(data));
                return;
            }

            // Crear usuario administrador
            Usuario admin = new Usuario("admin", "Administrador Barber Post", "owner");
            usuarioDAO.crear(admin, "admin123");

            // Crear usuario empleado de prueba
            Usuario empleado = new Usuario("empleado1", "Juan García", "employee");
            usuarioDAO.crear(empleado, "emp123");

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("mensaje",      "Sistema inicializado correctamente.");
            resultado.put("usuariosCreados", 2);
            resultado.put("credenciales",
                "admin / admin123 (propietario) | empleado1 / emp123 (empleado)");
            resultado.put("aviso",
                "Cambiá las contraseñas de inmediato desde el panel de administración.");

            res.setStatus(201);
            res.getWriter().write(JsonUtil.success(resultado));

        } catch (Exception e) {
            res.setStatus(500);
            res.getWriter().write(JsonUtil.error(
                "Error durante la inicialización: " + e.getMessage()
            ));
        }
    }
}

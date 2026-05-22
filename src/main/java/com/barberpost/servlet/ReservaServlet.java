package com.barberpost.servlet;

import com.barberpost.dao.ClienteDAO;
import com.barberpost.dao.ServicioDAO;
import com.barberpost.dao.TurnoDAO;
import com.barberpost.model.Cliente;
import com.barberpost.model.Servicio;
import com.barberpost.model.Turno;
import com.barberpost.model.Usuario;
import com.barberpost.util.JsonUtil;

import javax.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ReservaServlet — Controlador principal de reservas
 *
 * Endpoints:
 *   POST /api/reservas            → Crear nueva reserva (público)
 *   GET  /api/reservas/all        → Listar todas las reservas (requiere login)
 *   GET  /api/reservas/{id}       → Detalle de una reserva (requiere login)
 *   PUT  /api/reservas/{id}       → Actualizar estado/notas (requiere login)
 *   GET  /api/reservas/historial?dni=X → Historial por DNI del cliente
 *
 * Al crear una reserva exitosamente, la respuesta incluye un
 * "whatsappUrl" listo para abrir en el navegador y enviar el mensaje
 * de confirmación automáticamente al teléfono del cliente.
 *
 * @version 1.0.0
 */
public class ReservaServlet extends HttpServlet {

    private final ClienteDAO  clienteDAO  = new ClienteDAO();
    private final ServicioDAO servicioDAO = new ServicioDAO();
    private final TurnoDAO    turnoDAO    = new TurnoDAO();

    // ============================================================
    // POST — Crear una nueva reserva
    // ============================================================

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        try {
            // Leer el cuerpo JSON de la petición
            ReservaRequest data = JsonUtil.GSON.fromJson(req.getReader(), ReservaRequest.class);

            // ---- Validaciones básicas ----
            if (data == null) {
                res.setStatus(400);
                res.getWriter().write(JsonUtil.error("Cuerpo de la solicitud vacío o inválido"));
                return;
            }
            String validacion = validarDatos(data);
            if (validacion != null) {
                res.setStatus(400);
                res.getWriter().write(JsonUtil.error(validacion));
                return;
            }

            // ---- Verificar que el servicio exista y esté activo ----
            Servicio servicio = servicioDAO.buscarPorId(data.servicioId);
            if (servicio == null || !servicio.isActivo()) {
                res.setStatus(400);
                res.getWriter().write(JsonUtil.error("El servicio seleccionado no está disponible"));
                return;
            }

            // ---- Verificar que el slot no está ya ocupado ----
            LocalDate fecha = LocalDate.parse(data.fecha);
            LocalTime hora  = LocalTime.parse(data.hora);
            List<String> ocupadas = turnoDAO.getHorasOcupadas(fecha);
            if (ocupadas.contains(hora.format(DateTimeFormatter.ofPattern("HH:mm")))) {
                res.setStatus(409);
                res.getWriter().write(JsonUtil.error("El horario " + data.hora + " ya no está disponible. Por favor seleccioná otro."));
                return;
            }

            // ---- Guardar o actualizar el cliente ----
            Cliente cliente = new Cliente(
                    capitalizar(data.nombre),
                    capitalizar(data.apellido),
                    data.dni.trim(),
                    data.telefono.trim(),
                    data.email != null ? data.email.trim() : null
            );
            clienteDAO.guardarOActualizar(cliente);

            // ---- Crear el turno ----
            Turno turno = new Turno(
                    cliente.getId(),
                    servicio.getId(),
                    fecha,
                    hora,
                    servicio.getPrecio() // snapshot del precio actual
            );
            turnoDAO.crear(turno);

            // ---- Generar link de WhatsApp ----
            String whatsappUrl = generarWhatsAppUrl(cliente, turno, servicio);

            // ---- Construir respuesta ----
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("turnoId",     turno.getId());
            resultado.put("whatsappUrl", whatsappUrl);
            resultado.put("cliente",     cliente);
            resultado.put("servicio",    servicio.getNombre());
            resultado.put("fecha",       fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            resultado.put("hora",        data.hora);
            resultado.put("precio",      servicio.getPrecio());

            res.setStatus(201); // HTTP 201 Created
            res.getWriter().write(JsonUtil.success(resultado, "¡Reserva creada con éxito!"));

        } catch (Exception e) {
            res.setStatus(500);
            res.getWriter().write(JsonUtil.error("Error interno al crear la reserva: " + e.getMessage()));
        }
    }

    // ============================================================
    // GET — Consultas
    // ============================================================

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String pathInfo = req.getPathInfo() == null ? "" : req.getPathInfo();

        try {
            if (pathInfo.startsWith("/all")) {
                // Requiere login
                if (!estaAutenticado(req)) {
                    res.setStatus(401);
                    res.getWriter().write(JsonUtil.error("No autorizado"));
                    return;
                }
                String desde   = req.getParameter("desde");
                String hasta   = req.getParameter("hasta");
                String estado  = req.getParameter("estado");

                LocalDate desdeFecha = desde != null ? LocalDate.parse(desde) : LocalDate.now().minusDays(30);
                LocalDate hastaFecha = hasta != null ? LocalDate.parse(hasta) : LocalDate.now().plusDays(30);

                var turnos = turnoDAO.listar(desdeFecha, hastaFecha, estado);
                res.getWriter().write(JsonUtil.success(turnos));

            } else if (pathInfo.startsWith("/historial")) {
                // Historial del cliente por DNI
                String dni = req.getParameter("dni");
                if (dni == null || dni.isBlank()) {
                    res.setStatus(400);
                    res.getWriter().write(JsonUtil.error("Parámetro 'dni' requerido"));
                    return;
                }
                Cliente cliente = clienteDAO.buscarPorDni(dni.trim());
                if (cliente == null) {
                    res.getWriter().write(JsonUtil.success(new ArrayList<>()));
                    return;
                }
                // Retornar turnos del último año para ese cliente
                var turnos = turnoDAO.listar(LocalDate.now().minusYears(1), LocalDate.now(), null);
                // Filtrar por clienteId
                turnos.removeIf(t -> t.getClienteId() != cliente.getId());
                res.getWriter().write(JsonUtil.success(turnos));

            } else if (!pathInfo.isEmpty() && !pathInfo.equals("/")) {
                // Detalle de un turno por ID
                if (!estaAutenticado(req)) {
                    res.setStatus(401);
                    res.getWriter().write(JsonUtil.error("No autorizado"));
                    return;
                }
                int id = Integer.parseInt(pathInfo.substring(1));
                Turno turno = turnoDAO.buscarPorId(id);
                if (turno == null) {
                    res.setStatus(404);
                    res.getWriter().write(JsonUtil.error("Turno no encontrado"));
                    return;
                }
                res.getWriter().write(JsonUtil.success(turno));

            } else {
                res.setStatus(400);
                res.getWriter().write(JsonUtil.error("Endpoint no válido"));
            }

        } catch (Exception e) {
            res.setStatus(500);
            res.getWriter().write(JsonUtil.error("Error al obtener reservas: " + e.getMessage()));
        }
    }

    // ============================================================
    // PUT — Actualizar reserva
    // ============================================================

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        if (!estaAutenticado(req)) {
            res.setStatus(401);
            res.getWriter().write(JsonUtil.error("No autorizado"));
            return;
        }

        try {
            String pathInfo = req.getPathInfo();
            int id = Integer.parseInt(pathInfo.substring(1));

            @SuppressWarnings("unchecked")
            Map<String, Object> body = JsonUtil.GSON.fromJson(req.getReader(), Map.class);

            if (body.containsKey("estado")) {
                String estado = body.get("estado").toString();
                turnoDAO.actualizarEstado(id, estado);
            }
            if (body.containsKey("notas")) {
                String notas = body.get("notas").toString();
                turnoDAO.actualizarNotas(id, notas);
            }

            Turno actualizado = turnoDAO.buscarPorId(id);
            res.getWriter().write(JsonUtil.success(actualizado, "Turno actualizado"));

        } catch (NumberFormatException e) {
            res.setStatus(400);
            res.getWriter().write(JsonUtil.error("ID de turno inválido"));
        } catch (Exception e) {
            res.setStatus(500);
            res.getWriter().write(JsonUtil.error("Error al actualizar el turno: " + e.getMessage()));
        }
    }

    // ============================================================
    // Generación del link de WhatsApp
    // ============================================================

    /**
     * Genera la URL de WhatsApp Click-to-Chat con el mensaje de confirmación.
     * No requiere API de pago: usa el endpoint público wa.me de Meta.
     *
     * Formato: https://wa.me/54XXXXXXXXXX?text=MENSAJE_CODIFICADO
     *
     * El cliente sólo debe abrir el link y presionar "Enviar".
     */
    private String generarWhatsAppUrl(Cliente cliente, Turno turno, Servicio servicio) {
        String telefono = cliente.getTelefonoWhatsApp();

        String fechaFormateada = turno.getFecha()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String horaFormateada  = turno.getHora()
                .format(DateTimeFormatter.ofPattern("HH:mm"));

        String mensaje =
            "¡Hola " + cliente.getNombre() + "! 💈\n\n" +
            "Tu turno en *Barber Post* fue solicitado con éxito.\n\n" +
            "📋 *Detalles de tu reserva:*\n" +
            "• Servicio: " + servicio.getNombre() + "\n" +
            "• Fecha: " + fechaFormateada + "\n" +
            "• Hora: " + horaFormateada + "\n" +
            "• Precio: $" + servicio.getPrecio().toPlainString() + "\n\n" +
            "📍 Encontranos en nuestra barbería.\n" +
            "⏰ Por favor llegá 5 minutos antes.\n\n" +
            "Si necesitás cancelar o cambiar el turno, escribinos con anticipación.\n\n" +
            "¡Nos vemos! ✂️";

        String mensajeCodificado = URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
        return "https://wa.me/" + telefono + "?text=" + mensajeCodificado;
    }

    // ============================================================
    // Validaciones
    // ============================================================

    /**
     * Valida los campos requeridos de la solicitud de reserva.
     * Retorna null si todo está OK, o el mensaje de error.
     */
    private String validarDatos(ReservaRequest data) {
        if (data.nombre == null || data.nombre.isBlank())
            return "El nombre es requerido";
        if (data.apellido == null || data.apellido.isBlank())
            return "El apellido es requerido";
        if (data.dni == null || data.dni.isBlank())
            return "El DNI es requerido";
        if (data.telefono == null || data.telefono.isBlank())
            return "El teléfono es requerido";
        if (data.fecha == null || data.fecha.isBlank())
            return "La fecha es requerida (formato: YYYY-MM-DD)";
        if (data.hora == null || data.hora.isBlank())
            return "La hora es requerida (formato: HH:mm)";
        if (data.servicioId <= 0)
            return "Debe seleccionar un servicio";

        // Validar formato de fecha
        try {
            LocalDate.parse(data.fecha);
        } catch (Exception e) {
            return "Formato de fecha inválido. Use YYYY-MM-DD";
        }

        // Validar formato de hora
        try {
            LocalTime.parse(data.hora);
        } catch (Exception e1) {
            try {
                LocalTime.parse(data.hora + ":00");
            } catch (Exception e2) {
                return "Formato de hora inválido. Use HH:mm";
            }
        }

        return null;
    }

    /** Capitaliza la primera letra de un nombre */
    private String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) return texto;
        String trimmed = texto.trim();
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1).toLowerCase();
    }

    private boolean estaAutenticado(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && session.getAttribute("usuario") instanceof Usuario;
    }

    // ============================================================
    // DTO de entrada para deserializar el JSON de creación de reserva
    // ============================================================

    private static class ReservaRequest {
        String nombre;
        String apellido;
        String dni;
        String telefono;
        String email;
        String fecha;       // "YYYY-MM-DD"
        String hora;        // "HH:mm"
        int    servicioId;
    }
}

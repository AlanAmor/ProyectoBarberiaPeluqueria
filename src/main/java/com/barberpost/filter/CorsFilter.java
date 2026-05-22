package com.barberpost.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * CorsFilter — Filtro de cabeceras CORS
 *
 * Agrega las cabeceras necesarias para que el frontend pueda
 * comunicarse con el backend cuando están en distinto origen
 * (ej: desarrollo con live-reload en otro puerto).
 *
 * En producción con frontend y backend en el mismo dominio,
 * este filtro puede restringirse o eliminarse.
 *
 * @version 1.0.0
 */
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        // Permitir llamadas desde cualquier origen (ajustar en producción)
        response.setHeader("Access-Control-Allow-Origin",  "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setHeader("Access-Control-Max-Age",       "3600");

        // Las solicitudes OPTIONS son de preflight de CORS, responder OK
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // Todas las respuestas de la API son JSON con UTF-8
        response.setContentType("application/json;charset=UTF-8");

        chain.doFilter(req, res);
    }

    @Override public void init(FilterConfig filterConfig) {}
    @Override public void destroy() {}
}

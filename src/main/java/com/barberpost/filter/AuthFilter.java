package com.barberpost.filter;

import com.barberpost.model.Usuario;
import com.barberpost.util.JsonUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * AuthFilter — Filtro de autenticación para rutas protegidas
 *
 * Intercepta todas las peticiones a /api/dashboard/* y valida
 * que el usuario tenga una sesión activa.
 * Si no hay sesión, retorna HTTP 401 con mensaje JSON.
 *
 * @version 1.0.0
 */
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        HttpSession session = request.getSession(false);

        // Verificar que la sesión existe y tiene usuario autenticado
        boolean autenticado = session != null
                && session.getAttribute("usuario") instanceof Usuario;

        if (!autenticado) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(JsonUtil.error("Sesión expirada. Por favor, inicie sesión nuevamente."));
            return;
        }

        chain.doFilter(req, res);
    }

    @Override public void init(FilterConfig filterConfig) {}
    @Override public void destroy() {}
}

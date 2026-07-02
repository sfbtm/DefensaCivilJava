package com.defensacivil.config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filtro para el manejo del Intercambio de Recursos de Origen Cruzado (CORS).
 * Permite configurar las cabeceras HTTP necesarias para habilitar peticiones
 * desde diferentes dominios, así como gestionar solicitudes de tipo preflight (OPTIONS).
 */
@WebFilter("/*")
public class CorsFilter implements Filter {

    /**
     * Inicializa el filtro CORS.
     *
     * @param filterConfig Configuración del filtro proporcionada por el contenedor de servlets.
     * @throws ServletException Si ocurre un error durante la inicialización.
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    /**
     * Filtra las solicitudes HTTP entrantes para agregar las cabeceras de CORS correspondientes.
     * También intercepta y responde inmediatamente a las peticiones preflight de tipo OPTIONS.
     *
     * @param request  La solicitud servlet entrante.
     * @param response La respuesta servlet saliente.
     * @param chain    La cadena de filtros a seguir.
     * @throws IOException      Si ocurre un error de E/S al filtrar la petición.
     * @throws ServletException Si ocurre un error en el procesamiento de la solicitud.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // Obtener el origen de la solicitud
        String origin = req.getHeader("Origin");
        if (origin != null) {
            resp.setHeader("Access-Control-Allow-Origin", origin);
        } else {
            resp.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        }
        
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept, X-Requested-With");
        resp.setHeader("Access-Control-Max-Age", "3600");

        // Si es una peticion preflight (OPTIONS), responder con 200 OK inmediatamente
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * Destruye el filtro, liberando los recursos asignados en el ciclo de vida del filtro.
     */
    @Override
    public void destroy() {
    }
}

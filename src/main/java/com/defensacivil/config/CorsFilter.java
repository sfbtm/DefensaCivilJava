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
        // Bloque de inicialización del filtro: no se requiere lógica de inicialización específica para CORS
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
        // Bloque de ejecución principal del filtro CORS
        
        // Convertir los objetos genéricos de solicitud y respuesta a sus equivalentes HTTP para acceder a cabeceras y métodos específicos
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // Obtener el origen de la solicitud HTTP entrante para permitir accesos selectivos
        String origin = req.getHeader("Origin");
        
        // Bloque condicional: Verificar si la solicitud incluye una cabecera de origen cruzado (Origin)
        if (origin != null) {
            // Bloque de origen presente: permitir el origen específico que realizó la solicitud
            resp.setHeader("Access-Control-Allow-Origin", origin);
        } else {
            // Bloque de origen ausente: establecer un valor predeterminado seguro para desarrollo local
            resp.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        }
        
        // Indicar al cliente que se permiten credenciales en la petición (cookies, cabeceras de autorización, etc.)
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        // Especificar los métodos HTTP permitidos para interactuar con la API
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
        // Indicar qué cabeceras HTTP personalizadas se permiten enviar en las peticiones reales
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept, X-Requested-With");
        // Definir el tiempo de vida (en segundos) en caché para el resultado de una petición preflight
        resp.setHeader("Access-Control-Max-Age", "3600");

        // Bloque condicional: Verificar si el método HTTP de la solicitud es OPTIONS (petición de preflight)
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            // Bloque preflight OPTIONS: responder con un código 200 OK inmediatamente sin continuar en la cadena de filtros
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // Bloque de flujo normal: continuar con el procesamiento de la solicitud enviándola al siguiente filtro o servlet
        chain.doFilter(request, response);
    }

    /**
     * Destruye el filtro, liberando los recursos asignados en el ciclo de vida del filtro.
     */
    @Override
    public void destroy() {
        // Bloque de destrucción del filtro: no se requiere liberación de recursos adicionales
    }
}

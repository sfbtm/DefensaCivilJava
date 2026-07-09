package com.defensacivil.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Servlet de prueba sencillo para verificar que el servidor de aplicaciones
 * y el enrutamiento general del backend funcionen de manera correcta.
 * 
 * Endpoint:
 * - /api/test
 */
@WebServlet("/api/test")
public class TestServlet extends HttpServlet {

    /**
     * Procesa peticiones HTTP GET para retornar el estado de funcionamiento general de la API.
     * Retorna JSON indicando que el backend está operativo.
     * 
     * @param req Petición HTTP.
     * @param resp Respuesta HTTP en formato JSON.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Establecer el tipo de contenido de la respuesta HTTP a JSON
        resp.setContentType("application/json");

        // Bloque: Escribir la respuesta JSON de prueba que indica el estado del backend
        resp.getWriter().write("""
                {
                    "data": "Backend funcionando"
                }
                """);
    }
}
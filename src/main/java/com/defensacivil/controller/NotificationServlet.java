package com.defensacivil.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Servlet para la gestión y consulta de notificaciones de usuario.
 * Retorna datos en formato JSON simulando notificaciones leídas y no leídas para fines de la interfaz.
 * 
 * Endpoint:
 * - /api/notifications/*
 */
@WebServlet("/api/notifications/*")
public class NotificationServlet extends HttpServlet {

    /**
     * Procesa las solicitudes HTTP GET para obtener notificaciones.
     * 
     * Endpoints y Respuestas:
     * - GET /api/notifications/user/count/{userId}: Retorna la cantidad de notificaciones no leídas (simulado en 0). Retorna JSON: { "data": { "unread_notifications": 0 } }
     * - GET /api/notifications/user/{userId}: Retorna la lista de notificaciones del usuario (simulada vacía). Retorna JSON: { "data": [] }
     * 
     * @param req Petición HTTP.
     * @param resp Respuesta HTTP en formato JSON.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();

        if (pathInfo != null && pathInfo.contains("/count/")) {
            // GET /api/notifications/user/count/{userId}
            resp.getWriter().write("""
                    {
                        "data": {
                            "unread_notifications": 0
                        }
                    }
                    """);
        } else {
            // GET /api/notifications/user/{userId} o general
            resp.getWriter().write("""
                    {
                        "data": []
                    }
                    """);
        }
    }
}

package com.defensacivil.controller;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;

@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            // Leer el cuerpo de la peticion
            StringBuilder sb = new StringBuilder();
            String line;
            try (BufferedReader reader = req.getReader()) {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            // Deserializar usando Gson
            LoginRequest loginReq = gson.fromJson(sb.toString(), LoginRequest.class);

            if (loginReq == null || loginReq.email == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false,\"message\":\"Datos invalidos\"}");
                return;
            }

            // Mock login basico segun el email para no depender de una tabla de contraseñas inexistente
            int roleId = 3; // Voluntario por defecto
            String fullName = "Voluntario Defensa Civil";

            if (loginReq.email.toLowerCase().contains("admin")) {
                roleId = 1;
                fullName = "Administrador Sistema";
            } else if (loginReq.email.toLowerCase().contains("super")) {
                roleId = 2;
                fullName = "Supervisor Defensa Civil";
            }

            // Estructura de respuesta que espera el frontend
            String jsonResponse = String.format("""
                    {
                        "success": true,
                        "message": "Bienvenido, %s",
                        "data": {
                            "full_name": "%s",
                            "id": 1,
                            "permissions": "[]",
                            "role_id": %d,
                            "sectional_id": 1,
                            "gender": 1,
                            "token": "dummy-jwt-token-for-academic-purposes",
                            "refresh_token": "dummy-refresh-token-for-academic-purposes"
                        }
                    }
                    """, fullName, fullName, roleId);

            resp.getWriter().write(jsonResponse);

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"Error interno en el servidor\"}");
            e.printStackTrace();
        }
    }

    private static class LoginRequest {
        String email;
        String password;
    }
}

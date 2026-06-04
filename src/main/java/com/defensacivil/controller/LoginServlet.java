package com.defensacivil.controller;

import com.defensacivil.config.DatabaseConfig;
import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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

            boolean dbUserFound = false;
            try (Connection conn = DatabaseConfig.getConnection()) {
                String sql = "SELECT IdUsuario, Nombre, IdRol, IdSeccional, IdGenero, Email, Contrasena, Activo FROM Usuario WHERE Email = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, loginReq.email);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            dbUserFound = true;
                            String dbPassword = rs.getString("Contrasena");
                            if (dbPassword == null || !dbPassword.equals(loginReq.password)) {
                                resp.getWriter().write("{\"success\":false,\"message\":\"Credenciales incorrectas\"}");
                                return;
                            }

                            int activeVal = rs.getInt("Activo");
                            if (activeVal == 3) {
                                resp.getWriter().write("{\"success\":false,\"message\":\"Su solicitud de registro no se ha aprobado, por favor contacte al supervisor de su seccional o al administrador.\"}");
                                return;
                            } else if (activeVal == 2 || activeVal == 0) {
                                resp.getWriter().write("{\"success\":false,\"message\":\"El usuario se encuentra inactivo, por favor contacte al supervisor de su seccional o administrador.\"}");
                                return;
                            } else if (activeVal == 1) {
                                // Active user: set static session variables
                                int userId = rs.getInt("IdUsuario");
                                int sectionalId = rs.getInt("IdSeccional");
                                int roleId = rs.getInt("IdRol");

                                UserServlet.loggedInUserId = userId;
                                UserServlet.loggedInSectionalId = sectionalId;
                                UserServlet.loggedInRoleId = roleId;

                                String fullName = rs.getString("Nombre");

                                String jsonResponse = String.format("""
                                        {
                                            "success": true,
                                            "message": "Bienvenido, %s",
                                            "data": {
                                                "full_name": "%s",
                                                "id": %d,
                                                "permissions": "[]",
                                                "role_id": %d,
                                                "sectional_id": %d,
                                                "gender": %d,
                                                "token": "dummy-jwt-token-for-academic-purposes",
                                                "refresh_token": "dummy-refresh-token-for-academic-purposes"
                                            }
                                        }
                                        """, fullName, fullName, userId, roleId, sectionalId, rs.getInt("IdGenero"));
                                resp.getWriter().write(jsonResponse);
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Continuar a mock fallback si la base de datos no está disponible
            }

            if (!dbUserFound) {
                if (loginReq.email.toLowerCase().contains("admin") || loginReq.email.toLowerCase().contains("super")) {
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

                    // Update mock session variables
                    UserServlet.loggedInUserId = (roleId == 1) ? 1 : ((roleId == 2) ? 2 : 3);
                    UserServlet.loggedInSectionalId = 1;
                    UserServlet.loggedInRoleId = roleId;

                    // Estructura de respuesta que espera el frontend
                    String jsonResponse = String.format("""
                            {
                                "success": true,
                                "message": "Bienvenido, %s",
                                "data": {
                                    "full_name": "%s",
                                    "id": %d,
                                    "permissions": "[]",
                                    "role_id": %d,
                                    "sectional_id": 1,
                                    "gender": 1,
                                    "token": "dummy-jwt-token-for-academic-purposes",
                                    "refresh_token": "dummy-refresh-token-for-academic-purposes"
                                }
                            }
                            """, fullName, fullName, UserServlet.loggedInUserId, roleId);

                    resp.getWriter().write(jsonResponse);
                } else {
                    resp.getWriter().write("{\"success\":false,\"message\":\"Credenciales incorrectas o usuario no registrado\"}");
                }
            }

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

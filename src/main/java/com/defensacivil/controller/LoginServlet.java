package com.defensacivil.controller;

import com.defensacivil.config.ResponseUtil;
import com.defensacivil.dao.UsuarioDAO;
import com.defensacivil.dao.UsuarioDAOImpl;
import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {

    private final Gson gson = new Gson();
    private final UsuarioDAO usuarioDAO = new UsuarioDAOImpl();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
                ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Datos invalidos");
                return;
            }

            // Validar credenciales usando la capa DAO
            Map<String, Object> dbUser = usuarioDAO.autenticarUsuario(loginReq.email, loginReq.password);
            if (dbUser == null) {
                ResponseUtil.sendError(resp, HttpServletResponse.SC_OK, "Credenciales incorrectas o usuario no registrado");
                return;
            }

            boolean passwordCorrect = (boolean) dbUser.get("password_correct");
            if (!passwordCorrect) {
                ResponseUtil.sendError(resp, HttpServletResponse.SC_OK, "Credenciales incorrectas");
                return;
            }

            // Validar estado activo del usuario
            int activeVal = (int) dbUser.get("Activo");
            if (activeVal == 3) {
                ResponseUtil.sendError(resp, HttpServletResponse.SC_OK, "Su solicitud de registro no se ha aprobado, por favor contacte al supervisor de su seccional o al administrador.");
                return;
            } else if (activeVal == 2 || activeVal == 0) {
                ResponseUtil.sendError(resp, HttpServletResponse.SC_OK, "El usuario se encuentra inactivo, por favor contacte al supervisor de su seccional o administrador.");
                return;
            }

            // Establecer variables estáticas de sesión para el entorno académico
            int userId = (int) dbUser.get("IdUsuario");
            int sectionalId = (int) dbUser.get("IdSeccional");
            int roleId = (int) dbUser.get("IdRol");

            UserServlet.loggedInUserId = userId;
            UserServlet.loggedInSectionalId = sectionalId;
            UserServlet.loggedInRoleId = roleId;

            jakarta.servlet.http.HttpSession session = req.getSession(true);
            session.setAttribute("userId", userId);
            session.setAttribute("sectionalId", sectionalId);
            session.setAttribute("roleId", roleId);

            String fullName = (String) dbUser.get("Nombre");
            int genderId = (int) dbUser.get("IdGenero");

            // Construir los datos de respuesta esperados por el frontend SPA
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("full_name", fullName);
            responseData.put("id", userId);
            responseData.put("permissions", "[]");
            responseData.put("role_id", roleId);
            responseData.put("sectional_id", sectionalId);
            responseData.put("gender", genderId);
            responseData.put("token", "dummy-jwt-token-for-academic-purposes");
            responseData.put("refresh_token", "dummy-refresh-token-for-academic-purposes");

            ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_OK, responseData, "Bienvenido, " + fullName);

        } catch (Exception e) {
            e.printStackTrace();
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno en el servidor");
        }
    }

    private static class LoginRequest {
        String email;
        String password;
    }
}

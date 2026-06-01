package com.defensacivil.controller;

import com.defensacivil.config.DatabaseConfig;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/users/*")
public class UserServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getMethod().equalsIgnoreCase("PATCH")) {
            doPatch(req, resp);
        } else {
            super.service(req, resp);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"ID o ruta requerida\"}");
            return;
        }

        if (pathInfo.equals("/userForAdmin")) {
            // GET /api/users/userForAdmin -> Listar para administracion con paginacion
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = """
                    SELECT 
                        u.IdUsuario, u.Nombre, u.Documento, u.Email, u.Activo, u.IdRol,
                        r.Nombre AS RolNombre,
                        s.Nombre AS SeccionalNombre,
                        o.Nombre AS OrganizacionNombre
                    FROM Usuario u
                    LEFT JOIN Rol r ON u.IdRol = r.IdRol
                    LEFT JOIN Seccional s ON u.IdSeccional = s.IdSeccional
                    LEFT JOIN Organizacion o ON u.IdOrganizacion = o.IdOrganizacion
                    ORDER BY u.IdUsuario DESC
                    """;

            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", rs.getInt("IdUsuario"));
                    user.put("full_name", rs.getString("Nombre"));
                    user.put("document_number", rs.getString("Documento"));
                    user.put("email", rs.getString("Email"));
                    user.put("rol", rs.getString("RolNombre"));
                    user.put("state_user", rs.getBoolean("Activo") ? "Activo" : "Desactivado");
                    user.put("state_user_id", rs.getBoolean("Activo") ? 1 : 2);
                    user.put("sectional", rs.getString("SeccionalNombre") != null ? rs.getString("SeccionalNombre") : "Ninguna");
                    user.put("organization", rs.getString("OrganizacionNombre") != null ? rs.getString("OrganizacionNombre") : "Ninguna");
                    list.add(user);
                }

                // Estructura paginada que espera paginacion.js
                Map<String, Object> response = new HashMap<>();
                response.put("data", list);

                Map<String, Object> paginate = new HashMap<>();
                paginate.put("current_page", 1);
                paginate.put("last_page", 1);
                paginate.put("total", list.size());
                paginate.put("per_page", 10);
                response.put("paginate", paginate);

                resp.getWriter().write(gson.toJson(response));

            } catch (SQLException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"success\":false,\"message\":\"Error de base de datos\"}");
                e.printStackTrace();
            }
        } else {
            // GET /api/users/{id} -> Detalle de un usuario
            try {
                int id = Integer.parseInt(pathInfo.substring(1));
                String sql = """
                        SELECT 
                            u.IdUsuario, u.Nombre, u.Documento, u.Email, u.Telefono, u.FechaNacimiento, u.Activo, u.IdRol,
                            r.Nombre AS RolNombre,
                            g.Nombre AS GeneroNombre,
                            dt.Nombre AS DocumentoTipoNombre,
                            s.Nombre AS SeccionalNombre,
                            o.Nombre AS OrganizacionNombre
                        FROM Usuario u
                        LEFT JOIN Rol r ON u.IdRol = r.IdRol
                        LEFT JOIN Genero g ON u.IdGenero = g.IdGenero
                        LEFT JOIN DocumentoTipo dt ON u.IdDocumentoTipo = dt.IdDocumentoTipo
                        LEFT JOIN Seccional s ON u.IdSeccional = s.IdSeccional
                        LEFT JOIN Organizacion o ON u.IdOrganizacion = o.IdOrganizacion
                        WHERE u.IdUsuario = ?
                        """;

                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {

                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            Map<String, Object> user = new HashMap<>();
                            user.put("id", rs.getInt("IdUsuario"));
                            user.put("names", rs.getString("Nombre"));
                            user.put("last_names", "");
                            user.put("email", rs.getString("Email"));
                            user.put("document_type", rs.getString("DocumentoTipoNombre"));
                            user.put("document_number", rs.getString("Documento"));
                            user.put("birth_date", rs.getDate("FechaNacimiento") != null ? rs.getDate("FechaNacimiento").toString() : "");
                            user.put("gender", rs.getString("GeneroNombre"));
                            user.put("phone", rs.getString("Telefono"));
                            user.put("sectional", rs.getString("SeccionalNombre"));
                            user.put("organization", rs.getString("OrganizacionNombre"));
                            user.put("status", rs.getBoolean("Activo") ? "Activo" : "Desactivado");
                            user.put("state_user_id", rs.getBoolean("Activo") ? 1 : 2);

                            Map<String, Object> rol = new HashMap<>();
                            rol.put("id", rs.getInt("IdRol"));
                            rol.put("name", rs.getString("RolNombre"));
                            user.put("rol", rol);
                            user.put("rol_id", rs.getInt("IdRol"));

                            // Para que devuelva la prop data si el helper get o api espera data
                            Map<String, Object> dataResp = new HashMap<>();
                            dataResp.put("data", user);

                            resp.getWriter().write(gson.toJson(dataResp));
                        } else {
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            resp.getWriter().write("{\"success\":false,\"message\":\"Usuario no encontrado\"}");
                        }
                    }
                }
            } catch (NumberFormatException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false,\"message\":\"ID invalido\"}");
            } catch (SQLException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"success\":false,\"message\":\"Error de base de datos\"}");
                e.printStackTrace();
            }
        }
    }

    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"ID o ruta requerida\"}");
            return;
        }

        try {
            BufferedReader reader = req.getReader();
            Map<String, Object> body = gson.fromJson(reader, Map.class);

            if (body == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false,\"message\":\"Cuerpo vacio\"}");
                return;
            }

            if (pathInfo.startsWith("/role/")) {
                // PATCH /api/users/role/{id} -> Cambiar rol
                int id = Integer.parseInt(pathInfo.substring(6));
                String roleName = (String) body.get("role");
                int roleId = 3; // Voluntario por defecto
                if ("Administrador".equalsIgnoreCase(roleName)) roleId = 1;
                else if ("Supervisor".equalsIgnoreCase(roleName)) roleId = 2;

                String sql = "UPDATE Usuario SET IdRol = ? WHERE IdUsuario = ?";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {

                    ps.setInt(1, roleId);
                    ps.setInt(2, id);
                    int affected = ps.executeUpdate();

                    if (affected > 0) {
                        resp.getWriter().write("{\"success\":true,\"message\":\"Rol de usuario actualizado exitosamente\"}");
                    } else {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        resp.getWriter().write("{\"success\":false,\"message\":\"Usuario no encontrado\"}");
                    }
                }

            } else if (pathInfo.endsWith("/change-status")) {
                // PATCH /api/users/{id}/change-status -> Activar/Desactivar
                // Ejemplo pathInfo: /1/change-status
                String[] segments = pathInfo.split("/");
                int id = Integer.parseInt(segments[1]);

                Object stateObj = body.get("state_user_id");
                int stateId = 1;
                if (stateObj instanceof Number) {
                    stateId = ((Number) stateObj).intValue();
                } else if (stateObj instanceof String) {
                    stateId = Integer.parseInt((String) stateObj);
                }
                int active = (stateId == 1) ? 1 : 0;

                String sql = "UPDATE Usuario SET Activo = ? WHERE IdUsuario = ?";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {

                    ps.setInt(1, active);
                    ps.setInt(2, id);
                    int affected = ps.executeUpdate();

                    if (affected > 0) {
                        resp.getWriter().write("{\"success\":true,\"message\":\"Estado de usuario actualizado exitosamente\"}");
                    } else {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        resp.getWriter().write("{\"success\":false,\"message\":\"Usuario no encontrado\"}");
                    }
                }

            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false,\"message\":\"Ruta de actualizacion no soportada\"}");
            }

        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"ID invalido\"}");
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"Error de base de datos\"}");
            e.printStackTrace();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"ID requerido para eliminar\"}");
            return;
        }

        try {
            int id = Integer.parseInt(pathInfo.substring(1));
            String sql = "DELETE FROM Usuario WHERE IdUsuario = ?";

            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, id);
                int affected = ps.executeUpdate();

                if (affected > 0) {
                    resp.getWriter().write("{\"success\":true,\"message\":\"Usuario eliminado exitosamente\"}");
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{\"success\":false,\"message\":\"Usuario no encontrado\"}");
                }
            }
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"ID invalido\"}");
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"Error de base de datos o restriccion de clave foranea\"}");
            e.printStackTrace();
        }
    }
}

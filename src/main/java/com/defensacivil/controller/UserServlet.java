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

@WebServlet(urlPatterns = {
    "/api/users/*",
    "/api/register/*",
    "/api/register",
    "/api/public/document-types/*",
    "/api/public/document-types",
    "/api/public/genders/*",
    "/api/public/genders",
    "/api/public/sectionals/*",
    "/api/public/sectionals",
    "/api/public/organizations/sectional/*"
})
public class UserServlet extends HttpServlet {

    private final Gson gson = new Gson();

    public static int loggedInUserId = 1;
    public static int loggedInSectionalId = 1;
    public static int loggedInRoleId = 1;

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

        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        // 1. PUBLIC CATALOG ENDPOINTS
        if (servletPath != null && servletPath.contains("public/document-types")) {
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = "SELECT IdDocumentoTipo, Nombre FROM DocumentoTipo";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idVal = rs.getInt("IdDocumentoTipo");
                    String name = rs.getString("Nombre");
                    String acronym = switch (idVal) {
                        case 1 -> "CC";
                        case 2 -> "TI";
                        case 3 -> "CE";
                        default -> "CC";
                    };
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", idVal);
                    item.put("name", name);
                    item.put("acronym", acronym);
                    item.put("is_active", 1);
                    list.add(item);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            resp.getWriter().write(gson.toJson(Map.of("data", list)));
            return;
        }

        if (servletPath != null && servletPath.contains("public/genders")) {
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = "SELECT IdGenero, Nombre FROM Genero";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getInt("IdGenero"));
                    item.put("name", rs.getString("Nombre"));
                    item.put("is_active", 1);
                    list.add(item);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            resp.getWriter().write(gson.toJson(Map.of("data", list)));
            return;
        }

        if (servletPath != null && servletPath.contains("public/sectionals")) {
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = "SELECT IdSeccional, Nombre FROM Seccional";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getInt("IdSeccional"));
                    item.put("name", rs.getString("Nombre"));
                    item.put("is_active", 1);
                    list.add(item);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            resp.getWriter().write(gson.toJson(Map.of("data", list)));
            return;
        }

        if (servletPath != null && servletPath.contains("public/organizations/sectional")) {
            int sectionalId = 1;
            if (pathInfo != null && !pathInfo.equals("/")) {
                try {
                    sectionalId = Integer.parseInt(pathInfo.substring(1));
                } catch (Exception ignored) {}
            }
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = "SELECT IdOrganizacion, Nombre FROM Organizacion WHERE IdSeccional = ?";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, sectionalId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", rs.getInt("IdOrganizacion"));
                        item.put("name", rs.getString("Nombre"));
                        item.put("is_active", 1);
                        list.add(item);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            resp.getWriter().write(gson.toJson(Map.of("data", list)));
            return;
        }

        // 2. PROTECTED AND REQUEST ENDPOINTS
        if (pathInfo == null || pathInfo.equals("/")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"ID o ruta requerida\"}");
            return;
        }

        if (pathInfo.equals("/requestsAdmins")) {
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = """
                SELECT u.IdUsuario, u.Nombre, u.Email, u.Documento, dt.Nombre AS DocumentoTipoNombre, 
                       o.Nombre AS OrganizacionNombre, s.Nombre AS SeccionalNombre
                FROM Usuario u
                LEFT JOIN DocumentoTipo dt ON u.IdDocumentoTipo = dt.IdDocumentoTipo
                LEFT JOIN Organizacion o ON u.IdOrganizacion = o.IdOrganizacion
                LEFT JOIN Seccional s ON o.IdSeccional = s.IdSeccional
                WHERE u.Activo = 3 AND u.IdRol = 3
                ORDER BY u.IdUsuario DESC
                """;
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idVal = rs.getInt("IdUsuario");
                    String docTypeAcronym = "CC";
                    String docTypeName = rs.getString("DocumentoTipoNombre");
                    if (docTypeName != null) {
                        if (docTypeName.contains("Identidad")) docTypeAcronym = "TI";
                        else if (docTypeName.contains("Extranjería")) docTypeAcronym = "CE";
                    }

                    Map<String, Object> item = new HashMap<>();
                    item.put("id", idVal);
                    item.put("full_name", rs.getString("Nombre"));
                    item.put("email", rs.getString("Email"));
                    item.put("organization", rs.getString("OrganizacionNombre") != null ? rs.getString("OrganizacionNombre") : "Ninguna");
                    item.put("sectional", rs.getString("SeccionalNombre") != null ? rs.getString("SeccionalNombre") : "Ninguna");
                    item.put("document_number", rs.getString("Documento") + " " + docTypeAcronym);
                    list.add(item);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            Map<String, Object> response = new HashMap<>();
            response.put("data", list);
            response.put("paginate", Map.of(
                "current_page", 1,
                "per_page", 10,
                "total", list.size(),
                "last_page", 1
            ));
            resp.getWriter().write(gson.toJson(response));
            return;
        }

        if (pathInfo.equals("/requests/supervisors")) {
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = """
                SELECT u.IdUsuario, u.Nombre, u.Email, u.Documento, dt.Nombre AS DocumentoTipoNombre, 
                       o.Nombre AS OrganizacionNombre, s.Nombre AS SeccionalNombre
                FROM Usuario u
                LEFT JOIN DocumentoTipo dt ON u.IdDocumentoTipo = dt.IdDocumentoTipo
                LEFT JOIN Organizacion o ON u.IdOrganizacion = o.IdOrganizacion
                LEFT JOIN Seccional s ON o.IdSeccional = s.IdSeccional
                WHERE u.Activo = 3 AND u.IdRol = 2 AND o.IdSeccional = ?
                ORDER BY u.IdUsuario DESC
                """;
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, loggedInSectionalId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int idVal = rs.getInt("IdUsuario");
                        String docTypeAcronym = "CC";
                        String docTypeName = rs.getString("DocumentoTipoNombre");
                        if (docTypeName != null) {
                            if (docTypeName.contains("Identidad")) docTypeAcronym = "TI";
                            else if (docTypeName.contains("Extranjería")) docTypeAcronym = "CE";
                        }

                        Map<String, Object> item = new HashMap<>();
                        item.put("id", idVal);
                        item.put("full_name", rs.getString("Nombre"));
                        item.put("email", rs.getString("Email"));
                        item.put("organization", rs.getString("OrganizacionNombre") != null ? rs.getString("OrganizacionNombre") : "Ninguna");
                        item.put("sectional", rs.getString("SeccionalNombre") != null ? rs.getString("SeccionalNombre") : "Ninguna");
                        item.put("document_number", rs.getString("Documento") + " " + docTypeAcronym);
                        list.add(item);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            Map<String, Object> response = new HashMap<>();
            response.put("data", list);
            response.put("paginate", Map.of(
                "current_page", 1,
                "per_page", 10,
                "total", list.size(),
                "last_page", 1
            ));
            resp.getWriter().write(gson.toJson(response));
            return;
        }

        if (pathInfo.equals("/userForSupervisor")) {
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = """
                SELECT u.IdUsuario, u.Nombre, u.Email, u.Documento, dt.Nombre AS DocumentoTipoNombre, 
                       o.Nombre AS OrganizacionNombre, s.Nombre AS SeccionalNombre, u.Activo, u.IdRol, r.Nombre AS RolNombre
                FROM Usuario u
                LEFT JOIN DocumentoTipo dt ON u.IdDocumentoTipo = dt.IdDocumentoTipo
                LEFT JOIN Organizacion o ON u.IdOrganizacion = o.IdOrganizacion
                LEFT JOIN Seccional s ON o.IdSeccional = s.IdSeccional
                LEFT JOIN Rol r ON u.IdRol = r.IdRol
                WHERE u.Activo != 3 AND u.IdRol = 3 AND o.IdSeccional = ?
                ORDER BY u.IdUsuario DESC
                """;
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, loggedInSectionalId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int idVal = rs.getInt("IdUsuario");
                        int activeVal = rs.getInt("Activo");
                        String docTypeAcronym = "CC";
                        String docTypeName = rs.getString("DocumentoTipoNombre");
                        if (docTypeName != null) {
                            if (docTypeName.contains("Identidad")) docTypeAcronym = "TI";
                            else if (docTypeName.contains("Extranjería")) docTypeAcronym = "CE";
                        }

                        Map<String, Object> item = new HashMap<>();
                        item.put("id", idVal);
                        item.put("full_name", rs.getString("Nombre"));
                        item.put("email", rs.getString("Email"));
                        item.put("organization", rs.getString("OrganizacionNombre") != null ? rs.getString("OrganizacionNombre") : "Ninguna");
                        item.put("sectional", rs.getString("SeccionalNombre") != null ? rs.getString("SeccionalNombre") : "Ninguna");
                        item.put("document_number", rs.getString("Documento") + " " + docTypeAcronym);
                        item.put("state_user", (activeVal == 1) ? "Activo" : "Desactivado");
                        item.put("state_user_id", activeVal);
                        item.put("rol", rs.getString("RolNombre"));
                        item.put("rol_id", rs.getInt("IdRol"));
                        list.add(item);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            Map<String, Object> response = new HashMap<>();
            response.put("data", list);
            response.put("paginate", Map.of(
                "current_page", 1,
                "per_page", 10,
                "total", list.size(),
                "last_page", 1
            ));
            resp.getWriter().write(gson.toJson(response));
            return;
        }

        if (pathInfo.equals("/userForAdmin")) {
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = """
                    SELECT 
                        u.IdUsuario, u.Nombre, u.Documento, u.Email, u.Activo, u.IdRol,
                        r.Nombre AS RolNombre,
                        s.Nombre AS SeccionalNombre,
                        o.Nombre AS OrganizacionNombre
                    FROM Usuario u
                    LEFT JOIN Rol r ON u.IdRol = r.IdRol
                    LEFT JOIN Organizacion o ON u.IdOrganizacion = o.IdOrganizacion
                    LEFT JOIN Seccional s ON o.IdSeccional = s.IdSeccional
                    WHERE u.Activo != 3 AND u.IdRol != 1
                    ORDER BY u.IdUsuario DESC
                    """;

            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    int idVal = rs.getInt("IdUsuario");
                    int activeVal = rs.getInt("Activo");
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", idVal);
                    user.put("full_name", rs.getString("Nombre"));
                    user.put("document_number", rs.getString("Documento"));
                    user.put("email", rs.getString("Email"));
                    user.put("rol", rs.getString("RolNombre"));
                    user.put("state_user", (activeVal == 1) ? "Activo" : "Desactivado");
                    user.put("state_user_id", activeVal);
                    user.put("sectional", rs.getString("SeccionalNombre") != null ? rs.getString("SeccionalNombre") : "Ninguna");
                    user.put("organization", rs.getString("OrganizacionNombre") != null ? rs.getString("OrganizacionNombre") : "Ninguna");
                    list.add(user);
                }

                Map<String, Object> response = new HashMap<>();
                response.put("data", list);
                response.put("paginate", Map.of(
                    "current_page", 1,
                    "per_page", 10,
                    "total", list.size(),
                    "last_page", 1
                ));

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
                        LEFT JOIN Organizacion o ON u.IdOrganizacion = o.IdOrganizacion
                        LEFT JOIN Seccional s ON o.IdSeccional = s.IdSeccional
                        WHERE u.IdUsuario = ?
                        """;

                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {

                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int activeVal = rs.getInt("Activo");
                            Map<String, Object> user = new HashMap<>();
                            user.put("id", rs.getInt("IdUsuario"));
                             String fullName = rs.getString("Nombre");
                             String names = "";
                             String lastNames = "";
                             if (fullName != null) {
                                 fullName = fullName.trim();
                                 String[] parts = fullName.split("\\s+");
                                 if (parts.length >= 4) {
                                     names = parts[0] + " " + parts[1];
                                     StringBuilder sb = new StringBuilder();
                                     for (int i = 2; i < parts.length; i++) {
                                         if (i > 2) sb.append(" ");
                                         sb.append(parts[i]);
                                     }
                                     lastNames = sb.toString();
                                 } else if (parts.length == 3) {
                                     names = parts[0];
                                     lastNames = parts[1] + " " + parts[2];
                                 } else if (parts.length == 2) {
                                     names = parts[0];
                                     lastNames = parts[1];
                                 } else {
                                     names = fullName;
                                 }
                             }
                             user.put("names", names);
                             user.put("last_names", lastNames);
                            user.put("email", rs.getString("Email"));
                            user.put("document_type", rs.getString("DocumentoTipoNombre"));
                            user.put("document_number", rs.getString("Documento"));
                            user.put("birth_date", rs.getDate("FechaNacimiento") != null ? rs.getDate("FechaNacimiento").toString() : "");
                            user.put("gender", rs.getString("GeneroNombre"));
                            user.put("phone", rs.getString("Telefono"));
                            user.put("sectional", rs.getString("SeccionalNombre"));
                            user.put("organization", rs.getString("OrganizacionNombre"));
                            user.put("status", (activeVal == 1) ? "Activo" : (activeVal == 3 ? "Peticion" : "Desactivado"));
                            user.put("state_user_id", activeVal);

                            Map<String, Object> rol = new HashMap<>();
                            rol.put("id", rs.getInt("IdRol"));
                            rol.put("name", rs.getString("RolNombre"));
                            user.put("rol", rol);
                            user.put("rol_id", rs.getInt("IdRol"));

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

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String servletPath = req.getServletPath();
        if (servletPath != null && (servletPath.contains("register") || servletPath.contains("/register"))) {
            try {
                BufferedReader reader = req.getReader();
                RegisterRequest regReq = gson.fromJson(reader, RegisterRequest.class);

                if (regReq == null || regReq.email == null || regReq.password == null ||
                    regReq.names == null || regReq.names.trim().isEmpty() ||
                    regReq.document_number == null || regReq.document_number.trim().isEmpty() ||
                    regReq.gender_id == null || regReq.document_type_id == null) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("{\"success\":false,\"message\":\"Datos de registro incompletos. Por favor complete todos los campos requeridos.\"}");
                    return;
                }

                try (Connection conn = DatabaseConfig.getConnection()) {
                    // Check duplicate email
                    String checkEmailSql = "SELECT IdUsuario FROM Usuario WHERE Email = ?";
                    try (PreparedStatement ps = conn.prepareStatement(checkEmailSql)) {
                        ps.setString(1, regReq.email);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                                resp.getWriter().write("{\"success\":false,\"message\":\"El correo ya se encuentra registrado\"}");
                                return;
                            }
                        }
                    }

                    // Check duplicate document number
                    String checkDocSql = "SELECT IdUsuario FROM Usuario WHERE Documento = ?";
                    try (PreparedStatement ps = conn.prepareStatement(checkDocSql)) {
                        ps.setString(1, regReq.document_number);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                                resp.getWriter().write("{\"success\":false,\"message\":\"El documento ya se encuentra registrado\"}");
                                return;
                            }
                        }
                    }

                    java.sql.Date birthDate = null;
                    if (regReq.birth_date != null && !regReq.birth_date.trim().isEmpty()) {
                        try {
                            birthDate = java.sql.Date.valueOf(regReq.birth_date.trim());
                        } catch (IllegalArgumentException e) {
                            // ignore or set null
                        }
                    }

                    String insertSql = """
                        INSERT INTO Usuario (
                            Nombre, Documento, IdRol, IdGenero, IdDocumentoTipo, 
                            IdNacionalidad, Email, Contrasena, Telefono, 
                            FechaNacimiento, IdOrganizacion, Activo
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """;
                    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                        ps.setString(1, regReq.names + (regReq.last_names != null && !regReq.last_names.trim().isEmpty() ? " " + regReq.last_names : ""));
                        ps.setString(2, regReq.document_number);
                        ps.setInt(3, 3); // Voluntario
                        ps.setObject(4, regReq.gender_id, java.sql.Types.INTEGER);
                        ps.setObject(5, regReq.document_type_id, java.sql.Types.INTEGER);
                        ps.setInt(6, 1); // Nacionalidad Colombiana
                        ps.setString(7, regReq.email);
                        ps.setString(8, regReq.password);
                        ps.setString(9, regReq.phone);
                        ps.setDate(10, birthDate);
                        ps.setObject(11, regReq.organization_id, java.sql.Types.INTEGER);
                        ps.setInt(12, 3); // Activo = 3 (Peticion)

                        int affected = ps.executeUpdate();
                        if (affected > 0) {
                            resp.getWriter().write("{\"success\":true,\"message\":\"Usuario registrado exitosamente. Su solicitud está pendiente de aprobación.\"}");
                        } else {
                            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            resp.getWriter().write("{\"success\":false,\"message\":\"No se pudo registrar el usuario\"}");
                        }
                    }
                }
            } catch (SQLException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"success\":false,\"message\":\"Error de base de datos\"}");
                e.printStackTrace();
            }
        } else {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"Ruta POST no soportada\"}");
        }
    }

    private static class RegisterRequest {
        String names;
        String last_names;
        String birth_date;
        Integer document_type_id;
        String document_number;
        String phone;
        Integer gender_id;
        Integer organization_id;
        String email;
        String password;
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

                String sql = (roleId == 2)
                    ? "UPDATE Usuario SET IdRol = ?, Activo = 3 WHERE IdUsuario = ?"
                    : "UPDATE Usuario SET IdRol = ? WHERE IdUsuario = ?";
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
                int active = stateId;

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

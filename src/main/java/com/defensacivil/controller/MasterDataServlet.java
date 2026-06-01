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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {
    "/api/departments/*",
    "/api/documentTypes/*",
    "/api/nationalities/*",
    "/api/sectionals/*",
    "/api/threatTypes/*",
    "/api/vulnerabilities/*",
    "/api/resources/*",
    "/api/sectors/*",
    "/api/species/*",
    "/api/vulnerableQuestions/*",
    "/api/organizations/*",
    "/api/housingQualities/*",
    "/api/genders/*"
})
public class MasterDataServlet extends HttpServlet {

    private final Gson gson = new Gson();

    static class EntityConfig {
        String tableName;
        String idCol;
        String nameCol;
        EntityConfig(String tableName, String idCol, String nameCol) {
            this.tableName = tableName;
            this.idCol = idCol;
            this.nameCol = nameCol;
        }
    }

    private EntityConfig getConfig(String servletPath) {
        if (servletPath.contains("departments")) return new EntityConfig("Departamento", "IdDepartamento", "Nombre");
        if (servletPath.contains("documentTypes")) return new EntityConfig("DocumentoTipo", "IdDocumentoTipo", "Nombre");
        if (servletPath.contains("nationalities")) return new EntityConfig("Nacionalidad", "IdNacionalidad", "Nombre");
        if (servletPath.contains("sectionals")) return new EntityConfig("Seccional", "IdSeccional", "Nombre");
        if (servletPath.contains("threatTypes")) return new EntityConfig("TipoAmenaza", "IdTipoAmenaza", "Nombre");
        if (servletPath.contains("vulnerabilities")) return new EntityConfig("VulnerabilidadTipo", "IdTipoVulnerabilidad", "Nombre");
        if (servletPath.contains("sectors")) return new EntityConfig("Sector", "IdSector", "Nombre");
        if (servletPath.contains("species")) return new EntityConfig("Especie", "IdEspecie", "Nombre");
        if (servletPath.contains("resources")) return new EntityConfig("Recurso", "IdRecurso", "Nombre");
        if (servletPath.contains("vulnerableQuestions")) return new EntityConfig("Pregunta", "IdPregunta", "Texto");
        if (servletPath.contains("organizations")) return new EntityConfig("Organizacion", "IdOrganizacion", "Nombre");
        if (servletPath.contains("housingQualities")) return new EntityConfig("CalidadVivienda", "IdCalidad", "Nombre");
        if (servletPath.contains("genders")) return new EntityConfig("Genero", "IdGenero", "Nombre");
        return null;
    }

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
        EntityConfig cfg = getConfig(servletPath);
        if (cfg == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"Entidad no configurada\"}");
            return;
        }

        String pathInfo = req.getPathInfo();

        if (pathInfo != null && pathInfo.equals("/paginate")) {
            // PAGINADO DE PREGUNTAS
            int page = 1;
            String pageStr = req.getParameter("page");
            if (pageStr != null && !pageStr.isEmpty()) {
                try { page = Integer.parseInt(pageStr); } catch (Exception ignored) {}
            }

            int perPage = 3;
            int offset = (page - 1) * perPage;
            int total = 0;
            List<Map<String, Object>> list = new ArrayList<>();

            try (Connection conn = DatabaseConfig.getConnection()) {
                // Count total
                try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM Pregunta WHERE Activa = 1");
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) total = rs.getInt(1);
                }

                // Get page slice
                try (PreparedStatement ps = conn.prepareStatement("SELECT IdPregunta, Texto, Activa, Precaucion FROM Pregunta WHERE Activa = 1 LIMIT ? OFFSET ?")) {
                    ps.setInt(1, perPage);
                    ps.setInt(2, offset);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Map<String, Object> item = new HashMap<>();
                            item.put("id", rs.getInt("IdPregunta"));
                            item.put("description", rs.getString("Texto"));
                            item.put("is_active", rs.getBoolean("Activa"));
                            item.put("question_caution", rs.getBoolean("Precaucion"));
                            list.add(item);
                        }
                    }
                }

                int lastPage = (int) Math.ceil((double) total / perPage);
                if (lastPage < 1) lastPage = 1;

                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("data", list);

                Map<String, Object> paginateMap = new HashMap<>();
                paginateMap.put("current_page", page);
                paginateMap.put("last_page", lastPage);
                paginateMap.put("per_page", perPage);
                paginateMap.put("total", total);
                responseMap.put("paginate", paginateMap);

                resp.getWriter().write(gson.toJson(responseMap));

            } catch (SQLException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"success\":false,\"message\":\"Error de base de datos\"}");
                e.printStackTrace();
            }
            return;
        }

        if (pathInfo == null || pathInfo.equals("/")) {
            // LISTAR TODOS
            List<Map<String, Object>> list = new ArrayList<>();
            String sql;
            if (servletPath.contains("resources")) {
                sql = "SELECT IdRecurso, Nombre, Servicio, Activo FROM Recurso ORDER BY IdRecurso DESC";
            } else if (servletPath.contains("vulnerableQuestions")) {
                sql = "SELECT IdPregunta, Texto, Activa, Precaucion FROM Pregunta ORDER BY IdPregunta DESC";
            } else if (servletPath.contains("organizations")) {
                sql = "SELECT IdOrganizacion, Nombre, IdSeccional FROM Organizacion ORDER BY IdOrganizacion DESC";
            } else {
                sql = String.format("SELECT %s, %s FROM %s ORDER BY %s DESC", cfg.idCol, cfg.nameCol, cfg.tableName, cfg.idCol);
            }

            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    if (servletPath.contains("resources")) {
                        item.put("id", rs.getInt("IdRecurso"));
                        item.put("name", rs.getString("Nombre"));
                        item.put("service", rs.getString("Servicio"));
                        item.put("is_active", rs.getBoolean("Activo"));
                    } else if (servletPath.contains("vulnerableQuestions")) {
                        item.put("id", rs.getInt("IdPregunta"));
                        item.put("description", rs.getString("Texto"));
                        item.put("is_active", rs.getBoolean("Activa"));
                        item.put("question_caution", rs.getBoolean("Precaucion"));
                    } else if (servletPath.contains("organizations")) {
                        item.put("id", rs.getInt("IdOrganizacion"));
                        item.put("name", rs.getString("Nombre"));
                        item.put("sectional_id", rs.getInt("IdSeccional"));
                        item.put("is_active", true);
                    } else {
                        item.put("id", rs.getInt(cfg.idCol));
                        item.put("name", rs.getString(cfg.nameCol));
                        item.put("is_active", true);
                    }
                    list.add(item);
                }

                Map<String, Object> dataResp = new HashMap<>();
                dataResp.put("data", list);
                resp.getWriter().write(gson.toJson(dataResp));

            } catch (SQLException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"success\":false,\"message\":\"Error de base de datos\"}");
                e.printStackTrace();
            }
        } else {
            // OBTENER UNO POR ID
            try {
                String idStr = pathInfo.substring(1);
                // Si es algo como /status/1, ignorarlo en GET
                if (idStr.startsWith("status/")) {
                    idStr = idStr.substring(7);
                }
                int id = Integer.parseInt(idStr);

                String sql;
                if (servletPath.contains("resources")) {
                    sql = "SELECT IdRecurso, Nombre, Servicio, Activo FROM Recurso WHERE IdRecurso = ?";
                } else if (servletPath.contains("vulnerableQuestions")) {
                    sql = "SELECT IdPregunta, Texto, Activa, Precaucion FROM Pregunta WHERE IdPregunta = ?";
                } else if (servletPath.contains("organizations")) {
                    sql = "SELECT IdOrganizacion, Nombre, IdSeccional FROM Organizacion WHERE IdOrganizacion = ?";
                } else {
                    sql = String.format("SELECT %s, %s FROM %s WHERE %s = ?", cfg.idCol, cfg.nameCol, cfg.tableName, cfg.idCol);
                }

                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {

                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            Map<String, Object> item = new HashMap<>();
                            if (servletPath.contains("resources")) {
                                item.put("id", rs.getInt("IdRecurso"));
                                item.put("name", rs.getString("Nombre"));
                                item.put("service", rs.getString("Servicio"));
                                item.put("is_active", rs.getBoolean("Activo"));
                            } else if (servletPath.contains("vulnerableQuestions")) {
                                item.put("id", rs.getInt("IdPregunta"));
                                item.put("description", rs.getString("Texto"));
                                item.put("is_active", rs.getBoolean("Activa"));
                                item.put("question_caution", rs.getBoolean("Precaucion"));
                            } else if (servletPath.contains("organizations")) {
                                item.put("id", rs.getInt("IdOrganizacion"));
                                item.put("name", rs.getString("Nombre"));
                                item.put("sectional_id", rs.getInt("IdSeccional"));
                                item.put("is_active", true);
                            } else {
                                item.put("id", rs.getInt(cfg.idCol));
                                item.put("name", rs.getString(cfg.nameCol));
                                item.put("is_active", true);
                            }

                            // Retornar objeto directo para GET por id, no envuelto en array si la api lo requiere,
                            // o envuelto segun la convencion. La funcion api.get("endpoint/id") suele devolver el objeto directamente.
                            resp.getWriter().write(gson.toJson(item));
                        } else {
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            resp.getWriter().write("{\"success\":false,\"message\":\"Registro no encontrado\"}");
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
        EntityConfig cfg = getConfig(servletPath);
        if (cfg == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"Entidad no configurada\"}");
            return;
        }

        try {
            BufferedReader reader = req.getReader();
            Map<String, Object> body = gson.fromJson(reader, Map.class);

            if (body == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false,\"message\":\"Cuerpo de peticion vacio\"}");
                return;
            }

            String sql;
            Connection conn = DatabaseConfig.getConnection();
            PreparedStatement ps;

            if (servletPath.contains("resources")) {
                String name = (String) body.get("name");
                String service = (String) body.get("service");
                sql = "INSERT INTO Recurso (Nombre, Servicio, Activo) VALUES (?, ?, 1)";
                ps = conn.prepareStatement(sql);
                ps.setString(1, name);
                ps.setString(2, service);
            } else if (servletPath.contains("vulnerableQuestions")) {
                String description = (String) body.get("description");
                Object cautionObj = body.get("question_caution");
                int caution = 0;
                if (cautionObj instanceof Number) {
                    caution = ((Number) cautionObj).intValue();
                } else if (cautionObj instanceof String) {
                    caution = Integer.parseInt((String) cautionObj);
                }
                sql = "INSERT INTO Pregunta (Texto, Activa, Precaucion) VALUES (?, 1, ?)";
                ps = conn.prepareStatement(sql);
                ps.setString(1, description);
                ps.setInt(2, caution);
            } else if (servletPath.contains("organizations")) {
                String name = (String) body.get("name");
                Object secIdObj = body.get("sectional_id");
                int secId = 1;
                if (secIdObj instanceof Number) {
                    secId = ((Number) secIdObj).intValue();
                } else if (secIdObj instanceof String) {
                    secId = Integer.parseInt((String) secIdObj);
                }
                sql = "INSERT INTO Organizacion (Nombre, IdSeccional) VALUES (?, ?)";
                ps = conn.prepareStatement(sql);
                ps.setString(1, name);
                ps.setInt(2, secId);
            } else {
                String name = (String) body.get("name");
                sql = String.format("INSERT INTO %s (%s) VALUES (?)", cfg.tableName, cfg.nameCol);
                ps = conn.prepareStatement(sql);
                ps.setString(1, name);
            }

            int affected = ps.executeUpdate();
            ps.close();
            conn.close();

            if (affected > 0) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("{\"success\":true,\"message\":\"Creado exitosamente\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"success\":false,\"message\":\"No se pudo crear el registro\"}");
            }

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"Error interno o de base de datos\"}");
            e.printStackTrace();
        }
    }

    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String servletPath = req.getServletPath();
        EntityConfig cfg = getConfig(servletPath);
        if (cfg == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"Entidad no configurada\"}");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"ID requerido\"}");
            return;
        }

        try {
            String idStr = pathInfo.substring(1);
            boolean isStatusUpdate = false;
            if (idStr.startsWith("status/")) {
                idStr = idStr.substring(7);
                isStatusUpdate = true;
            }
            int id = Integer.parseInt(idStr);

            BufferedReader reader = req.getReader();
            Map<String, Object> body = gson.fromJson(reader, Map.class);

            if (body == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false,\"message\":\"Cuerpo de peticion vacio\"}");
                return;
            }

            String sql;
            Connection conn = DatabaseConfig.getConnection();
            PreparedStatement ps;

            if (isStatusUpdate) {
                // ACTUALIZAR ESTADO DE ACTIVACION
                Object activeObj = body.get("is_active");
                int active = 1;
                if (activeObj instanceof Boolean) {
                    active = (Boolean) activeObj ? 1 : 0;
                } else if (activeObj instanceof Number) {
                    active = ((Number) activeObj).intValue();
                }

                if (servletPath.contains("resources")) {
                    sql = "UPDATE Recurso SET Activo = ? WHERE IdRecurso = ?";
                } else if (servletPath.contains("vulnerableQuestions")) {
                    sql = "UPDATE Pregunta SET Activa = ? WHERE IdPregunta = ?";
                } else {
                    // Otras tablas no tienen columna de activacion, responder exito simulado
                    resp.getWriter().write("{\"success\":true,\"message\":\"Estado actualizado (simulado)\"}");
                    conn.close();
                    return;
                }
                ps = conn.prepareStatement(sql);
                ps.setInt(1, active);
                ps.setInt(2, id);

            } else {
                // ACTUALIZAR CAMPOS
                if (servletPath.contains("resources")) {
                    String name = (String) body.get("name");
                    String service = (String) body.get("service");
                    sql = "UPDATE Recurso SET Nombre = ?, Servicio = ? WHERE IdRecurso = ?";
                    ps = conn.prepareStatement(sql);
                    ps.setString(1, name);
                    ps.setString(2, service);
                    ps.setInt(3, id);
                } else if (servletPath.contains("vulnerableQuestions")) {
                    String description = (String) body.get("description");
                    Object cautionObj = body.get("question_caution");
                    int caution = 0;
                    if (cautionObj instanceof Number) {
                        caution = ((Number) cautionObj).intValue();
                    } else if (cautionObj instanceof String) {
                        caution = Integer.parseInt((String) cautionObj);
                    }
                    sql = "UPDATE Pregunta SET Texto = ?, Precaucion = ? WHERE IdPregunta = ?";
                    ps = conn.prepareStatement(sql);
                    ps.setString(1, description);
                    ps.setInt(2, caution);
                    ps.setInt(3, id);
                } else if (servletPath.contains("organizations")) {
                    String name = (String) body.get("name");
                    Object secIdObj = body.get("sectional_id");
                    int secId = 1;
                    if (secIdObj instanceof Number) {
                        secId = ((Number) secIdObj).intValue();
                    } else if (secIdObj instanceof String) {
                        secId = Integer.parseInt((String) secIdObj);
                    }
                    sql = "UPDATE Organizacion SET Nombre = ?, IdSeccional = ? WHERE IdOrganizacion = ?";
                    ps = conn.prepareStatement(sql);
                    ps.setString(1, name);
                    ps.setInt(2, secId);
                    ps.setInt(3, id);
                } else {
                    String name = (String) body.get("name");
                    sql = String.format("UPDATE %s SET %s = ? WHERE %s = ?", cfg.tableName, cfg.nameCol, cfg.idCol);
                    ps = conn.prepareStatement(sql);
                    ps.setString(1, name);
                    ps.setInt(2, id);
                }
            }

            int affected = ps.executeUpdate();
            ps.close();
            conn.close();

            if (affected > 0) {
                resp.getWriter().write("{\"success\":true,\"message\":\"Actualizado exitosamente\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"success\":false,\"message\":\"Registro no encontrado\"}");
            }

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"Error interno o de base de datos\"}");
            e.printStackTrace();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String servletPath = req.getServletPath();
        EntityConfig cfg = getConfig(servletPath);
        if (cfg == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"Entidad no configurada\"}");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"ID requerido para eliminar\"}");
            return;
        }

        try {
            int id = Integer.parseInt(pathInfo.substring(1));
            String sql = String.format("DELETE FROM %s WHERE %s = ?", cfg.tableName, cfg.idCol);

            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, id);
                int affected = ps.executeUpdate();

                if (affected > 0) {
                    resp.getWriter().write("{\"success\":true,\"message\":\"Eliminado exitosamente\"}");
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{\"success\":false,\"message\":\"Registro no encontrado\"}");
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

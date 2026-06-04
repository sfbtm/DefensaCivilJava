package com.defensacivil.controller;

import com.defensacivil.config.DatabaseConfig;
import com.defensacivil.config.ResponseUtil;
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
        String jsonNameKey;
        Map<String, String> extraColMap;

        EntityConfig(String tableName, String idCol, String nameCol) {
            this(tableName, idCol, nameCol, "name", Map.of());
        }

        EntityConfig(String tableName, String idCol, String nameCol, String jsonNameKey, Map<String, String> extraColMap) {
            this.tableName = tableName;
            this.idCol = idCol;
            this.nameCol = nameCol;
            this.jsonNameKey = jsonNameKey;
            this.extraColMap = extraColMap;
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
        if (servletPath.contains("resources")) return new EntityConfig("Recurso", "IdRecurso", "Nombre", "name", Map.of("Servicio", "service", "Activo", "is_active"));
        if (servletPath.contains("vulnerableQuestions")) return new EntityConfig("Pregunta", "IdPregunta", "Texto", "description", Map.of("Activa", "is_active", "Precaucion", "question_caution"));
        if (servletPath.contains("organizations")) return new EntityConfig("Organizacion", "IdOrganizacion", "Nombre", "name", Map.of("IdSeccional", "sectional_id"));
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

    private Map<String, Object> mapRow(ResultSet rs, EntityConfig cfg) throws SQLException {
        Map<String, Object> item = new HashMap<>();
        item.put("id", rs.getInt(cfg.idCol));
        item.put(cfg.jsonNameKey, rs.getString(cfg.nameCol));
        item.put("is_active", true);

        for (Map.Entry<String, String> entry : cfg.extraColMap.entrySet()) {
            String dbCol = entry.getKey();
            String jsonKey = entry.getValue();
            Object value = rs.getObject(dbCol);

            if (value instanceof Boolean) {
                item.put(jsonKey, value);
            } else if (dbCol.equalsIgnoreCase("Activo") || dbCol.equalsIgnoreCase("Activa") || dbCol.equalsIgnoreCase("Precaucion")) {
                item.put(jsonKey, rs.getBoolean(dbCol));
            } else {
                item.put(jsonKey, value);
            }
        }
        return item;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String servletPath = req.getServletPath();
        EntityConfig cfg = getConfig(servletPath);
        if (cfg == null) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Entidad no configurada");
            return;
        }

        String pathInfo = req.getPathInfo();

        if (pathInfo != null && pathInfo.equals("/paginate")) {
            int page = 1;
            String pageStr = req.getParameter("page");
            if (pageStr != null && !pageStr.isEmpty()) {
                try { page = Integer.parseInt(pageStr); } catch (Exception ignored) {}
            }

            int perPage = 3;
            int offset = (page - 1) * perPage;
            int total = 0;
            List<Map<String, Object>> list = new ArrayList<>();

            boolean hasActiveCol = cfg.extraColMap.containsKey("Activa") || cfg.extraColMap.containsKey("Activo");
            String activeColName = cfg.extraColMap.containsKey("Activa") ? "Activa" : "Activo";

            String countSql = hasActiveCol 
                ? String.format("SELECT COUNT(*) FROM %s WHERE %s = 1", cfg.tableName, activeColName)
                : String.format("SELECT COUNT(*) FROM %s", cfg.tableName);

            String selectSql = hasActiveCol 
                ? String.format("SELECT * FROM %s WHERE %s = 1 LIMIT ? OFFSET ?", cfg.tableName, activeColName)
                : String.format("SELECT * FROM %s LIMIT ? OFFSET ?", cfg.tableName);

            try (Connection conn = DatabaseConfig.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(countSql);
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) total = rs.getInt(1);
                }

                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, perPage);
                    ps.setInt(2, offset);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            list.add(mapRow(rs, cfg));
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

                ResponseUtil.sendSuccess(resp, responseMap);
            } catch (SQLException e) {
                e.printStackTrace();
                ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error de base de datos");
            }
            return;
        }

        if (pathInfo == null || pathInfo.equals("/")) {
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = String.format("SELECT * FROM %s ORDER BY %s DESC", cfg.tableName, cfg.idCol);

            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    list.add(mapRow(rs, cfg));
                }

                ResponseUtil.sendSuccess(resp, list);
            } catch (SQLException e) {
                e.printStackTrace();
                ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error de base de datos");
            }
        } else {
            try {
                String idStr = pathInfo.substring(1);
                if (idStr.startsWith("status/")) {
                    idStr = idStr.substring(7);
                }
                int id = Integer.parseInt(idStr);

                String sql = String.format("SELECT * FROM %s WHERE %s = ?", cfg.tableName, cfg.idCol);

                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {

                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            ResponseUtil.sendSuccess(resp, mapRow(rs, cfg));
                        } else {
                            ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Registro no encontrado");
                        }
                    }
                }
            } catch (NumberFormatException e) {
                ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "ID invalido");
            } catch (SQLException e) {
                e.printStackTrace();
                ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error de base de datos");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String servletPath = req.getServletPath();
        EntityConfig cfg = getConfig(servletPath);
        if (cfg == null) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Entidad no configurada");
            return;
        }

        try {
            BufferedReader reader = req.getReader();
            Map<String, Object> body = gson.fromJson(reader, Map.class);

            if (body == null) {
                ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Cuerpo de peticion vacio");
                return;
            }

            List<String> columns = new ArrayList<>();
            List<Object> values = new ArrayList<>();

            columns.add(cfg.nameCol);
            values.add(body.get(cfg.jsonNameKey));

            for (Map.Entry<String, String> entry : cfg.extraColMap.entrySet()) {
                String dbCol = entry.getKey();
                String jsonKey = entry.getValue();

                Object val = body.get(jsonKey);
                if (val == null) {
                    if (dbCol.equalsIgnoreCase("Activo") || dbCol.equalsIgnoreCase("Activa")) {
                        val = 1;
                    } else if (dbCol.equalsIgnoreCase("Precaucion")) {
                        val = 0;
                    }
                } else {
                    if (val instanceof Boolean) {
                        val = (Boolean) val ? 1 : 0;
                    } else if (val instanceof Number) {
                        val = ((Number) val).intValue();
                    } else if (val instanceof String) {
                        try {
                            val = Integer.parseInt((String) val);
                        } catch (NumberFormatException e) {
                            // Dejar como string
                        }
                    }
                }
                columns.add(dbCol);
                values.add(val);
            }

            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("INSERT INTO ").append(cfg.tableName).append(" (");
            for (int i = 0; i < columns.size(); i++) {
                sqlBuilder.append(columns.get(i));
                if (i < columns.size() - 1) sqlBuilder.append(", ");
            }
            sqlBuilder.append(") VALUES (");
            for (int i = 0; i < columns.size(); i++) {
                sqlBuilder.append("?");
                if (i < columns.size() - 1) sqlBuilder.append(", ");
            }
            sqlBuilder.append(")");

            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlBuilder.toString())) {

                for (int i = 0; i < values.size(); i++) {
                    ps.setObject(i + 1, values.get(i));
                }

                int affected = ps.executeUpdate();
                if (affected > 0) {
                    ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, null, "Creado exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No se pudo crear el registro");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno o de base de datos");
        }
    }

    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String servletPath = req.getServletPath();
        EntityConfig cfg = getConfig(servletPath);
        if (cfg == null) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Entidad no configurada");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "ID requerido");
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
                ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Cuerpo de peticion vacio");
                return;
            }

            if (isStatusUpdate) {
                Object activeObj = body.get("is_active");
                int active = 1;
                if (activeObj instanceof Boolean) {
                    active = (Boolean) activeObj ? 1 : 0;
                } else if (activeObj instanceof Number) {
                    active = ((Number) activeObj).intValue();
                }

                String activeCol = null;
                for (Map.Entry<String, String> entry : cfg.extraColMap.entrySet()) {
                    if (entry.getValue().equals("is_active")) {
                        activeCol = entry.getKey();
                        break;
                    }
                }

                if (activeCol == null) {
                    ResponseUtil.sendSuccess(resp, "Estado actualizado (simulado)");
                    return;
                }

                String sql = String.format("UPDATE %s SET %s = ? WHERE %s = ?", cfg.tableName, activeCol, cfg.idCol);
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, active);
                    ps.setInt(2, id);
                    int affected = ps.executeUpdate();
                    if (affected > 0) {
                        ResponseUtil.sendSuccess(resp, "Actualizado exitosamente");
                    } else {
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Registro no encontrado");
                    }
                }
            } else {
                List<String> setClauses = new ArrayList<>();
                List<Object> values = new ArrayList<>();

                if (body.containsKey(cfg.jsonNameKey)) {
                    setClauses.add(cfg.nameCol + " = ?");
                    values.add(body.get(cfg.jsonNameKey));
                }

                for (Map.Entry<String, String> entry : cfg.extraColMap.entrySet()) {
                    String dbCol = entry.getKey();
                    String jsonKey = entry.getValue();

                    if (body.containsKey(jsonKey)) {
                        setClauses.add(dbCol + " = ?");
                        Object val = body.get(jsonKey);
                        if (val instanceof Boolean) {
                            val = (Boolean) val ? 1 : 0;
                        } else if (val instanceof Number) {
                            val = ((Number) val).intValue();
                        } else if (val instanceof String) {
                            try {
                                val = Integer.parseInt((String) val);
                            } catch (NumberFormatException e) {
                                // Dejar como string
                            }
                        }
                        values.add(val);
                    }
                }

                if (setClauses.isEmpty()) {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "No hay campos para actualizar");
                    return;
                }

                String sql = String.format("UPDATE %s SET %s WHERE %s = ?", cfg.tableName, String.join(", ", setClauses), cfg.idCol);
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {

                    for (int i = 0; i < values.size(); i++) {
                        ps.setObject(i + 1, values.get(i));
                    }
                    ps.setInt(values.size() + 1, id);

                    int affected = ps.executeUpdate();
                    if (affected > 0) {
                        ResponseUtil.sendSuccess(resp, "Actualizado exitosamente");
                    } else {
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Registro no encontrado");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno o de base de datos");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String servletPath = req.getServletPath();
        EntityConfig cfg = getConfig(servletPath);
        if (cfg == null) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Entidad no configurada");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "ID requerido para eliminar");
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
                    ResponseUtil.sendSuccess(resp, "Eliminado exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Registro no encontrado");
                }
            }
        } catch (NumberFormatException e) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "ID invalido");
        } catch (SQLException e) {
            e.printStackTrace();
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error de base de datos o restriccion de clave foranea");
        }
    }
}

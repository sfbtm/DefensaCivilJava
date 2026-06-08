package com.defensacivil.controller;

import com.defensacivil.config.ResponseUtil;
import com.defensacivil.dao.MasterDataDAO;
import com.defensacivil.dao.MasterDataDAOImpl;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
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
    private final MasterDataDAO masterDataDAO = new MasterDataDAOImpl();

    private MasterDataDAO.EntityConfig getConfig(String servletPath) {
        if (servletPath.contains("departments")) return new MasterDataDAO.EntityConfig("Departamento", "IdDepartamento", "Nombre");
        if (servletPath.contains("documentTypes")) return new MasterDataDAO.EntityConfig("DocumentoTipo", "IdDocumentoTipo", "Nombre");
        if (servletPath.contains("nationalities")) return new MasterDataDAO.EntityConfig("Nacionalidad", "IdNacionalidad", "Nombre");
        if (servletPath.contains("sectionals")) return new MasterDataDAO.EntityConfig("Seccional", "IdSeccional", "Nombre");
        if (servletPath.contains("threatTypes")) return new MasterDataDAO.EntityConfig("TipoAmenaza", "IdTipoAmenaza", "Nombre");
        if (servletPath.contains("vulnerabilities")) return new MasterDataDAO.EntityConfig("VulnerabilidadTipo", "IdTipoVulnerabilidad", "Nombre");
        if (servletPath.contains("sectors")) return new MasterDataDAO.EntityConfig("Sector", "IdSector", "Nombre");
        if (servletPath.contains("species")) return new MasterDataDAO.EntityConfig("Especie", "IdEspecie", "Nombre");
        if (servletPath.contains("resources")) return new MasterDataDAO.EntityConfig("Recurso", "IdRecurso", "Nombre", "name", Map.of("Servicio", "service", "Activo", "is_active"));
        if (servletPath.contains("vulnerableQuestions")) return new MasterDataDAO.EntityConfig("Pregunta", "IdPregunta", "Texto", "description", Map.of("Activa", "is_active", "Precaucion", "question_caution"));
        if (servletPath.contains("organizations")) return new MasterDataDAO.EntityConfig("Organizacion", "IdOrganizacion", "Nombre", "name", Map.of("IdSeccional", "sectional_id"));
        if (servletPath.contains("housingQualities")) return new MasterDataDAO.EntityConfig("CalidadVivienda", "IdCalidad", "Nombre");
        if (servletPath.contains("genders")) return new MasterDataDAO.EntityConfig("Genero", "IdGenero", "Nombre");
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
        String servletPath = req.getServletPath();
        MasterDataDAO.EntityConfig cfg = getConfig(servletPath);
        if (cfg == null) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Entidad no configurada");
            return;
        }

        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo != null && pathInfo.equals("/paginate")) {
                int page = 1;
                String pageStr = req.getParameter("page");
                if (pageStr != null && !pageStr.isEmpty()) {
                    try { page = Integer.parseInt(pageStr); } catch (Exception ignored) {}
                }
                int perPage = 3;
                Map<String, Object> responseMap = masterDataDAO.getPaginated(cfg, page, perPage);
                ResponseUtil.sendSuccess(resp, responseMap);
                return;
            }

            if (pathInfo == null || pathInfo.equals("/")) {
                List<Map<String, Object>> list = masterDataDAO.getAll(cfg);
                ResponseUtil.sendSuccess(resp, list);
            } else {
                String idStr = pathInfo.substring(1);
                if (idStr.startsWith("status/")) {
                    idStr = idStr.substring(7);
                }
                int id = Integer.parseInt(idStr);
                Map<String, Object> item = masterDataDAO.getById(cfg, id);
                if (item != null) {
                    ResponseUtil.sendSuccess(resp, item);
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Registro no encontrado");
                }
            }
        } catch (NumberFormatException e) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "ID invalido");
        } catch (SQLException e) {
            e.printStackTrace();
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error de base de datos");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String servletPath = req.getServletPath();
        MasterDataDAO.EntityConfig cfg = getConfig(servletPath);
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

            boolean success = masterDataDAO.insert(cfg, body);
            if (success) {
                ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, null, "Creado exitosamente");
            } else {
                ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No se pudo crear el registro");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error de base de datos");
        } catch (Exception e) {
            e.printStackTrace();
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno");
        }
    }

    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String servletPath = req.getServletPath();
        MasterDataDAO.EntityConfig cfg = getConfig(servletPath);
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

                boolean success = masterDataDAO.updateStatus(cfg, id, active);
                if (success) {
                    ResponseUtil.sendSuccess(resp, "Actualizado exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Registro no encontrado");
                }
            } else {
                boolean success = masterDataDAO.update(cfg, id, body);
                if (success) {
                    ResponseUtil.sendSuccess(resp, "Actualizado exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "No se pudo actualizar (no hay campos o registro no encontrado)");
                }
            }
        } catch (NumberFormatException e) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "ID invalido");
        } catch (SQLException e) {
            e.printStackTrace();
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error de base de datos");
        } catch (Exception e) {
            e.printStackTrace();
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String servletPath = req.getServletPath();
        MasterDataDAO.EntityConfig cfg = getConfig(servletPath);
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
            boolean success = masterDataDAO.delete(cfg, id);
            if (success) {
                ResponseUtil.sendSuccess(resp, "Eliminado exitosamente");
            } else {
                ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Registro no encontrado");
            }
        } catch (NumberFormatException e) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "ID invalido");
        } catch (SQLException e) {
            e.printStackTrace();
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error de base de datos o restriccion de clave foranea");
        }
    }
}

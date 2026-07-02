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

/**
 * Servlet genérico para la gestión de datos maestros del sistema.
 * Provee un CRUD unificado para múltiples tablas secundarias/catálogos,
 * configurando dinámicamente nombres de columnas y mappings.
 * 
 * Endpoints mapeados:
 * - /api/departments/*
 * - /api/documentTypes/*
 * - /api/nationalities/*
 * - /api/sectionals/*
 * - /api/threatTypes/*
 * - /api/vulnerabilities/*
 * - /api/resources/*
 * - /api/sectors/*
 * - /api/species/*
 * - /api/vulnerableQuestions/*
 * - /api/organizations/*
 * - /api/housingQualities/*
 * - /api/genders/*
 */
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

    /**
     * Retorna la configuración de mapeo de base de datos correspondiente al endpoint del servlet solicitado.
     * Asocia la ruta con la tabla, clave primaria, columna descriptiva y campos extras necesarios.
     * 
     * @param servletPath Ruta del endpoint solicitado.
     * @return Objeto EntityConfig con los metadatos de la tabla, o null si la ruta no está configurada.
     */
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

    /**
     * Redirige las peticiones HTTP al método adecuado, proporcionando soporte para el método PATCH.
     * 
     * @param req Petición HTTP.
     * @param resp Respuesta HTTP.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getMethod().equalsIgnoreCase("PATCH")) {
            doPatch(req, resp);
        } else {
            super.service(req, resp);
        }
    }

    /**
     * Procesa las solicitudes HTTP GET para obtener registros de datos maestros.
     * Soporta consulta total, paginada o consulta individual por ID.
     * 
     * Endpoints y Respuestas:
     * - GET /api/{entidad}/paginate?page={page}: Retorna una lista paginada de registros. Retorna JSON: { "data": List, "total": int }
     * - GET /api/{entidad}: Retorna la lista completa de registros. Retorna JSON: [ { ... } ]
     * - GET /api/{entidad}/{id}: Retorna un registro específico por ID. Retorna JSON del registro u 404 si no existe.
     * 
     * @param req Petición HTTP.
     * @param resp Respuesta HTTP en formato JSON.
     * @throws IOException Si ocurre un error de E/S.
     */
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

    /**
     * Procesa las solicitudes HTTP POST para insertar nuevos registros de datos maestros.
     * 
     * Endpoint y Cuerpo:
     * - POST /api/{entidad}: Crea un registro. Cuerpo JSON con las propiedades requeridas de la entidad.
     * 
     * @param req Petición HTTP con JSON en el cuerpo.
     * @param resp Respuesta HTTP.
     * @throws IOException Si ocurre un error de E/S.
     */
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

    /**
     * Procesa las solicitudes HTTP PATCH para actualizar campos de registros existentes o cambiar el estado activo.
     * 
     * Endpoints y Cuerpo:
     * - PATCH /api/{entidad}/{id}: Modifica parcialmente un registro. Cuerpo JSON con las propiedades a actualizar.
     * - PATCH /api/{entidad}/status/{id}: Cambia el estado de activación de un registro. Cuerpo JSON: { "is_active": boolean|int }
     * 
     * @param req Petición HTTP con JSON en el cuerpo y el ID en la ruta.
     * @param resp Respuesta HTTP.
     * @throws IOException Si ocurre un error de E/S.
     */
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

    /**
     * Procesa las solicitudes HTTP DELETE para eliminar registros de datos maestros.
     * 
     * Endpoint:
     * - DELETE /api/{entidad}/{id}: Elimina físicamente el registro de la base de datos por su ID.
     * 
     * @param req Petición HTTP con el ID en la ruta.
     * @param resp Respuesta HTTP.
     * @throws IOException Si ocurre un error de E/S.
     */
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

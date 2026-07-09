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

    /**
     * Instancia de Gson utilizada para serializar y deserializar JSON en las peticiones y respuestas.
     */
    private final Gson gson = new Gson();

    /**
     * Objeto de acceso a datos (DAO) encargado de ejecutar las operaciones dinámicas de CRUD sobre tablas maestras.
     */
    private final MasterDataDAO masterDataDAO = new MasterDataDAOImpl();

    /**
     * Retorna la configuración de mapeo de base de datos correspondiente al endpoint del servlet solicitado.
     * Asocia la ruta con la tabla, clave primaria, columna descriptiva y campos extras necesarios.
     * 
     * @param servletPath Ruta del endpoint solicitado.
     * @return Objeto EntityConfig con los metadatos de la tabla, o null si la ruta no está configurada.
     */
    private MasterDataDAO.EntityConfig getConfig(String servletPath) {
        // Estructura de condicionales en serie para determinar la configuración de tabla según la ruta
        if (servletPath.contains("departments")) {
            // Bloque para departamentos
            return new MasterDataDAO.EntityConfig("Departamento", "IdDepartamento", "Nombre");
        }
        if (servletPath.contains("documentTypes")) {
            // Bloque para tipos de documentos
            return new MasterDataDAO.EntityConfig("DocumentoTipo", "IdDocumentoTipo", "Nombre");
        }
        if (servletPath.contains("nationalities")) {
            // Bloque para nacionalidades
            return new MasterDataDAO.EntityConfig("Nacionalidad", "IdNacionalidad", "Nombre");
        }
        if (servletPath.contains("sectionals")) {
            // Bloque para seccionales
            return new MasterDataDAO.EntityConfig("Seccional", "IdSeccional", "Nombre");
        }
        if (servletPath.contains("threatTypes")) {
            // Bloque para tipos de amenazas
            return new MasterDataDAO.EntityConfig("TipoAmenaza", "IdTipoAmenaza", "Nombre");
        }
        if (servletPath.contains("vulnerabilities")) {
            // Bloque para tipos de vulnerabilidades
            return new MasterDataDAO.EntityConfig("VulnerabilidadTipo", "IdTipoVulnerabilidad", "Nombre");
        }
        if (servletPath.contains("sectors")) {
            // Bloque para sectores de residencia
            return new MasterDataDAO.EntityConfig("Sector", "IdSector", "Nombre");
        }
        if (servletPath.contains("species")) {
            // Bloque para especies de mascotas
            return new MasterDataDAO.EntityConfig("Especie", "IdEspecie", "Nombre");
        }
        if (servletPath.contains("resources")) {
            // Bloque para recursos del plan familiar
            return new MasterDataDAO.EntityConfig("RecursoTipo", "IdRecursoTipo", "Nombre", "name", Map.of("Servicio", "service", "Activo", "is_active"));
        }
        if (servletPath.contains("vulnerableQuestions")) {
            // Bloque para preguntas del cuestionario de vulnerabilidades
            return new MasterDataDAO.EntityConfig("Pregunta", "IdPregunta", "Texto", "description", Map.of("Activa", "is_active", "Precaucion", "question_caution"));
        }
        if (servletPath.contains("organizations")) {
            // Bloque para organizaciones o entidades aliadas
            return new MasterDataDAO.EntityConfig("Organizacion", "IdOrganizacion", "Nombre", "name", Map.of("IdSeccional", "sectional_id"));
        }
        if (servletPath.contains("housingQualities")) {
            // Bloque para calidades de la vivienda
            return new MasterDataDAO.EntityConfig("CalidadVivienda", "IdCalidad", "Nombre");
        }
        if (servletPath.contains("genders")) {
            // Bloque para géneros de personas
            return new MasterDataDAO.EntityConfig("Genero", "IdGenero", "Nombre");
        }
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
        // Condicional para validar si se recibe una solicitud PATCH
        if (req.getMethod().equalsIgnoreCase("PATCH")) {
            // Bloque ejecutado para peticiones PATCH
            doPatch(req, resp);
        } else {
            // Bloque ejecutado para otros verbos HTTP tradicionales
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
        // Condicional para verificar si el recurso solicitado corresponde a una tabla maestra válida
        if (cfg == null) {
            // Bloque ejecutado si la entidad no está mapeada
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Entidad no configurada");
            return;
        }

        String pathInfo = req.getPathInfo();

        // Bloque try para capturar errores de base de datos o de parseo de datos en GET
        try {
            // Condicional para verificar si se solicita paginación de datos
            if (pathInfo != null && pathInfo.equals("/paginate")) {
                // Bloque ejecutado para listar datos maestros de forma paginada
                int page = 1;
                String pageStr = req.getParameter("page");
                // Condicional para verificar si se proveyó el parámetro de número de página
                if (pageStr != null && !pageStr.isEmpty()) {
                    // Bloque try para controlar errores de formato al parsear la página
                    try { 
                        // Intento de conversión a int
                        page = Integer.parseInt(pageStr); 
                    } catch (Exception ignored) {
                        // Bloque catch para omitir fallos de parseo
                    }
                }
                int perPage = 3;
                Map<String, Object> responseMap = masterDataDAO.getPaginated(cfg, page, perPage);
                ResponseUtil.sendSuccess(resp, responseMap);
                return;
            }

            // Condicional para validar si se solicita el listado total o un registro individual
            if (pathInfo == null || pathInfo.equals("/")) {
                // Bloque ejecutado para obtener todos los registros del maestro
                List<Map<String, Object>> list = masterDataDAO.getAll(cfg);
                ResponseUtil.sendSuccess(resp, list);
            } else {
                // Bloque ejecutado para consultar un único registro por su identificador primario
                String idStr = pathInfo.substring(1);
                // Condicional para limpiar el prefijo "status/" si está en la ruta
                if (idStr.startsWith("status/")) {
                    // Bloque ejecutado para extraer solo el ID
                    idStr = idStr.substring(7);
                }
                int id = Integer.parseInt(idStr);
                Map<String, Object> item = masterDataDAO.getById(cfg, id);
                // Condicional para comprobar si se encontró el registro
                if (item != null) {
                    // Bloque ejecutado si el mapa contiene datos
                    ResponseUtil.sendSuccess(resp, item);
                } else {
                    // Bloque ejecutado si el registro no existe
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Registro no encontrado");
                }
            }
        } catch (NumberFormatException e) {
            // Bloque catch para manejar errores en la conversión del ID numérico
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "ID invalido");
        } catch (SQLException e) {
            // Bloque catch para controlar fallos generados a nivel de persistencia (SQL)
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
        // Condicional para verificar si la entidad está configurada en el catálogo de datos maestros
        if (cfg == null) {
            // Bloque ejecutado si el recurso no es reconocido
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Entidad no configurada");
            return;
        }

        // Bloque try para atrapar errores durante la inserción
        try {
            BufferedReader reader = req.getReader();
            Map<String, Object> body = gson.fromJson(reader, Map.class);

            // Condicional para validar si se recibió un cuerpo de petición
            if (body == null) {
                // Bloque ejecutado si la petición no incluye parámetros en JSON
                ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Cuerpo de peticion vacio");
                return;
            }

            boolean success = masterDataDAO.insert(cfg, body);
            // Condicional para verificar si la inserción en la BD fue exitosa
            if (success) {
                // Bloque ejecutado si el registro se creó exitosamente
                ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, null, "Creado exitosamente");
            } else {
                // Bloque ejecutado si la inserción falló
                ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No se pudo crear el registro");
            }
        } catch (SQLException e) {
            // Bloque catch para registrar fallos en la consulta o restricciones de BD
            e.printStackTrace();
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error de base de datos");
        } catch (Exception e) {
            // Bloque catch para controlar otras excepciones generales
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
        // Condicional para verificar que el endpoint coincida con un maestro registrado
        if (cfg == null) {
            // Bloque ejecutado cuando no se reconoce el recurso maestro
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Entidad no configurada");
            return;
        }

        String pathInfo = req.getPathInfo();
        // Condicional para requerir la provisión del ID en la URL
        if (pathInfo == null || pathInfo.equals("/")) {
            // Bloque ejecutado si no se envía el ID a modificar
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "ID requerido");
            return;
        }

        // Bloque try para atrapar y controlar excepciones en la actualización parcial
        try {
            String idStr = pathInfo.substring(1);
            boolean isStatusUpdate = false;
            // Condicional para discernir si es una actualización del estado de activación
            if (idStr.startsWith("status/")) {
                // Bloque ejecutado si la ruta de actualización es /status/{id}
                idStr = idStr.substring(7);
                isStatusUpdate = true;
            }
            int id = Integer.parseInt(idStr);

            BufferedReader reader = req.getReader();
            Map<String, Object> body = gson.fromJson(reader, Map.class);

            // Condicional para rechazar solicitudes con cuerpo vacío
            if (body == null) {
                // Bloque ejecutado si no se envían datos a actualizar
                ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Cuerpo de peticion vacio");
                return;
            }

            // Condicional para discriminar el tipo de PATCH
            if (isStatusUpdate) {
                // Bloque ejecutado si es una actualización lógica del estado (activo/inactivo)
                Object activeObj = body.get("is_active");
                int active = 1;
                // Condicional para determinar la naturaleza del valor booleano o numérico provisto en is_active
                if (activeObj instanceof Boolean) {
                    // Bloque ejecutado si es booleano
                    active = (Boolean) activeObj ? 1 : 0;
                } else if (activeObj instanceof Number) {
                    // Bloque ejecutado si es numérico
                    active = ((Number) activeObj).intValue();
                }

                boolean success = masterDataDAO.updateStatus(cfg, id, active);
                // Condicional para validar si se actualizó el registro en la BD
                if (success) {
                    // Bloque ejecutado si la actualización del estado fue correcta
                    ResponseUtil.sendSuccess(resp, "Actualizado exitosamente");
                } else {
                    // Bloque ejecutado si el ID no existe en la base de datos
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Registro no encontrado");
                }
            } else {
                // Bloque ejecutado para una actualización ordinaria de campos del maestro
                boolean success = masterDataDAO.update(cfg, id, body);
                // Condicional para evaluar si los campos fueron actualizados en base de datos
                if (success) {
                    // Bloque ejecutado en caso de éxito
                    ResponseUtil.sendSuccess(resp, "Actualizado exitosamente");
                } else {
                    // Bloque ejecutado si falló la actualización o no se pasaron campos válidos
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "No se pudo actualizar (no hay campos o registro no encontrado)");
                }
            }
        } catch (NumberFormatException e) {
            // Bloque catch para manejar IDs no numéricos
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "ID invalido");
        } catch (SQLException e) {
            // Bloque catch para fallas del motor SQL o violaciones de clave foránea
            e.printStackTrace();
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error de base de datos");
        } catch (Exception e) {
            // Bloque catch para otros errores técnicos imprevistos
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
        // Condicional para validar la existencia de la tabla en el mapeo dinámico
        if (cfg == null) {
            // Bloque ejecutado si la entidad no está configurada
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Entidad no configurada");
            return;
        }

        String pathInfo = req.getPathInfo();
        // Condicional para requerir el ID del registro a eliminar
        if (pathInfo == null || pathInfo.equals("/")) {
            // Bloque ejecutado si no se provee el ID en la ruta
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "ID requerido para eliminar");
            return;
        }

        // Bloque try para capturar excepciones al realizar el borrado físico
        try {
            int id = Integer.parseInt(pathInfo.substring(1));
            boolean success = masterDataDAO.delete(cfg, id);
            // Condicional para validar si se borró el registro
            if (success) {
                // Bloque ejecutado si el borrado se efectúa con éxito
                ResponseUtil.sendSuccess(resp, "Eliminado exitosamente");
            } else {
                // Bloque ejecutado si no se encuentra el registro a eliminar
                ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Registro no encontrado");
            }
        } catch (NumberFormatException e) {
            // Bloque catch para capturar IDs erróneos
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "ID invalido");
        } catch (SQLException e) {
            // Bloque catch si se viola una restricción de clave foránea al eliminar
            e.printStackTrace();
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error de base de datos o restriccion de clave foranea");
        }
    }
}

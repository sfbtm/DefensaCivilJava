package com.defensacivil.controller;

import com.defensacivil.config.ResponseUtil;
import com.defensacivil.dao.PlanFamiliarDAO;
import com.defensacivil.dao.PlanFamiliarDAOImpl;
import com.defensacivil.dao.PlanComplementarioDAO;
import com.defensacivil.dao.PlanComplementarioDAOImpl;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Servlet principal para gestionar la información de los planes familiares de emergencia
 * (Family Plans) y sus recursos disponibles asociados. También provee datos estadísticos
 * consolidados para los paneles de administración y supervisión.
 * 
 * Endpoints mapeados:
 * - /api/familyPlans/*
 * - /api/availableResources/*
 * - /api/audits/*
 */
@WebServlet(urlPatterns = {
    "/api/familyPlans/*",
    "/api/availableResources/*",
    "/api/audits/*"
})
@MultipartConfig
public class PlanDetailsServlet extends HttpServlet {

    private final Gson gson = new Gson();

    // In-memory store for fields not mapped in the database schema (to align with legacy academic DB)
    private static final Map<String, Map<String, Object>> extraData = new ConcurrentHashMap<>();

    private final PlanFamiliarDAO planFamiliarDAO = new PlanFamiliarDAOImpl(extraData);
    private final PlanComplementarioDAO planComplementarioDAO = new PlanComplementarioDAOImpl(extraData);

    /**
     * Redirige las peticiones HTTP al método adecuado según el verbo HTTP, brindando soporte
     * específico para peticiones HTTP PATCH.
     * 
     * @param req Petición HTTP recibida.
     * @param resp Respuesta HTTP a enviar.
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
     * Procesa las solicitudes HTTP GET para obtener estadísticas de planes o información de recursos.
     * 
     * Endpoints y Respuestas:
     * - GET /api/audits/dashBoardSupervisor: Retorna métricas generales del supervisor (planes aprobados, en revisión, etc.).
     * - GET /api/audits/dashBoardAdmin: Retorna métricas consolidadas del administrador por seccional.
     * - GET /api/familyPlans: Lista los planes familiares filtrados según el rol, usuario y seccional de la sesión.
     * - GET /api/familyPlans/check-access/{id}: Verifica acceso del usuario al plan (siempre retorna true).
     * - GET /api/familyPlans/has-members/{id}: Determina si un plan familiar posee integrantes registrados.
     * - GET /api/familyPlans/validate-requirements/{id}: Valida los requerimientos mínimos del plan familiar para su radicación.
     * - GET /api/familyPlans/{id}: Obtiene los detalles completos de un plan familiar específico por su ID.
     * - GET /api/availableResources/familyPlan/{planId}: Lista de recursos de emergencia disponibles para el plan familiar dado.
     * - GET /api/availableResources/{id}: Detalles de un recurso disponible específico por su ID.
     * 
     * @param req Petición HTTP.
     * @param resp Respuesta HTTP en formato JSON.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            if (servletPath != null && servletPath.contains("audits")) {
                if (pathInfo != null && pathInfo.contains("dashBoardSupervisor")) {
                    responderDashboardSupervisor(resp);
                    return;
                } else if (pathInfo != null && pathInfo.contains("dashBoardAdmin")) {
                    responderDashboardAdmin(resp);
                    return;
                }
            }

            // FAMILY PLANS
            if (servletPath.contains("familyPlans")) {
                if (pathInfo == null || pathInfo.equals("/")) {
                    jakarta.servlet.http.HttpSession session = req.getSession();
                    Integer loggedInUserId = (Integer) session.getAttribute("userId");
                    Integer loggedInRoleId = (Integer) session.getAttribute("roleId");
                    Integer loggedInSectionalId = (Integer) session.getAttribute("sectionalId");

                    if (loggedInUserId == null || loggedInRoleId == null || loggedInSectionalId == null) {
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Usuario no autenticado o sesion invalida");
                        return;
                    }

                    int userId = loggedInUserId;
                    int roleId = loggedInRoleId;
                    int sectionalId = loggedInSectionalId;

                    List<Map<String, Object>> list = planFamiliarDAO.getFamilyPlans(roleId, userId, sectionalId);
                    ResponseUtil.sendSuccess(resp, list);
                    return;

                } else if (pathInfo.startsWith("/check-access/")) {
                    ResponseUtil.sendSuccess(resp, Map.of("has_access", true, "access_check", true));
                    return;

                } else if (pathInfo.startsWith("/has-members/")) {
                    int idVal = Integer.parseInt(pathInfo.substring(13));
                    boolean hasMembers = planFamiliarDAO.hasMembers(idVal);
                    ResponseUtil.sendSuccess(resp, Map.of("has_members", hasMembers));
                    return;

                } else if (pathInfo.startsWith("/validate-requirements/")) {
                    int idVal = Integer.parseInt(pathInfo.substring(23));
                    Map<String, Object> valMap = planFamiliarDAO.validateRequirements(idVal);
                    ResponseUtil.sendSuccess(resp, valMap);
                    return;

                } else {
                    // GET SINGLE PLAN BY ID
                    int idVal = Integer.parseInt(pathInfo.substring(1));
                    Map<String, Object> item = planFamiliarDAO.getPlanById(idVal);
                    if (item != null && !item.isEmpty()) {
                        ResponseUtil.sendSuccess(resp, item);
                    } else {
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Plan familiar no encontrado");
                    }
                    return;
                }
            }

            // GET /api/availableResources
            if (servletPath.contains("availableResources")) {
                if (pathInfo != null && pathInfo.startsWith("/familyPlan/")) {
                    int planId = Integer.parseInt(pathInfo.substring(12));
                    List<Map<String, Object>> list = planComplementarioDAO.getAvailableResourcesByPlan(planId);
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                    return;
                } else if (pathInfo != null && !pathInfo.equals("/")) {
                    int idVal = Integer.parseInt(pathInfo.substring(1));
                    Map<String, Object> item = planComplementarioDAO.getAvailableResourceById(idVal);
                    resp.getWriter().write(gson.toJson(Map.of("data", item)));
                    return;
                }
            }

            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta GET no soportada");

        } catch (Exception e) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Procesa las solicitudes HTTP POST para la creación de planes familiares o la adición de recursos disponibles.
     * Soporta cuerpos tanto JSON como multiparte (para alineación con la interfaz SPA).
     * 
     * Endpoints y Parámetros:
     * - POST /api/familyPlans: Crea un plan familiar para el usuario logueado. JSON/Multiparte requerido: { "last_names": String, "user_id": int (opcional) }
     * - POST /api/availableResources: Registra un recurso disponible. JSON requerido: { "family_plan_id": int, "resource_id": int, "description": String, "location": String, "distance": float, "phone": String }
     * 
     * @param req Petición HTTP.
     * @param resp Respuesta HTTP en formato JSON.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        Map<String, Object> body = new HashMap<>();
        String contentType = req.getContentType();
        boolean isMultipart = contentType != null && contentType.toLowerCase().contains("multipart/form-data");

        try {
            if (isMultipart) {
                for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
                    if (entry.getValue().length > 0) {
                        body.put(entry.getKey(), entry.getValue()[0]);
                    }
                }
                if (pathInfo != null && pathInfo.length() > 1) {
                    String[] segments = pathInfo.split("/");
                    if (segments.length > 1) {
                        try {
                            body.put("family_plan_id", Integer.parseInt(segments[1]));
                        } catch (NumberFormatException e) {
                            // Ignored
                        }
                    }
                }
            } else {
                BufferedReader reader = req.getReader();
                Map<String, Object> jsonBody = gson.fromJson(reader, Map.class);
                if (jsonBody != null) {
                    body.putAll(jsonBody);
                }
            }

            // Si el POST Se realiza hacian familyPlans:
            // POST /api/familyPlans
            if (servletPath.contains("familyPlans")) {
                String lastNames = (String) body.get("last_names");

                jakarta.servlet.http.HttpSession session = req.getSession();
//                Wrapper class por si el usuario no ha signeado
                Integer loggedInUserId = (Integer) session.getAttribute("userId");
                int userId;
                if (loggedInUserId != null) {
                    userId = loggedInUserId;
                } else {
//                    Uso de Object ya que se hace uso de JSON mixto
                    Object userIdObj = body.get("user_id");
                    if (userIdObj instanceof Number) {
                        userId = ((Number) userIdObj).intValue();
                    } else if (userIdObj instanceof String) {
                        userId = Integer.parseInt((String) userIdObj);
                    } else {
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Usuario no autenticado");
                        return;
                    }
                }

//                Mandar el planId al DAO para que sea creado en la DB
                int planId = planFamiliarDAO.createFamilyPlan(lastNames, userId, body);
                if (planId > 0) {
                    ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, Map.of("id", planId), "Plan familiar creado con exito");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al crear el plan familiar");
                }
                return;
            }

            // Si el POST Se realiza hacian availableResources (Recursos disponibles):
            // POST /api/availableResources
            if (servletPath.contains("availableResources")) {
                Object resourceIdObj = body.get("resource_id");
                int resourceId = 1;
//                Validar si es num o str, si es str convertir a num
                if (resourceIdObj instanceof Number) resourceId = ((Number) resourceIdObj).intValue();
                else if (resourceIdObj instanceof String) resourceId = Integer.parseInt((String) resourceIdObj);

                String description = (String) body.get("description");
                String location = (String) body.get("location");
                
                Object distanceObj = body.get("distance");
                float distance = 0.0f;
//                Lo mismo que con resourceId, pero con float
                if (distanceObj instanceof Number) distance = ((Number) distanceObj).floatValue();
                else if (distanceObj instanceof String && !((String) distanceObj).isEmpty()) distance = Float.parseFloat((String) distanceObj);

                String phone = (String) body.get("phone");

                Object planIdObj = body.get("family_plan_id");
                int planId = 1;
                if (planIdObj instanceof Number) planId = ((Number) planIdObj).intValue();
                else if (planIdObj instanceof String) planId = Integer.parseInt((String) planIdObj);

                if (planComplementarioDAO.insertAvailableResource(planId, resourceId, description, location, distance, phone)) {
                    ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, null, "Recurso disponible agregado exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al agregar recurso disponible");
                }
                return;
            }

            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta POST no soportada");

        } catch (Exception e) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Procesa las solicitudes HTTP PUT. Este servlet no soporta PUT y retorna un error 400 Bad Request.
     * 
     * @param req Petición HTTP.
     * @param resp Respuesta HTTP en formato JSON.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta PUT no soportada");
    }

    /**
     * Procesa las solicitudes HTTP PATCH para actualizar estados, identificaciones o tipos de familia en los planes familiares
     * y modificar información de recursos disponibles.
     * 
     * Endpoints y Parámetros:
     * - PATCH /api/familyPlans/{id}/identify: Actualiza los campos de identificación del plan familiar (dirección, teléfono, tipo de vivienda, calidad de vivienda, etc.).
     * - PATCH /api/familyPlans/{id}/change-status: Actualiza el estado del plan familiar (e.g., radicar, revisar, rechazar) y guarda observaciones. JSON: { "status_plan_id": int, "comentary": String }
     * - PATCH /api/familyPlans/{id}/change-family-type: Modifica el tipo de estructura familiar. JSON: { "family_type_id": int }
     * - PATCH /api/familyPlans/status/{id}: Cambia de manera rápida el estado del plan. JSON: { "status_plan_id": int }
     * - PATCH /api/availableResources/{id}: Modifica los datos de un recurso de emergencia disponible existente.
     * 
     * @param req Petición HTTP con JSON en el cuerpo.
     * @param resp Respuesta HTTP en formato JSON.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        try {
            BufferedReader reader = req.getReader();
            Map<String, Object> body = gson.fromJson(reader, Map.class);
            if (body == null) body = new HashMap<>();

            // PATCH /api/familyPlans/{id}/identify
            if (servletPath.contains("familyPlans") && pathInfo != null && pathInfo.endsWith("/identify")) {
                String[] segments = pathInfo.split("/");
                int planId = Integer.parseInt(segments[1]);

                boolean updated = planFamiliarDAO.updateIdentification(planId, body);
                if (updated) {
                    ResponseUtil.sendSuccess(resp, "Datos de identificacion actualizados");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Plan familiar no encontrado");
                }
                return;
            }

            // PATCH /api/familyPlans/{id}/change-status
            if (servletPath.contains("familyPlans") && pathInfo != null && pathInfo.endsWith("/change-status")) {
                String[] segments = pathInfo.split("/");
                int planId = Integer.parseInt(segments[1]);

                Object statusObj = body.get("status_plan_id");
                int statusId = 1;
                if (statusObj instanceof Number) statusId = ((Number) statusObj).intValue();

                String comment = (String) body.get("comentary");

                boolean updated = planFamiliarDAO.changeStatus(planId, statusId, comment);
                if (updated) {
                    ResponseUtil.sendSuccess(resp, "Estado de plan actualizado exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Plan familiar no encontrado");
                }
                return;
            }

            // PATCH /api/familyPlans/{id}/change-family-type
            if (servletPath.contains("familyPlans") && pathInfo != null && pathInfo.endsWith("/change-family-type")) {
                String[] segments = pathInfo.split("/");
                int planId = Integer.parseInt(segments[1]);

                Object ftIdObj = body.get("family_type_id");
                int familyTypeId = 3;
                if (ftIdObj instanceof Number) familyTypeId = ((Number) ftIdObj).intValue();
                else if (ftIdObj instanceof String) familyTypeId = Integer.parseInt((String) ftIdObj);

                boolean updated = planFamiliarDAO.changeFamilyType(planId, familyTypeId);
                if (updated) {
                    ResponseUtil.sendSuccess(resp, "Tipo de familia actualizado exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Plan familiar no encontrado");
                }
                return;
            }

            // PATCH /api/familyPlans/status/{id}
            if (servletPath.contains("familyPlans") && pathInfo != null && pathInfo.startsWith("/status/")) {
                int planId = Integer.parseInt(pathInfo.substring(8));
                Object statusObj = body.get("status_plan_id");
                int statusId = 1;
                if (statusObj instanceof Number) statusId = ((Number) statusObj).intValue();

                boolean updated = planFamiliarDAO.changeStatus(planId, statusId, null);
                if (updated) {
                    ResponseUtil.sendSuccess(resp, "Estado de plan actualizado exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Plan familiar no encontrado");
                }
                return;
            }

            // PATCH /api/availableResources/{id}
            if (servletPath.contains("availableResources")) {
                int idVal = Integer.parseInt(pathInfo.substring(1));
                Object resourceIdObj = body.get("resource_id");
                int resourceId = 1;
                if (resourceIdObj instanceof Number) resourceId = ((Number) resourceIdObj).intValue();
                else if (resourceIdObj instanceof String) resourceId = Integer.parseInt((String) resourceIdObj);

                String description = (String) body.get("description");
                String location = (String) body.get("location");

                Object distanceObj = body.get("distance");
                float distance = 0.0f;
                if (distanceObj instanceof Number) distance = ((Number) distanceObj).floatValue();
                else if (distanceObj instanceof String && !((String) distanceObj).isEmpty()) distance = Float.parseFloat((String) distanceObj);

                String phone = (String) body.get("phone");

                if (planComplementarioDAO.updateAvailableResource(idVal, resourceId, description, location, distance, phone)) {
                    ResponseUtil.sendSuccess(resp, "Recurso disponible actualizado exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Recurso disponible no encontrado");
                }
                return;
            }

            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta PATCH no soportada");

        } catch (Exception e) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Procesa las solicitudes HTTP DELETE para remover recursos de emergencia.
     * 
     * Endpoint:
     * - DELETE /api/availableResources/{id}: Elimina permanentemente el recurso disponible por su ID.
     * 
     * @param req Petición HTTP.
     * @param resp Respuesta HTTP en formato JSON.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        try {
            int idVal = Integer.parseInt(pathInfo.substring(1));

            // DELETE /api/availableResources/{id}
            if (servletPath.contains("availableResources")) {
                if (planComplementarioDAO.deleteAvailableResource(idVal)) {
                    ResponseUtil.sendSuccess(resp, "Recurso disponible eliminado exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Recurso disponible no encontrado");
                }
                return;
            }

            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta DELETE no soportada");

        } catch (Exception e) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Auxiliar para consultar y retornar los datos estadísticos consolidados para el supervisor.
     * 
     * @param resp Respuesta HTTP.
     * @throws IOException Si ocurre un error de E/S.
     * @throws SQLException Si ocurre un error de consulta SQL.
     */
    private void responderDashboardSupervisor(HttpServletResponse resp) throws IOException, SQLException {
        Map<String, Object> data = planFamiliarDAO.getSupervisorDashboard();
        ResponseUtil.sendSuccess(resp, data);
    }

    /**
     * Auxiliar para consultar y retornar los datos estadísticos consolidados para el administrador.
     * 
     * @param resp Respuesta HTTP.
     * @throws IOException Si ocurre un error de E/S.
     * @throws SQLException Si ocurre un error de consulta SQL.
     */
    private void responderDashboardAdmin(HttpServletResponse resp) throws IOException, SQLException {
        Map<String, Object> responseData = planFamiliarDAO.getAdminDashboard();
        ResponseUtil.sendSuccess(resp, responseData);
    }
}

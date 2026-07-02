package com.defensacivil.controller;

import com.defensacivil.config.ResponseUtil;
import com.defensacivil.dao.PlanComplementarioDAO;
import com.defensacivil.dao.PlanComplementarioDAOImpl;
import com.defensacivil.dto.ActionPlanDTO;
import com.defensacivil.dto.ActionDTO;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servlet que gestiona los planes de acción y las acciones asociadas a ellos.
 * Soporta operaciones para consultar, crear, actualizar y eliminar planes de acción
 * y acciones asociadas a través de endpoints REST.
 * 
 * Mapea las siguientes rutas:
 * - /api/actionPlans/*
 * - /api/actionPlanActions/*
 */
@WebServlet(urlPatterns = {
    "/api/actionPlans/*",
    "/api/actionPlanActions/*"
})
public class ActionPlanServlet extends HttpServlet {

    private final Gson gson = new Gson();
    private static final Map<String, Map<String, Object>> extraData = new ConcurrentHashMap<>();
    private final PlanComplementarioDAO planComplementarioDAO = new PlanComplementarioDAOImpl(extraData);

    /**
     * Controla el flujo de las peticiones HTTP redirigiéndolas al método correspondiente.
     * Permite el soporte para el método HTTP PATCH.
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
     * Procesa las solicitudes HTTP GET para obtener información de planes de acción y sus acciones.
     * 
     * Endpoints y Respuestas:
     * - GET /api/actionPlans/familyPlan/boolean/{planId}: Determina si un plan familiar posee un plan de acción. Retorna success: boolean.
     * - GET /api/actionPlans/familyPlan/{planId}: Recupera el plan de acción de un plan familiar. Retorna success: ActionPlanDTO o error 404.
     * - GET /api/actionPlanActions/actionPlan/{actionPlanId}: Recupera la lista de acciones de un plan de acción. Retorna success: List&lt;ActionDTO&gt;.
     * - GET /api/actionPlanActions/{id}: Recupera los detalles de una acción específica por su ID. Retorna success: ActionDTO o error 404.
     * 
     * @param req Petición HTTP.
     * @param resp Respuesta HTTP.
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
            // ACTION PLANS (PlanAccion)
            if (servletPath.contains("actionPlans")) {
                if (pathInfo != null && pathInfo.startsWith("/familyPlan/boolean/")) {
                    int planId = extractId(pathInfo, "/familyPlan/boolean");
                    boolean exists = planComplementarioDAO.hasActionPlan(planId);
                    ResponseUtil.sendSuccess(resp, exists);
                } else if (pathInfo != null && pathInfo.startsWith("/familyPlan/")) {
                    int planId = extractId(pathInfo, "/familyPlan");
                    ActionPlanDTO dto = planComplementarioDAO.getActionPlanByPlan(planId);
                    if (dto != null) {
                        ResponseUtil.sendSuccess(resp, dto);
                    } else {
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Plan de acción no encontrado");
                    }
                }
                return;
            }

            // ACTION PLAN ACTIONS (Accion)
            if (servletPath.contains("actionPlanActions")) {
                if (pathInfo != null && pathInfo.startsWith("/actionPlan/")) {
                    int actionPlanId = extractId(pathInfo, "/actionPlan");
                    List<ActionDTO> list = planComplementarioDAO.getActionsByActionPlan(actionPlanId);
                    ResponseUtil.sendSuccess(resp, list);
                } else {
                    int idVal = extractId(pathInfo, null);
                    ActionDTO dto = planComplementarioDAO.getActionById(idVal);
                    if (dto != null) {
                        ResponseUtil.sendSuccess(resp, dto);
                    } else {
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Acción no encontrada");
                    }
                }
                return;
            }

            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta GET no soportada");
        } catch (Exception e) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Procesa las solicitudes HTTP POST para crear nuevos planes de acción o acciones asociadas.
     * 
     * Endpoints y Parámetros en JSON:
     * - POST /api/actionPlans: Crea un nuevo plan de acción. JSON requerido: { "family_plan_id": int, "coordinator_id": int }
     * - POST /api/actionPlanActions: Registra una nueva acción. JSON requerido: { "action_plan_id": int, "member_id": int, "description": String, "action_type_id": int }
     * 
     * @param req Petición HTTP con el JSON en el cuerpo.
     * @param resp Respuesta HTTP.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String servletPath = req.getServletPath();
        BufferedReader reader = req.getReader();
        Map<String, Object> body = gson.fromJson(reader, Map.class);
        if (body == null) body = new HashMap<>();

        try {
            // POST /api/actionPlans
            if (servletPath.contains("actionPlans")) {
                int planId = extractId(body.get("family_plan_id"));
                int coordId = extractId(body.get("coordinator_id"));
                if (planComplementarioDAO.createActionPlan(planId, coordId)) {
                    ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, null, "Plan de accion creado exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al crear plan de accion");
                }
                return;
            }

            // POST /api/actionPlanActions
            if (servletPath.contains("actionPlanActions")) {
                int planAccionId = extractId(body.get("action_plan_id"));
                int memberId = extractId(body.get("member_id"));
                String description = (String) body.get("description");
                int typeId = extractId(body.get("action_type_id"));
                String stage = (typeId == 1) ? "antes" : (typeId == 2 ? "durante" : "despues");

                if (planComplementarioDAO.insertAction(planAccionId, memberId, stage, description)) {
                    ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, null, "Accion creada exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al crear accion");
                }
                return;
            }

            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta POST no soportada");
        } catch (Exception e) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Procesa las solicitudes HTTP PATCH para actualizar una acción existente.
     * 
     * Endpoint y Parámetros en JSON:
     * - PATCH /api/actionPlanActions/{id}: Modifica una acción. JSON requerido: { "member_id": int, "description": String }
     * 
     * @param req Petición HTTP con el JSON en el cuerpo.
     * @param resp Respuesta HTTP.
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

            // PATCH /api/actionPlanActions/{id}
            if (servletPath.contains("actionPlanActions")) {
                int idVal = extractId(pathInfo, null);
                int memberId = extractId(body.get("member_id"));
                String description = (String) body.get("description");
                if (planComplementarioDAO.updateAction(idVal, memberId, description)) {
                    ResponseUtil.sendSuccess(resp, "Accion de plan actualizada exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Accion de plan no encontrada");
                }
                return;
            }

            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta PATCH no soportada");
        } catch (Exception e) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Procesa las solicitudes HTTP DELETE para eliminar acciones.
     * 
     * Endpoint:
     * - DELETE /api/actionPlanActions/{id}: Elimina físicamente la acción identificada por el ID proporcionado.
     * 
     * @param req Petición HTTP con el ID de la acción en la ruta.
     * @param resp Respuesta HTTP.
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

            // DELETE /api/actionPlanActions/{id}
            if (servletPath.contains("actionPlanActions")) {
                if (planComplementarioDAO.deleteAction(idVal)) {
                    ResponseUtil.sendSuccess(resp, "Accion de plan de accion eliminada");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Accion no encontrada");
                }
                return;
            }

            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta DELETE no soportada");
        } catch (Exception e) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Extrae un identificador numérico de la información de la ruta (pathInfo) de la petición HTTP,
     * opcionalmente omitiendo un prefijo dado.
     * 
     * @param pathInfo Información de ruta del servlet.
     * @param prefix Prefijo que precede al ID en la ruta.
     * @return El ID numérico parseado, o 0 en caso de error de formato o nulidad.
     */
    private int extractId(String pathInfo, String prefix) {
        if (pathInfo == null) return 0;
        String path = pathInfo;
        if (prefix != null && path.startsWith(prefix)) {
            path = path.substring(prefix.length());
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        int slashIdx = path.indexOf('/');
        if (slashIdx != -1) {
            path = path.substring(0, slashIdx);
        }
        try {
            return Integer.parseInt(path);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Extrae un identificador numérico de un objeto de tipo genérico, el cual puede ser
     * numérico o una cadena que represente un entero.
     * 
     * @param obj Objeto que representa el ID.
     * @return El ID numérico parseado, o 0 en caso de error de formato o nulidad.
     */
    private int extractId(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}

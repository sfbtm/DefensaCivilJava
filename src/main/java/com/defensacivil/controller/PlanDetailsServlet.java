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

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getMethod().equalsIgnoreCase("PATCH")) {
            doPatch(req, resp);
        } else {
            super.service(req, resp);
        }
    }

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

            // POST /api/familyPlans
            if (servletPath.contains("familyPlans")) {
                String lastNames = (String) body.get("last_names");

                jakarta.servlet.http.HttpSession session = req.getSession();
                Integer loggedInUserId = (Integer) session.getAttribute("userId");
                int userId;
                if (loggedInUserId != null) {
                    userId = loggedInUserId;
                } else {
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

                int planId = planFamiliarDAO.createFamilyPlan(lastNames, userId, body);
                if (planId > 0) {
                    ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, Map.of("id", planId), "Plan familiar creado con exito");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al crear el plan familiar");
                }
                return;
            }

            // POST /api/availableResources
            if (servletPath.contains("availableResources")) {
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

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta PUT no soportada");
    }

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

    private void responderDashboardSupervisor(HttpServletResponse resp) throws IOException, SQLException {
        Map<String, Object> data = planFamiliarDAO.getSupervisorDashboard();
        ResponseUtil.sendSuccess(resp, data);
    }

    private void responderDashboardAdmin(HttpServletResponse resp) throws IOException, SQLException {
        Map<String, Object> responseData = planFamiliarDAO.getAdminDashboard();
        ResponseUtil.sendSuccess(resp, responseData);
    }
}

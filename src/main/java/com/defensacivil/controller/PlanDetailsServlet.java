package com.defensacivil.controller;

import com.defensacivil.config.DatabaseConfig;
import com.defensacivil.config.ResponseUtil;
import com.defensacivil.dao.PlanFamiliarDAO;
import com.defensacivil.dao.PlanFamiliarDAOImpl;
import com.defensacivil.dao.PlanComplementarioDAO;
import com.defensacivil.dao.PlanComplementarioDAOImpl;
import com.defensacivil.dto.HousingInfoDTO;
import com.defensacivil.dto.ActionPlanDTO;
import com.defensacivil.dto.ActionDTO;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebServlet(urlPatterns = {
    "/api/familyPlans/*",
    "/api/statusPlans",
    "/api/zones",
    "/api/cities",
    "/api/housingInfo/*",
    "/api/actionPlans/*",
    "/api/actionPlanActions/*",
    "/api/kinships",
    "/api/bloodGroups",
    "/api/animalGenders",
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
                    Map<String, Object> data = planFamiliarDAO.getSupervisorDashboard();
                    ResponseUtil.sendSuccess(resp, data);
                    return;
                } else if (pathInfo != null && pathInfo.contains("dashBoardAdmin")) {
                    Map<String, Object> responseData = planFamiliarDAO.getAdminDashboard();
                    ResponseUtil.sendSuccess(resp, responseData);
                    return;
                }
            }

            // MOCKS/CATALOGS
            if (servletPath.contains("statusPlans")) {
                List<Map<String, Object>> list = List.of(
                    Map.of("id", 1, "name", "Creado"),
                    Map.of("id", 3, "name", "En proceso"),
                    Map.of("id", 4, "name", "En Revision"),
                    Map.of("id", 5, "name", "Devuelto con observaciones"),
                    Map.of("id", 6, "name", "Rechazado"),
                    Map.of("id", 7, "name", "Completado")
                );
                resp.getWriter().write(gson.toJson(Map.of("data", list)));
                return;
            }

            if (servletPath.contains("zones")) {
                List<Map<String, Object>> list = List.of(
                    Map.of("id", 1, "name", "Urbana"),
                    Map.of("id", 2, "name", "Rural")
                );
                resp.getWriter().write(gson.toJson(Map.of("data", list)));
                return;
            }

            if (servletPath.contains("cities")) {
                List<Map<String, Object>> list = List.of(
                    Map.of("id", 1, "name", "Medellín"),
                    Map.of("id", 2, "name", "Bello"),
                    Map.of("id", 3, "name", "Itagüí")
                );
                resp.getWriter().write(gson.toJson(Map.of("data", list)));
                return;
            }

            if (servletPath.contains("kinships")) {
                if (pathInfo != null && !pathInfo.equals("/")) {
                    int id = Integer.parseInt(pathInfo.substring(1));
                    String name = switch (id) {
                        case 1 -> "Padre";
                        case 2 -> "Madre";
                        case 3 -> "Hijo/a";
                        default -> "Otro";
                    };
                    resp.getWriter().write(gson.toJson(Map.of("data", Map.of("id", id, "name", name))));
                } else {
                    List<Map<String, Object>> list = List.of(
                        Map.of("id", 1, "name", "Padre"),
                        Map.of("id", 2, "name", "Madre"),
                        Map.of("id", 3, "name", "Hijo/a"),
                        Map.of("id", 4, "name", "Otro")
                    );
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                }
                return;
            }

            if (servletPath.contains("bloodGroups")) {
                if (pathInfo != null && !pathInfo.equals("/")) {
                    int id = Integer.parseInt(pathInfo.substring(1));
                    String name = switch (id) {
                        case 1 -> "O+";
                        case 2 -> "O-";
                        case 3 -> "A+";
                        case 4 -> "A-";
                        case 5 -> "B+";
                        case 6 -> "B-";
                        case 7 -> "AB+";
                        case 8 -> "AB-";
                        default -> "O+";
                    };
                    resp.getWriter().write(gson.toJson(Map.of("data", Map.of("id", id, "name", name))));
                } else {
                    List<Map<String, Object>> list = List.of(
                        Map.of("id", 1, "name", "O+"),
                        Map.of("id", 2, "name", "O-"),
                        Map.of("id", 3, "name", "A+"),
                        Map.of("id", 4, "name", "A-"),
                        Map.of("id", 5, "name", "B+"),
                        Map.of("id", 6, "name", "B-"),
                        Map.of("id", 7, "name", "AB+"),
                        Map.of("id", 8, "name", "AB-")
                    );
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                }
                return;
            }

            if (servletPath.contains("animalGenders")) {
                if (pathInfo != null && pathInfo.startsWith("/pet/")) {
                    int petId = Integer.parseInt(pathInfo.substring(5));
                    String sql = "SELECT m.IdGenero, g.Nombre AS GeneroNombre FROM Mascotas m LEFT JOIN Genero g ON m.IdGenero = g.IdGenero WHERE m.IdMascota = ?";
                    Map<String, Object> gender = Map.of("id", 1, "name", "Macho");
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, petId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                int genderId = rs.getInt("IdGenero");
                                String genderName = rs.getString("GeneroNombre");
                                gender = Map.of("id", genderId, "name", genderName != null ? genderName : "Macho");
                            }
                        }
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", gender)));
                    return;
                } else if (pathInfo != null && !pathInfo.equals("/")) {
                    int id = Integer.parseInt(pathInfo.substring(1));
                    String name = (id == 1) ? "Macho" : "Hembra";
                    resp.getWriter().write(gson.toJson(Map.of("data", Map.of("id", id, "name", name))));
                } else {
                    List<Map<String, Object>> list = List.of(
                        Map.of("id", 1, "name", "Macho"),
                        Map.of("id", 2, "name", "Hembra")
                    );
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                }
                return;
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
                    List<Map<String, Object>> list = new ArrayList<>();
                    String sql = """
                        SELECT rd.IdRecurso, rd.Ubicacion, rd.Distancia, rd.Telefono, rt.Nombre AS RecursoNombre, s.Nombre AS ServicioNombre
                        FROM RecursoDisponible rd
                        JOIN RecursoTipo rt ON rd.IdRecursoTipo = rt.IdRecursoTipo
                        JOIN Servicio s ON rd.IdServicio = s.IdServicio
                        WHERE rd.IdPlanFamiliar = ?
                        """;
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, planId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                Map<String, Object> item = new HashMap<>();
                                int resourceId = rs.getInt("IdRecurso");
                                item.put("id", resourceId);
                                item.put("resource_name", rs.getString("RecursoNombre"));
                                item.put("location", rs.getString("Ubicacion") != null ? rs.getString("Ubicacion") : "");
                                item.put("distance", rs.getFloat("Distancia"));
                                item.put("service", rs.getString("ServicioNombre") != null ? rs.getString("ServicioNombre") : "");
                                
                                // Load description from extraData
                                Map<String, Object> extra = extraData.getOrDefault("resource_" + resourceId, Map.of());
                                item.put("description", extra.getOrDefault("description", ""));
                                item.put("phone", rs.getString("Telefono") != null ? rs.getString("Telefono") : "");
                                list.add(item);
                            }
                        }
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                    return;
                } else if (pathInfo != null && !pathInfo.equals("/")) {
                    int idVal = Integer.parseInt(pathInfo.substring(1));
                    Map<String, Object> item = new HashMap<>();
                    String sql = """
                        SELECT rd.IdRecurso, rd.IdRecursoTipo, rd.IdServicio, rd.Ubicacion, rd.Distancia, rd.Telefono, rt.Nombre AS RecursoNombre
                        FROM RecursoDisponible rd
                        JOIN RecursoTipo rt ON rd.IdRecursoTipo = rt.IdRecursoTipo
                        WHERE rd.IdRecurso = ?
                        """;
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, idVal);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                item.put("id", rs.getInt("IdRecurso"));
                                item.put("phone", rs.getString("Telefono") != null ? rs.getString("Telefono") : "");
                                item.put("distance", rs.getFloat("Distancia"));
                                item.put("location", rs.getString("Ubicacion") != null ? rs.getString("Ubicacion") : "");
                                item.put("resource_id", rs.getInt("IdRecursoTipo"));
                                item.put("resource_name", rs.getString("RecursoNombre"));
                                
                                Map<String, Object> extra = extraData.getOrDefault("resource_" + idVal, Map.of());
                                item.put("description", extra.getOrDefault("description", ""));
                            }
                        }
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", item)));
                    return;
                }
            }

            // HOUSING INFO (GraficoVivienda with EsEntorno = 1 for type 2, or 0 for type 1)
            if (servletPath.contains("housingInfo")) {
                int planId = extractId(pathInfo, null);
                int typeId = extractHousingTypeId(pathInfo);
                HousingInfoDTO dto = planComplementarioDAO.getHousingInfo(planId, typeId);
                if (dto != null) {
                    ResponseUtil.sendSuccess(resp, dto);
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Información de vivienda no encontrada");
                }
                return;
            }

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

                // Fetch service ID associated with the resource
                int serviceId = 1;
                String serviceSql = "SELECT s.IdServicio FROM Servicio s WHERE s.Nombre = (SELECT r.Servicio FROM Recurso r WHERE r.IdRecurso = ?)";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(serviceSql)) {
                    ps.setInt(1, resourceId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            serviceId = rs.getInt("IdServicio");
                        }
                    }
                } catch (Exception e) {
                    // Fallback to default service ID 1
                }

                String sql = "INSERT INTO RecursoDisponible (IdPlanFamiliar, IdRecursoTipo, IdServicio, Ubicacion, Distancia, Telefono) VALUES (?, ?, ?, ?, ?, ?)";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, planId);
                    ps.setInt(2, resourceId); // Using resourceId directly as IdRecursoTipo
                    ps.setInt(3, serviceId);
                    ps.setString(4, location != null ? location : "");
                    ps.setFloat(5, distance);
                    ps.setString(6, phone != null ? phone : "");
                    ps.executeUpdate();

                    int generatedId = 0;
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) generatedId = rs.getInt(1);
                    }
                    if (generatedId > 0) {
                        Map<String, Object> extra = new HashMap<>();
                        extra.put("description", description != null ? description : "");
                        extraData.put("resource_" + generatedId, extra);
                    }

                    ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, null, "Recurso disponible agregado exitosamente");
                    return;
                }
            }

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

                // Fetch service ID associated with the resource
                int serviceId = 1;
                String serviceSql = "SELECT s.IdServicio FROM Servicio s WHERE s.Nombre = (SELECT r.Servicio FROM Recurso r WHERE r.IdRecurso = ?)";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(serviceSql)) {
                    ps.setInt(1, resourceId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            serviceId = rs.getInt("IdServicio");
                        }
                    }
                } catch (Exception e) {
                    // Fallback to default service ID 1
                }

                String sql = "UPDATE RecursoDisponible SET IdRecursoTipo = ?, IdServicio = ?, Ubicacion = ?, Distancia = ?, Telefono = ? WHERE IdRecurso = ?";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, resourceId);
                    ps.setInt(2, serviceId);
                    ps.setString(3, location != null ? location : "");
                    ps.setFloat(4, distance);
                    ps.setString(5, phone != null ? phone : "");
                    ps.setInt(6, idVal);
                    ps.executeUpdate();
                }

                // Update extraData cache
                Map<String, Object> extra = extraData.computeIfAbsent("resource_" + idVal, k -> new HashMap<>());
                extra.put("description", description != null ? description : "");

                ResponseUtil.sendSuccess(resp, "Recurso disponible actualizado exitosamente");
                return;
            }

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
                String sql = "DELETE FROM RecursoDisponible WHERE IdRecurso = ?";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, idVal);
                    ps.executeUpdate();
                }
                extraData.remove("resource_" + idVal);
                ResponseUtil.sendSuccess(resp, "Recurso disponible eliminado exitosamente");
                return;
            }

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
            e.printStackTrace();
        }
    }

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

    private int extractHousingTypeId(String pathInfo) {
        if (pathInfo == null) return 2; // Default is 2 (Entorno)
        String[] segments = pathInfo.split("/");
        if (segments.length >= 4 && "type".equalsIgnoreCase(segments[2])) {
            try {
                return Integer.parseInt(segments[3]);
            } catch (NumberFormatException e) {
                // Fallback
            }
        }
        return 2;
    }

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

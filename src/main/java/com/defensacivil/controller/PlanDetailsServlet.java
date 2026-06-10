package com.defensacivil.controller;

import com.defensacivil.config.DatabaseConfig;
import com.defensacivil.config.ResponseUtil;
import com.defensacivil.dao.MascotaDAO;
import com.defensacivil.dao.MascotaDAOImpl;
import com.defensacivil.dao.PlanFamiliarDAO;
import com.defensacivil.dao.PlanFamiliarDAOImpl;
import com.defensacivil.dao.IntegranteDAO;
import com.defensacivil.dao.IntegranteDAOImpl;
import com.defensacivil.dao.VulnerabilidadDAO;
import com.defensacivil.dao.VulnerabilidadDAOImpl;
import com.defensacivil.dao.PlanComplementarioDAO;
import com.defensacivil.dao.PlanComplementarioDAOImpl;
import com.defensacivil.dto.HousingInfoDTO;
import com.defensacivil.dto.ActionPlanDTO;
import com.defensacivil.dto.ActionDTO;
import com.defensacivil.dto.VaccineDTO;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebServlet(urlPatterns = {
    "/api/familyPlans/*",
    "/api/members/*",
    "/api/conditionMembers/*",
    "/api/conditionTypes/*",
    "/api/pets/*",
    "/api/petVaccines/*",
    "/api/riskFactors/*",
    "/api/vulnerabilityFactors/*",
    "/api/riskReductionActions/*",
    "/api/vulnerabilityGrades/*",
    "/api/statusPlans/*",
    "/api/zones/*",
    "/api/cities/*",
    "/api/housingInfo/*",
    "/api/housingGraphics/*",
    "/api/actionPlans/*",
    "/api/actionPlanActions/*",
    "/api/familyMembers/*",
    "/api/kinships/*",
    "/api/bloodGroups/*",
    "/api/vulnerableTest/*",
    "/api/animalGenders/*",
    "/api/availableResources/*",
    "/api/audits/*",
    "/storage/*"
})
@MultipartConfig
public class PlanDetailsServlet extends HttpServlet {

    private final Gson gson = new Gson();

    // In-memory store for fields not mapped in the database schema (to align with legacy academic DB)
    private static final Map<String, Map<String, Object>> extraData = new ConcurrentHashMap<>();

    private final MascotaDAO mascotaDAO = new MascotaDAOImpl(extraData);
    private final PlanFamiliarDAO planFamiliarDAO = new PlanFamiliarDAOImpl(extraData);
    private final IntegranteDAO integranteDAO = new IntegranteDAOImpl(extraData);
    private final VulnerabilidadDAO vulnerabilidadDAO = new VulnerabilidadDAOImpl(extraData);
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
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        if (servletPath != null && servletPath.contains("storage")) {
            String requestedFile = pathInfo != null ? pathInfo.substring(1) : "";
            if (requestedFile.isEmpty() && servletPath.length() > 9) {
                requestedFile = servletPath.substring(9);
            }
            java.io.File file = new java.io.File("/home/dylan/Documents/projects/df/DefensaCivilAPI/storage", requestedFile);
            if (!file.exists() || file.isDirectory()) {
                // Fallback to default mock image
                file = new java.io.File("/home/dylan/Documents/projects/df/UIDefensaCivil_Modificado/public/familia.png");
            }
            if (file.exists()) {
                String mimeType = getServletContext().getMimeType(file.getName());
                if (mimeType == null) {
                    mimeType = "image/png";
                }
                resp.setContentType(mimeType);
                try (java.io.FileInputStream in = new java.io.FileInputStream(file);
                     java.io.OutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
            return;
        }

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

            if (servletPath.contains("vulnerabilityGrades")) {
                List<Map<String, Object>> list = List.of(
                    Map.of("id", 1, "name", "Bajo"),
                    Map.of("id", 2, "name", "Medio"),
                    Map.of("id", 3, "name", "Alto")
                );
                resp.getWriter().write(gson.toJson(Map.of("data", list)));
                return;
            }

            if (servletPath.contains("conditionTypes")) {
                List<Map<String, Object>> list = List.of(
                    Map.of("id", 1, "name", "Diabetes"),
                    Map.of("id", 2, "name", "Hipertensión"),
                    Map.of("id", 3, "name", "Asma"),
                    Map.of("id", 4, "name", "Alergia"),
                    Map.of("id", 5, "name", "Otra")
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

            // MEMBERS
            if (servletPath.contains("members")) {
                if (pathInfo != null && pathInfo.startsWith("/familyPlan/select/")) {
                    int familyPlanId = Integer.parseInt(pathInfo.substring(19));
                    List<Map<String, Object>> list = integranteDAO.getMembersForSelect(familyPlanId);
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                    return;

                } else if (pathInfo != null && pathInfo.startsWith("/familyPlan/")) {
                    int familyPlanId = Integer.parseInt(pathInfo.substring(12));
                    List<Map<String, Object>> list = integranteDAO.getMembersByFamilyPlan(familyPlanId);
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                    return;

                } else if (pathInfo != null && !pathInfo.equals("/")) {
                    // SINGLE MEMBER BY ID
                    int idVal = Integer.parseInt(pathInfo.substring(1));
                    Map<String, Object> member = integranteDAO.getMemberById(idVal);
                    resp.getWriter().write(gson.toJson(Map.of("data", member)));
                    return;
                }
            }

            // FAMILY MEMBERS (SUPERVISOR BULK ACCESS)
            if (servletPath.contains("familyMembers")) {
                List<Map<String, Object>> list = integranteDAO.getAllFamilyMembers();
                resp.getWriter().write(gson.toJson(Map.of("data", list)));
                return;
            }

            // CONDITION MEMBERS (Enfermedades)
            if (servletPath.contains("conditionMembers")) {
                if (pathInfo != null && pathInfo.startsWith("/member/")) {
                    int memberId = Integer.parseInt(pathInfo.substring(8));
                    List<Map<String, Object>> list = new ArrayList<>();
                    String sql = "SELECT IdEnfermedad, Nombre, Medicina, Dosis FROM Enfermedad WHERE IdIntegrante = ?";
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, memberId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                Map<String, Object> cond = new HashMap<>();
                                int condId = rs.getInt("IdEnfermedad");
                                cond.put("id", condId);
                                cond.put("name", rs.getString("Nombre"));
                                cond.put("dose", rs.getString("Dosis"));
                                cond.put("medicine", rs.getString("Medicina"));

                                Map<String, Object> extra = extraData.getOrDefault("condition_" + condId, Map.of());
                                int ctId = 1;
                                if (extra.containsKey("condition_type_id")) {
                                    Object val = extra.get("condition_type_id");
                                    if (val instanceof Number) ctId = ((Number) val).intValue();
                                    else if (val instanceof String) ctId = Integer.parseInt((String) val);
                                }
                                String ctName = switch (ctId) {
                                    case 1 -> "Diabetes";
                                    case 2 -> "Hipertensión";
                                    case 3 -> "Asma";
                                    case 4 -> "Alergia";
                                    case 5 -> "Otra";
                                    default -> "Diabetes";
                                };
                                cond.put("condition_type_id", ctId);
                                cond.put("condition_type", Map.of("id", ctId, "name", ctName));
                                list.add(cond);
                            }
                        }
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                    return;

                } else if (pathInfo != null && !pathInfo.equals("/")) {
                    int idVal = Integer.parseInt(pathInfo.substring(1));
                    String sql = "SELECT IdEnfermedad, IdIntegrante, Nombre, Medicina, Dosis FROM Enfermedad WHERE IdEnfermedad = ?";
                    Map<String, Object> cond = new HashMap<>();
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, idVal);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                cond.put("id", rs.getInt("IdEnfermedad"));
                                cond.put("name", rs.getString("Nombre"));
                                cond.put("dose", rs.getString("Dosis"));
                                cond.put("medicine", rs.getString("Medicina"));

                                Map<String, Object> extra = extraData.getOrDefault("condition_" + idVal, Map.of());
                                int ctId = 1;
                                if (extra.containsKey("condition_type_id")) {
                                    Object val = extra.get("condition_type_id");
                                    if (val instanceof Number) ctId = ((Number) val).intValue();
                                    else if (val instanceof String) ctId = Integer.parseInt((String) val);
                                }
                                String ctName = switch (ctId) {
                                    case 1 -> "Diabetes";
                                    case 2 -> "Hipertensión";
                                    case 3 -> "Asma";
                                    case 4 -> "Alergia";
                                    case 5 -> "Otra";
                                    default -> "Diabetes";
                                };
                                cond.put("condition_type_id", ctId);
                                cond.put("condition_type", Map.of("id", ctId, "name", ctName));
                                cond.put("member_id", rs.getInt("IdIntegrante"));
                            }
                        }
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", cond)));
                    return;
                }
            }

            // PETS
            if (servletPath.contains("pets")) {
                if (pathInfo != null && pathInfo.startsWith("/familyPlan/")) {
                    int planId = Integer.parseInt(pathInfo.substring(12));
                    List<Map<String, Object>> list = mascotaDAO.getPetsByFamilyPlan(planId);
                    ResponseUtil.sendSuccess(resp, list);
                    return;
                } else if (pathInfo != null && !pathInfo.equals("/")) {
                    int idVal = Integer.parseInt(pathInfo.substring(1));
                    Map<String, Object> pet = mascotaDAO.getPetById(idVal);
                    if (pet != null) {
                        ResponseUtil.sendSuccess(resp, pet);
                    } else {
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Mascota no encontrada");
                    }
                    return;
                } else {
                    List<Map<String, Object>> list = mascotaDAO.getAllPets();
                    ResponseUtil.sendSuccess(resp, list);
                    return;
                }
            }

            // PET VACCINES
            if (servletPath.contains("petVaccines")) {
                if (pathInfo != null && pathInfo.startsWith("/pet/")) {
                    int petId = extractId(pathInfo, "/pet");
                    List<VaccineDTO> list = planComplementarioDAO.getVaccinesByPet(petId);
                    ResponseUtil.sendSuccess(resp, list);
                } else {
                    int idVal = extractId(pathInfo, null);
                    VaccineDTO dto = planComplementarioDAO.getVaccineById(idVal);
                    if (dto != null) {
                        ResponseUtil.sendSuccess(resp, dto);
                    } else {
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Vacuna no encontrada");
                    }
                }
                return;
            }

            // RISK FACTORS
            if (servletPath.contains("riskFactors")) {
                if (pathInfo != null && pathInfo.startsWith("/familyPlan/select/")) {
                    int planId = Integer.parseInt(pathInfo.substring(19));
                    List<Map<String, Object>> list = vulnerabilidadDAO.getRiskFactorsForSelect(planId);
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                    return;

                } else if (pathInfo != null && pathInfo.startsWith("/familyPlan/")) {
                    int planId = Integer.parseInt(pathInfo.substring(12));
                    List<Map<String, Object>> list = vulnerabilidadDAO.getRiskFactorsByPlan(planId);
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                    return;

                } else if (pathInfo != null && !pathInfo.equals("/")) {
                    int idVal = Integer.parseInt(pathInfo.substring(1));
                    Map<String, Object> risk = vulnerabilidadDAO.getRiskFactorById(idVal);
                    resp.getWriter().write(gson.toJson(Map.of("data", risk)));
                    return;

                } else {
                    // LIST ALL RISK FACTORS
                    List<Map<String, Object>> list = vulnerabilidadDAO.getAllRiskFactors();
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                    return;
                }
            }

            // VULNERABILITY FACTORS
            if (servletPath.contains("vulnerabilityFactors")) {
                if (pathInfo != null && pathInfo.startsWith("/riskFactor/")) {
                    int riskId = Integer.parseInt(pathInfo.substring(12));
                    List<Map<String, Object>> list = vulnerabilidadDAO.getVulnerabilitiesByRiskFactor(riskId);
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                    return;

                } else if (pathInfo != null && !pathInfo.equals("/")) {
                    int idVal = Integer.parseInt(pathInfo.substring(1));
                    Map<String, Object> item = vulnerabilidadDAO.getVulnerabilityById(idVal);
                    resp.getWriter().write(gson.toJson(Map.of("data", item)));
                    return;
                }
            }

            // RISK REDUCTION ACTIONS (Mocked database store mapped to memory because DB is unmapped for these)
            if (servletPath.contains("riskReductionActions")) {
                if (pathInfo != null && pathInfo.startsWith("/riskFactor/")) {
                    int riskId = Integer.parseInt(pathInfo.substring(12));
                    List<Map<String, Object>> list = new ArrayList<>();
                    // Retrieve from memory or return mock
                    Map<String, Object> extra = extraData.getOrDefault("risk_actions_" + riskId, Map.of());
                    if (!extra.isEmpty()) {
                        list.add(extra);
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                    return;

                } else if (pathInfo != null && !pathInfo.equals("/")) {
                    int idVal = Integer.parseInt(pathInfo.substring(1));
                    Map<String, Object> extra = extraData.getOrDefault("reduction_action_" + idVal, Map.of(
                        "id", idVal,
                        "action", "Revisión estructural",
                        "member_id", 1,
                        "member", Map.of("names", "Voluntario", "last_names", "Civil"),
                        "end_date", LocalDate.now().toString()
                    ));
                    resp.getWriter().write(gson.toJson(Map.of("data", extra)));
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

            // HOUSING GRAPHICS (GraficoVivienda with EsEntorno = 0)
            if (servletPath.contains("housingGraphics")) {
                if (pathInfo != null && pathInfo.startsWith("/familyPlan/")) {
                    int planId = extractId(pathInfo, "/familyPlan");
                    List<HousingInfoDTO> list = planComplementarioDAO.getHousingGraphicsByPlan(planId);
                    ResponseUtil.sendSuccess(resp, list);
                } else {
                    int idVal = extractId(pathInfo, null);
                    HousingInfoDTO dto = planComplementarioDAO.getHousingGraphicById(idVal);
                    if (dto != null) {
                        ResponseUtil.sendSuccess(resp, dto);
                    } else {
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Gráfico no encontrado");
                    }
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

            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"Ruta GET no soportada\"}");

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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

            // POST /api/vulnerableTest
            if (servletPath.contains("vulnerableTest")) {
                Object questionIdObj = body.get("vulnerable_question_id");
                Object planIdObj = body.get("family_plan_id");
                Object answerObj = body.get("answer");

                int questionId = 0;
                if (questionIdObj instanceof Number) questionId = ((Number) questionIdObj).intValue();
                else if (questionIdObj instanceof String) questionId = Integer.parseInt((String) questionIdObj);

                int planId = 0;
                if (planIdObj instanceof Number) planId = ((Number) planIdObj).intValue();
                else if (planIdObj instanceof String) planId = Integer.parseInt((String) planIdObj);

                boolean answer = false;
                if (answerObj instanceof Boolean) answer = (Boolean) answerObj;
                else if (answerObj instanceof String) answer = Boolean.parseBoolean((String) answerObj);

                try {
                    boolean success = vulnerabilidadDAO.saveOrUpdateVulnerableTestAnswer(planId, questionId, answer);
                    if (success) {
                        resp.setStatus(HttpServletResponse.SC_CREATED);
                        resp.getWriter().write("{\"success\":true,\"message\":\"Respuesta guardada exitosamente\"}");
                    } else {
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        resp.getWriter().write("{\"success\":false,\"message\":\"No se pudo guardar la respuesta\"}");
                    }
                } catch (SQLException e) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().write("{\"success\":false,\"message\":\"Error de base de datos\"}");
                    e.printStackTrace();
                }
                return;
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

            // POST /api/members/{familyPlanId}
            if (servletPath.contains("members")) {
                int familyPlanId = Integer.parseInt(pathInfo.substring(1));
                int memberId = integranteDAO.addMember(familyPlanId, body);
                if (memberId > 0) {
                    resp.setStatus(HttpServletResponse.SC_CREATED);
                    resp.getWriter().write(String.format("{\"success\":true,\"message\":\"Integrante agregado exitosamente\",\"data\":{\"id\":%d}}", memberId));
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al agregar integrante");
                }
                return;
            }

            // POST /api/conditionMembers
            if (servletPath.contains("conditionMembers")) {
                Object memberIdObj = body.get("member_id");
                int memberId = 1;
                if (memberIdObj instanceof Number) memberId = ((Number) memberIdObj).intValue();
                else if (memberIdObj instanceof String) memberId = Integer.parseInt((String) memberIdObj);

                String name = (String) body.get("name");
                String dose = (String) body.get("dose");

                String sql = "INSERT INTO Enfermedad (IdIntegrante, Nombre, Medicina, Dosis) VALUES (?, ?, ?, ?)";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, memberId);
                    ps.setString(2, name);
                    ps.setString(3, name); // Map medicine to same
                    ps.setString(4, dose != null ? dose : "");
                    ps.executeUpdate();

                    int generatedId = 0;
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            generatedId = rs.getInt(1);
                        }
                    }

                    if (generatedId > 0) {
                        Object conditionTypeIdObj = body.get("condition_type_id");
                        int conditionTypeId = 1;
                        if (conditionTypeIdObj instanceof Number) {
                            conditionTypeId = ((Number) conditionTypeIdObj).intValue();
                        } else if (conditionTypeIdObj instanceof String) {
                            conditionTypeId = Integer.parseInt((String) conditionTypeIdObj);
                        }
                        Map<String, Object> extra = new HashMap<>();
                        extra.put("condition_type_id", conditionTypeId);
                        extraData.put("condition_" + generatedId, extra);
                    }

                    resp.setStatus(HttpServletResponse.SC_CREATED);
                    resp.getWriter().write("{\"success\":true,\"message\":\"Afeccion agregada exitosamente\"}");
                    return;
                }
            }

            // POST /api/pets
            if (servletPath.contains("pets")) {
                int generatedId = mascotaDAO.insertPet(body);
                if (generatedId > 0) {
                    ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, Map.of("id", generatedId), "Mascota agregada exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al agregar mascota");
                }
                return;
            }

            // POST /api/petVaccines
            if (servletPath.contains("petVaccines")) {
                VaccineDTO dto = gson.fromJson(gson.toJson(body), VaccineDTO.class);
                if (planComplementarioDAO.insertVaccine(dto) > 0) {
                    ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, null, "Vacuna agregada exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al agregar vacuna");
                }
                return;
            }

            // POST /api/riskFactors
            if (servletPath.contains("riskFactors")) {
                try {
                    int generatedId = vulnerabilidadDAO.addRiskFactor(body);
                    if (generatedId > 0) {
                        resp.setStatus(HttpServletResponse.SC_CREATED);
                        resp.getWriter().write("{\"success\":true,\"message\":\"Factor de riesgo agregado exitosamente\"}");
                    } else {
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al agregar factor de riesgo");
                    }
                } catch (SQLException e) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().write("{\"success\":false,\"message\":\"Error de base de datos\"}");
                    e.printStackTrace();
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

                    resp.setStatus(HttpServletResponse.SC_CREATED);
                    resp.getWriter().write("{\"success\":true,\"message\":\"Recurso disponible agregado exitosamente\"}");
                    return;
                }
            }

            // POST /api/vulnerabilityFactors
            if (servletPath.contains("vulnerabilityFactors")) {
                try {
                    boolean success = vulnerabilidadDAO.addVulnerability(body);
                    if (success) {
                        resp.setStatus(HttpServletResponse.SC_CREATED);
                        resp.getWriter().write("{\"success\":true,\"message\":\"Vulnerabilidad agregada exitosamente\"}");
                    } else {
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al agregar vulnerabilidad");
                    }
                } catch (SQLException e) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().write("{\"success\":false,\"message\":\"Error de base de datos\"}");
                    e.printStackTrace();
                }
                return;
            }

            // POST /api/riskReductionActions (Save in memory store)
            if (servletPath.contains("riskReductionActions")) {
                Object riskIdObj = body.get("risk_factor_id");
                int riskId = 1;
                if (riskIdObj instanceof Number) riskId = ((Number) riskIdObj).intValue();
                else if (riskIdObj instanceof String && !((String) riskIdObj).isEmpty()) riskId = Integer.parseInt((String) riskIdObj);

                int randId = (int) (Math.random() * 1000) + 1;
                Map<String, Object> extra = new HashMap<>();
                extra.put("id", randId);
                extra.put("action", body.get("action"));
                extra.put("member_id", body.get("member_id"));
                extra.put("member", Map.of("names", "Encargado", "last_names", ""));
                extra.put("end_date", body.get("end_date"));

                extraData.put("risk_actions_" + riskId, extra);
                extraData.put("reduction_action_" + randId, extra);

                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("{\"success\":true,\"message\":\"Accion de reduccion agregada exitosamente\"}");
                return;
            }

            // POST /api/housingInfo and housingGraphics (Multipart upload with real file persistence)
            if (servletPath.contains("housingInfo") || servletPath.contains("housingGraphics")) {
                boolean esEntorno = servletPath.contains("housingInfo");
                int planId = extractId(body.get("family_plan_id"));
                int typeId = esEntorno ? 2 : 1;
                if (body.containsKey("housing_info_type_id")) {
                    typeId = extractId(body.get("housing_info_type_id"));
                }
                if (pathInfo != null) {
                    typeId = extractHousingTypeId(pathInfo);
                }
                int esEntornoVal = (typeId == 2) ? 1 : 0;
                String description = body.containsKey("description") ? (String) body.get("description") : "Grafico del plan";

                String savedFileName = saveUploadedFile(req);
                if (planComplementarioDAO.saveOrUpdateHousingGraphic(planId, savedFileName, description, esEntornoVal)) {
                    ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, null, "Archivo subido exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al guardar el croquis/gráfico");
                }
                return;
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

            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"Ruta POST no soportada\"}");

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            e.printStackTrace();
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        try {
            BufferedReader reader = req.getReader();
            Map<String, Object> body = gson.fromJson(reader, Map.class);
            if (body == null) body = new HashMap<>();

            int idVal = Integer.parseInt(pathInfo.substring(1));

            // PUT /api/members/{id}
            if (servletPath.contains("members")) {
                boolean updated = integranteDAO.updateMember(idVal, body);
                if (updated) {
                    resp.getWriter().write("{\"success\":true,\"message\":\"Integrante actualizado exitosamente\"}");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Integrante no encontrado");
                }
                return;
            }

            // PUT /api/conditionMembers/{id}
            if (servletPath.contains("conditionMembers")) {
                String name = (String) body.get("name");
                String dose = (String) body.get("dose");

                String sql = "UPDATE Enfermedad SET Nombre = ?, Medicina = ?, Dosis = ? WHERE IdEnfermedad = ?";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, name);
                    ps.setString(2, name);
                    ps.setString(3, dose != null ? dose : "");
                    ps.setInt(4, idVal);
                    ps.executeUpdate();

                    Object conditionTypeIdObj = body.get("condition_type_id");
                    int conditionTypeId = 1;
                    if (conditionTypeIdObj instanceof Number) {
                        conditionTypeId = ((Number) conditionTypeIdObj).intValue();
                    } else if (conditionTypeIdObj instanceof String) {
                        conditionTypeId = Integer.parseInt((String) conditionTypeIdObj);
                    }
                    Map<String, Object> extra = extraData.computeIfAbsent("condition_" + idVal, k -> new HashMap<>());
                    extra.put("condition_type_id", conditionTypeId);

                    resp.getWriter().write("{\"success\":true,\"message\":\"Afeccion actualizada exitosamente\"}");
                    return;
                }
            }

            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"Ruta PUT no soportada\"}");

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            e.printStackTrace();
        }
    }

    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        try {
                BufferedReader reader = req.getReader();
                Map<String, Object> body = gson.fromJson(reader, Map.class);
                if (body == null) body = new HashMap<>();

                // PATCH /api/familyPlans/{id}/identify
                if (servletPath.contains("familyPlans") && pathInfo.endsWith("/identify")) {
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
                if (servletPath.contains("familyPlans") && pathInfo.endsWith("/change-status")) {
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
                if (servletPath.contains("familyPlans") && pathInfo.endsWith("/change-family-type")) {
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
                if (servletPath.contains("familyPlans") && pathInfo.startsWith("/status/")) {
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

                // PATCH /api/pets/{id}
                if (servletPath.contains("pets")) {
                int petId = Integer.parseInt(pathInfo.substring(1));
                boolean updated = mascotaDAO.updatePet(petId, body);
                if (updated) {
                    ResponseUtil.sendSuccess(resp, "Mascota actualizada exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Mascota no encontrada");
                }
                return;
            }

            // PATCH /api/petVaccines/{id}
            if (servletPath.contains("petVaccines")) {
                int vaccineId = extractId(pathInfo, null);
                VaccineDTO dto = gson.fromJson(gson.toJson(body), VaccineDTO.class);
                if (planComplementarioDAO.updateVaccine(vaccineId, dto)) {
                    ResponseUtil.sendSuccess(resp, "Vacuna actualizada exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Vacuna no encontrada");
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

                resp.getWriter().write("{\"success\":true,\"message\":\"Recurso disponible actualizado exitosamente\"}");
                return;
            }

            // PATCH /api/riskFactors/{id}
            if (servletPath.contains("riskFactors")) {
                int idVal = Integer.parseInt(pathInfo.substring(1));
                try {
                    boolean success = vulnerabilidadDAO.updateRiskFactor(idVal, body);
                    if (success) {
                        resp.getWriter().write("{\"success\":true,\"message\":\"Factor de riesgo actualizado exitosamente\"}");
                    } else {
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Factor de riesgo no encontrado");
                    }
                } catch (SQLException e) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().write("{\"success\":false,\"message\":\"Error de base de datos\"}");
                    e.printStackTrace();
                }
                return;
            }

            // PATCH /api/vulnerabilityFactors/{id}
            if (servletPath.contains("vulnerabilityFactors")) {
                int idVal = Integer.parseInt(pathInfo.substring(1));
                try {
                    boolean success = vulnerabilidadDAO.updateVulnerability(idVal, body);
                    if (success) {
                        resp.getWriter().write("{\"success\":true,\"message\":\"Vulnerabilidad actualizada exitosamente\"}");
                    } else {
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Vulnerabilidad no encontrada");
                    }
                } catch (SQLException e) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().write("{\"success\":false,\"message\":\"Error de base de datos\"}");
                    e.printStackTrace();
                }
                return;
            }

            // PATCH /api/riskReductionActions/{id}
            if (servletPath.contains("riskReductionActions")) {
                int idVal = Integer.parseInt(pathInfo.substring(1));
                Map<String, Object> extra = extraData.getOrDefault("reduction_action_" + idVal, new HashMap<>());
                extra.put("action", body.get("action"));
                extra.put("member_id", body.get("member_id"));
                extra.put("end_date", body.get("end_date"));
                extraData.put("reduction_action_" + idVal, extra);

                resp.getWriter().write("{\"success\":true,\"message\":\"Accion de reduccion actualizada exitosamente\"}");
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

            // PATCH /api/housingGraphics/{id}/description
            if (servletPath.contains("housingGraphics") && pathInfo != null && pathInfo.endsWith("/description")) {
                int graficoId = extractId(pathInfo, null);
                String description = (String) body.get("description");
                if (planComplementarioDAO.updateHousingGraphicDescription(graficoId, description)) {
                    ResponseUtil.sendSuccess(resp, "Descripción de gráfico actualizada exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Gráfico no encontrado");
                }
                return;
            }

            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"Ruta PATCH no soportada\"}");

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            e.printStackTrace();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        try {
            int idVal = Integer.parseInt(pathInfo.substring(1));

            // DELETE /api/members/{id}
            if (servletPath.contains("members")) {
                boolean deleted = integranteDAO.deleteMember(idVal);
                if (deleted) {
                    resp.getWriter().write("{\"success\":true,\"message\":\"Integrante eliminado exitosamente\"}");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Integrante no encontrado");
                }
                return;
            }

            // DELETE /api/conditionMembers/{id}
            if (servletPath.contains("conditionMembers")) {
                String sql = "DELETE FROM Enfermedad WHERE IdEnfermedad = ?";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, idVal);
                    ps.executeUpdate();
                }
                extraData.remove("condition_" + idVal);
                resp.getWriter().write("{\"success\":true,\"message\":\"Afeccion eliminada exitosamente\"}");
                return;
            }

            // DELETE /api/pets/{id}
            if (servletPath.contains("pets")) {
                boolean deleted = mascotaDAO.deletePet(idVal);
                if (deleted) {
                    ResponseUtil.sendSuccess(resp, "Mascota eliminada exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Mascota no encontrada");
                }
                return;
            }

            // DELETE /api/petVaccines/{id}
            if (servletPath.contains("petVaccines")) {
                if (planComplementarioDAO.deleteVaccine(idVal)) {
                    ResponseUtil.sendSuccess(resp, "Vacuna eliminada exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Vacuna no encontrada");
                }
                return;
            }

            // DELETE /api/riskFactors/{id}
            if (servletPath.contains("riskFactors")) {
                try {
                    boolean success = vulnerabilidadDAO.deleteRiskFactor(idVal);
                    if (success) {
                        resp.getWriter().write("{\"success\":true,\"message\":\"Factor de riesgo eliminado exitosamente\"}");
                    } else {
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Factor de riesgo no encontrado");
                    }
                } catch (SQLException e) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().write("{\"success\":false,\"message\":\"Error de base de datos: " + e.getMessage() + "\"}");
                    e.printStackTrace();
                }
                return;
            }

            // DELETE /api/availableResources/{id}
            if (servletPath.contains("availableResources")) {
                String sql = "DELETE FROM RecursoDisponible WHERE IdRecurso = ?";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, idVal);
                    ps.executeUpdate();
                }
                extraData.remove("resource_" + idVal);
                resp.getWriter().write("{\"success\":true,\"message\":\"Recurso disponible eliminado exitosamente\"}");
                return;
            }

            // DELETE /api/vulnerabilityFactors/{id}
            if (servletPath.contains("vulnerabilityFactors")) {
                try {
                    boolean success = vulnerabilidadDAO.deleteVulnerability(idVal);
                    if (success) {
                        resp.getWriter().write("{\"success\":true,\"message\":\"Vulnerabilidad eliminada exitosamente\"}");
                    } else {
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Vulnerabilidad no encontrada");
                    }
                } catch (SQLException e) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().write("{\"success\":false,\"message\":\"Error de base de datos: " + e.getMessage() + "\"}");
                    e.printStackTrace();
                }
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

            // DELETE /api/housingGraphics/{id}
            if (servletPath.contains("housingGraphics")) {
                HousingInfoDTO dto = planComplementarioDAO.getHousingGraphicById(idVal);
                if (dto != null && planComplementarioDAO.deleteHousingGraphic(idVal)) {
                    String fileName = dto.getPath();
                    if (fileName != null && !fileName.equals("mock_graphic.png")) {
                        java.io.File file = new java.io.File("/home/dylan/Documents/projects/df/DefensaCivilAPI/storage", fileName);
                        if (file.exists() && file.isFile()) {
                            file.delete();
                        }
                    }
                    ResponseUtil.sendSuccess(resp, "Gráfico de vivienda eliminado exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Gráfico no encontrado");
                }
                return;
            }

            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"Ruta DELETE no soportada\"}");

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            e.printStackTrace();
        }
    }

    private String getStatusName(int statusId) {
        return switch (statusId) {
            case 1 -> "Creado";
            case 3 -> "En proceso";
            case 4 -> "En Revision";
            case 5 -> "Devuelto con observaciones";
            case 6 -> "Rechazado";
            case 7 -> "Completado";
            default -> "Creado";
        };
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

    private String saveUploadedFile(HttpServletRequest req) {
        String savedFileName = "mock_graphic.png";
        try {
            jakarta.servlet.http.Part filePart = req.getPart("path");
            if (filePart != null) {
                String fileName = filePart.getSubmittedFileName();
                if (fileName != null && !fileName.isEmpty()) {
                    savedFileName = "upload_" + System.currentTimeMillis() + "_" + fileName;
                    String storageDirPath = "/home/dylan/Documents/projects/df/DefensaCivilAPI/storage";
                    java.io.File storageDir = new java.io.File(storageDirPath);
                    if (!storageDir.exists()) {
                        storageDir.mkdirs();
                    }
                    java.io.File fileToSave = new java.io.File(storageDir, savedFileName);
                    try (java.io.InputStream input = filePart.getInputStream();
                         java.io.OutputStream output = new java.io.FileOutputStream(fileToSave)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = input.read(buffer)) != -1) {
                            output.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Fallback to default
        }
        return savedFileName;
    }
}

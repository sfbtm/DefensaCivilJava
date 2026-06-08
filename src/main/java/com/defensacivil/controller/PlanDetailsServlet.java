package com.defensacivil.controller;

import com.defensacivil.config.DatabaseConfig;
import com.defensacivil.config.ResponseUtil;
import com.defensacivil.dao.MascotaDAO;
import com.defensacivil.dao.MascotaDAOImpl;
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
                    int pendingCount = 0;
                    int approvedCount = 0;
                    int rejectedCount = 0;
                    int inRevisionCount = 0;

                    List<Map<String, Object>> latestPlans = new ArrayList<>();
                    try (Connection conn = DatabaseConfig.getConnection()) {
                        // pending_plans: status 1 or 3
                        String sqlPending = "SELECT COUNT(*) FROM PlanFamiliar WHERE Estado IN ('1', '3')";
                        try (PreparedStatement ps = conn.prepareStatement(sqlPending);
                             ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) pendingCount = rs.getInt(1);
                        }

                        // in_revision_plans: status 4
                        String sqlRevision = "SELECT COUNT(*) FROM PlanFamiliar WHERE Estado = '4'";
                        try (PreparedStatement ps = conn.prepareStatement(sqlRevision);
                             ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) inRevisionCount = rs.getInt(1);
                        }

                        // approved_plans: status 7
                        String sqlApproved = "SELECT COUNT(*) FROM PlanFamiliar WHERE Estado = '7'";
                        try (PreparedStatement ps = conn.prepareStatement(sqlApproved);
                             ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) approvedCount = rs.getInt(1);
                        }

                        // rejected_plans: status 5 or 6
                        String sqlRejected = "SELECT COUNT(*) FROM PlanFamiliar WHERE Estado IN ('5', '6')";
                        try (PreparedStatement ps = conn.prepareStatement(sqlRejected);
                             ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) rejectedCount = rs.getInt(1);
                        }

                        // latest_plans: 5 latest plans
                        String sqlLatest = """
                            SELECT p.IdPlanFamiliar, f.Nombre AS FamiliaNombre, p.Estado, p.Fecha, 
                                   u.Nombre AS VoluntarioNombre, s.Nombre AS SeccionalNombre
                            FROM PlanFamiliar p
                            JOIN Familia f ON p.IdFamilia = f.IdFamilia
                            LEFT JOIN Usuario u ON p.IdUsuario = u.IdUsuario
                            LEFT JOIN Organizacion org ON u.IdOrganizacion = org.IdOrganizacion
                            LEFT JOIN Seccional s ON org.IdSeccional = s.IdSeccional
                            ORDER BY p.IdPlanFamiliar DESC
                            LIMIT 5
                            """;
                        try (PreparedStatement ps = conn.prepareStatement(sqlLatest);
                             ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                Map<String, Object> item = new HashMap<>();
                                int idVal = rs.getInt("IdPlanFamiliar");
                                String estadoStr = rs.getString("Estado");
                                int statusId = 1;
                                try { statusId = Integer.parseInt(estadoStr); } catch (Exception ignored) {}

                                item.put("id", idVal);
                                item.put("family_name", rs.getString("FamiliaNombre"));
                                item.put("status_id", statusId);
                                item.put("status", getStatusName(statusId));
                                item.put("date", rs.getDate("Fecha") != null ? rs.getDate("Fecha").toString() : "");
                                item.put("volunteer", rs.getString("VoluntarioNombre") != null ? rs.getString("VoluntarioNombre") : "Voluntario Civil");
                                item.put("sectional", rs.getString("SeccionalNombre") != null ? rs.getString("SeccionalNombre") : "Antioquia");
                                latestPlans.add(item);
                            }
                        }
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("pending_plans", pendingCount);
                    data.put("in_revision_plans", inRevisionCount);
                    data.put("approved_plans", approvedCount);
                    data.put("rejected_plans", rejectedCount);
                    data.put("time_validation", 120);
                    data.put("latest_plans", latestPlans);

                    resp.getWriter().write(gson.toJson(Map.of("data", data)));
                    return;
                } else if (pathInfo != null && pathInfo.contains("dashBoardAdmin")) {
                    int activeCount = 0;
                    int inactiveCount = 0;
                    int requestCount = 0;
                    int volunteerCount = 0;
                    int supervisorCount = 0;

                    try (Connection conn = DatabaseConfig.getConnection()) {
                        // Summary active: Activo = 1
                        String sqlActive = "SELECT COUNT(*) FROM Usuario WHERE Activo = 1";
                        try (PreparedStatement ps = conn.prepareStatement(sqlActive);
                             ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) activeCount = rs.getInt(1);
                        }

                        // Summary inactive: Activo = 2 or Activo = 0
                        String sqlInactive = "SELECT COUNT(*) FROM Usuario WHERE Activo = 2 OR Activo = 0";
                        try (PreparedStatement ps = conn.prepareStatement(sqlInactive);
                             ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) inactiveCount = rs.getInt(1);
                        }

                        // Summary request: Activo = 3
                        String sqlRequest = "SELECT COUNT(*) FROM Usuario WHERE Activo = 3";
                        try (PreparedStatement ps = conn.prepareStatement(sqlRequest);
                             ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) requestCount = rs.getInt(1);
                        }

                        // Roles volunteer: IdRol = 3
                        String sqlVolunteer = "SELECT COUNT(*) FROM Usuario WHERE IdRol = 3";
                        try (PreparedStatement ps = conn.prepareStatement(sqlVolunteer);
                             ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) volunteerCount = rs.getInt(1);
                        }

                        // Roles supervisor: IdRol = 2
                        String sqlSupervisor = "SELECT COUNT(*) FROM Usuario WHERE IdRol = 2";
                        try (PreparedStatement ps = conn.prepareStatement(sqlSupervisor);
                             ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) supervisorCount = rs.getInt(1);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    Map<String, Object> summary = new HashMap<>();
                    summary.put("active", activeCount);
                    summary.put("inactive", inactiveCount);
                    summary.put("request", requestCount);

                    Map<String, Object> rols = new HashMap<>();
                    rols.put("volunteer", volunteerCount);
                    rols.put("supervisor", supervisorCount);

                    List<Map<String, Object>> historyGeneral = List.of(
                        Map.of(
                            "name_model", "Seccional Antioquia",
                            "action_execute", "Creación",
                            "user_name", "Administrador Sistema",
                            "rol", "Administrador",
                            "date_time", "2026-06-01 10:15:30",
                            "status_old", "Activo",
                            "status_new", "Activo"
                        ),
                        Map.of(
                            "name_model", "Organización Cruz Roja 1",
                            "action_execute", "Actualización",
                            "user_name", "Administrador Sistema",
                            "rol", "Administrador",
                            "date_time", "2026-06-02 09:30:15",
                            "status_old", "Inactivo",
                            "status_new", "Activo"
                        )
                    );

                    List<Map<String, Object>> historyMembers = List.of(
                        Map.of(
                            "name_model", "Juan Perez",
                            "action_execute", "Aprobación",
                            "user_name", "Administrador Sistema",
                            "rol", "Administrador",
                            "date_time", "2026-06-03 14:05:10",
                            "status_old", "Peticion",
                            "status_new", "Activo"
                        ),
                        Map.of(
                            "name_model", "Voluntario Civil",
                            "action_execute", "Creación",
                            "user_name", "Administrador Sistema",
                            "rol", "Administrador",
                            "date_time", "2026-06-02 11:20:00",
                            "status_old", "Peticion",
                            "status_new", "Activo"
                        )
                    );

                    List<Map<String, Object>> monthlyChanges = List.of(
                        Map.of("month", "Enero", "total", 15),
                        Map.of("month", "Febrero", "total", 20),
                        Map.of("month", "Marzo", "total", 8),
                        Map.of("month", "Abril", "total", 12),
                        Map.of("month", "Mayo", "total", 30),
                        Map.of("month", "Junio", "total", 25)
                    );

                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("summary", summary);
                    responseData.put("rols", rols);
                    responseData.put("history_general", historyGeneral);
                    responseData.put("history_members", historyMembers);
                    responseData.put("monthly_changes", monthlyChanges);

                    resp.getWriter().write(gson.toJson(Map.of("data", responseData)));
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
                    // LIST ALL PLANS
                    List<Map<String, Object>> list = new ArrayList<>();
                    String sql;
                    if (UserServlet.loggedInRoleId == 3) { // Volunteer
                        sql = """
                            SELECT p.IdPlanFamiliar, f.Nombre AS FamiliaNombre, p.Estado, p.Fecha, u.Nombre AS VoluntarioNombre
                            FROM PlanFamiliar p
                            JOIN Familia f ON p.IdFamilia = f.IdFamilia
                            JOIN Usuario u ON p.IdUsuario = u.IdUsuario
                            WHERE p.IdUsuario = ?
                            ORDER BY p.IdPlanFamiliar DESC
                            """;
                    } else if (UserServlet.loggedInRoleId == 2) { // Supervisor
                        sql = """
                            SELECT p.IdPlanFamiliar, f.Nombre AS FamiliaNombre, p.Estado, p.Fecha, u.Nombre AS VoluntarioNombre
                            FROM PlanFamiliar p
                            JOIN Familia f ON p.IdFamilia = f.IdFamilia
                            JOIN Usuario u ON p.IdUsuario = u.IdUsuario
                            LEFT JOIN Organizacion org ON u.IdOrganizacion = org.IdOrganizacion
                            WHERE org.IdSeccional = ?
                            ORDER BY p.IdPlanFamiliar DESC
                            """;
                    } else { // Admin / default
                        sql = """
                            SELECT p.IdPlanFamiliar, f.Nombre AS FamiliaNombre, p.Estado, p.Fecha, u.Nombre AS VoluntarioNombre
                            FROM PlanFamiliar p
                            JOIN Familia f ON p.IdFamilia = f.IdFamilia
                            JOIN Usuario u ON p.IdUsuario = u.IdUsuario
                            ORDER BY p.IdPlanFamiliar DESC
                            """;
                    }

                    try (Connection conn = DatabaseConfig.getConnection();
                          PreparedStatement ps = conn.prepareStatement(sql)) {
                        if (UserServlet.loggedInRoleId == 3) {
                            ps.setInt(1, UserServlet.loggedInUserId);
                        } else if (UserServlet.loggedInRoleId == 2) {
                            ps.setInt(1, UserServlet.loggedInSectionalId);
                        }
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                int idVal = rs.getInt("IdPlanFamiliar");
                                String estadoStr = rs.getString("Estado");
                                int statusId = 1;
                                try { statusId = Integer.parseInt(estadoStr); } catch (Exception ignored) {}
                                String statusText = getStatusName(statusId);

                                Map<String, Object> item = new HashMap<>();
                                item.put("id", idVal);
                                item.put("last_names", rs.getString("FamiliaNombre"));
                                item.put("status", statusText);
                                item.put("status_id", statusId);
                                item.put("responsable", rs.getString("VoluntarioNombre"));
                                Map<String, Object> extra = extraData.getOrDefault("plan_" + idVal, Map.of());
                                int familyTypeId = 3;
                                Object ftIdObj = extra.get("family_type_id");
                                if (ftIdObj instanceof Number) familyTypeId = ((Number) ftIdObj).intValue();
                                else if (ftIdObj instanceof String) familyTypeId = Integer.parseInt((String) ftIdObj);
                                
                                String familyType = "Por Definir";
                                if (familyTypeId == 1) familyType = "Vulnerable";
                                else if (familyTypeId == 2) familyType = "No Vulnerable";

                                item.put("family_type", familyType);
                                item.put("family_type_id", familyTypeId);
                                item.put("department", "Antioquia");
                                item.put("city", "Medellín");
                                item.put("date_create", rs.getDate("Fecha") != null ? rs.getDate("Fecha").toString() : "");
                                list.add(item);
                            }
                        }
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                    return;

                } else if (pathInfo.startsWith("/check-access/")) {
                    resp.getWriter().write("{\"success\":true,\"data\":{\"has_access\":true,\"access_check\":true}}");
                    return;

                } else if (pathInfo.startsWith("/has-members/")) {
                    int idVal = Integer.parseInt(pathInfo.substring(13));
                    int count = 0;
                    String sql = "SELECT COUNT(*) FROM Integrante WHERE IdPlanFamiliar = ?";
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, idVal);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) count = rs.getInt(1);
                        }
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", Map.of("has_members", count > 0))));
                    return;

                } else if (pathInfo.startsWith("/validate-requirements/")) {
                    int idVal = Integer.parseInt(pathInfo.substring(23));
                    int memberCount = 0;
                    int riskCount = 0;
                    int resourceCount = 0;

                    try (Connection conn = DatabaseConfig.getConnection()) {
                        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM Integrante WHERE IdPlanFamiliar = ?")) {
                            ps.setInt(1, idVal);
                            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) memberCount = rs.getInt(1); }
                        }
                        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM FactorRiesgo WHERE IdPlanFamiliar = ?")) {
                            ps.setInt(1, idVal);
                            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) riskCount = rs.getInt(1); }
                        }
                        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM RecursoDisponible WHERE IdPlanFamiliar = ?")) {
                            ps.setInt(1, idVal);
                            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) resourceCount = rs.getInt(1); }
                        }
                    }

                    Map<String, Object> valMap = new HashMap<>();
                    valMap.put("is_valid", memberCount >= 2 && riskCount >= 1 && resourceCount >= 1);
                    valMap.put("has_min_members", memberCount >= 2);
                    valMap.put("has_risk_factors", riskCount >= 1);
                    valMap.put("has_resources", resourceCount >= 1);
                    valMap.put("has_photos", true); // Pass mock
                    valMap.put("has_graphics", true); // Pass mock
                    valMap.put("has_action_before", true);
                    valMap.put("has_action_during", true);
                    valMap.put("has_action_after", true);

                    resp.getWriter().write(gson.toJson(Map.of("data", valMap)));
                    return;

                } else {
                    // GET SINGLE PLAN BY ID
                    int idVal = Integer.parseInt(pathInfo.substring(1));
                    String sql = """
                        SELECT p.IdPlanFamiliar, f.Nombre AS FamiliaNombre, f.Telefono, f.Sector, f.CalidadVivienda, p.Estado
                        FROM PlanFamiliar p
                        JOIN Familia f ON p.IdFamilia = f.IdFamilia
                        WHERE p.IdPlanFamiliar = ?
                        """;
                    Map<String, Object> item = new HashMap<>();
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, idVal);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                int statusId = 1;
                                try { statusId = Integer.parseInt(rs.getString("Estado")); } catch (Exception ignored) {}
                                
                                // Read unmapped fields from memory
                                Map<String, Object> extra = extraData.getOrDefault("plan_" + idVal, Map.of());
                                int familyTypeId = 3;
                                Object ftIdObj = extra.get("family_type_id");
                                if (ftIdObj instanceof Number) familyTypeId = ((Number) ftIdObj).intValue();
                                else if (ftIdObj instanceof String) familyTypeId = Integer.parseInt((String) ftIdObj);
                                
                                String familyType = "Por Definir";
                                if (familyTypeId == 1) familyType = "Vulnerable";
                                else if (familyTypeId == 2) familyType = "No Vulnerable";

                                item.put("id", rs.getInt("IdPlanFamiliar"));
                                item.put("last_names", rs.getString("FamiliaNombre"));
                                item.put("family_type", familyType);
                                item.put("family_type_id", familyTypeId);
                                item.put("landline_phone", rs.getString("Telefono") != null ? rs.getString("Telefono") : "");
                                item.put("status", getStatusName(statusId));
                                item.put("status_plan_id", statusId);
                                item.put("status_id", statusId);

                                item.put("zone_id", extra.getOrDefault("zone_id", 1));
                                item.put("department_id", extra.getOrDefault("department_id", 1));
                                item.put("city_id", extra.getOrDefault("city_id", 1));
                                item.put("address", extra.getOrDefault("address", ""));
                                item.put("sector_id", extra.getOrDefault("sector_id", 1));
                                item.put("sector_name", extra.getOrDefault("sector_name", ""));
                                item.put("housing_quality_id", extra.getOrDefault("housing_quality_id", 1));
                                item.put("housing_quality", "Propia");
                                item.put("city", "Medellín");
                                item.put("department", "Antioquia");
                                item.put("created_at", LocalDate.now().toString());

                                // Read comment from ValidacionPlan
                                String commentSql = "SELECT Comentario FROM ValidacionPlan WHERE IdPlanFamiliar = ? ORDER BY IdValidacion DESC LIMIT 1";
                                try (PreparedStatement psC = conn.prepareStatement(commentSql)) {
                                    psC.setInt(1, idVal);
                                    try (ResultSet rsC = psC.executeQuery()) {
                                        if (rsC.next()) item.put("comentary", rsC.getString("Comentario"));
                                    }
                                }
                            }
                        }
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", item)));
                    return;
                }
            }

            // MEMBERS
            if (servletPath.contains("members")) {
                if (pathInfo != null && pathInfo.startsWith("/familyPlan/select/")) {
                    int familyPlanId = Integer.parseInt(pathInfo.substring(19));
                    List<Map<String, Object>> list = new ArrayList<>();
                    String sql = "SELECT IdIntegrante, Nombre, Apellido FROM Integrante WHERE IdPlanFamiliar = ?";
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, familyPlanId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                Map<String, Object> member = new HashMap<>();
                                member.put("id", rs.getInt("IdIntegrante"));
                                member.put("full_name", rs.getString("Nombre") + " " + rs.getString("Apellido"));
                                list.add(member);
                            }
                        }
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                    return;

                } else if (pathInfo != null && pathInfo.startsWith("/familyPlan/")) {
                    int familyPlanId = Integer.parseInt(pathInfo.substring(12));
                    List<Map<String, Object>> list = new ArrayList<>();
                    String sql = """
                        SELECT i.IdIntegrante, i.Nombre, i.Apellido, i.Parentesco, i.Telefono, i.IdGenero, i.IdDocumentoTipo, i.IdNacionalidad,
                               g.Nombre AS GeneroNombre, dt.Nombre AS DocumentoNombre, n.Nombre AS NacionalidadNombre
                        FROM Integrante i
                        LEFT JOIN Genero g ON i.IdGenero = g.IdGenero
                        LEFT JOIN DocumentoTipo dt ON i.IdDocumentoTipo = dt.IdDocumentoTipo
                        LEFT JOIN Nacionalidad n ON i.IdNacionalidad = n.IdNacionalidad
                        WHERE i.IdPlanFamiliar = ?
                        """;
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, familyPlanId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                int memberId = rs.getInt("IdIntegrante");
                                Map<String, Object> member = new HashMap<>();
                                member.put("id", memberId);
                                member.put("names", rs.getString("Nombre"));
                                member.put("last_names", rs.getString("Apellido"));
                                member.put("full_name", rs.getString("Nombre") + " " + rs.getString("Apellido"));
                                member.put("relationship", rs.getString("Parentesco"));
                                member.put("phone", rs.getString("Telefono"));
                                member.put("gender", Map.of("name", rs.getString("GeneroNombre") != null ? rs.getString("GeneroNombre") : ""));
                                member.put("document_type", Map.of("acronym", rs.getString("DocumentoNombre") != null ? rs.getString("DocumentoNombre") : ""));
                                member.put("nationality", Map.of("name", rs.getString("NacionalidadNombre") != null ? rs.getString("NacionalidadNombre") : ""));
                                
                                Map<String, Object> extra = extraData.getOrDefault("member_" + memberId, Map.of());
                                member.put("blood_group", extra.getOrDefault("blood_group", "O+"));
                                member.put("eps", extra.getOrDefault("eps", "Compensar"));
                                member.put("birth_date", extra.getOrDefault("birth_date", "1990-01-01"));
                                member.put("document_number", extra.getOrDefault("document_number", "1000000" + memberId));
                                member.put("kinship", rs.getString("Parentesco"));
                                list.add(member);
                            }
                        }
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                    return;

                } else if (pathInfo != null && !pathInfo.equals("/")) {
                    // SINGLE MEMBER BY ID
                    int idVal = Integer.parseInt(pathInfo.substring(1));
                    String sql = """
                        SELECT i.IdIntegrante, i.Nombre, i.Apellido, i.Parentesco, i.Telefono, i.IdGenero, i.IdDocumentoTipo, i.IdNacionalidad,
                               g.Nombre AS GeneroNombre, dt.Nombre AS DocumentoNombre, n.Nombre AS NacionalidadNombre
                        FROM Integrante i
                        LEFT JOIN Genero g ON i.IdGenero = g.IdGenero
                        LEFT JOIN DocumentoTipo dt ON i.IdDocumentoTipo = dt.IdDocumentoTipo
                        LEFT JOIN Nacionalidad n ON i.IdNacionalidad = n.IdNacionalidad
                        WHERE i.IdIntegrante = ?
                        """;
                    Map<String, Object> member = new HashMap<>();
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, idVal);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                member.put("id", rs.getInt("IdIntegrante"));
                                member.put("names", rs.getString("Nombre"));
                                member.put("last_names", rs.getString("Apellido"));
                                member.put("relationship", rs.getString("Parentesco"));
                                member.put("phone", rs.getString("Telefono"));
                                member.put("gender_id", rs.getInt("IdGenero"));
                                member.put("document_type_id", rs.getInt("IdDocumentoTipo"));
                                member.put("nationality_id", rs.getInt("IdNacionalidad"));
                                member.put("gender", Map.of("name", rs.getString("GeneroNombre") != null ? rs.getString("GeneroNombre") : ""));
                                member.put("document_type", Map.of("acronym", rs.getString("DocumentoNombre") != null ? rs.getString("DocumentoNombre") : ""));
                                member.put("nationality", Map.of("name", rs.getString("NacionalidadNombre") != null ? rs.getString("NacionalidadNombre") : ""));
                                member.put("kinship", Map.of("name", rs.getString("Parentesco") != null ? rs.getString("Parentesco") : ""));
                                
                                Map<String, Object> extra = extraData.getOrDefault("member_" + idVal, Map.of());
                                int bloodGroupId = 1;
                                Object bgIdObj = extra.get("blood_group_id");
                                if (bgIdObj instanceof Number) bloodGroupId = ((Number) bgIdObj).intValue();
                                else if (bgIdObj instanceof String) bloodGroupId = Integer.parseInt((String) bgIdObj);

                                member.put("blood_group_id", bloodGroupId);
                                member.put("blood_group", Map.of("name", extra.getOrDefault("blood_group", "O+")));
                                member.put("eps", extra.getOrDefault("eps", "Compensar"));
                                member.put("birth_date", extra.getOrDefault("birth_date", "1990-01-01"));
                                member.put("document_number", extra.getOrDefault("document_number", "1000000" + idVal));
                                member.put("kinship_id", 1);
                            }
                        }
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", member)));
                    return;
                }
            }

            // FAMILY MEMBERS (SUPERVISOR BULK ACCESS)
            if (servletPath.contains("familyMembers")) {
                List<Map<String, Object>> list = new ArrayList<>();
                String sql = "SELECT IdIntegrante, IdPlanFamiliar FROM Integrante";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(Map.of(
                            "member_id", rs.getInt("IdIntegrante"),
                            "family_plan_id", rs.getInt("IdPlanFamiliar")
                        ));
                    }
                }
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
                    int petId = Integer.parseInt(pathInfo.substring(5));
                    List<Map<String, Object>> list = mascotaDAO.getVaccinesByPet(petId);
                    ResponseUtil.sendSuccess(resp, list);
                    return;
                } else if (pathInfo != null && !pathInfo.equals("/")) {
                    int idVal = Integer.parseInt(pathInfo.substring(1));
                    Map<String, Object> vac = mascotaDAO.getVaccineById(idVal);
                    if (vac != null) {
                        ResponseUtil.sendSuccess(resp, vac);
                    } else {
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Vacuna no encontrada");
                    }
                    return;
                }
            }

            // RISK FACTORS
            if (servletPath.contains("riskFactors")) {
                if (pathInfo != null && pathInfo.startsWith("/familyPlan/select/")) {
                    int planId = Integer.parseInt(pathInfo.substring(19));
                    List<Map<String, Object>> list = new ArrayList<>();
                    String sql = """
                        SELECT f.IdFactorRiesgo, t.Nombre AS AmenazaNombre, f.Ubicacion
                        FROM FactorRiesgo f
                        JOIN TipoAmenaza t ON f.IdTipoAmenaza = t.IdTipoAmenaza
                        WHERE f.IdPlanFamiliar = ?
                        """;
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, planId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                Map<String, Object> risk = new HashMap<>();
                                risk.put("id", rs.getInt("IdFactorRiesgo"));
                                risk.put("name", rs.getString("AmenazaNombre") + " - " + rs.getString("Ubicacion"));
                                list.add(risk);
                            }
                        }
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                    return;

                } else if (pathInfo != null && pathInfo.startsWith("/familyPlan/")) {
                    int planId = Integer.parseInt(pathInfo.substring(12));
                    List<Map<String, Object>> list = new ArrayList<>();
                    String sql = """
                        SELECT f.IdFactorRiesgo, f.IdTipoAmenaza, f.Ubicacion, f.AccionReduccion, t.Nombre AS AmenazaNombre
                        FROM FactorRiesgo f
                        JOIN TipoAmenaza t ON f.IdTipoAmenaza = t.IdTipoAmenaza
                        WHERE f.IdPlanFamiliar = ?
                        """;
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, planId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                Map<String, Object> risk = new HashMap<>();
                                int riskId = rs.getInt("IdFactorRiesgo");
                                String amenazaNombre = rs.getString("AmenazaNombre");
                                risk.put("id", riskId);
                                risk.put("threat_type_id", rs.getInt("IdTipoAmenaza"));
                                risk.put("threat_type_name", amenazaNombre);
                                risk.put("threat_type", Map.of("id", rs.getInt("IdTipoAmenaza"), "name", amenazaNombre));
                                risk.put("ubication", rs.getString("Ubicacion"));

                                // Load extra fields from cache (description, distance, basic_reduction_action)
                                Map<String, Object> extra = extraData.getOrDefault("risk_" + riskId, Map.of());
                                risk.put("description", extra.getOrDefault("description", rs.getString("AccionReduccion") != null ? rs.getString("AccionReduccion") : ""));
                                risk.put("distance", extra.getOrDefault("distance", ""));

                                risk.put("family_plan_id", planId);
                                list.add(risk);
                            }
                        }
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                    return;

                } else if (pathInfo != null && !pathInfo.equals("/")) {
                    int idVal = Integer.parseInt(pathInfo.substring(1));
                    String sql = """
                        SELECT f.IdFactorRiesgo, f.IdTipoAmenaza, f.Ubicacion, f.AccionReduccion, t.Nombre AS AmenazaNombre
                        FROM FactorRiesgo f
                        JOIN TipoAmenaza t ON f.IdTipoAmenaza = t.IdTipoAmenaza
                        WHERE f.IdFactorRiesgo = ?
                        """;
                    Map<String, Object> risk = new HashMap<>();
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, idVal);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                String amenazaNombre = rs.getString("AmenazaNombre");
                                risk.put("id", rs.getInt("IdFactorRiesgo"));
                                risk.put("threat_type_id", rs.getInt("IdTipoAmenaza"));
                                risk.put("threat_type_name", amenazaNombre);
                                risk.put("threat_type", Map.of("id", rs.getInt("IdTipoAmenaza"), "name", amenazaNombre));
                                risk.put("ubication", rs.getString("Ubicacion"));

                                // Load extra fields from cache
                                Map<String, Object> extra = extraData.getOrDefault("risk_" + idVal, Map.of());
                                risk.put("description", extra.getOrDefault("description", rs.getString("AccionReduccion") != null ? rs.getString("AccionReduccion") : ""));
                                risk.put("distance", extra.getOrDefault("distance", ""));
                            }
                        }
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", risk)));
                    return;

                } else {
                    // LIST ALL RISK FACTORS
                    List<Map<String, Object>> list = new ArrayList<>();
                    String sql = """
                        SELECT f.IdFactorRiesgo, f.IdPlanFamiliar, f.IdTipoAmenaza, f.Ubicacion, t.Nombre AS AmenazaNombre
                        FROM FactorRiesgo f
                        JOIN TipoAmenaza t ON f.IdTipoAmenaza = t.IdTipoAmenaza
                        """;
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql);
                         ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int threatTypeId = rs.getInt("IdTipoAmenaza");
                            String threatTypeName = rs.getString("AmenazaNombre");
                            Map<String, Object> risk = new HashMap<>();
                            risk.put("id", rs.getInt("IdFactorRiesgo"));
                            risk.put("family_plan_id", rs.getInt("IdPlanFamiliar"));
                            risk.put("threat_type_id", threatTypeId);
                            risk.put("threat_type_name", threatTypeName);
                            risk.put("threat_type", Map.of("id", threatTypeId, "name", threatTypeName != null ? threatTypeName : ""));
                            risk.put("ubication", rs.getString("Ubicacion"));
                            list.add(risk);
                        }
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                    return;
                }
            }

            // VULNERABILITY FACTORS
            if (servletPath.contains("vulnerabilityFactors")) {
                if (pathInfo != null && pathInfo.startsWith("/riskFactor/")) {
                    int riskId = Integer.parseInt(pathInfo.substring(12));
                    List<Map<String, Object>> list = new ArrayList<>();
                    String sql = """
                        SELECT v.IdVulnerabilidad, v.IdTipoVulnerabilidad, v.Grado, vt.Nombre AS VulnNombre
                        FROM Vulnerabilidad v
                        JOIN VulnerabilidadTipo vt ON v.IdTipoVulnerabilidad = vt.IdTipoVulnerabilidad
                        WHERE v.IdFactorRiesgo = ?
                        """;
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, riskId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                Map<String, Object> item = new HashMap<>();
                                item.put("id", rs.getInt("IdVulnerabilidad"));
                                item.put("vulnerability", Map.of("name", rs.getString("VulnNombre")));
                                item.put("vulnerability_grade", Map.of("name", rs.getString("Grado") != null ? rs.getString("Grado") : ""));
                                list.add(item);
                            }
                        }
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                    return;

                } else if (pathInfo != null && !pathInfo.equals("/")) {
                    int idVal = Integer.parseInt(pathInfo.substring(1));
                    String sql = """
                        SELECT v.IdVulnerabilidad, v.IdFactorRiesgo, v.IdTipoVulnerabilidad, v.Grado, vt.Nombre AS VulnNombre
                        FROM Vulnerabilidad v
                        JOIN VulnerabilidadTipo vt ON v.IdTipoVulnerabilidad = vt.IdTipoVulnerabilidad
                        WHERE v.IdVulnerabilidad = ?
                        """;
                    Map<String, Object> item = new HashMap<>();
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, idVal);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                item.put("id", rs.getInt("IdVulnerabilidad"));
                                item.put("vulnerability_id", rs.getInt("IdTipoVulnerabilidad"));
                                item.put("vulnerability", Map.of("name", rs.getString("VulnNombre")));
                                item.put("vulnerability_grade_id", 1);
                                item.put("vulnerability_grade", Map.of("name", rs.getString("Grado") != null ? rs.getString("Grado") : ""));
                                item.put("risk_factor_id", rs.getInt("IdFactorRiesgo"));
                            }
                        }
                    }
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
                if (pathInfo != null && !pathInfo.equals("/")) {
                    // Path format: /housingInfo/{planId}/type/{typeId}
                    String[] segments = pathInfo.split("/");
                    int planId = Integer.parseInt(segments[1]);
                    int typeId = 2; // Default: 2 (Entorno)
                    if (segments.length > 3) {
                        try {
                            typeId = Integer.parseInt(segments[3]);
                        } catch (NumberFormatException e) {
                            // Ignored
                        }
                    }
                    int esEntornoVal = (typeId == 2) ? 1 : 0;

                    String sql = "SELECT IdGrafico, RutaImagen FROM GraficoVivienda WHERE IdPlanFamiliar = ? AND EsEntorno = ? LIMIT 1";
                    Map<String, Object> item = null;
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, planId);
                        ps.setInt(2, esEntornoVal);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                item = new HashMap<>();
                                item.put("id", rs.getInt("IdGrafico"));
                                item.put("path", rs.getString("RutaImagen"));
                                item.put("family_plan_id", planId);
                            }
                        }
                    }
                    if (item != null) {
                        resp.getWriter().write(gson.toJson(Map.of("data", item)));
                    } else {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        resp.getWriter().write("{\"success\":false,\"data\":null}");
                    }
                    return;
                }
            }

            // HOUSING GRAPHICS (GraficoVivienda with EsEntorno = 0)
            if (servletPath.contains("housingGraphics")) {
                if (pathInfo != null && pathInfo.startsWith("/familyPlan/")) {
                    int planId = Integer.parseInt(pathInfo.substring(12));
                    List<Map<String, Object>> list = new ArrayList<>();
                    String sql = "SELECT IdGrafico, RutaImagen, Descripcion FROM GraficoVivienda WHERE IdPlanFamiliar = ? AND EsEntorno = 0";
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, planId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                Map<String, Object> item = new HashMap<>();
                                item.put("id", rs.getInt("IdGrafico"));
                                item.put("path", rs.getString("RutaImagen"));
                                item.put("description", rs.getString("Descripcion") != null ? rs.getString("Descripcion") : "");
                                list.add(item);
                            }
                        }
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                    return;

                } else if (pathInfo != null && !pathInfo.equals("/")) {
                    int idVal = Integer.parseInt(pathInfo.substring(1));
                    String sql = "SELECT IdGrafico, RutaImagen, Descripcion, IdPlanFamiliar FROM GraficoVivienda WHERE IdGrafico = ?";
                    Map<String, Object> item = new HashMap<>();
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, idVal);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                item.put("id", rs.getInt("IdGrafico"));
                                item.put("path", rs.getString("RutaImagen"));
                                item.put("description", rs.getString("Descripcion") != null ? rs.getString("Descripcion") : "");
                                item.put("family_plan_id", rs.getInt("IdPlanFamiliar"));
                            }
                        }
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", item)));
                    return;
                }
            }

            // ACTION PLANS (PlanAccion)
            if (servletPath.contains("actionPlans")) {
                if (pathInfo != null && pathInfo.startsWith("/familyPlan/boolean/")) {
                    int planId = Integer.parseInt(pathInfo.substring(20));
                    boolean exists = false;
                    String sql = "SELECT COUNT(*) FROM PlanAccion pa JOIN FactorRiesgo fr ON pa.IdFactorRiesgo = fr.IdFactorRiesgo WHERE fr.IdPlanFamiliar = ?";
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, planId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) exists = rs.getInt(1) > 0;
                        }
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", exists)));
                    return;

                } else if (pathInfo != null && pathInfo.startsWith("/familyPlan/")) {
                    int planId = Integer.parseInt(pathInfo.substring(12));
                    Map<String, Object> item = null;
                    String sql = "SELECT pa.IdPlanAccion, pa.IdCoordinador FROM PlanAccion pa JOIN FactorRiesgo fr ON pa.IdFactorRiesgo = fr.IdFactorRiesgo WHERE fr.IdPlanFamiliar = ? LIMIT 1";
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, planId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                item = new HashMap<>();
                                item.put("id", rs.getInt("IdPlanAccion"));
                                item.put("family_plan_id", planId);
                                item.put("coordinator_id", rs.getInt("IdCoordinador"));
                            }
                        }
                    }
                    if (item == null) {
                        // Create layout if not exists
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        resp.getWriter().write("{\"success\":false}");
                    } else {
                        resp.getWriter().write(gson.toJson(Map.of("data", item)));
                    }
                    return;
                }
            }

            // ACTION PLAN ACTIONS (Accion)
            if (servletPath.contains("actionPlanActions")) {
                if (pathInfo != null && pathInfo.startsWith("/actionPlan/")) {
                    int planAccionId = Integer.parseInt(pathInfo.substring(12));
                    List<Map<String, Object>> list = new ArrayList<>();
                    String sql = """
                        SELECT a.IdAccion, a.IdResponsable, a.Etapa, a.Descripcion, i.Nombre, i.Apellido
                        FROM Accion a
                        JOIN Integrante i ON a.IdResponsable = i.IdIntegrante
                        WHERE a.IdPlanAccion = ?
                        """;
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, planAccionId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                Map<String, Object> item = new HashMap<>();
                                item.put("id", rs.getInt("IdAccion"));
                                item.put("description", rs.getString("Descripcion"));
                                item.put("stage", rs.getString("Etapa"));
                                item.put("member_id", rs.getInt("IdResponsable"));
                                item.put("member", Map.of(
                                    "names", rs.getString("Nombre"),
                                    "last_names", rs.getString("Apellido")
                                ));
                                list.add(item);
                            }
                        }
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                    return;

                } else if (pathInfo != null && !pathInfo.equals("/")) {
                    int idVal = Integer.parseInt(pathInfo.substring(1));
                    String sql = """
                        SELECT a.IdAccion, a.IdResponsable, a.Etapa, a.Descripcion, i.Nombre, i.Apellido
                        FROM Accion a
                        JOIN Integrante i ON a.IdResponsable = i.IdIntegrante
                        WHERE a.IdAccion = ?
                        """;
                    Map<String, Object> item = new HashMap<>();
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, idVal);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                item.put("id", rs.getInt("IdAccion"));
                                item.put("description", rs.getString("Descripcion"));
                                item.put("stage", rs.getString("Etapa"));
                                item.put("member_id", rs.getInt("IdResponsable"));
                                item.put("member", Map.of(
                                    "names", rs.getString("Nombre"),
                                    "last_names", rs.getString("Apellido")
                                ));
                            }
                        }
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", item)));
                    return;
                }
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

                try (Connection conn = DatabaseConfig.getConnection()) {
                    int existingId = 0;
                    String checkSql = "SELECT IdRespuestaPlan FROM RespuestaPlan WHERE IdPregunta = ? AND IdPlanFamiliar = ?";
                    try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                        checkPs.setInt(1, questionId);
                        checkPs.setInt(2, planId);
                        try (ResultSet rs = checkPs.executeQuery()) {
                            if (rs.next()) {
                                existingId = rs.getInt(1);
                            }
                        }
                    }

                    if (existingId > 0) {
                        String updateSql = "UPDATE RespuestaPlan SET Valor = ? WHERE IdRespuestaPlan = ?";
                        try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                            updatePs.setBoolean(1, answer);
                            updatePs.setInt(2, existingId);
                            updatePs.executeUpdate();
                        }
                    } else {
                        String insertSql = "INSERT INTO RespuestaPlan (IdPregunta, IdPlanFamiliar, Valor) VALUES (?, ?, ?)";
                        try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                            insertPs.setInt(1, questionId);
                            insertPs.setInt(2, planId);
                            insertPs.setBoolean(3, answer);
                            insertPs.executeUpdate();
                        }
                    }
                } catch (SQLException e) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().write("{\"success\":false,\"message\":\"Error de base de datos\"}");
                    e.printStackTrace();
                    return;
                }
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("{\"success\":true,\"message\":\"Respuesta guardada exitosamente\"}");
                return;
            }

            // POST /api/familyPlans
            if (servletPath.contains("familyPlans")) {
                String lastNames = (String) body.get("last_names");
                Object userIdObj = body.get("user_id");
                int userId = 1;
                if (userIdObj instanceof Number) userId = ((Number) userIdObj).intValue();
                else if (userIdObj instanceof String) userId = Integer.parseInt((String) userIdObj);

                int familyId = 0;
                try (Connection conn = DatabaseConfig.getConnection()) {
                    String sqlFam = "INSERT INTO Familia (Nombre) VALUES (?)";
                    try (PreparedStatement ps = conn.prepareStatement(sqlFam, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, "Familia " + lastNames);
                        ps.executeUpdate();
                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            if (rs.next()) familyId = rs.getInt(1);
                        }
                    }

                    String sqlPlan = "INSERT INTO PlanFamiliar (IdFamilia, IdUsuario, Fecha, Estado) VALUES (?, ?, ?, '1')";
                    try (PreparedStatement ps = conn.prepareStatement(sqlPlan, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setInt(1, familyId);
                        ps.setInt(2, userId);
                        ps.setDate(3, Date.valueOf(LocalDate.now()));
                        ps.executeUpdate();
                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            if (rs.next()) {
                                int planId = rs.getInt(1);

                                // Save extra location metadata to memory
                                Map<String, Object> extra = new HashMap<>();
                                extra.put("zone_id", body.get("zone_id"));
                                extra.put("city_id", body.get("city_id"));
                                extraData.put("plan_" + planId, extra);

                                resp.setStatus(HttpServletResponse.SC_CREATED);
                                resp.getWriter().write(String.format("{\"success\":true,\"message\":\"Plan familiar creado con exito\",\"data\":{\"id\":%d}}", planId));
                                return;
                            }
                        }
                    }
                }
            }

            // POST /api/members/{familyPlanId}
            if (servletPath.contains("members")) {
                int familyPlanId = Integer.parseInt(pathInfo.substring(1));
                String names = (String) body.get("names");
                String lastNames = (String) body.get("last_names");
                String relationship = (String) body.get("relationship");
                String phone = (String) body.get("phone");

                Object genderObj = body.get("gender_id");
                int genderId = 1;
                if (genderObj instanceof Number) genderId = ((Number) genderObj).intValue();

                Object docTypeObj = body.get("document_type_id");
                int docTypeId = 1;
                if (docTypeObj instanceof Number) docTypeId = ((Number) docTypeObj).intValue();

                Object natObj = body.get("nationality_id");
                int natId = 1;
                if (natObj instanceof Number) natId = ((Number) natObj).intValue();

                String sql = "INSERT INTO Integrante (IdPlanFamiliar, Nombre, Apellido, Parentesco, Telefono, IdGenero, IdDocumentoTipo, IdNacionalidad) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, familyPlanId);
                    ps.setString(2, names);
                    ps.setString(3, lastNames);
                    ps.setString(4, relationship != null ? relationship : "Familiar");
                    ps.setString(5, phone != null ? phone : "");
                    ps.setInt(6, genderId);
                    ps.setInt(7, docTypeId);
                    ps.setInt(8, natId);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            int memberId = rs.getInt(1);

                            // Save extra unmapped fields to memory
                            Map<String, Object> extra = new HashMap<>();
                            extra.put("eps", body.get("eps"));
                            extra.put("birth_date", body.get("birth_date"));
                            extra.put("document_number", body.get("document_number"));

                            Object bgIdObj = body.get("blood_group_id");
                            int bgId = 1;
                            if (bgIdObj instanceof Number) bgId = ((Number) bgIdObj).intValue();
                            else if (bgIdObj instanceof String) bgId = Integer.parseInt((String) bgIdObj);

                            String bgName = "O+";
                            if (bgId == 1) bgName = "O+";
                            else if (bgId == 2) bgName = "O-";
                            else if (bgId == 3) bgName = "A+";
                            else if (bgId == 4) bgName = "A-";
                            else if (bgId == 5) bgName = "B+";
                            else if (bgId == 6) bgName = "B-";
                            else if (bgId == 7) bgName = "AB+";
                            else if (bgId == 8) bgName = "AB-";
                            extra.put("blood_group", bgName);
                            extra.put("blood_group_id", bgId);

                            extraData.put("member_" + memberId, extra);

                            resp.setStatus(HttpServletResponse.SC_CREATED);
                            resp.getWriter().write(String.format("{\"success\":true,\"message\":\"Integrante agregado exitosamente\",\"data\":{\"id\":%d}}", memberId));
                            return;
                        }
                    }
                }
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
                int generatedId = mascotaDAO.insertVaccine(body);
                if (generatedId > 0) {
                    ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, null, "Vacuna agregada exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al agregar vacuna");
                }
                return;
            }

            // POST /api/riskFactors
            if (servletPath.contains("riskFactors")) {
                Object threatObj = body.get("threat_type_id");
                int threatId = 1;
                if (threatObj instanceof Number) threatId = ((Number) threatObj).intValue();
                else if (threatObj instanceof String) threatId = Integer.parseInt((String) threatObj);

                // Frontend sends 'ubication'; fallback to 'location'
                String ubicacion = body.containsKey("ubication") ? (String) body.get("ubication") : (String) body.get("location");
                String description = (String) body.get("description");
                String distanceStr = body.get("distance") != null ? String.valueOf(body.get("distance")) : "";

                Object planIdObj = body.get("family_plan_id");
                int planId = 1;
                if (planIdObj instanceof Number) planId = ((Number) planIdObj).intValue();
                else if (planIdObj instanceof String) planId = Integer.parseInt((String) planIdObj);

                // Store description in AccionReduccion column (best available fit)
                String accionReduccion = description != null && !description.isEmpty() ? description : "Sin descripción";

                String sql = "INSERT INTO FactorRiesgo (IdPlanFamiliar, IdTipoAmenaza, Ubicacion, AccionReduccion) VALUES (?, ?, ?, ?)";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, planId);
                    ps.setInt(2, threatId);
                    ps.setString(3, ubicacion != null ? ubicacion : "");
                    ps.setString(4, accionReduccion);
                    ps.executeUpdate();

                    int generatedId = 0;
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) generatedId = rs.getInt(1);
                    }
                    if (generatedId > 0) {
                        Map<String, Object> extra = new HashMap<>();
                        extra.put("description", description != null ? description : "");
                        extra.put("distance", distanceStr);
                        extraData.put("risk_" + generatedId, extra);
                    }

                    resp.setStatus(HttpServletResponse.SC_CREATED);
                    resp.getWriter().write("{\"success\":true,\"message\":\"Factor de riesgo agregado exitosamente\"}");
                    return;
                }
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
                Object vulnIdObj = body.get("vulnerability_id");
                int vulnId = 1;
                if (vulnIdObj instanceof Number) vulnId = ((Number) vulnIdObj).intValue();
                else if (vulnIdObj instanceof String && !((String) vulnIdObj).isEmpty()) vulnId = Integer.parseInt((String) vulnIdObj);

                Object gradeObj = body.get("vulnerability_grade_id");
                int gradeId = 1;
                if (gradeObj instanceof Number) gradeId = ((Number) gradeObj).intValue();
                else if (gradeObj instanceof String && !((String) gradeObj).isEmpty()) gradeId = Integer.parseInt((String) gradeObj);
                String gradeStr = (gradeId == 1) ? "Bajo" : (gradeId == 2 ? "Medio" : "Alto");

                Object riskIdObj = body.get("risk_factor_id");
                int riskId = 1;
                if (riskIdObj instanceof Number) riskId = ((Number) riskIdObj).intValue();
                else if (riskIdObj instanceof String && !((String) riskIdObj).isEmpty()) riskId = Integer.parseInt((String) riskIdObj);

                String sql = "INSERT INTO Vulnerabilidad (IdFactorRiesgo, IdTipoVulnerabilidad, Grado) VALUES (?, ?, ?)";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, riskId);
                    ps.setInt(2, vulnId);
                    ps.setString(3, gradeStr);
                    ps.executeUpdate();
                    resp.setStatus(HttpServletResponse.SC_CREATED);
                    resp.getWriter().write("{\"success\":true,\"message\":\"Vulnerabilidad agregada exitosamente\"}");
                    return;
                }
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
                Object planIdObj = body.get("family_plan_id");
                int planId = 1;
                if (planIdObj instanceof Number) planId = ((Number) planIdObj).intValue();
                else if (planIdObj instanceof String) planId = Integer.parseInt((String) planIdObj);

                int typeId = esEntorno ? 2 : 1; // Default: 2 (Entorno) for housingInfo
                Object typeIdObj = body.get("housing_info_type_id");
                if (typeIdObj instanceof Number) typeId = ((Number) typeIdObj).intValue();
                else if (typeIdObj instanceof String && !((String) typeIdObj).isEmpty()) typeId = Integer.parseInt((String) typeIdObj);

                if (pathInfo != null && pathInfo.length() > 1) {
                    String[] segments = pathInfo.split("/");
                    if (segments.length > 3) {
                        try {
                            typeId = Integer.parseInt(segments[3]);
                        } catch (NumberFormatException e) {
                            // Ignored
                        }
                    }
                }
                int esEntornoVal = (typeId == 2) ? 1 : 0;

                String description = body.containsKey("description") ? (String) body.get("description") : "Grafico del plan";

                String savedFileName = "mock_graphic.png";
                jakarta.servlet.http.Part filePart = null;
                try {
                    filePart = req.getPart("path");
                } catch (Exception e) {
                    // Ignored
                }
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

                try (Connection conn = DatabaseConfig.getConnection()) {
                    boolean exists = false;
                    // Only check exists if it's housingInfo (esEntorno is true)
                    if (esEntorno) {
                        String checkSql = "SELECT IdGrafico FROM GraficoVivienda WHERE IdPlanFamiliar = ? AND EsEntorno = ?";
                        try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                            checkPs.setInt(1, planId);
                            checkPs.setInt(2, esEntornoVal);
                            try (ResultSet rs = checkPs.executeQuery()) {
                                if (rs.next()) {
                                    exists = true;
                                }
                            }
                        }
                    }

                    if (exists) {
                        String updateSql = "UPDATE GraficoVivienda SET RutaImagen = ?, Descripcion = ? WHERE IdPlanFamiliar = ? AND EsEntorno = ?";
                        try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                            updatePs.setString(1, savedFileName);
                            updatePs.setString(2, description);
                            updatePs.setInt(3, planId);
                            updatePs.setInt(4, esEntornoVal);
                            updatePs.executeUpdate();
                        }
                    } else {
                        String insertSql = "INSERT INTO GraficoVivienda (IdPlanFamiliar, RutaImagen, Descripcion, EsEntorno) VALUES (?, ?, ?, ?)";
                        try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                            insertPs.setInt(1, planId);
                            insertPs.setString(2, savedFileName);
                            insertPs.setString(3, description);
                            insertPs.setInt(4, esEntornoVal);
                            insertPs.executeUpdate();
                        }
                    }
                    resp.setStatus(HttpServletResponse.SC_CREATED);
                    resp.getWriter().write("{\"success\":true,\"message\":\"Archivo subido exitosamente\"}");
                    return;
                }
            }

            // POST /api/actionPlans
            if (servletPath.contains("actionPlans")) {
                Object planIdObj = body.get("family_plan_id");
                int planId = 1;
                if (planIdObj instanceof Number) planId = ((Number) planIdObj).intValue();
                else if (planIdObj instanceof String) planId = Integer.parseInt((String) planIdObj);

                Object coordIdObj = body.get("coordinator_id");
                int coordId = 0;
                if (coordIdObj instanceof Number) coordId = ((Number) coordIdObj).intValue();

                String sql = "INSERT INTO PlanAccion (IdFactorRiesgo, IdCoordinador) VALUES (?, ?)";
                try (Connection conn = DatabaseConfig.getConnection()) {
                    // Try to find a risk factor to link to
                    int riskId = 0;
                    try (PreparedStatement psR = conn.prepareStatement("SELECT IdFactorRiesgo FROM FactorRiesgo WHERE IdPlanFamiliar = ? LIMIT 1")) {
                        psR.setInt(1, planId);
                        try (ResultSet rsR = psR.executeQuery()) { if (rsR.next()) riskId = rsR.getInt(1); }
                    }

                    if (riskId == 0) {
                        // Create dummy risk factor if none exists to satisfy foreign keys
                        try (PreparedStatement psR = conn.prepareStatement("INSERT INTO FactorRiesgo (IdPlanFamiliar, IdTipoAmenaza, Ubicacion) VALUES (?, 1, 'General')", Statement.RETURN_GENERATED_KEYS)) {
                            psR.setInt(1, planId);
                            psR.executeUpdate();
                            try (ResultSet rsR = psR.getGeneratedKeys()) { if (rsR.next()) riskId = rsR.getInt(1); }
                        }
                    }

                    if (coordId == 0) {
                        // Find a member to link as coordinator
                        try (PreparedStatement psM = conn.prepareStatement("SELECT IdIntegrante FROM Integrante WHERE IdPlanFamiliar = ? LIMIT 1")) {
                            psM.setInt(1, planId);
                            try (ResultSet rsM = psM.executeQuery()) { if (rsM.next()) coordId = rsM.getInt(1); }
                        }
                    }

                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, riskId);
                        ps.setInt(2, coordId);
                        ps.executeUpdate();
                        resp.setStatus(HttpServletResponse.SC_CREATED);
                        resp.getWriter().write("{\"success\":true,\"message\":\"Plan de accion creado exitosamente\"}");
                        return;
                    }
                }
            }

            // POST /api/actionPlanActions
            if (servletPath.contains("actionPlanActions")) {
                Object planAccionObj = body.get("action_plan_id");
                int planAccionId = 1;
                if (planAccionObj instanceof Number) planAccionId = ((Number) planAccionObj).intValue();

                Object memberIdObj = body.get("member_id");
                int memberId = 1;
                if (memberIdObj instanceof Number) memberId = ((Number) memberIdObj).intValue();

                String description = (String) body.get("description");

                Object typeIdObj = body.get("action_type_id");
                int typeId = 1;
                if (typeIdObj instanceof Number) typeId = ((Number) typeIdObj).intValue();
                String stage = (typeId == 1) ? "antes" : (typeId == 2 ? "durante" : "despues");

                String sql = "INSERT INTO Accion (IdPlanAccion, IdResponsable, Etapa, Descripcion) VALUES (?, ?, ?, ?)";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, planAccionId);
                    ps.setInt(2, memberId);
                    ps.setString(3, stage);
                    ps.setString(4, description);
                    ps.executeUpdate();
                    resp.setStatus(HttpServletResponse.SC_CREATED);
                    resp.getWriter().write("{\"success\":true,\"message\":\"Accion creada exitosamente\"}");
                    return;
                }
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
                String names = (String) body.get("names");
                String lastNames = (String) body.get("last_names");
                String relationship = (String) body.get("relationship");
                String phone = (String) body.get("phone");

                Object genderObj = body.get("gender_id");
                int genderId = 1;
                if (genderObj instanceof Number) genderId = ((Number) genderObj).intValue();

                Object docTypeObj = body.get("document_type_id");
                int docTypeId = 1;
                if (docTypeObj instanceof Number) docTypeId = ((Number) docTypeObj).intValue();

                Object natObj = body.get("nationality_id");
                int natId = 1;
                if (natObj instanceof Number) natId = ((Number) natObj).intValue();

                String sql = "UPDATE Integrante SET Nombre = ?, Apellido = ?, Parentesco = ?, Telefono = ?, IdGenero = ?, IdDocumentoTipo = ?, IdNacionalidad = ? WHERE IdIntegrante = ?";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, names);
                    ps.setString(2, lastNames);
                    ps.setString(3, relationship != null ? relationship : "Familiar");
                    ps.setString(4, phone != null ? phone : "");
                    ps.setInt(5, genderId);
                    ps.setInt(6, docTypeId);
                    ps.setInt(7, natId);
                    ps.setInt(8, idVal);
                    ps.executeUpdate();

                    // Save extra unmapped fields to memory
                    Map<String, Object> extra = extraData.computeIfAbsent("member_" + idVal, k -> new HashMap<>());
                    extra.put("eps", body.get("eps"));
                    extra.put("birth_date", body.get("birth_date"));
                    extra.put("document_number", body.get("document_number"));

                    Object bgIdObj = body.get("blood_group_id");
                    int bgId = 1;
                    if (bgIdObj instanceof Number) bgId = ((Number) bgIdObj).intValue();
                    else if (bgIdObj instanceof String) bgId = Integer.parseInt((String) bgIdObj);

                    String bgName = "O+";
                    if (bgId == 1) bgName = "O+";
                    else if (bgId == 2) bgName = "O-";
                    else if (bgId == 3) bgName = "A+";
                    else if (bgId == 4) bgName = "A-";
                    else if (bgId == 5) bgName = "B+";
                    else if (bgId == 6) bgName = "B-";
                    else if (bgId == 7) bgName = "AB+";
                    else if (bgId == 8) bgName = "AB-";
                    extra.put("blood_group", bgName);
                    extra.put("blood_group_id", bgId);

                    resp.getWriter().write("{\"success\":true,\"message\":\"Integrante actualizado exitosamente\"}");
                    return;
                }
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

                String lastNames = (String) body.get("last_names");
                String landlinePhone = (String) body.get("landline_phone");

                try (Connection conn = DatabaseConfig.getConnection()) {
                    // Get Familia ID from PlanFamiliar
                    int familyId = 0;
                    String getFamSql = "SELECT IdFamilia FROM PlanFamiliar WHERE IdPlanFamiliar = ?";
                    try (PreparedStatement ps = conn.prepareStatement(getFamSql)) {
                        ps.setInt(1, planId);
                        try (ResultSet rs = ps.executeQuery()) { if (rs.next()) familyId = rs.getInt(1); }
                    }

                    if (familyId > 0) {
                        String sql = "UPDATE Familia SET Nombre = ?, Telefono = ?, CalidadVivienda = ? WHERE IdFamilia = ?";
                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, "Familia " + lastNames);
                            ps.setString(2, landlinePhone != null ? landlinePhone : "");
                            ps.setString(3, "propietario"); // Default calidad
                            ps.setInt(4, familyId);
                            ps.executeUpdate();
                        }
                    }
                }

                // Save other unmapped fields in memory
                Map<String, Object> extra = extraData.computeIfAbsent("plan_" + planId, k -> new HashMap<>());
                extra.put("zone_id", body.get("zone_id"));
                extra.put("department_id", body.get("department_id"));
                extra.put("city_id", body.get("city_id"));
                extra.put("address", body.get("address"));
                extra.put("sector_id", body.get("sector_id"));
                extra.put("sector_name", body.get("sector_name"));
                extra.put("housing_quality_id", body.get("housing_quality_id"));

                resp.getWriter().write("{\"success\":true,\"message\":\"Datos de identificacion actualizados\"}");
                return;
            }

            // PATCH /api/familyPlans/{id}/change-status
            if (servletPath.contains("familyPlans") && pathInfo.endsWith("/change-status")) {
                String[] segments = pathInfo.split("/");
                int planId = Integer.parseInt(segments[1]);

                Object statusObj = body.get("status_plan_id");
                int statusId = 1;
                if (statusObj instanceof Number) statusId = ((Number) statusObj).intValue();

                String sql = "UPDATE PlanFamiliar SET Estado = ? WHERE IdPlanFamiliar = ?";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, String.valueOf(statusId));
                    ps.setInt(2, planId);
                    ps.executeUpdate();

                    // If commentary is present (rejected with changes), save to ValidacionPlan
                    String comment = (String) body.get("comentary");
                    if (comment != null) {
                        String valSql = "INSERT INTO ValidacionPlan (IdPlanFamiliar, IdSupervisor, Fecha, Estado, Comentario) VALUES (?, 2, ?, ?, ?)";
                        try (PreparedStatement psV = conn.prepareStatement(valSql)) {
                            psV.setInt(1, planId);
                            psV.setDate(2, Date.valueOf(LocalDate.now()));
                            psV.setString(3, "Rechazado");
                            psV.setString(4, comment);
                            psV.executeUpdate();
                        }
                    }
                }
                resp.getWriter().write("{\"success\":true,\"message\":\"Estado de plan actualizado exitosamente\"}");
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

                Map<String, Object> extra = extraData.computeIfAbsent("plan_" + planId, k -> new HashMap<>());
                extra.put("family_type_id", familyTypeId);

                resp.getWriter().write("{\"success\":true,\"message\":\"Tipo de familia actualizado exitosamente\"}");
                return;
            }

            // PATCH /api/familyPlans/status/{id}
            if (servletPath.contains("familyPlans") && pathInfo.startsWith("/status/")) {
                int planId = Integer.parseInt(pathInfo.substring(8));
                Object statusObj = body.get("status_plan_id");
                int statusId = 1;
                if (statusObj instanceof Number) statusId = ((Number) statusObj).intValue();

                String sql = "UPDATE PlanFamiliar SET Estado = ? WHERE IdPlanFamiliar = ?";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, String.valueOf(statusId));
                    ps.setInt(2, planId);
                    ps.executeUpdate();
                }
                resp.getWriter().write("{\"success\":true,\"message\":\"Estado de plan actualizado exitosamente\"}");
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
                int vaccineId = Integer.parseInt(pathInfo.substring(1));
                boolean updated = mascotaDAO.updateVaccine(vaccineId, body);
                if (updated) {
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
                Object threatObj = body.get("threat_type_id");
                int threatId = 1;
                if (threatObj instanceof Number) threatId = ((Number) threatObj).intValue();
                else if (threatObj instanceof String) threatId = Integer.parseInt((String) threatObj);

                // Frontend sends 'ubication' on create but 'location' on edit — support both
                String ubicacion = body.containsKey("ubication") ? (String) body.get("ubication") : (String) body.get("location");
                String description = (String) body.get("description");
                String distanceStr = body.get("distance") != null ? String.valueOf(body.get("distance")) : "";

                String accionReduccion = description != null && !description.isEmpty() ? description : "Sin descripción";

                String sql = "UPDATE FactorRiesgo SET IdTipoAmenaza = ?, Ubicacion = ?, AccionReduccion = ? WHERE IdFactorRiesgo = ?";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, threatId);
                    ps.setString(2, ubicacion != null ? ubicacion : "");
                    ps.setString(3, accionReduccion);
                    ps.setInt(4, idVal);
                    ps.executeUpdate();
                }

                // Update cache with latest description and distance
                Map<String, Object> extra = extraData.computeIfAbsent("risk_" + idVal, k -> new HashMap<>());
                extra.put("description", description != null ? description : "");
                extra.put("distance", distanceStr);

                resp.getWriter().write("{\"success\":true,\"message\":\"Factor de riesgo actualizado exitosamente\"}");
                return;
            }

            // PATCH /api/vulnerabilityFactors/{id}
            if (servletPath.contains("vulnerabilityFactors")) {
                int idVal = Integer.parseInt(pathInfo.substring(1));
                Object vulnIdObj = body.get("vulnerability_id");
                int vulnId = 1;
                if (vulnIdObj instanceof Number) vulnId = ((Number) vulnIdObj).intValue();

                Object gradeObj = body.get("vulnerability_grade_id");
                int gradeId = 1;
                if (gradeObj instanceof Number) gradeId = ((Number) gradeObj).intValue();
                String gradeStr = (gradeId == 1) ? "Bajo" : (gradeId == 2 ? "Medio" : "Alto");

                String sql = "UPDATE Vulnerabilidad SET IdTipoVulnerabilidad = ?, Grado = ? WHERE IdVulnerabilidad = ?";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, vulnId);
                    ps.setString(2, gradeStr);
                    ps.setInt(3, idVal);
                    ps.executeUpdate();
                }
                resp.getWriter().write("{\"success\":true,\"message\":\"Vulnerabilidad actualizada exitosamente\"}");
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
                int idVal = Integer.parseInt(pathInfo.substring(1));
                Object memberIdObj = body.get("member_id");
                int memberId = 1;
                if (memberIdObj instanceof Number) memberId = ((Number) memberIdObj).intValue();
                String description = (String) body.get("description");

                String sql = "UPDATE Accion SET IdResponsable = ?, Descripcion = ? WHERE IdAccion = ?";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, memberId);
                    ps.setString(2, description);
                    ps.setInt(3, idVal);
                    ps.executeUpdate();
                }
                resp.getWriter().write("{\"success\":true,\"message\":\"Accion de plan actualizada exitosamente\"}");
                return;
            }

            // PATCH /api/housingGraphics/{id}/description
            if (servletPath.contains("housingGraphics") && pathInfo != null && pathInfo.endsWith("/description")) {
                String[] segments = pathInfo.split("/");
                int graficoId = Integer.parseInt(segments[1]);
                String description = (String) body.get("description");

                String sql = "UPDATE GraficoVivienda SET Descripcion = ? WHERE IdGrafico = ?";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, description != null ? description : "");
                    ps.setInt(2, graficoId);
                    ps.executeUpdate();
                }
                resp.getWriter().write("{\"success\":true,\"message\":\"Descripción de gráfico actualizada exitosamente\"}");
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
                // Delete related diseases first to respect foreign key constraints
                try (Connection conn = DatabaseConfig.getConnection()) {
                    try (PreparedStatement psD = conn.prepareStatement("DELETE FROM Enfermedad WHERE IdIntegrante = ?")) {
                        psD.setInt(1, idVal);
                        psD.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Integrante WHERE IdIntegrante = ?")) {
                        ps.setInt(1, idVal);
                        ps.executeUpdate();
                    }
                }
                resp.getWriter().write("{\"success\":true,\"message\":\"Integrante eliminado exitosamente\"}");
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
                boolean deleted = mascotaDAO.deleteVaccine(idVal);
                if (deleted) {
                    ResponseUtil.sendSuccess(resp, "Vacuna eliminada exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Vacuna no encontrada");
                }
                return;
            }

            // DELETE /api/riskFactors/{id}
            if (servletPath.contains("riskFactors")) {
                try (Connection conn = DatabaseConfig.getConnection()) {
                    // 1. Delete child vulnerabilities first (FK constraint)
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Vulnerabilidad WHERE IdFactorRiesgo = ?")) {
                        ps.setInt(1, idVal); ps.executeUpdate();
                    }
                    // 2. Delete the risk factor itself
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM FactorRiesgo WHERE IdFactorRiesgo = ?")) {
                        ps.setInt(1, idVal); ps.executeUpdate();
                    }
                }
                extraData.remove("risk_" + idVal);
                resp.getWriter().write("{\"success\":true,\"message\":\"Factor de riesgo eliminado exitosamente\"}");
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
                String sql = "DELETE FROM Vulnerabilidad WHERE IdVulnerabilidad = ?";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, idVal);
                    ps.executeUpdate();
                }
                resp.getWriter().write("{\"success\":true,\"message\":\"Vulnerabilidad eliminada exitosamente\"}");
                return;
            }

            // DELETE /api/actionPlanActions/{id}
            if (servletPath.contains("actionPlanActions")) {
                String sql = "DELETE FROM Accion WHERE IdAccion = ?";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, idVal);
                    ps.executeUpdate();
                }
                resp.getWriter().write("{\"success\":true,\"message\":\"Accion de plan de accion eliminada\"}");
                return;
            }

            // DELETE /api/housingGraphics/{id}
            if (servletPath.contains("housingGraphics")) {
                String fileName = null;
                // Fetch the filename first to delete the file on disk
                String selectSql = "SELECT RutaImagen FROM GraficoVivienda WHERE IdGrafico = ?";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, idVal);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            fileName = rs.getString("RutaImagen");
                        }
                    }
                } catch (Exception e) {
                    // Ignored
                }

                // Delete from DB
                String sql = "DELETE FROM GraficoVivienda WHERE IdGrafico = ?";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, idVal);
                    ps.executeUpdate();
                }

                // Delete physical file
                if (fileName != null && !fileName.equals("mock_graphic.png")) {
                    java.io.File file = new java.io.File("/home/dylan/Documents/projects/df/DefensaCivilAPI/storage", fileName);
                    if (file.exists() && file.isFile()) {
                        file.delete();
                    }
                }

                resp.getWriter().write("{\"success\":true,\"message\":\"Gráfico de vivienda eliminado exitosamente\"}");
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
}

package com.defensacivil.controller;

import com.defensacivil.config.ResponseUtil;
import com.defensacivil.dao.IntegranteDAO;
import com.defensacivil.dao.IntegranteDAOImpl;
import com.defensacivil.dto.MemberDTO;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servlet que gestiona los integrantes de los planes familiares y sus condiciones especiales de salud (afecciones).
 * Provee operaciones para listar, agregar, actualizar y eliminar integrantes y sus afecciones.
 * 
 * Endpoints mapeados:
 * - /api/members/*
 * - /api/conditionMembers/*
 * - /api/familyMembers
 */
@WebServlet(urlPatterns = {
        "/api/members/*",
        "/api/conditionMembers/*",
        "/api/familyMembers"
})
public class MemberServlet extends HttpServlet {

    private final Gson gson = new Gson();
    // Reutilizar el almacén extra intermedio si es requerido por herencia académica
    private static final Map<String, Map<String, Object>> extraData = new ConcurrentHashMap<>();
    private final IntegranteDAO integranteDAO = new IntegranteDAOImpl(extraData);

    /**
     * Procesa las solicitudes HTTP GET para obtener información sobre integrantes y afecciones médicas.
     * 
     * Endpoints y Respuestas:
     * - GET /api/familyMembers: Obtiene la lista completa de todos los integrantes de familias registrados en el sistema. Retorna success con List&lt;MemberDTO&gt;.
     * - GET /api/conditionMembers/member/{memberId}: Obtiene la lista de afecciones médicas asociadas a un integrante. Retorna success con lista de afecciones.
     * - GET /api/conditionMembers/{id}: Obtiene los detalles de una afección médica específica por su ID.
     * - GET /api/members/familyPlan/select/{planId}: Obtiene una lista simplificada de integrantes listos para selección en un plan familiar.
     * - GET /api/members/familyPlan/{planId}: Obtiene la lista completa de integrantes asociados a un plan familiar.
     * - GET /api/members/{id}: Obtiene los detalles de un integrante específico por su ID.
     * 
     * @param req Petición HTTP.
     * @param resp Respuesta HTTP en formato JSON.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        try {
            // Manejar /api/familyMembers
            if (servletPath.contains("familyMembers")) {
                ResponseUtil.sendSuccess(resp, integranteDAO.getAllFamilyMembers());
                return;
            }

            // Manejar /api/conditionMembers
            if (servletPath.contains("conditionMembers")) {
                if (pathInfo != null && pathInfo.startsWith("/member/")) {
                    int memberId = Integer.parseInt(pathInfo.substring(8));
                    ResponseUtil.sendSuccess(resp, integranteDAO.getConditionsByMember(memberId));
                } else if (pathInfo != null && !pathInfo.equals("/")) {
                    int condId = Integer.parseInt(pathInfo.substring(1));
                    ResponseUtil.sendSuccess(resp, integranteDAO.getConditionById(condId));
                }
                return;
            }

            // Manejar /api/members
            if (servletPath.contains("members")) {
                if (pathInfo != null && pathInfo.startsWith("/familyPlan/select/")) {
                    int planId = Integer.parseInt(pathInfo.substring(19));
                    ResponseUtil.sendSuccess(resp, integranteDAO.getMembersForSelect(planId));
                } else if (pathInfo != null && pathInfo.startsWith("/familyPlan/")) {
                    int planId = Integer.parseInt(pathInfo.substring(12));
                    ResponseUtil.sendSuccess(resp, integranteDAO.getMembersByFamilyPlan(planId));
                } else if (pathInfo != null && !pathInfo.equals("/")) {
                    int memberId = Integer.parseInt(pathInfo.substring(1));
                    ResponseUtil.sendSuccess(resp, integranteDAO.getMemberById(memberId));
                }
            }
        } catch (Exception e) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    /**
     * Procesa las solicitudes HTTP POST para crear nuevos integrantes o registrar afecciones de salud.
     * 
     * Endpoints y Parámetros:
     * - POST /api/conditionMembers: Registra una nueva afección. Cuerpo JSON: { "member_id": int, "name": String, "dose": String }
     * - POST /api/members/{familyPlanId}: Registra un nuevo integrante en el plan familiar. Cuerpo JSON representando un MemberDTO.
     * 
     * @param req Petición HTTP con JSON en el cuerpo.
     * @param resp Respuesta HTTP con estado de creación y JSON de éxito.
     * @throws IOException Si ocurre un error de E/S o al deserializar el cuerpo.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        try {
            if (servletPath.contains("conditionMembers")) {
                BufferedReader reader = req.getReader();
                Map<String, Object> body = gson.fromJson(reader, Map.class);
                if (body == null) body = new HashMap<>();
                int memberId = ((Number) body.get("member_id")).intValue();
                String name = (String) body.get("name");
                String dose = (String) body.get("dose");

                if (integranteDAO.addCondition(memberId, name, dose)) {
                    resp.setStatus(HttpServletResponse.SC_CREATED);
                    resp.getWriter().write("{\"success\":true,\"message\":\"Afección agregada exitosamente\"}");
                }
                return;
            }

            if (servletPath.contains("members") && pathInfo != null) {
                BufferedReader reader = req.getReader();
                MemberDTO dto = gson.fromJson(reader, MemberDTO.class);
                if (dto == null) dto = new MemberDTO();
                int familyPlanId = Integer.parseInt(pathInfo.substring(1));
                int memberId = integranteDAO.addMember(familyPlanId, dto);
                if (memberId > 0) {
                    resp.setStatus(HttpServletResponse.SC_CREATED);
                    resp.getWriter().write(String.format("{\"success\":true,\"message\":\"Integrante agregado exitosamente\",\"data\":{\"id\":%d}}", memberId));
                }
                return;
            }
        } catch (Exception e) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Procesa las solicitudes HTTP PUT para actualizar información de integrantes existentes o sus afecciones.
     * 
     * Endpoints y Parámetros:
     * - PUT /api/members/{id}: Actualiza los datos de un integrante. Cuerpo JSON representando un MemberDTO.
     * - PUT /api/conditionMembers/{id}: Actualiza los datos de una afección de salud. Cuerpo JSON: { "name": String, "dose": String }
     * 
     * @param req Petición HTTP con JSON en el cuerpo e ID en la ruta.
     * @param resp Respuesta HTTP con JSON de confirmación.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        try {
            int idVal = Integer.parseInt(pathInfo.substring(1));

            if (servletPath.contains("members")) {
                BufferedReader reader = req.getReader();
                MemberDTO dto = gson.fromJson(reader, MemberDTO.class);
                if (dto == null) dto = new MemberDTO();
                if (integranteDAO.updateMember(idVal, dto)) {
                    resp.getWriter().write("{\"success\":true,\"message\":\"Integrante actualizado exitosamente\"}");
                }
                return;
            }

            if (servletPath.contains("conditionMembers")) {
                BufferedReader reader = req.getReader();
                Map<String, Object> body = gson.fromJson(reader, Map.class);
                if (body == null) body = new HashMap<>();
                String name = (String) body.get("name");
                String dose = (String) body.get("dose");
                if (integranteDAO.updateCondition(idVal, name, dose)) {
                    resp.getWriter().write("{\"success\":true,\"message\":\"Afección actualizada exitosamente\"}");
                }
            }
        } catch (Exception e) {
            ResponseUtil.sendError(resp, 500, e.getMessage());
        }
    }

    /**
     * Procesa las solicitudes HTTP DELETE para eliminar integrantes o afecciones médicas.
     * 
     * Endpoints:
     * - DELETE /api/members/{id}: Elimina un integrante por su ID de la base de datos.
     * - DELETE /api/conditionMembers/{id}: Elimina una afección médica por su ID.
     * 
     * @param req Petición HTTP con ID del recurso en la ruta.
     * @param resp Respuesta HTTP con JSON de confirmación.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        try {
            int idVal = Integer.parseInt(pathInfo.substring(1));

            if (servletPath.contains("members")) {
                if (integranteDAO.deleteMember(idVal)) {
                    resp.getWriter().write("{\"success\":true,\"message\":\"Integrante eliminado exitosamente\"}");
                }
                return;
            }

            if (servletPath.contains("conditionMembers")) {
                if (integranteDAO.deleteCondition(idVal)) {
                    resp.getWriter().write("{\"success\":true,\"message\":\"Afección eliminada exitosamente\"}");
                }
            }
        } catch (Exception e) {
            ResponseUtil.sendError(resp, 500, e.getMessage());
        }
    }
}
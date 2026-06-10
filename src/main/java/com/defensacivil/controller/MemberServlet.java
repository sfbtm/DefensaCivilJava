package com.defensacivil.controller;

import com.defensacivil.config.ResponseUtil;
import com.defensacivil.dao.IntegranteDAO;
import com.defensacivil.dao.IntegranteDAOImpl;
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

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        try {
            BufferedReader reader = req.getReader();
            Map<String, Object> body = gson.fromJson(reader, Map.class);
            if (body == null) body = new HashMap<>();

            if (servletPath.contains("conditionMembers")) {
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
                int familyPlanId = Integer.parseInt(pathInfo.substring(1));
                int memberId = integranteDAO.addMember(familyPlanId, body);
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

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        try {
            int idVal = Integer.parseInt(pathInfo.substring(1));
            BufferedReader reader = req.getReader();
            Map<String, Object> body = gson.fromJson(reader, Map.class);

            if (servletPath.contains("members")) {
                if (integranteDAO.updateMember(idVal, body)) {
                    resp.getWriter().write("{\"success\":true,\"message\":\"Integrante actualizado exitosamente\"}");
                }
                return;
            }

            if (servletPath.contains("conditionMembers")) {
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
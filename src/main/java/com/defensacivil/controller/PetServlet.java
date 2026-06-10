package com.defensacivil.controller;

import com.defensacivil.config.ResponseUtil;
import com.defensacivil.dao.MascotaDAO;
import com.defensacivil.dao.MascotaDAOImpl;
import com.defensacivil.dao.PlanComplementarioDAO;
import com.defensacivil.dao.PlanComplementarioDAOImpl;
import com.defensacivil.dto.VaccineDTO;
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

@WebServlet(urlPatterns = {
        "/api/pets/*",
        "/api/petVaccines/*"
})
public class PetServlet extends HttpServlet {

    private final Gson gson = new Gson();
    private static final Map<String, Map<String, Object>> extraData = new ConcurrentHashMap<>();
    private final MascotaDAO mascotaDAO = new MascotaDAOImpl(extraData);
    private final PlanComplementarioDAO planComplementarioDAO = new PlanComplementarioDAOImpl(extraData);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        try {
            if (servletPath.contains("pets")) {
                if (pathInfo != null && pathInfo.startsWith("/familyPlan/")) {
                    int planId = extractId(pathInfo, "/familyPlan");
                    ResponseUtil.sendSuccess(resp, mascotaDAO.getPetsByFamilyPlan(planId));
                } else {
                    int id = extractId(pathInfo, null);
                    ResponseUtil.sendSuccess(resp, mascotaDAO.getPetById(id));
                }
                return;
            }

            if (servletPath.contains("petVaccines")) {
                if (pathInfo != null && pathInfo.startsWith("/pet/")) {
                    int petId = extractId(pathInfo, "/pet");
                    List<VaccineDTO> list = planComplementarioDAO.getVaccinesByPet(petId);
                    ResponseUtil.sendSuccess(resp, list);
                } else {
                    int id = extractId(pathInfo, null);
                    VaccineDTO dto = planComplementarioDAO.getVaccineById(id);
                    if (dto != null) {
                        ResponseUtil.sendSuccess(resp, dto);
                    } else {
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Vacuna no encontrada");
                    }
                }
                return;
            }
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta GET no soportada");
        } catch (Exception e) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String servletPath = req.getServletPath();

        try {
            BufferedReader reader = req.getReader();
            Map<String, Object> body = gson.fromJson(reader, Map.class);
            if (body == null) body = new HashMap<>();

            if (servletPath.contains("pets")) {
                int generatedId = mascotaDAO.insertPet(body);
                if (generatedId > 0) {
                    ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, Map.of("id", generatedId), "Mascota agregada exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al agregar mascota");
                }
                return;
            }

            if (servletPath.contains("petVaccines")) {
                VaccineDTO dto = gson.fromJson(gson.toJson(body), VaccineDTO.class);
                int generatedId = planComplementarioDAO.insertVaccine(dto);
                if (generatedId > 0) {
                    ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, null, "Vacuna agregada exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al agregar vacuna");
                }
                return;
            }
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta POST no soportada");
        } catch (Exception e) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        try {
            int idVal = extractId(pathInfo, null);

            if (servletPath.contains("pets")) {
                if (mascotaDAO.deletePet(idVal)) {
                    ResponseUtil.sendSuccess(resp, "Mascota eliminada exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Mascota no encontrada");
                }
                return;
            }

            if (servletPath.contains("petVaccines")) {
                if (planComplementarioDAO.deleteVaccine(idVal)) {
                    ResponseUtil.sendSuccess(resp, "Vacuna eliminada exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Vacuna no encontrada");
                }
                return;
            }
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta DELETE no soportada");
        } catch (Exception e) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getMethod().equalsIgnoreCase("PATCH")) {
            doPatch(req, resp);
        } else {
            super.service(req, resp);
        }
    }

    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        try {
            BufferedReader reader = req.getReader();
            Map<String, Object> body = gson.fromJson(reader, Map.class);
            if (body == null) body = new HashMap<>();

            int idVal = extractId(pathInfo, null);

            if (servletPath.contains("pets")) {
                if (mascotaDAO.updatePet(idVal, body)) {
                    ResponseUtil.sendSuccess(resp, "Mascota actualizada exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Mascota no encontrada");
                }
                return;
            }

            if (servletPath.contains("petVaccines")) {
                VaccineDTO dto = gson.fromJson(gson.toJson(body), VaccineDTO.class);
                if (planComplementarioDAO.updateVaccine(idVal, dto)) {
                    ResponseUtil.sendSuccess(resp, "Vacuna actualizada exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Vacuna no encontrada");
                }
                return;
            }
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta PATCH no soportada");
        } catch (Exception e) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
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
}

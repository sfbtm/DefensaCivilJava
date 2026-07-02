package com.defensacivil.controller;

import com.defensacivil.config.ResponseUtil;
import com.defensacivil.dao.MascotaDAO;
import com.defensacivil.dao.MascotaDAOImpl;
import com.defensacivil.dao.PlanComplementarioDAO;
import com.defensacivil.dao.PlanComplementarioDAOImpl;
import com.defensacivil.dto.VaccineDTO;
import com.defensacivil.dto.PetDTO;
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
 * Servlet que gestiona el registro de mascotas y sus respectivos carnés de vacunas
 * dentro de los planes familiares de emergencia.
 * Provee operaciones CRUD completas para mascotas y vacunas asociadas.
 * 
 * Endpoints mapeados:
 * - /api/pets/*
 * - /api/petVaccines/*
 */
@WebServlet(urlPatterns = {
        "/api/pets/*",
        "/api/petVaccines/*"
})
public class PetServlet extends HttpServlet {

    private final Gson gson = new Gson();
    private static final Map<String, Map<String, Object>> extraData = new ConcurrentHashMap<>();
    private final MascotaDAO mascotaDAO = new MascotaDAOImpl(extraData);
    private final PlanComplementarioDAO planComplementarioDAO = new PlanComplementarioDAOImpl(extraData);

    /**
     * Procesa las solicitudes HTTP GET para consultar información de mascotas o sus vacunas.
     * 
     * Endpoints y Respuestas:
     * - GET /api/pets/familyPlan/{planId}: Obtiene la lista de mascotas registradas en un plan familiar.
     * - GET /api/pets/{id}: Obtiene los detalles de una mascota específica por su ID.
     * - GET /api/petVaccines/pet/{petId}: Obtiene la lista de vacunas asociadas a una mascota específica.
     * - GET /api/petVaccines/{id}: Obtiene los detalles de un registro de vacuna específico por su ID.
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

    /**
     * Procesa las solicitudes HTTP POST para crear un registro de mascota o de vacuna.
     * 
     * Endpoints y Parámetros:
     * - POST /api/pets: Agrega una mascota. Cuerpo JSON representa un PetDTO.
     * - POST /api/petVaccines: Agrega un carné de vacuna. Cuerpo JSON representa un VaccineDTO.
     * 
     * @param req Petición HTTP con JSON en el cuerpo.
     * @param resp Respuesta HTTP con estado de creación y JSON confirmando la operación.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String servletPath = req.getServletPath();

        try {
            if (servletPath.contains("pets")) {
                BufferedReader reader = req.getReader();
                PetDTO dto = gson.fromJson(reader, PetDTO.class);
                if (dto == null) dto = new PetDTO();
                int generatedId = mascotaDAO.insertPet(dto);
                if (generatedId > 0) {
                    ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, Map.of("id", generatedId), "Mascota agregada exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al agregar mascota");
                }
                return;
            }

            if (servletPath.contains("petVaccines")) {
                BufferedReader reader = req.getReader();
                VaccineDTO dto = gson.fromJson(reader, VaccineDTO.class);
                if (dto == null) dto = new VaccineDTO();
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

    /**
     * Procesa las solicitudes HTTP DELETE para eliminar una mascota o una de sus vacunas.
     * 
     * Endpoints:
     * - DELETE /api/pets/{id}: Elimina una mascota de la base de datos por su ID.
     * - DELETE /api/petVaccines/{id}: Elimina una vacuna de la base de datos por su ID.
     * 
     * @param req Petición HTTP con el ID del recurso en la ruta.
     * @param resp Respuesta HTTP con JSON de confirmación.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
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

    /**
     * Redirige las peticiones HTTP al método adecuado según el verbo, soportando solicitudes HTTP PATCH.
     * 
     * @param req Petición HTTP.
     * @param resp Respuesta HTTP.
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
     * Procesa las solicitudes HTTP PATCH para actualizar datos parciales de mascotas o de sus vacunas.
     * 
     * Endpoints:
     * - PATCH /api/pets/{id}: Actualiza datos de la mascota. Cuerpo JSON representa un PetDTO con campos modificados.
     * - PATCH /api/petVaccines/{id}: Actualiza datos de la vacuna. Cuerpo JSON representa un VaccineDTO con campos modificados.
     * 
     * @param req Petición HTTP con JSON en el cuerpo e ID en la ruta.
     * @param resp Respuesta HTTP con JSON de confirmación.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        try {
            int idVal = extractId(pathInfo, null);

            if (servletPath.contains("pets")) {
                BufferedReader reader = req.getReader();
                PetDTO dto = gson.fromJson(reader, PetDTO.class);
                if (dto == null) dto = new PetDTO();
                if (mascotaDAO.updatePet(idVal, dto)) {
                    ResponseUtil.sendSuccess(resp, "Mascota actualizada exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Mascota no encontrada");
                }
                return;
            }

            if (servletPath.contains("petVaccines")) {
                BufferedReader reader = req.getReader();
                VaccineDTO dto = gson.fromJson(reader, VaccineDTO.class);
                if (dto == null) dto = new VaccineDTO();
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

    /**
     * Extrae un identificador numérico de la información de la ruta (pathInfo) de la petición HTTP,
     * omitiendo opcionalmente un prefijo dado.
     * 
     * @param pathInfo Información de ruta del servlet.
     * @param prefix Prefijo que precede al ID en la ruta.
     * @return ID numérico parseado, o 0 si no es válido.
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
}

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
        "/api/familyMembers",
        "/api/familyMembers/*"
})
public class MemberServlet extends HttpServlet {

    /** Instancia de Gson para parsear datos de peticiones y respuestas. */
    private final Gson gson = new Gson();
    
    /** Almacén intermedio estático en memoria para datos extra. */
    private static final Map<String, Map<String, Object>> extraData = new ConcurrentHashMap<>();
    
    /** Instancia del DAO para operaciones de persistencia de integrantes y condiciones. */
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
        // Establecer el tipo de contenido y la codificación de la respuesta
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        // Obtener la ruta del servlet y la información de la ruta adicional
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        // Bloque try para capturar y manejar cualquier excepción durante la consulta de integrantes o afecciones
        try {
            // Condicional: Validar si la petición corresponde a la lista general de integrantes
            if (servletPath.contains("familyMembers")) {
                // Bloque: Enviar la lista de todos los integrantes de la familia y retornar
                ResponseUtil.sendSuccess(resp, integranteDAO.getAllFamilyMembers());
                return;
            }

            // Condicional: Validar si la petición corresponde al módulo de afecciones de salud
            if (servletPath.contains("conditionMembers")) {
                // Condicional: Si solicita afecciones médicas específicas de un integrante por su ID
                if (pathInfo != null && pathInfo.startsWith("/member/")) {
                    // Bloque: Extraer ID del integrante y enviar su lista de condiciones médicas
                    int memberId = Integer.parseInt(pathInfo.substring(8));
                    ResponseUtil.sendSuccess(resp, integranteDAO.getConditionsByMember(memberId));
                } 
                // Condicional: Si solicita una afección específica por el ID de la afección
                else if (pathInfo != null && !pathInfo.equals("/")) {
                    // Bloque: Extraer ID de la afección y enviar sus detalles
                    int condId = Integer.parseInt(pathInfo.substring(1));
                    ResponseUtil.sendSuccess(resp, integranteDAO.getConditionById(condId));
                }
                return;
            }

            // Condicional: Validar si la petición corresponde al módulo principal de integrantes (members)
            if (servletPath.contains("members")) {
                // Condicional: Si solicita integrantes aptos para selección asociados a un plan
                if (pathInfo != null && pathInfo.startsWith("/familyPlan/select/")) {
                    // Bloque: Extraer ID del plan y enviar la lista simplificada
                    int planId = Integer.parseInt(pathInfo.substring(19));
                    ResponseUtil.sendSuccess(resp, integranteDAO.getMembersForSelect(planId));
                } 
                // Condicional: Si solicita la lista completa de integrantes de un plan familiar
                else if (pathInfo != null && pathInfo.startsWith("/familyPlan/")) {
                    // Bloque: Extraer ID del plan y retornar su lista de integrantes
                    int planId = Integer.parseInt(pathInfo.substring(12));
                    ResponseUtil.sendSuccess(resp, integranteDAO.getMembersByFamilyPlan(planId));
                } 
                // Condicional: Si solicita los detalles de un integrante en específico por su ID
                else if (pathInfo != null && !pathInfo.equals("/")) {
                    // Bloque: Extraer ID del integrante y retornar su información detallada
                    int memberId = Integer.parseInt(pathInfo.substring(1));
                    ResponseUtil.sendSuccess(resp, integranteDAO.getMemberById(memberId));
                }
            }
        } 
        // Catch: Capturar cualquier excepción que ocurra al consultar la base de datos
        catch (Exception e) {
            // Bloque: Retornar código de error interno del servidor con el detalle
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
        // Establecer el tipo de contenido y la codificación de la respuesta
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        // Obtener la ruta del servlet y la información de la ruta adicional
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        // Bloque try para capturar y manejar cualquier excepción durante la inserción de datos
        try {
            // Condicional: Validar si la petición corresponde al registro de condiciones médicas
            if (servletPath.contains("conditionMembers")) {
                // Bloque: Leer el flujo de caracteres de la petición y parsear a un mapa asociativo
                BufferedReader reader = req.getReader();
                Map<String, Object> body = gson.fromJson(reader, Map.class);
                
                // Condicional: Si no hay cuerpo, inicializar un mapa vacío
                if (body == null) {
                    body = new HashMap<>();
                }
                
                // Extraer parámetros obligatorios
                int memberId = ((Number) body.get("member_id")).intValue();
                String name = (String) body.get("name");
                String dose = (String) body.get("dose");

                // Condicional: Si se agrega correctamente la afección mediante el DAO
                if (integranteDAO.addCondition(memberId, name, dose)) {
                    // Bloque: Establecer estado CREATED (201) y responder éxito
                    resp.setStatus(HttpServletResponse.SC_CREATED);
                    resp.getWriter().write("{\"success\":true,\"message\":\"Afección agregada exitosamente\"}");
                }
                return;
            }

            // Condicional: Validar si la petición corresponde a agregar integrantes y se provee información en ruta
            if (servletPath.contains("members") && pathInfo != null) {
                // Bloque: Leer el flujo de caracteres y deserializar a un objeto MemberDTO
                BufferedReader reader = req.getReader();
                MemberDTO dto = gson.fromJson(reader, MemberDTO.class);
                
                // Condicional: Si el DTO es nulo, instanciar un nuevo MemberDTO por defecto
                if (dto == null) {
                    dto = new MemberDTO();
                }
                
                // Obtener ID del plan de la ruta y registrar el nuevo integrante
                int familyPlanId = Integer.parseInt(pathInfo.substring(1));
                int memberId = integranteDAO.addMember(familyPlanId, dto);
                
                // Condicional: Si el integrante fue insertado exitosamente (ID mayor que cero)
                if (memberId > 0) {
                    // Bloque: Establecer estado CREATED (201) y retornar el ID generado en formato JSON
                    resp.setStatus(HttpServletResponse.SC_CREATED);
                    resp.getWriter().write(String.format("{\"success\":true,\"message\":\"Integrante agregado exitosamente\",\"data\":{\"id\":%d}}", memberId));
                }
                return;
            }
        } 
        // Catch: Capturar cualquier excepción de parseo o inserción
        catch (Exception e) {
            // Bloque: Enviar respuesta de error de servidor con el mensaje
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
        // Obtener la ruta del servlet y la información de la ruta adicional
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        // Bloque try para capturar y manejar cualquier excepción durante la actualización de datos
        try {
            // Extraer el ID del recurso que se actualizará desde la ruta
            int idVal = Integer.parseInt(pathInfo.substring(1));

            // Condicional: Validar si la petición es para actualizar un integrante
            if (servletPath.contains("members")) {
                // Bloque: Leer el flujo de caracteres y deserializar a MemberDTO
                BufferedReader reader = req.getReader();
                MemberDTO dto = gson.fromJson(reader, MemberDTO.class);
                
                // Condicional: Si el DTO es nulo, inicializar uno por defecto
                if (dto == null) {
                    dto = new MemberDTO();
                }
                
                // Condicional: Si la actualización en el DAO se realiza de forma exitosa
                if (integranteDAO.updateMember(idVal, dto)) {
                    // Bloque: Retornar mensaje de confirmación de éxito en formato JSON
                    resp.getWriter().write("{\"success\":true,\"message\":\"Integrante actualizado exitosamente\"}");
                }
                return;
            }

            // Condicional: Validar si la petición es para actualizar una afección médica
            if (servletPath.contains("conditionMembers")) {
                // Bloque: Leer el cuerpo de la petición HTTP y deserializarlo a un mapa
                BufferedReader reader = req.getReader();
                Map<String, Object> body = gson.fromJson(reader, Map.class);
                
                // Condicional: Si el cuerpo es nulo, inicializar un mapa vacío
                if (body == null) {
                    body = new HashMap<>();
                }
                
                // Obtener los datos modificados
                String name = (String) body.get("name");
                String dose = (String) body.get("dose");
                
                // Condicional: Si la afección médica se actualiza de forma correcta en el DAO
                if (integranteDAO.updateCondition(idVal, name, dose)) {
                    // Bloque: Retornar mensaje de confirmación en JSON
                    resp.getWriter().write("{\"success\":true,\"message\":\"Afección actualizada exitosamente\"}");
                }
            }
        } 
        // Catch: Capturar cualquier excepción de negocio o base de datos
        catch (Exception e) {
            // Bloque: Enviar error 500 con el mensaje
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
        // Obtener la ruta del servlet y la información de la ruta adicional
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        // Bloque try para capturar y manejar cualquier excepción durante la eliminación de datos
        try {
            // Extraer el ID del recurso que se eliminará
            int idVal = Integer.parseInt(pathInfo.substring(1));

            // Condicional: Validar si la petición es para eliminar un integrante
            if (servletPath.contains("members")) {
                // Condicional: Si se elimina de forma exitosa en el DAO
                if (integranteDAO.deleteMember(idVal)) {
                    // Bloque: Retornar JSON de confirmación exitosa
                    resp.getWriter().write("{\"success\":true,\"message\":\"Integrante eliminado exitosamente\"}");
                }
                return;
            }

            // Condicional: Validar si la petición es para eliminar una afección de salud
            if (servletPath.contains("conditionMembers")) {
                // Condicional: Si se elimina de forma exitosa en el DAO
                if (integranteDAO.deleteCondition(idVal)) {
                    // Bloque: Retornar JSON de confirmación exitosa
                    resp.getWriter().write("{\"success\":true,\"message\":\"Afección eliminada exitosamente\"}");
                }
            }
        } 
        // Catch: Capturar cualquier excepción que ocurra en el borrado
        catch (Exception e) {
            // Bloque: Retornar error interno con estado 500
            ResponseUtil.sendError(resp, 500, e.getMessage());
        }
    }
}
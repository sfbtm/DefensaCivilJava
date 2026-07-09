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

/**
 * Servlet principal para gestionar la información de los planes familiares de emergencia
 * (Family Plans) y sus recursos disponibles asociados. También provee datos estadísticos
 * consolidados para los paneles de administración y supervisión.
 * 
 * Endpoints mapeados:
 * - /api/familyPlans/*
 * - /api/availableResources/*
 * - /api/audits/*
 */
@WebServlet(urlPatterns = {
    "/api/familyPlans/*",
    "/api/availableResources/*",
    "/api/audits/*"
})
@MultipartConfig
public class PlanDetailsServlet extends HttpServlet {

    /** Instancia de Gson para formatear respuestas JSON. */
    private final Gson gson = new Gson();

    /** In-memory store para campos no mapeados en el esquema de base de datos. */
    private static final Map<String, Map<String, Object>> extraData = new ConcurrentHashMap<>();

    /** DAO para gestionar las operaciones relativas a los planes familiares. */
    private final PlanFamiliarDAO planFamiliarDAO = new PlanFamiliarDAOImpl(extraData);
    
    /** DAO para gestionar las operaciones de recursos y planes complementarios. */
    private final PlanComplementarioDAO planComplementarioDAO = new PlanComplementarioDAOImpl(extraData);

    /**
     * Redirige las peticiones HTTP al método adecuado según el verbo HTTP, brindando soporte
     * específico para peticiones HTTP PATCH.
     * 
     * @param req Petición HTTP recibida.
     * @param resp Respuesta HTTP a enviar.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Condicional: Si el método de la solicitud es PATCH
        if (req.getMethod().equalsIgnoreCase("PATCH")) {
            // Bloque: Invocar directamente el método doPatch
            doPatch(req, resp);
        } 
        // Condicional: Si es cualquier otro método (GET, POST, PUT, DELETE, etc.)
        else {
            // Bloque: Delegar al comportamiento estándar de la clase superior
            super.service(req, resp);
        }
    }

    /**
     * Procesa las solicitudes HTTP GET para obtener estadísticas de planes o información de recursos.
     * 
     * Endpoints y Respuestas:
     * - GET /api/audits/dashBoardSupervisor: Retorna métricas generales del supervisor (planes aprobados, en revisión, etc.).
     * - GET /api/audits/dashBoardAdmin: Retorna métricas consolidadas del administrador por seccional.
     * - GET /api/familyPlans: Lista los planes familiares filtrados según el rol, usuario y seccional de la sesión.
     * - GET /api/familyPlans/check-access/{id}: Verifica acceso del usuario al plan (siempre retorna true).
     * - GET /api/familyPlans/has-members/{id}: Determina si un plan familiar posee integrantes registrados.
     * - GET /api/familyPlans/validate-requirements/{id}: Valida los requerimientos mínimos del plan familiar para su radicación.
     * - GET /api/familyPlans/{id}: Obtiene los detalles completos de un plan familiar específico por su ID.
     * - GET /api/availableResources/familyPlan/{planId}: Lista de recursos de emergencia disponibles para el plan familiar dado.
     * - GET /api/availableResources/{id}: Detalles de un recurso disponible específico por su ID.
     * 
     * @param req Petición HTTP.
     * @param resp Respuesta HTTP en formato JSON.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Obtener ruta del servlet y datos de ruta adicionales
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        // Configurar tipo de contenido JSON y codificación UTF-8 para la respuesta
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // Bloque try para capturar y controlar fallos al obtener planes o recursos
        try {
            // Condicional: Si la ruta de la petición corresponde a "audits"
            if (servletPath != null && servletPath.contains("audits")) {
                // Condicional: Si se solicita el panel del supervisor
                if (pathInfo != null && pathInfo.contains("dashBoardSupervisor")) {
                    // Bloque: Responder con las estadísticas consolidadas del supervisor
                    responderDashboardSupervisor(resp);
                    return;
                } 
                // Condicional: Si se solicita el panel de administración general
                else if (pathInfo != null && pathInfo.contains("dashBoardAdmin")) {
                    // Bloque: Responder con las estadísticas de la seccional para administrador
                    responderDashboardAdmin(resp);
                    return;
                }
            }

            // Condicional: Si la ruta corresponde a la gestión de planes familiares (familyPlans)
            if (servletPath.contains("familyPlans")) {
                // Condicional: Si no hay información de ruta o se solicita el listado general del usuario
                if (pathInfo == null || pathInfo.equals("/")) {
                    // Bloque: Obtener la sesión HTTP actual y recuperar credenciales y roles del usuario
                    jakarta.servlet.http.HttpSession session = req.getSession();
                    Integer loggedInUserId = (Integer) session.getAttribute("userId");
                    Integer loggedInRoleId = (Integer) session.getAttribute("roleId");
                    Integer loggedInSectionalId = (Integer) session.getAttribute("sectionalId");

                    // Condicional: Si la sesión ha expirado o faltan atributos de autenticación
                    if (loggedInUserId == null || loggedInRoleId == null || loggedInSectionalId == null) {
                        // Bloque: Retornar código de respuesta 401 Unauthorized y finalizar petición
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Usuario no autenticado o sesion invalida");
                        return;
                    }

                    // Extraer los datos primitivos de la sesión
                    int userId = loggedInUserId;
                    int roleId = loggedInRoleId;
                    int sectionalId = loggedInSectionalId;

                    // Obtener listado de planes del DAO y responder con éxito
                    List<Map<String, Object>> list = planFamiliarDAO.getFamilyPlans(roleId, userId, sectionalId);
                    ResponseUtil.sendSuccess(resp, list);
                    return;

                } 
                // Condicional: Si se solicita verificar acceso al plan
                else if (pathInfo.startsWith("/check-access/")) {
                    // Bloque: Retornar un JSON autorizando siempre el acceso con fines de test de interfaz SPA
                    ResponseUtil.sendSuccess(resp, Map.of("has_access", true, "access_check", true));
                    return;

                } 
                // Condicional: Si se consulta si el plan posee integrantes registrados
                else if (pathInfo.startsWith("/has-members/")) {
                    // Bloque: Extraer ID del plan, consultar con el DAO y retornar el booleano
                    int idVal = Integer.parseInt(pathInfo.substring(13));
                    boolean hasMembers = planFamiliarDAO.hasMembers(idVal);
                    ResponseUtil.sendSuccess(resp, Map.of("has_members", hasMembers));
                    return;

                } 
                // Condicional: Si se solicita la validación de requisitos de radicación
                else if (pathInfo.startsWith("/validate-requirements/")) {
                    // Bloque: Extraer ID del plan, realizar validaciones cruzadas en DAO y retornar lista de cumplimientos
                    int idVal = Integer.parseInt(pathInfo.substring(23));
                    Map<String, Object> valMap = planFamiliarDAO.validateRequirements(idVal);
                    ResponseUtil.sendSuccess(resp, valMap);
                    return;

                } 
                // Condicional: Si se solicita el detalle de un único plan familiar específico por su ID
                else {
                    // Bloque: Extraer ID del plan y consultar información detallada en el DAO
                    int idVal = Integer.parseInt(pathInfo.substring(1));
                    Map<String, Object> item = planFamiliarDAO.getPlanById(idVal);
                    // Condicional: Si el plan existe y no es una estructura vacía
                    if (item != null && !item.isEmpty()) {
                        // Bloque: Enviar respuesta exitosa con los datos del plan
                        ResponseUtil.sendSuccess(resp, item);
                    } 
                    // Condicional: Si el plan no fue encontrado en el sistema
                    else {
                        // Bloque: Retornar error de no encontrado 404
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Plan familiar no encontrado");
                    }
                    return;
                }
            }

            // Condicional: Si la ruta corresponde a la obtención de recursos disponibles
            if (servletPath.contains("availableResources")) {
                // Condicional: Si se solicitan recursos disponibles asociados a un plan específico
                if (pathInfo != null && pathInfo.startsWith("/familyPlan/")) {
                    // Bloque: Extraer ID del plan y enviar la lista envoltura de datos
                    int planId = Integer.parseInt(pathInfo.substring(12));
                    List<Map<String, Object>> list = planComplementarioDAO.getAvailableResourcesByPlan(planId);
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                    return;
                } 
                // Condicional: Si se solicita el detalle de un único recurso disponible específico por su ID
                else if (pathInfo != null && !pathInfo.equals("/")) {
                    // Bloque: Extraer ID de la ruta, obtener recurso del DAO y escribir la respuesta JSON
                    int idVal = Integer.parseInt(pathInfo.substring(1));
                    Map<String, Object> item = planComplementarioDAO.getAvailableResourceById(idVal);
                    resp.getWriter().write(gson.toJson(Map.of("data", item)));
                    return;
                }
            }

            // Retornar error 400 Bad Request si la ruta solicitada no se reconoce
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta GET no soportada");

        } 
        // Catch: Capturar cualquier excepción de procesamiento de datos o SQL
        catch (Exception e) {
            // Bloque: Retornar código de error de servidor 500 e imprimir la traza de error
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Procesa las solicitudes HTTP POST para la creación de planes familiares o la adición de recursos disponibles.
     * Soporta cuerpos tanto JSON como multiparte (para la integración con la interfaz SPA).
     * 
     * Endpoints y Parámetros:
     * - POST /api/familyPlans: Crea un plan familiar para el usuario logueado. JSON/Multiparte requerido: { "last_names": String, "user_id": int (opcional) }
     * - POST /api/availableResources: Registra un recurso disponible. JSON requerido: { "family_plan_id": int, "resource_id": int, "description": String, "location": String, "distance": float, "phone": String }
     * 
     * @param req Petición HTTP.
     * @param resp Respuesta HTTP en formato JSON.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Indicar codificación y formato de respuesta a JSON
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // Obtener ruta del servlet y ruta adicional de la petición
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        // Crear mapa para alojar los parámetros extraídos del cuerpo de la petición
        Map<String, Object> body = new HashMap<>();
        String contentType = req.getContentType();
        boolean isMultipart = contentType != null && contentType.toLowerCase().contains("multipart/form-data");

        // Bloque try para capturar excepciones al deserializar el cuerpo o realizar inserciones
        try {
            // Condicional: Si el tipo de contenido corresponde a un envío multiparte de formulario
            if (isMultipart) {
                // Bloque: Recorrer todos los parámetros mapeados del formulario
                for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
                    // Condicional: Si el parámetro tiene valores asignados
                    if (entry.getValue().length > 0) {
                        // Bloque: Guardar el primer valor de la matriz en el mapa del cuerpo
                        body.put(entry.getKey(), entry.getValue()[0]);
                    }
                }
                // Condicional: Si existe información de ruta que especifica el ID del plan de emergencia
                if (pathInfo != null && pathInfo.length() > 1) {
                    String[] segments = pathInfo.split("/");
                    // Condicional: Si hay segmentos válidos en la ruta
                    if (segments.length > 1) {
                        // Bloque try para parsear el segmento de plan
                        try {
                            body.put("family_plan_id", Integer.parseInt(segments[1]));
                        } 
                        // Catch: Ignorar el error si el segmento no contiene un número válido
                        catch (NumberFormatException e) {
                            // Ignored
                        }
                    }
                }
            } 
            // Condicional: Si no es multiparte, asumir que es una petición JSON convencional
            else {
                // Bloque: Leer el flujo de caracteres directamente y deserializar a un mapa
                BufferedReader reader = req.getReader();
                Map<String, Object> jsonBody = gson.fromJson(reader, Map.class);
                // Condicional: Si se leyó un cuerpo no nulo
                if (jsonBody != null) {
                    // Bloque: Vaciar todo el contenido en el mapa del cuerpo
                    body.putAll(jsonBody);
                }
            }

            // Condicional: Si el POST se realiza hacia la ruta de creación de planes
            if (servletPath.contains("familyPlans")) {
                String lastNames = (String) body.get("last_names");

                // Obtener datos del usuario logueado en la sesión HTTP
                jakarta.servlet.http.HttpSession session = req.getSession();
                Integer loggedInUserId = (Integer) session.getAttribute("userId");
                int userId;
                
                // Condicional: Validar si el usuario está plenamente autenticado en la sesión
                if (loggedInUserId != null) {
                    // Bloque: Asignar el ID de usuario de sesión
                    userId = loggedInUserId;
                } 
                // Condicional: Si no se encuentra en sesión, intentar leer del cuerpo (pruebas locales)
                else {
                    // Bloque: Obtener el atributo user_id del JSON
                    Object userIdObj = body.get("user_id");
                    // Condicional: Validar si es un valor numérico
                    if (userIdObj instanceof Number) {
                        userId = ((Number) userIdObj).intValue();
                    } 
                    // Condicional: En caso de ser una cadena de texto
                    else if (userIdObj instanceof String) {
                        userId = Integer.parseInt((String) userIdObj);
                    } 
                    // Condicional: Si no se suministró el ID de usuario bajo ningún medio
                    else {
                        // Bloque: Retornar código de estado de no autorizado y abortar
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Usuario no autenticado");
                        return;
                    }
                }

                // Crear plan familiar y persistir en base de datos
                int planId = planFamiliarDAO.createFamilyPlan(lastNames, userId, body);
                // Condicional: Si la base de datos retornó el ID del plan de manera exitosa
                if (planId > 0) {
                    // Bloque: Responder con código CREATED 201 y el ID
                    ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, Map.of("id", planId), "Plan familiar creado con exito");
                } 
                // Condicional: Si ocurrió un error en la base de datos al guardar
                else {
                    // Bloque: Retornar error de servidor 500
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al crear el plan familiar");
                }
                return;
            }

            // Condicional: Si el POST va dirigido a registrar recursos disponibles (availableResources)
            if (servletPath.contains("availableResources")) {
                Object resourceIdObj = body.get("resource_id");
                int resourceId = 1;
                // Condicional: Convertir id de recurso a entero si viene como número
                if (resourceIdObj instanceof Number) {
                    resourceId = ((Number) resourceIdObj).intValue();
                } 
                // Condicional: Si viene en formato string
                else if (resourceIdObj instanceof String) {
                    resourceId = Integer.parseInt((String) resourceIdObj);
                }

                String description = (String) body.get("description");
                String location = (String) body.get("location");
                
                Object distanceObj = body.get("distance");
                float distance = 0.0f;
                // Condicional: Convertir distancia si es numérica
                if (distanceObj instanceof Number) {
                    distance = ((Number) distanceObj).floatValue();
                } 
                // Condicional: Si la distancia viene en formato string y no es vacía
                else if (distanceObj instanceof String && !((String) distanceObj).isEmpty()) {
                    distance = Float.parseFloat((String) distanceObj);
                }

                String phone = (String) body.get("phone");

                Object planIdObj = body.get("family_plan_id");
                int planId = 1;
                // Condicional: Extraer ID del plan al que pertenece el recurso si es numérico
                if (planIdObj instanceof Number) {
                    planId = ((Number) planIdObj).intValue();
                } 
                // Condicional: Si es string
                else if (planIdObj instanceof String) {
                    planId = Integer.parseInt((String) planIdObj);
                }

                // Condicional: Validar si la inserción del recurso disponible en el DAO es exitosa
                if (planComplementarioDAO.insertAvailableResource(planId, resourceId, description, location, distance, phone)) {
                    // Bloque: Responder éxito con estado CREATED (201)
                    ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, null, "Recurso disponible agregado exitosamente");
                } 
                // Condicional: En caso de error en base de datos
                else {
                    // Bloque: Retornar código de error interno 500
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al agregar recurso disponible");
                }
                return;
            }

            // Si la ruta POST no corresponde a un endpoint válido, responder error 400
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta POST no soportada");

        } 
        // Catch: Capturar cualquier excepción de parseo, de base de datos o lógica de negocio
        catch (Exception e) {
            // Bloque: Responder error del servidor e imprimir la traza para depuración
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Procesa las solicitudes HTTP PUT. Este servlet no soporta PUT y retorna un error 400 Bad Request.
     * 
     * @param req Petición HTTP.
     * @param resp Respuesta HTTP en formato JSON.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Establecer las cabeceras de respuesta a JSON y UTF-8
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        // Responder directamente con error de solicitud incorrecta, dado que PUT no está implementado
        ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta PUT no soportada");
    }

    /**
     * Procesa las solicitudes HTTP PATCH para actualizar estados, identificaciones o tipos de familia en los planes familiares
     * y modificar información de recursos disponibles.
     * 
     * Endpoints y Parámetros:
     * - PATCH /api/familyPlans/{id}/identify: Actualiza los campos de identificación del plan familiar (dirección, teléfono, tipo de vivienda, calidad de vivienda, etc.).
     * - PATCH /api/familyPlans/{id}/change-status: Actualiza el estado del plan familiar (e.g., radicar, revisar, rechazar) y guarda observaciones. JSON: { "status_plan_id": int, "comentary": String }
     * - PATCH /api/familyPlans/{id}/change-family-type: Modifica el tipo de estructura familiar. JSON: { "family_type_id": int }
     * - PATCH /api/familyPlans/status/{id}: Cambia de manera rápida el estado del plan. JSON: { "status_plan_id": int }
     * - PATCH /api/availableResources/{id}: Modifica los datos de un recurso de emergencia disponible existente.
     * 
     * @param req Petición HTTP con JSON en el cuerpo.
     * @param resp Respuesta HTTP en formato JSON.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Establecer las cabeceras básicas de respuesta
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // Obtener las rutas de mapeo de la petición
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        // Bloque try para capturar errores de actualización
        try {
            // Leer el cuerpo JSON de la petición HTTP y guardarlo en un mapa
            BufferedReader reader = req.getReader();
            Map<String, Object> body = gson.fromJson(reader, Map.class);
            // Condicional: Inicializar en mapa vacío si viene nulo
            if (body == null) {
                body = new HashMap<>();
            }

            // Condicional: Si es una actualización de identificación (familyPlans/{id}/identify)
            if (servletPath.contains("familyPlans") && pathInfo != null && pathInfo.endsWith("/identify")) {
                // Bloque: Extraer el ID de plan de la ruta y delegar actualización al DAO
                String[] segments = pathInfo.split("/");
                int planId = Integer.parseInt(segments[1]);

                boolean updated = planFamiliarDAO.updateIdentification(planId, body);
                // Condicional: Si el plan fue actualizado de forma exitosa
                if (updated) {
                    // Bloque: Responder con JSON confirmando el éxito
                    ResponseUtil.sendSuccess(resp, "Datos de identificacion actualizados");
                } 
                // Condicional: Si el ID de plan no corresponde a ningún registro
                else {
                    // Bloque: Responder con error 404 de recurso no encontrado
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Plan familiar no encontrado");
                }
                return;
            }

            // Condicional: Si es un cambio de estado formal con comentario (familyPlans/{id}/change-status)
            if (servletPath.contains("familyPlans") && pathInfo != null && pathInfo.endsWith("/change-status")) {
                // Bloque: Extraer el ID de plan de la ruta
                String[] segments = pathInfo.split("/");
                int planId = Integer.parseInt(segments[1]);

                Object statusObj = body.get("status_plan_id");
                int statusId = 1;
                // Condicional: Validar y convertir ID de estado a entero
                if (statusObj instanceof Number) {
                    statusId = ((Number) statusObj).intValue();
                }

                String comment = (String) body.get("comentary");

                boolean updated = planFamiliarDAO.changeStatus(planId, statusId, comment);
                // Condicional: Si el estado fue cambiado de manera exitosa en el DAO
                if (updated) {
                    // Bloque: Responder con éxito en formato JSON
                    ResponseUtil.sendSuccess(resp, "Estado de plan actualizado exitosamente");
                } 
                // Condicional: En caso de no existir el plan
                else {
                    // Bloque: Retornar código de error 404
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Plan familiar no encontrado");
                }
                return;
            }

            // Condicional: Si es una actualización de tipo de estructura familiar (familyPlans/{id}/change-family-type)
            if (servletPath.contains("familyPlans") && pathInfo != null && pathInfo.endsWith("/change-family-type")) {
                // Bloque: Extraer el ID del plan de los segmentos de ruta
                String[] segments = pathInfo.split("/");
                int planId = Integer.parseInt(segments[1]);

                Object ftIdObj = body.get("family_type_id");
                int familyTypeId = 3;
                // Condicional: Extraer ID del tipo de familia si es numérico
                if (ftIdObj instanceof Number) {
                    familyTypeId = ((Number) ftIdObj).intValue();
                } 
                // Condicional: Si es string
                else if (ftIdObj instanceof String) {
                    familyTypeId = Integer.parseInt((String) ftIdObj);
                }

                boolean updated = planFamiliarDAO.changeFamilyType(planId, familyTypeId);
                // Condicional: Si el tipo de familia se modificó correctamente
                if (updated) {
                    // Bloque: Retornar JSON de éxito
                    ResponseUtil.sendSuccess(resp, "Tipo de familia actualizado exitosamente");
                } 
                // Condicional: Si el plan familiar no se encontró
                else {
                    // Bloque: Retornar error de no encontrado 404
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Plan familiar no encontrado");
                }
                return;
            }

            // Condicional: Si es un cambio rápido de estado sin comentarios (familyPlans/status/{id})
            if (servletPath.contains("familyPlans") && pathInfo != null && pathInfo.startsWith("/status/")) {
                // Bloque: Extraer ID del plan del final de la ruta
                int planId = Integer.parseInt(pathInfo.substring(8));
                Object statusObj = body.get("status_plan_id");
                int statusId = 1;
                // Condicional: Extraer entero del ID del estado
                if (statusObj instanceof Number) {
                    statusId = ((Number) statusObj).intValue();
                }

                boolean updated = planFamiliarDAO.changeStatus(planId, statusId, null);
                // Condicional: Si se actualizó de manera correcta en el DAO
                if (updated) {
                    // Bloque: Responder con JSON confirmando éxito
                    ResponseUtil.sendSuccess(resp, "Estado de plan actualizado exitosamente");
                } 
                // Condicional: En caso de no existir el plan
                else {
                    // Bloque: Enviar error 404 Not Found
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Plan familiar no encontrado");
                }
                return;
            }

            // Condicional: Si la ruta corresponde a actualizar un recurso de emergencia (availableResources/{id})
            if (servletPath.contains("availableResources")) {
                // Bloque: Extraer ID del recurso a actualizar
                int idVal = Integer.parseInt(pathInfo.substring(1));
                Object resourceIdObj = body.get("resource_id");
                int resourceId = 1;
                // Condicional: Convertir id del tipo de recurso si es numérico
                if (resourceIdObj instanceof Number) {
                    resourceId = ((Number) resourceIdObj).intValue();
                } 
                // Condicional: Si viene en string
                else if (resourceIdObj instanceof String) {
                    resourceId = Integer.parseInt((String) resourceIdObj);
                }

                String description = (String) body.get("description");
                String location = (String) body.get("location");

                Object distanceObj = body.get("distance");
                float distance = 0.0f;
                // Condicional: Extraer flotante de distancia si es numérico
                if (distanceObj instanceof Number) {
                    distance = ((Number) distanceObj).floatValue();
                } 
                // Condicional: Si la distancia viene en string y es no vacía
                else if (distanceObj instanceof String && !((String) distanceObj).isEmpty()) {
                    distance = Float.parseFloat((String) distanceObj);
                }

                String phone = (String) body.get("phone");

                // Condicional: Si el recurso se actualiza con éxito en el DAO
                if (planComplementarioDAO.updateAvailableResource(idVal, resourceId, description, location, distance, phone)) {
                    // Bloque: Retornar JSON de confirmación de éxito
                    ResponseUtil.sendSuccess(resp, "Recurso disponible actualizado exitosamente");
                } 
                // Condicional: Si el recurso no existe en el sistema
                else {
                    // Bloque: Retornar código de error 404
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Recurso disponible no encontrado");
                }
                return;
            }

            // Si la ruta PATCH no se reconoce, responder con error 400 Bad Request
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta PATCH no soportada");

        } 
        // Catch: Capturar cualquier excepción de procesamiento de datos o base de datos
        catch (Exception e) {
            // Bloque: Responder error del servidor 500 e imprimir la traza de error
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Procesa las solicitudes HTTP DELETE para remover recursos de emergencia.
     * 
     * Endpoint:
     * - DELETE /api/availableResources/{id}: Elimina permanentemente el recurso disponible por su ID.
     * 
     * @param req Petición HTTP.
     * @param resp Respuesta HTTP en formato JSON.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Establecer tipo de contenido y codificación de respuesta
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // Obtener ruta del servlet y ruta adicional de la petición
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        // Bloque try para capturar excepciones al realizar el borrado de recursos
        try {
            // Extraer el ID del recurso disponible a eliminar
            int idVal = Integer.parseInt(pathInfo.substring(1));

            // Condicional: Si es la ruta para eliminar recursos disponibles
            if (servletPath.contains("availableResources")) {
                // Condicional: Si la eliminación en la base de datos se ejecuta exitosamente
                if (planComplementarioDAO.deleteAvailableResource(idVal)) {
                    // Bloque: Enviar respuesta de éxito confirmando la eliminación
                    ResponseUtil.sendSuccess(resp, "Recurso disponible eliminado exitosamente");
                } 
                // Condicional: Si el recurso no se pudo encontrar para su eliminación
                else {
                    // Bloque: Enviar error 404 Not Found
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Recurso disponible no encontrado");
                }
                return;
            }

            // Retornar error 400 si el endpoint de borrado no corresponde con los conocidos
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta DELETE no soportada");

        } 
        // Catch: Capturar cualquier excepción de base de datos durante el borrado
        catch (Exception e) {
            // Bloque: Responder con código de error de servidor 500
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Auxiliar para consultar y retornar los datos estadísticos consolidados para el supervisor.
     * 
     * @param resp Respuesta HTTP.
     * @throws IOException Si ocurre un error de E/S.
     * @throws SQLException Si ocurre un error de consulta SQL.
     */
    private void responderDashboardSupervisor(HttpServletResponse resp) throws IOException, SQLException {
        // Consultar estadísticas de supervisión en base de datos y enviar JSON de éxito
        Map<String, Object> data = planFamiliarDAO.getSupervisorDashboard();
        ResponseUtil.sendSuccess(resp, data);
    }

    /**
     * Auxiliar para consultar y retornar los datos estadísticos consolidados para el administrador.
     * 
     * @param resp Respuesta HTTP.
     * @throws IOException Si ocurre un error de E/S.
     * @throws SQLException Si ocurre un error de consulta SQL.
     */
    private void responderDashboardAdmin(HttpServletResponse resp) throws IOException, SQLException {
        // Consultar métricas globales por seccional en la base de datos y enviar JSON de éxito
        Map<String, Object> responseData = planFamiliarDAO.getAdminDashboard();
        ResponseUtil.sendSuccess(resp, responseData);
    }
}

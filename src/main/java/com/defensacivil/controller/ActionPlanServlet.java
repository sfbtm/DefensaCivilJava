package com.defensacivil.controller;

import com.defensacivil.config.ResponseUtil;
import com.defensacivil.dao.PlanComplementarioDAO;
import com.defensacivil.dao.PlanComplementarioDAOImpl;
import com.defensacivil.dto.ActionPlanDTO;
import com.defensacivil.dto.ActionDTO;
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
 * Servlet que gestiona los planes de acción y las acciones asociadas a ellos.
 * Soporta operaciones para consultar, crear, actualizar y eliminar planes de acción
 * y acciones asociadas a través de endpoints REST.
 * 
 * Mapea las siguientes rutas:
 * - /api/actionPlans/*
 * - /api/actionPlanActions/*
 */
@WebServlet(urlPatterns = {
    "/api/actionPlans/*",
    "/api/actionPlanActions/*"
})
public class ActionPlanServlet extends HttpServlet {

    /**
     * Instancia de Gson utilizada para la serialización y deserialización de objetos a formato JSON.
     */
    private final Gson gson = new Gson();

    /**
     * Mapa concurrente que almacena datos extra y configuraciones de prueba compartidos en memoria.
     */
    private static final Map<String, Map<String, Object>> extraData = new ConcurrentHashMap<>();

    /**
     * Objeto de acceso a datos (DAO) encargado de las operaciones de negocio sobre planes complementarios y acciones.
     */
    private final PlanComplementarioDAO planComplementarioDAO = new PlanComplementarioDAOImpl(extraData);

    /**
     * Controla el flujo de las peticiones HTTP redirigiéndolas al método correspondiente.
     * Permite el soporte para el método HTTP PATCH.
     * 
     * @param req Petición HTTP recibida.
     * @param resp Respuesta HTTP a enviar.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Estructura condicional para capturar y procesar el método PATCH que no está soportado por defecto en HttpServlet
        if (req.getMethod().equalsIgnoreCase("PATCH")) {
            // Bloque que se ejecuta si la petición es un PATCH
            doPatch(req, resp);
        } else {
            // Bloque por defecto para otros verbos HTTP soportados tradicionalmente
            super.service(req, resp);
        }
    }

    /**
     * Procesa las solicitudes HTTP GET para obtener información de planes de acción y sus acciones.
     * 
     * Endpoints y Respuestas:
     * - GET /api/actionPlans/familyPlan/boolean/{planId}: Determina si un plan familiar posee un plan de acción. Retorna success: boolean.
     * - GET /api/actionPlans/familyPlan/{planId}: Recupera el plan de acción de un plan familiar. Retorna success: ActionPlanDTO o error 404.
     * - GET /api/actionPlanActions/actionPlan/{actionPlanId}: Recupera la lista de acciones de un plan de acción. Retorna success: List&lt;ActionDTO&gt;.
     * - GET /api/actionPlanActions/{id}: Recupera los detalles de una acción específica por su ID. Retorna success: ActionDTO o error 404.
     * 
     * @param req Petición HTTP.
     * @param resp Respuesta HTTP.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Obtener la ruta del servlet y la información de la ruta adicional (path info)
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        // Configurar tipo de contenido y codificación de caracteres a JSON
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // Bloque try para capturar cualquier excepción durante el procesamiento de la petición GET
        try {
            // Condicional para verificar si la solicitud va dirigida al recurso de planes de acción
            if (servletPath.contains("actionPlans")) {
                // Condicional para verificar si se solicita validar la existencia de un plan de acción por plan familiar
                if (pathInfo != null && pathInfo.startsWith("/familyPlan/boolean/")) {
                    // Bloque ejecutado si la ruta indica una consulta de existencia (booleana)
                    int planId = extractId(pathInfo, "/familyPlan/boolean");
                    boolean exists = planComplementarioDAO.hasActionPlan(planId);
                    ResponseUtil.sendSuccess(resp, exists);
                // Condicional alternativo para verificar si se solicita el plan de acción completo de un plan familiar
                } else if (pathInfo != null && pathInfo.startsWith("/familyPlan/")) {
                    // Bloque ejecutado si la ruta indica la obtención del detalle de un plan de acción
                    int planId = extractId(pathInfo, "/familyPlan");
                    ActionPlanDTO dto = planComplementarioDAO.getActionPlanByPlan(planId);
                    // Condicional para validar si se encontró la información del plan de acción
                    if (dto != null) {
                        // Bloque ejecutado si el DTO no es nulo
                        ResponseUtil.sendSuccess(resp, dto);
                    } else {
                        // Bloque ejecutado si el DTO es nulo (plan de acción no registrado)
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Plan de acción no encontrado");
                    }
                }
                return;
            }

            // Condicional para verificar si la solicitud va dirigida a las acciones dentro de los planes de acción
            if (servletPath.contains("actionPlanActions")) {
                // Condicional para verificar si se solicitan las acciones asociadas a un plan de acción específico
                if (pathInfo != null && pathInfo.startsWith("/actionPlan/")) {
                    // Bloque ejecutado cuando se desea obtener la lista de acciones de un plan de acción
                    int actionPlanId = extractId(pathInfo, "/actionPlan");
                    List<ActionDTO> list = planComplementarioDAO.getActionsByActionPlan(actionPlanId);
                    ResponseUtil.sendSuccess(resp, list);
                } else {
                    // Bloque ejecutado por defecto para obtener la información detallada de una acción por su ID
                    int idVal = extractId(pathInfo, null);
                    ActionDTO dto = planComplementarioDAO.getActionById(idVal);
                    // Condicional para validar si la acción especificada fue encontrada
                    if (dto != null) {
                        // Bloque ejecutado si la acción existe en el almacén de datos
                        ResponseUtil.sendSuccess(resp, dto);
                    } else {
                        // Bloque ejecutado si la acción no existe
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Acción no encontrada");
                    }
                }
                return;
            }

            // Responder con 400 Bad Request si la ruta solicitada no coincide con ningún endpoint soportado
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta GET no soportada");
        } catch (Exception e) {
            // Bloque catch para manejar fallas técnicas e imprevistas enviando un código de error 500
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Procesa las solicitudes HTTP POST para crear nuevos planes de acción o acciones asociadas.
     * 
     * Endpoints y Parámetros en JSON:
     * - POST /api/actionPlans: Crea un nuevo plan de acción. JSON requerido: { "family_plan_id": int, "coordinator_id": int }
     * - POST /api/actionPlanActions: Registra una nueva acción. JSON requerido: { "action_plan_id": int, "member_id": int, "description": String, "action_type_id": int }
     * 
     * @param req Petición HTTP con el JSON en el cuerpo.
     * @param resp Respuesta HTTP.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Configurar tipo de contenido y codificación de caracteres de la respuesta HTTP
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String servletPath = req.getServletPath();
        BufferedReader reader = req.getReader();
        Map<String, Object> body = gson.fromJson(reader, Map.class);
        // Condicional para evitar que el cuerpo de la petición sea nulo instanciando un mapa vacío
        if (body == null) {
            // Bloque ejecutado para inicializar el body y evitar un NullPointerException
            body = new HashMap<>();
        }

        // Bloque try para capturar y procesar excepciones durante la creación del recurso
        try {
            // Condicional para verificar si se solicita la creación de un Plan de Acción
            if (servletPath.contains("actionPlans")) {
                // Bloque ejecutado para procesar la creación de un nuevo plan de acción
                int planId = extractId(body.get("family_plan_id"));
                int coordId = extractId(body.get("coordinator_id"));
                // Condicional para evaluar si la persistencia del plan de acción fue exitosa
                if (planComplementarioDAO.createActionPlan(planId, coordId)) {
                    // Bloque ejecutado si se inserta el plan de acción correctamente
                    ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, null, "Plan de accion creado exitosamente");
                } else {
                    // Bloque ejecutado si ocurre un error a nivel de almacenamiento
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al crear plan de accion");
                }
                return;
            }

            // Condicional para verificar si se solicita la creación de una Acción de Plan de Acción
            if (servletPath.contains("actionPlanActions")) {
                // Bloque ejecutado para procesar la inserción de una nueva acción
                int planAccionId = extractId(body.get("action_plan_id"));
                int memberId = extractId(body.get("member_id"));
                String description = (String) body.get("description");
                int typeId = extractId(body.get("action_type_id"));
                // Condicional/Operador ternario para asignar la etapa basándose en el tipo de acción
                String stage = (typeId == 1) ? "antes" : (typeId == 2 ? "durante" : "despues");

                // Condicional para evaluar si la inserción de la acción fue exitosa en el DAO
                if (planComplementarioDAO.insertAction(planAccionId, memberId, stage, description)) {
                    // Bloque ejecutado si la acción se inserta exitosamente
                    ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, null, "Accion creada exitosamente");
                } else {
                    // Bloque ejecutado si falla el proceso de guardado de la acción
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al crear accion");
                }
                return;
            }

            // Respuesta para solicitudes de rutas no configuradas
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta POST no soportada");
        } catch (Exception e) {
            // Bloque catch para controlar excepciones no deseadas enviando error 500
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Procesa las solicitudes HTTP PATCH para actualizar una acción existente.
     * 
     * Endpoint y Parámetros en JSON:
     * - PATCH /api/actionPlanActions/{id}: Modifica una acción. JSON requerido: { "member_id": int, "description": String }
     * 
     * @param req Petición HTTP con el JSON en el cuerpo.
     * @param resp Respuesta HTTP.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Configurar tipo de contenido y codificación de caracteres de la respuesta HTTP
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        // Bloque try para contener excepciones durante el flujo de actualización
        try {
            BufferedReader reader = req.getReader();
            Map<String, Object> body = gson.fromJson(reader, Map.class);
            // Condicional para verificar y solventar un cuerpo de solicitud nulo
            if (body == null) {
                // Bloque ejecutado para asegurar un mapa vacío en lugar de nulo
                body = new HashMap<>();
            }

            // Condicional para procesar la actualización parcial de una acción
            if (servletPath.contains("actionPlanActions")) {
                // Bloque ejecutado para actualizar los datos de la acción seleccionada
                int idVal = extractId(pathInfo, null);
                int memberId = extractId(body.get("member_id"));
                String description = (String) body.get("description");
                // Condicional para validar el resultado de la actualización de la acción en base de datos
                if (planComplementarioDAO.updateAction(idVal, memberId, description)) {
                    // Bloque ejecutado si la actualización se concreta de forma exitosa
                    ResponseUtil.sendSuccess(resp, "Accion de plan actualizada exitosamente");
                } else {
                    // Bloque ejecutado si no se encuentra la acción a actualizar o no se modifica
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Accion de plan no encontrada");
                }
                return;
            }

            // Respuesta por defecto si el endpoint de PATCH no es el esperado
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta PATCH no soportada");
        } catch (Exception e) {
            // Bloque catch para encapsular errores internos durante la transacción PATCH
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Procesa las solicitudes HTTP DELETE para eliminar acciones.
     * 
     * Endpoint:
     * - DELETE /api/actionPlanActions/{id}: Elimina físicamente la acción identificada por el ID proporcionado.
     * 
     * @param req Petición HTTP con el ID de la acción en la ruta.
     * @param resp Respuesta HTTP.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Configurar el tipo y codificación de caracteres de la respuesta HTTP
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        // Bloque try para capturar y registrar excepciones durante el proceso de eliminación física
        try {
            int idVal = Integer.parseInt(pathInfo.substring(1));

            // Condicional para verificar si la petición de eliminación corresponde a las acciones
            if (servletPath.contains("actionPlanActions")) {
                // Condicional para verificar si la acción pudo ser eliminada físicamente a nivel del DAO
                if (planComplementarioDAO.deleteAction(idVal)) {
                    // Bloque ejecutado si la eliminación fue correcta
                    ResponseUtil.sendSuccess(resp, "Accion de plan de accion eliminada");
                } else {
                    // Bloque ejecutado si la acción no existe o no se eliminó nada
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Accion no encontrada");
                }
                return;
            }

            // Respuesta si la ruta de eliminación no es soportada
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta DELETE no soportada");
        } catch (Exception e) {
            // Bloque catch para gestionar fallos inesperados del servidor en DELETE
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Extrae un identificador numérico de la información de la ruta (pathInfo) de la petición HTTP,
     * opcionalmente omitiendo un prefijo dado.
     * 
     * @param pathInfo Información de ruta del servlet.
     * @param prefix Prefijo que precede al ID en la ruta.
     * @return El ID numérico parseado, o 0 en caso de error de formato o nulidad.
     */
    private int extractId(String pathInfo, String prefix) {
        // Condicional para controlar un pathInfo nulo retornando 0 por defecto
        if (pathInfo == null) {
            // Bloque ejecutado cuando el pathInfo no contiene información
            return 0;
        }
        String path = pathInfo;
        // Condicional para limpiar el prefijo de la ruta si está presente
        if (prefix != null && path.startsWith(prefix)) {
            // Bloque ejecutado para remover el prefijo especificado
            path = path.substring(prefix.length());
        }
        // Condicional para quitar una barra diagonal inicial (slash) si existe
        if (path.startsWith("/")) {
            // Bloque ejecutado para omitir el primer caracter de la cadena
            path = path.substring(1);
        }
        int slashIdx = path.indexOf('/');
        // Condicional para limpiar cualquier información posterior a una barra diagonal interna
        if (slashIdx != -1) {
            // Bloque ejecutado para recortar la ruta y conservar únicamente el fragmento del ID
            path = path.substring(0, slashIdx);
        }
        // Bloque try para capturar errores de formato al convertir la cadena a un valor entero
        try {
            // Bloque de intento de parseo
            return Integer.parseInt(path);
        } catch (NumberFormatException e) {
            // Bloque catch si la cadena no representa un valor numérico entero válido
            return 0;
        }
    }

    /**
     * Extrae un identificador numérico de un objeto de tipo genérico, el cual puede ser
     * numérico o una cadena que represente un entero.
     * 
     * @param obj Objeto que representa el ID.
     * @return El ID numérico parseado, o 0 en caso de error de formato o nulidad.
     */
    private int extractId(Object obj) {
        // Condicional para verificar si el objeto a evaluar es nulo
        if (obj == null) {
            // Bloque ejecutado cuando el objeto no tiene referencia
            return 0;
        }
        // Condicional para determinar si el objeto es una instancia de la clase Number
        if (obj instanceof Number) {
            // Bloque ejecutado para retornar directamente el valor entero del Number
            return ((Number) obj).intValue();
        }
        // Condicional para determinar si el objeto es una instancia de la clase String
        if (obj instanceof String) {
            // Bloque try para controlar posibles excepciones de formato al convertir el String
            try {
                // Bloque de intento de conversión a tipo primitivo int
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                // Bloque catch si el texto no puede ser interpretado como un entero
                return 0;
            }
        }
        return 0;
    }
}

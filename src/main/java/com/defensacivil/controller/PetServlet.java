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

    /** Instancia de Gson para el procesamiento de datos JSON. */
    private final Gson gson = new Gson();
    
    /** Almacén intermedio estático en memoria para simular/gestionar persistencia extra. */
    private static final Map<String, Map<String, Object>> extraData = new ConcurrentHashMap<>();
    
    /** DAO para la gestión de las mascotas en la base de datos. */
    private final MascotaDAO mascotaDAO = new MascotaDAOImpl(extraData);
    
    /** DAO para la gestión de planes complementarios, incluyendo carnés de vacunas. */
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
        // Obtener la ruta del servlet y la información de la ruta adicional
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        // Bloque try para capturar y manejar cualquier excepción durante la obtención de datos de mascotas/vacunas
        try {
            // Condicional: Validar si la petición corresponde a la gestión de mascotas
            if (servletPath.contains("pets")) {
                // Condicional: Si se solicita la lista de mascotas de un plan familiar específico
                if (pathInfo != null && pathInfo.startsWith("/familyPlan/")) {
                    // Bloque: Extraer el ID de plan de la ruta y enviar la lista de mascotas correspondientes
                    int planId = extractId(pathInfo, "/familyPlan");
                    ResponseUtil.sendSuccess(resp, mascotaDAO.getPetsByFamilyPlan(planId));
                } 
                // Condicional: En caso de que no sea por plan, buscar por ID o traer todas
                else {
                    // Bloque: Extraer el ID de la ruta
                    int id = extractId(pathInfo, null);
                    // Condicional: Si se proporcionó un ID válido mayor que 0
                    if (id > 0) {
                        // Bloque: Retornar los detalles de la mascota específica
                        ResponseUtil.sendSuccess(resp, mascotaDAO.getPetById(id));
                    } 
                    // Condicional: Si no se especificó ID (ID <= 0)
                    else {
                        // Bloque: Retornar todas las mascotas del sistema
                        ResponseUtil.sendSuccess(resp, mascotaDAO.getAllPets());
                    }
                }
                return;
            }

            // Condicional: Validar si la petición corresponde a la gestión de vacunas (petVaccines)
            if (servletPath.contains("petVaccines")) {
                // Condicional: Si se solicita vacunas asociadas a una mascota específica
                if (pathInfo != null && pathInfo.startsWith("/pet/")) {
                    // Bloque: Extraer el ID de la mascota y obtener su carné de vacunas
                    int petId = extractId(pathInfo, "/pet");
                    List<VaccineDTO> list = planComplementarioDAO.getVaccinesByPet(petId);
                    ResponseUtil.sendSuccess(resp, list);
                } 
                // Condicional: En caso de solicitar una vacuna individual por su ID de vacuna
                else {
                    // Bloque: Extraer el ID de la vacuna de la ruta y consultar el DAO
                    int id = extractId(pathInfo, null);
                    VaccineDTO dto = planComplementarioDAO.getVaccineById(id);
                    // Condicional: Si se encontró la vacuna en la base de datos
                    if (dto != null) {
                        // Bloque: Enviar respuesta de éxito con los detalles de la vacuna
                        ResponseUtil.sendSuccess(resp, dto);
                    } 
                    // Condicional: Si no se encontró el registro de vacuna
                    else {
                        // Bloque: Retornar error 404 de no encontrado
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Vacuna no encontrada");
                    }
                }
                return;
            }
            
            // Si la ruta GET no coincide con las rutas conocidas, enviar error 400 Bad Request
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta GET no soportada");
        } 
        // Catch: Capturar cualquier excepción de base de datos o lógica de negocio
        catch (Exception e) {
            // Bloque: Enviar código de estado 500 indicando error del servidor
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
        // Obtener la ruta del servlet
        String servletPath = req.getServletPath();

        // Bloque try para capturar y manejar excepciones al momento de insertar mascotas o vacunas
        try {
            // Condicional: Si la petición va dirigida al registro de mascotas
            if (servletPath.contains("pets")) {
                // Bloque: Deserializar el JSON recibido en el cuerpo a un PetDTO
                BufferedReader reader = req.getReader();
                PetDTO dto = gson.fromJson(reader, PetDTO.class);
                
                // Condicional: Inicializar el DTO si se recibió nulo
                if (dto == null) {
                    dto = new PetDTO();
                }
                
                // Insertar mascota a través del DAO y obtener el ID generado
                int generatedId = mascotaDAO.insertPet(dto);
                // Condicional: Si el ID generado es mayor que 0 (inserción exitosa)
                if (generatedId > 0) {
                    // Bloque: Responder con estado CREATED (201) y los detalles del registro creado
                    ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, Map.of("id", generatedId), "Mascota agregada exitosamente");
                } 
                // Condicional: Si el ID generado es 0 o menor (error en la base de datos)
                else {
                    // Bloque: Responder con un error interno del servidor 500
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al agregar mascota");
                }
                return;
            }

            // Condicional: Si la petición va dirigida al registro de vacunas de mascotas
            if (servletPath.contains("petVaccines")) {
                // Bloque: Deserializar el JSON recibido a un VaccineDTO
                BufferedReader reader = req.getReader();
                VaccineDTO dto = gson.fromJson(reader, VaccineDTO.class);
                
                // Condicional: Inicializar el DTO si es nulo
                if (dto == null) {
                    dto = new VaccineDTO();
                }
                
                // Insertar vacuna a través del DAO y obtener el ID generado
                int generatedId = planComplementarioDAO.insertVaccine(dto);
                // Condicional: Si la vacuna fue insertada de forma exitosa
                if (generatedId > 0) {
                    // Bloque: Retornar estado CREATED (201) y el mensaje de confirmación
                    ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, null, "Vacuna agregada exitosamente");
                } 
                // Condicional: En caso de fallo en la persistencia de la vacuna
                else {
                    // Bloque: Retornar error interno del servidor 500
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al agregar vacuna");
                }
                return;
            }
            
            // Si la ruta POST no corresponde a una ruta conocida, responder error 400 Bad Request
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta POST no soportada");
        } 
        // Catch: Capturar cualquier excepción ocurrida en el proceso de inserción
        catch (Exception e) {
            // Bloque: Responder con código de error 500 y el mensaje detallado
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
        // Obtener la ruta del servlet y la información de la ruta adicional
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        // Bloque try para capturar y manejar excepciones al eliminar mascotas o vacunas
        try {
            // Extraer el ID del recurso a eliminar
            int idVal = extractId(pathInfo, null);

            // Condicional: Validar si la petición es para eliminar una mascota
            if (servletPath.contains("pets")) {
                // Condicional: Validar si la mascota fue borrada exitosamente en el DAO
                if (mascotaDAO.deletePet(idVal)) {
                    // Bloque: Enviar respuesta exitosa indicando la eliminación
                    ResponseUtil.sendSuccess(resp, "Mascota eliminada exitosamente");
                } 
                // Condicional: Si no se pudo encontrar la mascota para borrar
                else {
                    // Bloque: Retornar código de error 404 Not Found
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Mascota no encontrada");
                }
                return;
            }

            // Condicional: Validar si la petición es para eliminar una vacuna
            if (servletPath.contains("petVaccines")) {
                // Condicional: Validar si la vacuna fue borrada exitosamente en el DAO
                if (planComplementarioDAO.deleteVaccine(idVal)) {
                    // Bloque: Enviar respuesta de éxito confirmando la eliminación
                    ResponseUtil.sendSuccess(resp, "Vacuna eliminada exitosamente");
                } 
                // Condicional: Si no se encontró la vacuna en el sistema para borrar
                else {
                    // Bloque: Retornar código de error 404 Not Found
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Vacuna no encontrada");
                }
                return;
            }
            
            // Retornar error en caso de que la ruta DELETE no sea soportada
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta DELETE no soportada");
        } 
        // Catch: Capturar cualquier excepción de base de datos en el borrado
        catch (Exception e) {
            // Bloque: Retornar código de error de servidor 500
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
        // Condicional: Detectar si el método HTTP de la petición es un PATCH
        if (req.getMethod().equalsIgnoreCase("PATCH")) {
            // Bloque: Invocar al método doPatch de forma directa
            doPatch(req, resp);
        } 
        // Condicional: Si no es PATCH, derivar el comportamiento a la clase padre HttpServlet
        else {
            // Bloque: Invocar el flujo normal del servlet
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
        // Obtener la ruta del servlet y la información de la ruta adicional
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        // Bloque try para capturar y manejar excepciones durante la actualización parcial por PATCH
        try {
            // Extraer el ID del recurso que se va a actualizar
            int idVal = extractId(pathInfo, null);

            // Condicional: Si la ruta corresponde a la gestión de mascotas
            if (servletPath.contains("pets")) {
                // Bloque: Deserializar el JSON recibido en la petición a PetDTO
                BufferedReader reader = req.getReader();
                PetDTO dto = gson.fromJson(reader, PetDTO.class);
                
                // Condicional: Inicializar el DTO si es nulo
                if (dto == null) {
                    dto = new PetDTO();
                }
                
                // Condicional: Si se actualizan correctamente los campos de la mascota en el DAO
                if (mascotaDAO.updatePet(idVal, dto)) {
                    // Bloque: Retornar mensaje de éxito en formato JSON
                    ResponseUtil.sendSuccess(resp, "Mascota actualizada exitosamente");
                } 
                // Condicional: Si no se encontró la mascota para realizar el parche de datos
                else {
                    // Bloque: Retornar un error 404 de no encontrado
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Mascota no encontrada");
                }
                return;
            }

            // Condicional: Si la ruta corresponde a la actualización parcial de vacunas
            if (servletPath.contains("petVaccines")) {
                // Bloque: Deserializar el JSON del flujo de entrada a VaccineDTO
                BufferedReader reader = req.getReader();
                VaccineDTO dto = gson.fromJson(reader, VaccineDTO.class);
                
                // Condicional: Inicializar el DTO si es nulo
                if (dto == null) {
                    dto = new VaccineDTO();
                }
                
                // Condicional: Si se actualiza correctamente la vacuna en el DAO
                if (planComplementarioDAO.updateVaccine(idVal, dto)) {
                    // Bloque: Retornar respuesta de éxito en JSON
                    ResponseUtil.sendSuccess(resp, "Vacuna actualizada exitosamente");
                } 
                // Condicional: Si no se encuentra la vacuna a actualizar
                else {
                    // Bloque: Retornar código de error 404
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Vacuna no encontrada");
                }
                return;
            }
            
            // Responder error en caso de que la ruta PATCH no sea soportada
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta PATCH no soportada");
        } 
        // Catch: Capturar cualquier excepción de base de datos o lógica de negocio
        catch (Exception e) {
            // Bloque: Retornar error interno del servidor 500
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
        // Condicional: Si pathInfo es nulo, no hay ID que extraer
        if (pathInfo == null) {
            return 0;
        }
        
        String path = pathInfo;
        // Condicional: Si se suministró un prefijo y la ruta comienza con él
        if (prefix != null && path.startsWith(prefix)) {
            // Bloque: Recortar el prefijo de la ruta
            path = path.substring(prefix.length());
        }
        
        // Condicional: Si la ruta empieza con una diagonal '/'
        if (path.startsWith("/")) {
            // Bloque: Eliminar la primera diagonal
            path = path.substring(1);
        }
        
        // Buscar el índice de la siguiente diagonal para omitir parámetros anidados
        int slashIdx = path.indexOf('/');
        // Condicional: Si se encontró otra diagonal
        if (slashIdx != -1) {
            // Bloque: Quedarse únicamente con el segmento anterior a la diagonal
            path = path.substring(0, slashIdx);
        }
        
        // Bloque try para intentar parsear el fragmento resultante a entero
        try {
            return Integer.parseInt(path);
        } 
        // Catch: Capturar excepción si el segmento no contiene un formato de número válido
        catch (NumberFormatException e) {
            // Bloque: Retornar 0 indicando ID inválido
            return 0;
        }
    }
}

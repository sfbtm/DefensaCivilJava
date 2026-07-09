package com.defensacivil.controller;

import com.defensacivil.config.ResponseUtil;
import com.defensacivil.dao.PlanComplementarioDAO;
import com.defensacivil.dao.PlanComplementarioDAOImpl;
import com.defensacivil.dto.HousingInfoDTO;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
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
 * Servlet que gestiona la información gráfica y grafico de las viviendas asociadas a planes familiares.
 * Permite subir archivos de imagen (grafico/gráficos), consultar información gráfica, actualizar
 * descripciones y eliminar estos recursos, además de servir las imágenes almacenadas.
 * 
 * Mapea las siguientes rutas:
 * - /api/housingInfo/*
 * - /api/housingGraphics/*
 * - /storage/*
 */
@WebServlet(urlPatterns = {
        "/api/housingInfo/*",
        "/api/housingGraphics/*",
        "/storage/*"
})
@MultipartConfig
public class HousingGraphicServlet extends HttpServlet {

    /**
     * Instancia de Gson utilizada para serializar y deserializar JSON.
     */
    private final Gson gson = new Gson();

    /**
     * Mapa concurrente que simula el almacenamiento en memoria del estado de la aplicación.
     */
    private static final Map<String, Map<String, Object>> extraData = new ConcurrentHashMap<>();

    /**
     * Objeto de acceso a datos para gestionar información física y lógica de las viviendas y sus grafico.
     */
    private final PlanComplementarioDAO planComplementarioDAO = new PlanComplementarioDAOImpl(extraData);

    /**
     * Redirige las peticiones HTTP al método adecuado según el verbo, brindando soporte
     * específico para peticiones HTTP PATCH.
     * 
     * @param req Petición HTTP recibida.
     * @param resp Respuesta HTTP a enviar.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Condicional para dar soporte al verbo HTTP PATCH
        if (req.getMethod().equalsIgnoreCase("PATCH")) {
            // Bloque ejecutado si es una petición PATCH
            doPatch(req, resp);
        } else {
            // Bloque ejecutado para otros verbos HTTP tradicionales
            super.service(req, resp);
        }
    }

    /**
     * Procesa las solicitudes HTTP GET para servir las imágenes almacenadas o consultar datos de vivienda.
     * 
     * Rutas y respuestas:
     * - GET /storage/{fileName}: Sirve la imagen física del grafico. Si no existe, entrega la imagen por defecto (/public/familia.png).
     * - GET /api/housingInfo/{planId}/type/{typeId}: Obtiene la información de vivienda de un plan familiar y tipo específicos (entorno = 2, interno = 1). Retorna success: HousingInfoDTO.
     * - GET /api/housingGraphics/familyPlan/{planId}: Obtiene la lista de gráficos/grafico asociados a un plan familiar. Retorna success: List&lt;HousingInfoDTO&gt;.
     * - GET /api/housingGraphics/{id}: Obtiene los detalles de un gráfico/grafico de vivienda específico por su ID. Retorna success: HousingInfoDTO.
     * 
     * @param req Petición HTTP.
     * @param resp Respuesta HTTP (JSON o flujo de bytes de imagen).
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        // Condicional para verificar si se solicita servir una imagen guardada en /storage/*
        if (servletPath != null && servletPath.contains("storage")) {
            // Bloque ejecutado para transferir la imagen física al cliente
            String requestedFile = pathInfo != null ? pathInfo.substring(1) : "";
            // Condicional secundario si el pathInfo es nulo pero el servletPath contiene el nombre del archivo
            if (requestedFile.isEmpty() && servletPath.length() > 9) {
                // Bloque para extraer el nombre del archivo del servletPath
                requestedFile = servletPath.substring(9);
            }
            
            String storagePath = getServletContext().getRealPath("/storage");
            java.io.File storageDir = new java.io.File(storagePath);
            // Condicional para crear el directorio de almacenamiento en caso de no existir
            if (!storageDir.exists()) {
                // Bloque ejecutado si no existe la carpeta storage en el servidor
                storageDir.mkdirs();
            }
            
            java.io.File file = new java.io.File(storageDir, requestedFile);
            // Condicional para verificar la existencia del archivo solicitado
            if (!file.exists() || file.isDirectory()) {
                // Bloque ejecutado si el archivo solicitado no existe; se define una imagen fallback
                String fallbackPath = getServletContext().getRealPath("/public/familia.png");
                // Condicional para asegurar que la ruta por defecto no sea nula
                if (fallbackPath != null) {
                    // Bloque ejecutado para cargar el archivo fallback
                    file = new java.io.File(fallbackPath);
                }
            }
            
            // Condicional final para validar si se puede retornar el archivo (solicitado o fallback)
            if (file.exists()) {
                // Bloque ejecutado si el archivo físico está disponible
                String mimeType = getServletContext().getMimeType(file.getName());
                // Condicional para definir tipo MIME por defecto si el servidor no lo reconoce
                if (mimeType == null) {
                    // Bloque ejecutado para forzar tipo MIME de imagen PNG
                    mimeType = "image/png";
                }
                resp.setContentType(mimeType);
                // Bloque try-with-resources para transferir los bytes del archivo a la respuesta HTTP
                try (java.io.FileInputStream in = new java.io.FileInputStream(file);
                     java.io.OutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    // Bucle para leer el archivo en fragmentos de buffer de 4KB y escribirlos en el stream de respuesta
                    while ((bytesRead = in.read(buffer)) != -1) {
                        // Bloque ejecutado por cada fragmento leido
                        out.write(buffer, 0, bytesRead);
                    }
                }
            } else {
                // Bloque ejecutado si no se encuentra ni la imagen original ni la de fallback
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
            return;
        }

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // Bloque try para capturar y procesar excepciones durante las consultas REST
        try {
            // Condicional para procesar consultas de información de vivienda
            if (servletPath.contains("housingInfo")) {
                // Bloque ejecutado para consultar la vivienda por ID de plan y tipo (interno/externo)
                int planId = extractId(pathInfo, null);
                int typeId = extractHousingTypeId(pathInfo);
                HousingInfoDTO dto = planComplementarioDAO.getHousingInfo(planId, typeId);
                // Condicional para evaluar si la consulta devolvió resultados
                if (dto != null) {
                    // Bloque ejecutado si el DTO no es nulo
                    ResponseUtil.sendSuccess(resp, dto);
                } else {
                    // Bloque ejecutado si el DTO es nulo (no existe la vivienda)
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Información de vivienda no encontrada");
                }
                return;
            }

            // Condicional para procesar consultas de gráficos o grafico
            if (servletPath.contains("housingGraphics")) {
                // Condicional para verificar si se consultan todos los grafico de un plan familiar
                if (pathInfo != null && pathInfo.startsWith("/familyPlan/")) {
                    // Bloque ejecutado al solicitar grafico asociados a un plan
                    int planId = extractId(pathInfo, "/familyPlan");
                    List<HousingInfoDTO> list = planComplementarioDAO.getHousingGraphicsByPlan(planId);
                    ResponseUtil.sendSuccess(resp, list);
                } else {
                    // Bloque ejecutado por defecto para consultar un gráfico por su identificador único
                    int idVal = extractId(pathInfo, null);
                    HousingInfoDTO dto = planComplementarioDAO.getHousingGraphicById(idVal);
                    // Condicional para evaluar si se localizó el grafico solicitado
                    if (dto != null) {
                        // Bloque ejecutado si el DTO existe
                        ResponseUtil.sendSuccess(resp, dto);
                    } else {
                        // Bloque ejecutado si el grafico no existe
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Gráfico no encontrado");
                    }
                }
                return;
            }
            // Respuesta para solicitudes GET no soportadas
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta GET no soportada");
        } catch (Exception e) {
            // Bloque catch para capturar errores internos en GET
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Procesa las solicitudes HTTP POST para subir archivos y crear grafico/gráficos de vivienda.
     * Soporta solicitudes multiparte (multipart/form-data) para la subida de imágenes de grafico.
     * 
     * Endpoints:
     * - POST /api/housingInfo o POST /api/housingGraphics: Guarda un gráfico/grafico subido. 
     *   Parámetros (en multipart): family_plan_id (int), description (String), path (archivo de imagen).
     * 
     * @param req Petición HTTP que contiene la imagen y metadatos.
     * @param resp Respuesta HTTP en formato JSON.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        Map<String, Object> body = new HashMap<>();
        String contentType = req.getContentType();
        // Operador condicional/Ternario para determinar si la petición es multipart/form-data
        boolean isMultipart = contentType != null && contentType.toLowerCase().contains("multipart/form-data");

        // Bloque try para capturar excepciones durante la carga de imágenes y metadatos
        try {
            // Condicional para evaluar cómo parsear la petición
            if (isMultipart) {
                // Bloque ejecutado si es una subida de archivos (multipart/form-data)
                // Bucle para iterar sobre los parámetros comunes en la petición multiparte
                for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
                    // Condicional para evitar mapear valores vacíos
                    if (entry.getValue().length > 0) {
                        // Bloque para almacenar el primer valor del parámetro en el body
                        body.put(entry.getKey(), entry.getValue()[0]);
                    }
                }
                // Condicional para extraer el family_plan_id desde la URL en caso de estar presente
                if (pathInfo != null && pathInfo.length() > 1) {
                    // Bloque ejecutado para parsear segmentos de la URL
                    String[] segments = pathInfo.split("/");
                    // Condicional para validar que existan segmentos en la ruta de solicitud
                    if (segments.length > 1) {
                        // Bloque try para controlar errores al parsear el ID del plan de la URL
                        try {
                            // Intento de conversión a entero
                            body.put("family_plan_id", Integer.parseInt(segments[1]));
                        } catch (NumberFormatException e) {
                            // Bloque catch vacío para omitir la falla y buscar el ID de plan por otro medio
                        }
                    }
                }
            } else {
                // Bloque ejecutado para peticiones tradicionales con cuerpo en formato JSON
                BufferedReader reader = req.getReader();
                Map<String, Object> jsonBody = gson.fromJson(reader, Map.class);
                // Condicional para asegurar que el cuerpo JSON deserializado no sea nulo
                if (jsonBody != null) {
                    // Bloque ejecutado para volcar los valores al mapa body
                    body.putAll(jsonBody);
                }
            }

            // Condicional para procesar la subida o inserción lógica del grafico de la vivienda
            if (servletPath.contains("housingInfo") || servletPath.contains("housingGraphics")) {
                // Bloque ejecutado para persistir la información gráfica en el DAO
                boolean esEntorno = servletPath.contains("housingInfo");
                int planId = extractId(body.get("family_plan_id"));
                // Operador condicional ternario para inferir el tipo basándose en la ruta del endpoint
                int typeId = esEntorno ? 2 : 1;
                // Condicional si se suministra explícitamente el tipo de grafico en el body de la petición
                if (body.containsKey("housing_info_type_id")) {
                    // Bloque para extraer el tipo de información gráfica
                    typeId = extractId(body.get("housing_info_type_id"));
                }
                // Condicional si se especifica el tipo de grafico en el pathInfo de la URL
                if (pathInfo != null) {
                    // Bloque ejecutado para extraer el tipo desde la URL
                    typeId = extractHousingTypeId(pathInfo);
                }
                // Operador condicional/Ternario para representar si es entorno en valor numérico (1: Sí, 0: No)
                int esEntornoVal = (typeId == 2) ? 1 : 0;
                // Operador condicional/Ternario para obtener o definir una descripción por defecto
                String description = body.containsKey("description") ? (String) body.get("description") : "Grafico del plan";

                String savedFileName = saveUploadedFile(req);
                // Condicional para verificar el resultado de registrar el grafico en el DAO
                if (planComplementarioDAO.saveOrUpdateHousingGraphic(planId, savedFileName, description, esEntornoVal)) {
                    // Bloque ejecutado si el grafico es guardado con éxito
                    ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, null, "Archivo subido exitosamente");
                } else {
                    // Bloque ejecutado si falla el almacenamiento lógico en la base de datos
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al guardar el grafico/gráfico");
                }
                return;
            }
            // Respuesta para solicitudes POST de rutas incorrectas
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta POST no soportada");
        } catch (Exception e) {
            // Bloque catch para capturar fallos inesperados del servidor durante el procesamiento del POST
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Procesa las solicitudes HTTP DELETE para eliminar un grafico/gráfico de vivienda y su archivo físico.
     * 
     * Endpoint:
     * - DELETE /api/housingGraphics/{id}: Elimina el registro del gráfico por ID y borra el archivo de imagen del almacenamiento.
     * 
     * @param req Petición HTTP.
     * @param resp Respuesta HTTP en formato JSON.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        // Bloque try para capturar y procesar excepciones durante la eliminación del recurso gráfico
        try {
            int idVal = extractId(pathInfo, null);

            // Condicional para evaluar si la petición de eliminación va dirigida a los grafico
            if (servletPath.contains("housingGraphics")) {
                // Bloque ejecutado para consultar y borrar el grafico
                HousingInfoDTO dto = planComplementarioDAO.getHousingGraphicById(idVal);
                // Condicional para validar la existencia del grafico y que la eliminación en la BD sea exitosa
                if (dto != null && planComplementarioDAO.deleteHousingGraphic(idVal)) {
                    // Bloque ejecutado si el registro fue borrado exitosamente de la BD
                    String fileName = dto.getPath();
                    if (fileName != null ) {
                        // Bloque ejecutado para borrar el archivo físico en disco
                        String storagePath = getServletContext().getRealPath("/storage");
                        java.io.File storageDir = new java.io.File(storagePath);
                        java.io.File file = new java.io.File(storageDir, fileName);
                        // Condicional para comprobar si el archivo físico de la imagen existe en disco
                        if (file.exists() && file.isFile()) {
                            // Bloque ejecutado para eliminar físicamente la imagen de la carpeta storage
                            file.delete();
                        }
                    }
                    ResponseUtil.sendSuccess(resp, "Gráfico de vivienda eliminado exitosamente");
                } else {
                    // Bloque ejecutado si no se encuentra el grafico a eliminar
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Gráfico no encontrado");
                }
                return;
            }
            // Respuesta para solicitudes de eliminación de rutas no mapeadas
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta DELETE no soportada");
        } catch (Exception e) {
            // Bloque catch para procesar errores internos al borrar el grafico
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Procesa las solicitudes HTTP PATCH para actualizar la descripción de un gráfico.
     * 
     * Endpoint:
     * - PATCH /api/housingGraphics/{id}/description: Actualiza la descripción. Cuerpo JSON: { "description": String }
     * 
     * @param req Petición HTTP con JSON en el cuerpo.
     * @param resp Respuesta HTTP en formato JSON.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        // Bloque try para capturar fallos durante la actualización parcial de la descripción
        try {
            BufferedReader reader = req.getReader();
            Map<String, Object> body = gson.fromJson(reader, Map.class);
            // Condicional para inicializar el body y prevenir nulos
            if (body == null) {
                // Bloque para asegurar una estructura de mapa vacía
                body = new HashMap<>();
            }

            int idVal = extractId(pathInfo, null);

            // Condicional para validar si se solicita actualizar la descripción del grafico por su ID
            if (servletPath.contains("housingGraphics") && pathInfo != null && pathInfo.endsWith("/description")) {
                // Bloque ejecutado para realizar la actualización de la descripción del grafico
                int graficoId = extractId(pathInfo, null);
                String description = (String) body.get("description");
                // Condicional para evaluar si la actualización en la BD fue exitosa
                if (planComplementarioDAO.updateHousingGraphicDescription(graficoId, description)) {
                    // Bloque ejecutado si la actualización se completa exitosamente
                    ResponseUtil.sendSuccess(resp, "Descripción de gráfico actualizada exitosamente");
                } else {
                    // Bloque ejecutado si no se encuentra el registro del gráfico
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Gráfico no encontrado");
                }
                return;
            }
            // Respuesta para solicitudes PATCH no mapeadas
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta PATCH no soportada");
        } catch (Exception e) {
            // Bloque catch para capturar excepciones técnicas del servidor en PATCH
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Extrae un identificador numérico a partir del pathInfo, omitiendo opcionalmente un prefijo dado.
     * 
     * @param pathInfo Información de ruta de la petición.
     * @param prefix Prefijo a omitir de la ruta.
     * @return ID numérico parseado, o 0 si no es válido.
     */
    private int extractId(String pathInfo, String prefix) {
        // Condicional para validar que pathInfo contenga información
        if (pathInfo == null) {
            // Bloque ejecutado si es nulo
            return 0;
        }
        String path = pathInfo;
        // Condicional para omitir el prefijo indicado
        if (prefix != null && path.startsWith(prefix)) {
            // Bloque ejecutado para recortar la parte del prefijo
            path = path.substring(prefix.length());
        }
        // Condicional para omitir la barra diagonal inicial de la ruta
        if (path.startsWith("/")) {
            // Bloque ejecutado si empieza con "/"
            path = path.substring(1);
        }
        int slashIdx = path.indexOf('/');
        // Condicional para limpiar cualquier ruta secundaria posterior al ID
        if (slashIdx != -1) {
            // Bloque ejecutado para quedarse únicamente con el primer segmento (el ID)
            path = path.substring(0, slashIdx);
        }
        // Bloque try para parsear el valor restante a un valor numérico int
        try {
            // Bloque de intento de parseo
            return Integer.parseInt(path);
        } catch (NumberFormatException e) {
            // Bloque catch ejecutado si la cadena de texto no es un número entero válido
            return 0;
        }
    }

    /**
     * Extrae el ID de tipo de información de vivienda (interno/externo) de la ruta.
     * Por ejemplo, de /api/housingInfo/{planId}/type/{typeId}.
     * 
     * @param pathInfo Información de ruta.
     * @return El ID del tipo extraído (por defecto retorna 2 si no se encuentra).
     */
    private int extractHousingTypeId(String pathInfo) {
        // Condicional para retornar valor predeterminado si el pathInfo es nulo
        if (pathInfo == null) {
            // Bloque ejecutado si es nulo
            return 2;
        }
        String[] segments = pathInfo.split("/");
        // Condicional para verificar la estructura correcta de la ruta para extraer el tipo de vivienda
        if (segments.length >= 4 && "type".equalsIgnoreCase(segments[2])) {
            // Bloque ejecutado si la URL contiene los segmentos esperados
            // Bloque try para parsear de forma segura el ID de tipo
            try {
                // Bloque de intento de parseo
                return Integer.parseInt(segments[3]);
            } catch (NumberFormatException e) {
                // Bloque catch si ocurre una falla en el formato del número en la URL
            }
        }
        return 2;
    }

    /**
     * Convierte un objeto genérico a un entero si es numérico o una cadena numérica.
     * 
     * @param obj Objeto a evaluar.
     * @return Valor numérico entero, o 0 si no es válido.
     */
    private int extractId(Object obj) {
        // Condicional si el objeto suministrado es nulo
        if (obj == null) {
            // Bloque ejecutado si no tiene referencia
            return 0;
        }
        // Condicional si el objeto es instancia directa de Number
        if (obj instanceof Number) {
            // Bloque ejecutado para obtener su valor entero
            return ((Number) obj).intValue();
        }
        // Condicional si el objeto es una cadena de texto (String)
        if (obj instanceof String) {
            // Bloque try para realizar el casteo seguro de String a int
            try {
                // Bloque de conversión de formato
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                // Bloque catch si la cadena no puede representarse numéricamente
                return 0;
            }
        }
        return 0;
    }

    /**
     * Guarda el archivo subido en el almacenamiento del contexto del servlet (/storage).
     * Genera un nombre de archivo único para evitar colisiones.
     * 
     * @param req Petición HTTP que contiene la parte (part) del archivo.
     * @return El nombre del archivo guardado físicamente, o "mock_graphic.png" por defecto.
     */
    private String saveUploadedFile(HttpServletRequest req) {
        String savedFileName = "mock_graphic.png";
        // Bloque try para procesar el archivo cargado en la petición multiparte
        try {
            jakarta.servlet.http.Part filePart = req.getPart("path");
            // Condicional para validar si se incluyó un archivo con la propiedad "path"
            if (filePart != null) {
                // Bloque ejecutado si se suministra el archivo en el POST
                String fileName = filePart.getSubmittedFileName();
                // Condicional para verificar que el nombre del archivo no esté vacío
                if (fileName != null && !fileName.isEmpty()) {
                    // Bloque ejecutado si el nombre del archivo es válido
                    savedFileName = "upload_" + System.currentTimeMillis() + "_" + fileName;
                    String storagePath = getServletContext().getRealPath("/storage");
                    java.io.File storageDir = new java.io.File(storagePath);
                    // Condicional para asegurar que exista el directorio de almacenamiento físico
                    if (!storageDir.exists()) {
                        // Bloque ejecutado para crear la carpeta si es requerido
                        storageDir.mkdirs();
                    }
                    java.io.File fileToSave = new java.io.File(storageDir, savedFileName);
                    // Bloque try-with-resources para transferir la imagen subida al archivo en el servidor
                    try (java.io.InputStream input = filePart.getInputStream();
                         java.io.OutputStream output = new java.io.FileOutputStream(fileToSave)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        // Bucle para transferir bytes de la petición al archivo físico
                        while ((bytesRead = input.read(buffer)) != -1) {
                            // Bloque ejecutado en cada iteración de escritura física en disco
                            output.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Bloque catch para omitir fallas y retornar el nombre de la imagen mock predeterminada
        }
        return savedFileName;
    }
}

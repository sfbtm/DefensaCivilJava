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
 * Servlet que gestiona la información gráfica y croquis de las viviendas asociadas a planes familiares.
 * Permite subir archivos de imagen (croquis/gráficos), consultar información gráfica, actualizar
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

    private final Gson gson = new Gson();
    private static final Map<String, Map<String, Object>> extraData = new ConcurrentHashMap<>();
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
        if (req.getMethod().equalsIgnoreCase("PATCH")) {
            doPatch(req, resp);
        } else {
            super.service(req, resp);
        }
    }

    /**
     * Procesa las solicitudes HTTP GET para servir las imágenes almacenadas o consultar datos de vivienda.
     * 
     * Rutas y respuestas:
     * - GET /storage/{fileName}: Sirve la imagen física del croquis. Si no existe, entrega la imagen por defecto (/public/familia.png).
     * - GET /api/housingInfo/{planId}/type/{typeId}: Obtiene la información de vivienda de un plan familiar y tipo específicos (entorno = 2, interno = 1). Retorna success: HousingInfoDTO.
     * - GET /api/housingGraphics/familyPlan/{planId}: Obtiene la lista de gráficos/croquis asociados a un plan familiar. Retorna success: List&lt;HousingInfoDTO&gt;.
     * - GET /api/housingGraphics/{id}: Obtiene los detalles de un gráfico/croquis de vivienda específico por su ID. Retorna success: HousingInfoDTO.
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

        // Manejar /storage/*
        if (servletPath != null && servletPath.contains("storage")) {
            String requestedFile = pathInfo != null ? pathInfo.substring(1) : "";
            if (requestedFile.isEmpty() && servletPath.length() > 9) {
                requestedFile = servletPath.substring(9);
            }
            
            String storagePath = getServletContext().getRealPath("/storage");
            java.io.File storageDir = new java.io.File(storagePath);
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }
            
            java.io.File file = new java.io.File(storageDir, requestedFile);
            if (!file.exists() || file.isDirectory()) {
                String fallbackPath = getServletContext().getRealPath("/public/familia.png");
                if (fallbackPath != null) {
                    file = new java.io.File(fallbackPath);
                }
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
            if (servletPath.contains("housingInfo")) {
                int planId = extractId(pathInfo, null);
                int typeId = extractHousingTypeId(pathInfo);
                HousingInfoDTO dto = planComplementarioDAO.getHousingInfo(planId, typeId);
                if (dto != null) {
                    ResponseUtil.sendSuccess(resp, dto);
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Información de vivienda no encontrada");
                }
                return;
            }

            if (servletPath.contains("housingGraphics")) {
                if (pathInfo != null && pathInfo.startsWith("/familyPlan/")) {
                    int planId = extractId(pathInfo, "/familyPlan");
                    List<HousingInfoDTO> list = planComplementarioDAO.getHousingGraphicsByPlan(planId);
                    ResponseUtil.sendSuccess(resp, list);
                } else {
                    int idVal = extractId(pathInfo, null);
                    HousingInfoDTO dto = planComplementarioDAO.getHousingGraphicById(idVal);
                    if (dto != null) {
                        ResponseUtil.sendSuccess(resp, dto);
                    } else {
                        ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Gráfico no encontrado");
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
     * Procesa las solicitudes HTTP POST para subir archivos y crear croquis/gráficos de vivienda.
     * Soporta solicitudes multiparte (multipart/form-data) para la subida de imágenes de croquis.
     * 
     * Endpoints:
     * - POST /api/housingInfo o POST /api/housingGraphics: Guarda un gráfico/croquis subido. 
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

            if (servletPath.contains("housingInfo") || servletPath.contains("housingGraphics")) {
                boolean esEntorno = servletPath.contains("housingInfo");
                int planId = extractId(body.get("family_plan_id"));
                int typeId = esEntorno ? 2 : 1;
                if (body.containsKey("housing_info_type_id")) {
                    typeId = extractId(body.get("housing_info_type_id"));
                }
                if (pathInfo != null) {
                    typeId = extractHousingTypeId(pathInfo);
                }
                int esEntornoVal = (typeId == 2) ? 1 : 0;
                String description = body.containsKey("description") ? (String) body.get("description") : "Grafico del plan";

                String savedFileName = saveUploadedFile(req);
                if (planComplementarioDAO.saveOrUpdateHousingGraphic(planId, savedFileName, description, esEntornoVal)) {
                    ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_CREATED, null, "Archivo subido exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al guardar el croquis/gráfico");
                }
                return;
            }
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta POST no soportada");
        } catch (Exception e) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Procesa las solicitudes HTTP DELETE para eliminar un croquis/gráfico de vivienda y su archivo físico.
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

        try {
            int idVal = extractId(pathInfo, null);

            if (servletPath.contains("housingGraphics")) {
                HousingInfoDTO dto = planComplementarioDAO.getHousingGraphicById(idVal);
                if (dto != null && planComplementarioDAO.deleteHousingGraphic(idVal)) {
                    String fileName = dto.getPath();
                    if (fileName != null && !fileName.equals("mock_graphic.png")) {
                        String storagePath = getServletContext().getRealPath("/storage");
                        java.io.File storageDir = new java.io.File(storagePath);
                        java.io.File file = new java.io.File(storageDir, fileName);
                        if (file.exists() && file.isFile()) {
                            file.delete();
                        }
                    }
                    ResponseUtil.sendSuccess(resp, "Gráfico de vivienda eliminado exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Gráfico no encontrado");
                }
                return;
            }
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta DELETE no soportada");
        } catch (Exception e) {
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

        try {
            BufferedReader reader = req.getReader();
            Map<String, Object> body = gson.fromJson(reader, Map.class);
            if (body == null) body = new HashMap<>();

            int idVal = extractId(pathInfo, null);

            if (servletPath.contains("housingGraphics") && pathInfo != null && pathInfo.endsWith("/description")) {
                int graficoId = extractId(pathInfo, null);
                String description = (String) body.get("description");
                if (planComplementarioDAO.updateHousingGraphicDescription(graficoId, description)) {
                    ResponseUtil.sendSuccess(resp, "Descripción de gráfico actualizada exitosamente");
                } else {
                    ResponseUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Gráfico no encontrado");
                }
                return;
            }
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta PATCH no soportada");
        } catch (Exception e) {
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

    /**
     * Extrae el ID de tipo de información de vivienda (interno/externo) de la ruta.
     * Por ejemplo, de /api/housingInfo/{planId}/type/{typeId}.
     * 
     * @param pathInfo Información de ruta.
     * @return El ID del tipo extraído (por defecto retorna 2 si no se encuentra).
     */
    private int extractHousingTypeId(String pathInfo) {
        if (pathInfo == null) return 2;
        String[] segments = pathInfo.split("/");
        if (segments.length >= 4 && "type".equalsIgnoreCase(segments[2])) {
            try {
                return Integer.parseInt(segments[3]);
            } catch (NumberFormatException e) {
                // Fallback
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
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
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
        try {
            jakarta.servlet.http.Part filePart = req.getPart("path");
            if (filePart != null) {
                String fileName = filePart.getSubmittedFileName();
                if (fileName != null && !fileName.isEmpty()) {
                    savedFileName = "upload_" + System.currentTimeMillis() + "_" + fileName;
                    String storagePath = getServletContext().getRealPath("/storage");
                    java.io.File storageDir = new java.io.File(storagePath);
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
        } catch (Exception e) {
            // Fallback
        }
        return savedFileName;
    }
}

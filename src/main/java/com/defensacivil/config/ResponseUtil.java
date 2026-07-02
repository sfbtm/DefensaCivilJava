package com.defensacivil.config;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Clase utilitaria para estandarizar las respuestas JSON de la aplicación.
 * Permite enviar respuestas exitosas y de error de manera estructurada a través
 * del objeto {@link HttpServletResponse}.
 */
public class ResponseUtil {

    private static final Gson gson = new Gson();

    /**
     * Envía una respuesta estructurada de éxito en formato JSON.
     *
     * @param resp    El objeto {@link HttpServletResponse} para escribir la respuesta.
     * @param status  El código de estado HTTP de la respuesta.
     * @param data    Los datos u objeto de carga que se enviarán en la respuesta. Puede ser nulo.
     * @param message Mensaje descriptivo del resultado. Puede ser nulo.
     * @throws IOException Si ocurre un error de E/S al escribir en la respuesta.
     */
    public static void sendSuccess(HttpServletResponse resp, int status, Object data, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        if (message != null) {
            responseMap.put("message", message);
        }
        if (data != null) {
            responseMap.put("data", data);
        }

        resp.getWriter().write(gson.toJson(responseMap));
    }

    /**
     * Envía una respuesta de éxito en formato JSON con estado 200 OK y datos asociados.
     *
     * @param resp El objeto {@link HttpServletResponse} para escribir la respuesta.
     * @param data Los datos que se enviarán en la respuesta.
     * @throws IOException Si ocurre un error de E/S al escribir en la respuesta.
     */
    public static void sendSuccess(HttpServletResponse resp, Object data) throws IOException {
        sendSuccess(resp, HttpServletResponse.SC_OK, data, null);
    }

    /**
     * Envía una respuesta de éxito en formato JSON con estado 200 OK y un mensaje.
     *
     * @param resp    El objeto {@link HttpServletResponse} para escribir la respuesta.
     * @param message El mensaje descriptivo del resultado.
     * @throws IOException Si ocurre un error de E/S al escribir en la respuesta.
     */
    public static void sendSuccess(HttpServletResponse resp, String message) throws IOException {
        sendSuccess(resp, HttpServletResponse.SC_OK, null, message);
    }

    /**
     * Envía una respuesta estructurada de error en formato JSON.
     *
     * @param resp    El objeto {@link HttpServletResponse} para escribir la respuesta.
     * @param status  El código de estado HTTP del error.
     * @param message El mensaje de error explicativo.
     * @throws IOException Si ocurre un error de E/S al escribir en la respuesta.
     */
    public static void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", false);
        responseMap.put("message", message);

        resp.getWriter().write(gson.toJson(responseMap));
    }
}

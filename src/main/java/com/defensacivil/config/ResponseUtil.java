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

    // Instancia de Gson utilizada para serializar objetos Java a cadenas JSON
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
        // Bloque del método para enviar respuesta exitosa detallada
        
        // Establecer el código de estado HTTP de la respuesta
        resp.setStatus(status);
        // Indicar que el contenido de la respuesta será en formato JSON
        resp.setContentType("application/json");
        // Asegurar el encoding en UTF-8 para soporte de caracteres especiales
        resp.setCharacterEncoding("UTF-8");

        // Construir la estructura estándar del JSON de respuesta
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        
        // Bloque condicional: Verificar si se proporcionó un mensaje descriptivo
        if (message != null) {
            // Bloque de mensaje presente: agregar el mensaje al mapa de respuesta
            responseMap.put("message", message);
        }
        
        // Bloque condicional: Verificar si se proporcionaron datos u objeto de carga
        if (data != null) {
            // Bloque de datos presentes: agregar la información de carga al mapa de respuesta
            responseMap.put("data", data);
        }

        // Serializar el mapa de respuesta a JSON y escribirlo directamente en el flujo de salida de la respuesta
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
        // Bloque del método para enviar éxito con datos y código de estado predeterminado
        
        // Redirigir el llamado estableciendo el estado 200 OK y mensaje nulo
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
        // Bloque del método para enviar éxito con mensaje y código de estado predeterminado
        
        // Redirigir el llamado estableciendo el estado 200 OK y datos nulos
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
        // Bloque del método para enviar respuesta de error
        
        // Establecer el código de estado del error HTTP
        resp.setStatus(status);
        // Indicar que la respuesta es de tipo JSON
        resp.setContentType("application/json");
        // Forzar codificación UTF-8
        resp.setCharacterEncoding("UTF-8");

        // Construir la estructura estándar para reportes de error
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", false);
        responseMap.put("message", message);

        // Convertir la estructura a formato JSON y escribir en la respuesta
        resp.getWriter().write(gson.toJson(responseMap));
    }
}

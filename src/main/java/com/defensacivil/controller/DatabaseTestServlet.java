package com.defensacivil.controller;

import com.defensacivil.config.DatabaseConfig;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;

/**
 * Servlet de prueba para validar la conexión con la base de datos.
 * 
 * Endpoint:
 * - /api/db-test
 */
@WebServlet("/api/db-test")
public class DatabaseTestServlet extends HttpServlet {

    /**
     * Procesa solicitudes HTTP GET para verificar si la conexión a la base de datos
     * está activa y configurada correctamente.
     * 
     * Retorna JSON con un mensaje de éxito ("Conexion exitosa") o un estado 500
     * con un mensaje de error ("Error de conexion") en caso de fallo.
     * 
     * @param req Petición HTTP.
     * @param resp Respuesta HTTP con formato JSON.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json");

        // Bloque try-with-resources para obtener y cerrar automáticamente la conexión a la base de datos
        try (Connection conn = DatabaseConfig.getConnection()) {

            // Bloque ejecutado si la conexión se establece exitosamente sin lanzar excepciones
            resp.getWriter().write("""
                    {
                        "data": "Conexion exitosa"
                    }
                    """);

        } catch (Exception e) {

            // Bloque catch ejecutado en caso de fallo de conexión o error SQL
            resp.setStatus(500);

            // Responder con un mensaje JSON de error
            resp.getWriter().write("""
                    {
                        "error": "Error de conexion"
                    }
                    """);

            // Imprimir la traza de la excepción para facilitar el diagnóstico en los logs
            e.printStackTrace();
        }
    }
}
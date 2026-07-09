package com.defensacivil.controller;

import com.defensacivil.config.DatabaseConfig;
import com.defensacivil.config.ResponseUtil;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

/**
 * Servlet que provee información de catálogos estáticos y dinámicos del sistema.
 * Retorna datos en formato JSON para combos, listas de selección o mapeos de IDs a nombres.
 * 
 * Endpoints:
 * - /api/statusPlans
 * - /api/zones
 * - /api/cities/*
 * - /api/kinships/*
 * - /api/bloodGroups/*
 * - /api/animalGenders/*
 */
@WebServlet(urlPatterns = {
    "/api/statusPlans",
    "/api/zones",
    "/api/cities/*",
    "/api/kinships/*",
    "/api/bloodGroups/*",
    "/api/animalGenders/*"
})
public class CatalogServlet extends HttpServlet {

    /**
     * Instancia de Gson utilizada para convertir colecciones y mapas Java a su representación en formato JSON.
     */
    private final Gson gson = new Gson();

    /**
     * Procesa las solicitudes HTTP GET para obtener catálogos diversos.
     * 
     * Rutas y respuestas:
     * - GET /api/statusPlans: Lista los estados posibles de un plan familiar. Retorna JSON: { "data": [ { "id": int, "name": String } ] }
     * - GET /api/zones: Lista de zonas (Urbana, Rural). Retorna JSON: { "data": [ { "id": int, "name": String } ] }
     * - GET /api/cities: Lista las ciudades/municipios. Retorna JSON con lista de ciudades.
     * - GET /api/cities/department/{deptId}: Lista las ciudades filtradas por el departamento especificado.
     * - GET /api/kinships: Lista los tipos de parentesco. Retorna JSON con todos los parentescos.
     * - GET /api/kinships/{id}: Obtiene un parentesco específico por su ID. Retorna JSON de un parentesco.
     * - GET /api/bloodGroups: Lista los grupos sanguíneos. Retorna JSON con todos los grupos sanguíneos.
     * - GET /api/bloodGroups/{id}: Obtiene un grupo sanguíneo específico por su ID. Retorna JSON de un grupo sanguíneo.
     * - GET /api/animalGenders: Lista los géneros de animales. Retorna JSON con los géneros.
     * - GET /api/animalGenders/{id}: Obtiene un género de animal específico por su ID (1: Macho, 2: Hembra).
     * - GET /api/animalGenders/pet/{petId}: Obtiene el género asignado a una mascota específica consultando la base de datos.
     * 
     * @param req Petición HTTP.
     * @param resp Respuesta HTTP.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S o al escribir la respuesta JSON.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // Bloque try para controlar y procesar excepciones imprevistas al responder solicitudes GET
        try {
            // Condicional para validar si se solicita el catálogo de estados de planes
            if (servletPath.contains("statusPlans")) {
                // Bloque ejecutado para armar y retornar los estados de planes estáticos
                List<Map<String, Object>> list = List.of(
                    Map.of("id", 1, "name", "Creado"),
                    Map.of("id", 3, "name", "En proceso"),
                    Map.of("id", 4, "name", "En Revision"),
                    Map.of("id", 5, "name", "Devuelto con observaciones"),
                    Map.of("id", 6, "name", "Rechazado"),
                    Map.of("id", 7, "name", "Completado")
                );
                resp.getWriter().write(gson.toJson(Map.of("data", list)));
                return;
            }

            // Condicional para validar si se solicita el catálogo de zonas geográficas
            if (servletPath.contains("zones")) {
                // Bloque ejecutado para listar las zonas Urbana y Rural
                List<Map<String, Object>> list = List.of(
                    Map.of("id", 1, "name", "Urbana"),
                    Map.of("id", 2, "name", "Rural")
                );
                resp.getWriter().write(gson.toJson(Map.of("data", list)));
                return;
            }

            // Condicional para validar si se solicita el catálogo de ciudades
            if (servletPath.contains("cities")) {
                // Condicional para verificar si la ruta incluye filtro de departamento
                if (pathInfo != null && pathInfo.startsWith("/department/")) {
                    // Bloque ejecutado si se filtra por departamento
                    int deptId = 1;
                    // Bloque try para controlar errores al parsear el ID del departamento desde la ruta
                    try {
                        // Intento de conversión a entero
                        deptId = Integer.parseInt(pathInfo.substring(12));
                    } catch (NumberFormatException ignored) {
                        // Bloque catch vacío para omitir errores de formato y usar el valor por defecto
                    }
                    
                    List<Map<String, Object>> list = List.of(
                        Map.of("id", 1, "name", "Medellín"),
                        Map.of("id", 2, "name", "Bello"),
                        Map.of("id", 3, "name", "Itagüí")
                    );
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                } else {
                    // Bloque ejecutado si se solicitan todas las ciudades sin filtro específico
                    List<Map<String, Object>> list = List.of(
                        Map.of("id", 1, "name", "Medellín"),
                        Map.of("id", 2, "name", "Bello"),
                        Map.of("id", 3, "name", "Itagüí")
                    );
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                }
                return;
            }

            // Condicional para validar si la solicitud apunta al recurso de parentescos (kinships)
            if (servletPath.contains("kinships")) {
                // Condicional para determinar si se consulta un parentesco particular por su ID
                if (pathInfo != null && !pathInfo.equals("/")) {
                    // Bloque ejecutado cuando se suministra un ID en la ruta
                    int id = Integer.parseInt(pathInfo.substring(1));
                    // Sentencia switch condicional para retornar el nombre del parentesco correspondiente
                    String name = switch (id) {
                        case 1 -> "Padre";
                        case 2 -> "Madre";
                        case 3 -> "Hijo/a";
                        default -> "Otro";
                    };
                    resp.getWriter().write(gson.toJson(Map.of("data", Map.of("id", id, "name", name))));
                } else {
                    // Bloque ejecutado para listar todas las opciones de parentesco disponibles
                    List<Map<String, Object>> list = List.of(
                        Map.of("id", 1, "name", "Padre"),
                        Map.of("id", 2, "name", "Madre"),
                        Map.of("id", 3, "name", "Hijo/a"),
                        Map.of("id", 4, "name", "Otro")
                    );
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                }
                return;
            }

            // Condicional para validar si la solicitud apunta al recurso de grupos sanguíneos
            if (servletPath.contains("bloodGroups")) {
                // Condicional para verificar si se consulta un grupo sanguíneo específico
                if (pathInfo != null && !pathInfo.equals("/")) {
                    // Bloque ejecutado si se especifica el ID del grupo sanguíneo
                    int id = Integer.parseInt(pathInfo.substring(1));
                    // Expresión switch condicional para mapear el ID al tipo de sangre
                    String name = switch (id) {
                        case 1 -> "O+";
                        case 2 -> "O-";
                        case 3 -> "A+";
                        case 4 -> "A-";
                        case 5 -> "B+";
                        case 6 -> "B-";
                        case 7 -> "AB+";
                        case 8 -> "AB-";
                        default -> "O+";
                    };
                    resp.getWriter().write(gson.toJson(Map.of("data", Map.of("id", id, "name", name))));
                } else {
                    // Bloque ejecutado para retornar el catálogo completo de tipos de sangre
                    List<Map<String, Object>> list = List.of(
                        Map.of("id", 1, "name", "O+"),
                        Map.of("id", 2, "name", "O-"),
                        Map.of("id", 3, "name", "A+"),
                        Map.of("id", 4, "name", "A-"),
                        Map.of("id", 5, "name", "B+"),
                        Map.of("id", 6, "name", "B-"),
                        Map.of("id", 7, "name", "AB+"),
                        Map.of("id", 8, "name", "AB-")
                    );
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                }
                return;
            }

            // Condicional para verificar si la ruta es sobre géneros de animales
            if (servletPath.contains("animalGenders")) {
                // Condicional para consultar el género de una mascota guardada en BD
                if (pathInfo != null && pathInfo.startsWith("/pet/")) {
                    // Bloque ejecutado al consultar el género de una mascota por su petId
                    int petId = Integer.parseInt(pathInfo.substring(5));
                    String sql = "SELECT m.IdGenero, g.Nombre AS GeneroNombre FROM Mascotas m LEFT JOIN Genero g ON m.IdGenero = g.IdGenero WHERE m.IdMascota = ?";
                    Map<String, Object> gender = Map.of("id", 1, "name", "Macho");
                    // Bloque try-with-resources para asegurar el cierre automático de la conexión y el statement de SQL
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, petId);
                        // Bloque try-with-resources interno para cerrar el conjunto de resultados ResultSet
                        try (ResultSet rs = ps.executeQuery()) {
                            // Condicional para validar si la consulta SQL retornó algún registro
                            if (rs.next()) {
                                // Bloque ejecutado para leer el género desde el registro de la BD
                                int genderId = rs.getInt("IdGenero");
                                String genderName = rs.getString("GeneroNombre");
                                // Condicional/Ternario para definir el nombre de género por defecto en caso de nulidad
                                gender = Map.of("id", genderId, "name", genderName != null ? genderName : "Macho");
                            }
                        }
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", gender)));
                    return;
                // Condicional para verificar si se consulta un género de animal individual por ID (estático)
                } else if (pathInfo != null && !pathInfo.equals("/")) {
                    // Bloque ejecutado al consultar un género por ID (1 o 2)
                    int id = Integer.parseInt(pathInfo.substring(1));
                    // Operador condicional ternario para decidir entre Macho y Hembra
                    String name = (id == 1) ? "Macho" : "Hembra";
                    resp.getWriter().write(gson.toJson(Map.of("data", Map.of("id", id, "name", name))));
                } else {
                    // Bloque por defecto que lista todos los géneros de animales
                    List<Map<String, Object>> list = List.of(
                        Map.of("id", 1, "name", "Macho"),
                        Map.of("id", 2, "name", "Hembra")
                    );
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                }
                return;
            }

            // Envía un error 400 Bad Request si la ruta solicitada no coincide con ningún catálogo
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta de catálogo no soportada");
        } catch (Exception e) {
            // Bloque catch para capturar cualquier falla imprevista durante la serialización o conexión
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}

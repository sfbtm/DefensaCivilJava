package com.defensacivil.controller;

import com.defensacivil.config.DatabaseConfig;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet que gestiona la administración de usuarios del sistema (Voluntarios, Supervisores y Administradores).
 * Provee endpoints públicos para el catálogo de tipos de documento, géneros, seccionales y organizaciones,
 * y endpoints protegidos para solicitudes de aprobación, asignación de roles, cambio de estado y eliminación.
 * 
 * Endpoints mapeados:
 * - /api/users/*
 * - /api/register/*
 * - /api/register
 * - /api/public/document-types/*
 * - /api/public/document-types
 * - /api/public/genders/*
 * - /api/public/genders
 * - /api/public/sectionals/*
 * - /api/public/sectionals
 * - /api/public/organizations/sectional/*
 */
@WebServlet(urlPatterns = {
    "/api/users/*",
    "/api/register/*",
    "/api/register",
    "/api/public/document-types/*",
    "/api/public/document-types",
    "/api/public/genders/*",
    "/api/public/genders",
    "/api/public/sectionals/*",
    "/api/public/sectionals",
    "/api/public/organizations/sectional/*"
})
public class UserServlet extends HttpServlet {

    /** Instancia de Gson para codificación y decodificación de datos JSON. */
    private final Gson gson = new Gson();

    /** ID del usuario logueado en la sesión simulada del sistema. */
    public static int loggedInUserId = 1;
    
    /** ID de la seccional de la Defensa Civil correspondiente al usuario actual. */
    public static int loggedInSectionalId = 1;
    
    /** ID del rol asignado al usuario logueado. */
    public static int loggedInRoleId = 1;

    /**
     * Redirige las peticiones HTTP al método correspondiente según el verbo, soportando solicitudes HTTP PATCH.
     * 
     * @param req Petición HTTP.
     * @param resp Respuesta HTTP.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Condicional: Validar si el método de la solicitud es PATCH
        if (req.getMethod().equalsIgnoreCase("PATCH")) {
            // Bloque: Invocar doPatch directamente para procesar la actualización parcial
            doPatch(req, resp);
        } 
        // Condicional: En caso de ser otro verbo HTTP tradicional
        else {
            // Bloque: Delegar al flujo del ciclo de vida estándar de la superclase
            super.service(req, resp);
        }
    }

    /**
     * Procesa las solicitudes HTTP GET para obtener listados de usuarios, solicitudes de registro o catálogos públicos.
     * 
     * Endpoints y Respuestas:
     * - GET /api/public/document-types: Lista los tipos de documento en formato JSON.
     * - GET /api/public/genders: Lista los géneros de personas registrados en formato JSON.
     * - GET /api/public/sectionals: Lista las seccionales del sistema.
     * - GET /api/public/organizations/sectional/{sectionalId}: Lista las organizaciones correspondientes a la seccional dada.
     * - GET /api/users/requestsAdmins: Lista las solicitudes pendientes (Activo = 3) de usuarios con rol de Voluntario (para administrador).
     * - GET /api/users/requests/supervisors: Lista las solicitudes pendientes (Activo = 3) de usuarios con rol de Supervisor de la seccional del usuario logueado.
     * - GET /api/users/userForSupervisor: Lista los usuarios activos e inactivos con rol de Voluntario bajo la misma seccional.
     * - GET /api/users/userForAdmin: Lista todos los usuarios activos e inactivos (excluyendo administradores) para el panel de administración.
     * - GET /api/users/{id}: Obtiene la ficha detallada de un usuario específico por su ID.
     * 
     * @param req Petición HTTP.
     * @param resp Respuesta HTTP en formato JSON.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Configurar la codificación y el formato JSON para la respuesta
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // Obtener ruta del servlet e información adicional de ruta
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        // 1. PUBLIC CATALOG ENDPOINTS
        // Condicional: Si la ruta de la petición corresponde a obtener tipos de documento
        if (servletPath != null && servletPath.contains("public/document-types")) {
            // Bloque: Consultar base de datos y construir el listado de tipos de documento
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = "SELECT IdDocumentoTipo, Nombre FROM DocumentoTipo";
            
            // Try-with-resources: Establecer conexión, preparar y ejecutar la consulta SQL
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                
                // Loop: Iterar sobre cada registro del conjunto de resultados
                while (rs.next()) {
                    int idVal = rs.getInt("IdDocumentoTipo");
                    String name = rs.getString("Nombre");
                    
                    // Condicional switch para establecer la abreviatura del documento
                    String acronym = switch (idVal) {
                        case 1 -> "CC";
                        case 2 -> "TI";
                        case 3 -> "CE";
                        default -> "CC";
                    };
                    
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", idVal);
                    item.put("name", name);
                    item.put("acronym", acronym);
                    item.put("is_active", 1);
                    list.add(item);
                }
            } 
            // Catch: Capturar cualquier excepción de SQL
            catch (SQLException e) {
                // Bloque: Imprimir la traza de error
                e.printStackTrace();
            }
            
            // Escribir el listado en formato JSON
            resp.getWriter().write(gson.toJson(Map.of("data", list)));
            return;
        }

        // Condicional: Si la ruta corresponde a la obtención de catálogos de géneros
        if (servletPath != null && servletPath.contains("public/genders")) {
            // Bloque: Consultar base de datos y construir listado de géneros
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = "SELECT IdGenero, Nombre FROM Genero";
            
            // Try-with-resources: Conectar, preparar y ejecutar consulta SQL de géneros
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                
                // Loop: Iterar sobre los registros de género de la base de datos
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getInt("IdGenero"));
                    item.put("name", rs.getString("Nombre"));
                    item.put("is_active", 1);
                    list.add(item);
                }
            } 
            // Catch: Capturar excepciones en la consulta SQL de géneros
            catch (SQLException e) {
                // Bloque: Imprimir la traza del error
                e.printStackTrace();
            }
            
            // Responder con el JSON estructurado
            resp.getWriter().write(gson.toJson(Map.of("data", list)));
            return;
        }

        // Condicional: Si la ruta corresponde al catálogo de seccionales del país
        if (servletPath != null && servletPath.contains("public/sectionals")) {
            // Bloque: Consultar base de datos y listar las seccionales disponibles
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = "SELECT IdSeccional, Nombre FROM Seccional";
            
            // Try-with-resources: Establecer conexión, preparar y ejecutar consulta SQL de seccionales
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                
                // Loop: Iterar por cada seccional obtenida de la base de datos
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getInt("IdSeccional"));
                    item.put("name", rs.getString("Nombre"));
                    item.put("is_active", 1);
                    list.add(item);
                }
            } 
            // Catch: Capturar errores SQL
            catch (SQLException e) {
                // Bloque: Imprimir traza del error
                e.printStackTrace();
            }
            
            // Responder con el JSON estructurado de seccionales
            resp.getWriter().write(gson.toJson(Map.of("data", list)));
            return;
        }

        // Condicional: Si la ruta de petición es para obtener organizaciones filtradas por seccional
        if (servletPath != null && servletPath.contains("public/organizations/sectional")) {
            // Bloque: Extraer el ID de la seccional de la ruta y listar sus organizaciones
            int sectionalId = 1;
            // Condicional: Validar si la ruta posee información sobre el ID de la seccional
            if (pathInfo != null && !pathInfo.equals("/")) {
                // Bloque try para parsear el parámetro
                try {
                    sectionalId = Integer.parseInt(pathInfo.substring(1));
                } 
                // Catch: Ignorar el error si no viene en un formato numérico adecuado y usar valor por defecto
                catch (Exception ignored) {}
            }
            
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = "SELECT IdOrganizacion, Nombre FROM Organizacion WHERE IdSeccional = ?";
            
            // Try-with-resources: Conectar, preparar y enlazar parámetros para consultar organizaciones
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                
                ps.setInt(1, sectionalId);
                // Try-with-resources: Ejecutar la consulta SQL y obtener ResultSet
                try (ResultSet rs = ps.executeQuery()) {
                    // Loop: Iterar sobre cada organización encontrada para la seccional
                    while (rs.next()) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", rs.getInt("IdOrganizacion"));
                        item.put("name", rs.getString("Nombre"));
                        item.put("is_active", 1);
                        list.add(item);
                    }
                }
            } 
            // Catch: Capturar excepciones SQL
            catch (SQLException e) {
                // Bloque: Imprimir traza del error
                e.printStackTrace();
            }
            
            // Responder JSON de organizaciones asociadas
            resp.getWriter().write(gson.toJson(Map.of("data", list)));
            return;
        }

        // 2. PROTECTED AND REQUEST ENDPOINTS
        // Condicional: Si no se provee información de ruta para endpoints protegidos
        if (pathInfo == null || pathInfo.equals("/")) {
            // Bloque: Retornar código de error 400 indicando requerimiento de ID
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"ID o ruta requerida\"}");
            return;
        }

        // Condicional: Si la ruta solicita solicitudes pendientes para los administradores
        if (pathInfo.equals("/requestsAdmins")) {
            // Bloque: Consultar usuarios pendientes (Activo = 3) que son de rol Voluntario
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = """
                SELECT u.IdUsuario, u.Nombre, u.Email, u.Documento, dt.Nombre AS DocumentoTipoNombre, 
                       o.Nombre AS OrganizacionNombre, s.Nombre AS SeccionalNombre
                FROM Usuario u
                LEFT JOIN DocumentoTipo dt ON u.IdDocumentoTipo = dt.IdDocumentoTipo
                LEFT JOIN Organizacion o ON u.IdOrganizacion = o.IdOrganizacion
                LEFT JOIN Seccional s ON COALESCE(u.IdSeccional, o.IdSeccional) = s.IdSeccional
                WHERE u.Activo = 3 AND u.IdRol = 2
                ORDER BY u.IdUsuario DESC
                """;
                
            // Try-with-resources: Conectar, preparar y ejecutar consulta SQL de solicitudes para admins
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                
                // Loop: Iterar sobre cada solicitud de voluntario pendiente
                while (rs.next()) {
                    int idVal = rs.getInt("IdUsuario");
                    String docTypeAcronym = "CC";
                    String docTypeName = rs.getString("DocumentoTipoNombre");
                    // Condicional: Si el nombre del tipo de documento es no nulo
                    if (docTypeName != null) {
                        // Condicional: Asignar sigla según coincidencia de nombre
                        if (docTypeName.contains("Identidad")) {
                            docTypeAcronym = "TI";
                        } 
                        // Condicional: Si es cédula de extranjería
                        else if (docTypeName.contains("Extranjería")) {
                            docTypeAcronym = "CE";
                        }
                    }

                    Map<String, Object> item = new HashMap<>();
                    item.put("id", idVal);
                    item.put("full_name", rs.getString("Nombre"));
                    item.put("email", rs.getString("Email"));
                    item.put("organization", rs.getString("OrganizacionNombre") != null ? rs.getString("OrganizacionNombre") : "Ninguna");
                    item.put("sectional", rs.getString("SeccionalNombre") != null ? rs.getString("SeccionalNombre") : "Ninguna");
                    item.put("document_number", rs.getString("Documento") + " " + docTypeAcronym);
                    list.add(item);
                }
            } 
            // Catch: Capturar excepciones SQL
            catch (SQLException e) {
                // Bloque: Imprimir traza del error
                e.printStackTrace();
            }
            
            // Armar respuesta con paginado simulado
            Map<String, Object> response = new HashMap<>();
            response.put("data", list);
            response.put("paginate", Map.of(
                "current_page", 1,
                "per_page", 10,
                "total", list.size(),
                "last_page", 1
            ));
            resp.getWriter().write(gson.toJson(response));
            return;
        }

        // Condicional: Si la ruta solicita solicitudes pendientes para los supervisores
        if (pathInfo.equals("/requests/supervisors")) {
            // Bloque: Consultar solicitudes de voluntarios de la seccional asignada al supervisor
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = """
                SELECT u.IdUsuario, u.Nombre, u.Email, u.Documento, dt.Nombre AS DocumentoTipoNombre, 
                       o.Nombre AS OrganizacionNombre, s.Nombre AS SeccionalNombre
                FROM Usuario u
                LEFT JOIN DocumentoTipo dt ON u.IdDocumentoTipo = dt.IdDocumentoTipo
                LEFT JOIN Organizacion o ON u.IdOrganizacion = o.IdOrganizacion
                LEFT JOIN Seccional s ON COALESCE(u.IdSeccional, o.IdSeccional) = s.IdSeccional
                WHERE u.Activo = 3 AND u.IdRol = 3 AND COALESCE(u.IdSeccional, o.IdSeccional) = ?
                ORDER BY u.IdUsuario DESC
                """;
                
            // Try-with-resources: Conectar, preparar y enlazar parámetros para consultar solicitudes por seccional
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                
                ps.setInt(1, loggedInSectionalId);
                // Try-with-resources: Ejecutar la consulta SQL
                try (ResultSet rs = ps.executeQuery()) {
                    // Loop: Iterar sobre solicitudes del supervisor
                    while (rs.next()) {
                        int idVal = rs.getInt("IdUsuario");
                        String docTypeAcronym = "CC";
                        String docTypeName = rs.getString("DocumentoTipoNombre");
                        // Condicional: Evaluar la sigla según el tipo de documento
                        if (docTypeName != null) {
                            // Condicional: Asignar abreviatura
                            if (docTypeName.contains("Identidad")) {
                                docTypeAcronym = "TI";
                            } 
                            // Condicional: Si es cédula de extranjería
                            else if (docTypeName.contains("Extranjería")) {
                                docTypeAcronym = "CE";
                            }
                        }

                        Map<String, Object> item = new HashMap<>();
                        item.put("id", idVal);
                        item.put("full_name", rs.getString("Nombre"));
                        item.put("email", rs.getString("Email"));
                        item.put("organization", rs.getString("OrganizacionNombre") != null ? rs.getString("OrganizacionNombre") : "Ninguna");
                        item.put("sectional", rs.getString("SeccionalNombre") != null ? rs.getString("SeccionalNombre") : "Ninguna");
                        item.put("document_number", rs.getString("Documento") + " " + docTypeAcronym);
                        list.add(item);
                    }
                }
            } 
            // Catch: Capturar excepciones SQL
            catch (SQLException e) {
                // Bloque: Imprimir traza del error
                e.printStackTrace();
            }
            
            // Armar respuesta con paginado simulado para supervisor
            Map<String, Object> response = new HashMap<>();
            response.put("data", list);
            response.put("paginate", Map.of(
                "current_page", 1,
                "per_page", 10,
                "total", list.size(),
                "last_page", 1
            ));
            resp.getWriter().write(gson.toJson(response));
            return;
        }

        // Condicional: Si solicita los usuarios voluntarios bajo la seccional del supervisor (userForSupervisor)
        if (pathInfo.equals("/userForSupervisor")) {
            // Bloque: Consultar usuarios de rol Voluntario (no en estado petición) de la misma seccional
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = """
                SELECT u.IdUsuario, u.Nombre, u.Email, u.Documento, dt.Nombre AS DocumentoTipoNombre, 
                       o.Nombre AS OrganizacionNombre, s.Nombre AS SeccionalNombre, u.Activo, u.IdRol, r.Nombre AS RolNombre
                FROM Usuario u
                LEFT JOIN DocumentoTipo dt ON u.IdDocumentoTipo = dt.IdDocumentoTipo
                LEFT JOIN Organizacion o ON u.IdOrganizacion = o.IdOrganizacion
                LEFT JOIN Seccional s ON COALESCE(u.IdSeccional, o.IdSeccional) = s.IdSeccional
                LEFT JOIN Rol r ON u.IdRol = r.IdRol
                WHERE u.Activo != 3 AND u.IdRol = 3 AND COALESCE(u.IdSeccional, o.IdSeccional) = ?
                ORDER BY u.IdUsuario DESC
                """;
                
            // Try-with-resources: Conectar y preparar consulta con parámetro de seccional
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                
                ps.setInt(1, loggedInSectionalId);
                // Try-with-resources: Ejecutar y obtener ResultSet
                try (ResultSet rs = ps.executeQuery()) {
                    // Loop: Iterar sobre voluntarios registrados de la seccional
                    while (rs.next()) {
                        int idVal = rs.getInt("IdUsuario");
                        int activeVal = rs.getInt("Activo");
                        String docTypeAcronym = "CC";
                        String docTypeName = rs.getString("DocumentoTipoNombre");
                        // Condicional: Evaluar sigla de documento
                        if (docTypeName != null) {
                            // Condicional: Validar coincidencia de nombre
                            if (docTypeName.contains("Identidad")) {
                                docTypeAcronym = "TI";
                            } 
                            // Condicional: Si es extranjería
                            else if (docTypeName.contains("Extranjería")) {
                                docTypeAcronym = "CE";
                            }
                        }

                        Map<String, Object> item = new HashMap<>();
                        item.put("id", idVal);
                        item.put("full_name", rs.getString("Nombre"));
                        item.put("email", rs.getString("Email"));
                        item.put("organization", rs.getString("OrganizacionNombre") != null ? rs.getString("OrganizacionNombre") : "Ninguna");
                        item.put("sectional", rs.getString("SeccionalNombre") != null ? rs.getString("SeccionalNombre") : "Ninguna");
                        item.put("document_number", rs.getString("Documento") + " " + docTypeAcronym);
                        item.put("state_user", (activeVal == 1) ? "Activo" : "Desactivado");
                        item.put("state_user_id", activeVal);
                        item.put("rol", rs.getString("RolNombre"));
                        item.put("rol_id", rs.getInt("IdRol"));
                        list.add(item);
                    }
                }
            } 
            // Catch: Capturar excepciones SQL
            catch (SQLException e) {
                // Bloque: Imprimir traza del error
                e.printStackTrace();
            }
            
            // Armar respuesta con paginado simulado
            Map<String, Object> response = new HashMap<>();
            response.put("data", list);
            response.put("paginate", Map.of(
                "current_page", 1,
                "per_page", 10,
                "total", list.size(),
                "last_page", 1
            ));
            resp.getWriter().write(gson.toJson(response));
            return;
        }

        // Condicional: Si solicita los usuarios voluntarios y supervisores para el panel de administración
        if (pathInfo.equals("/userForAdmin")) {
            // Bloque: Consultar todos los usuarios activos/desactivados que no son administradores (IdRol != 1)
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = """
                    SELECT 
                        u.IdUsuario, u.Nombre, u.Documento, u.Email, u.Activo, u.IdRol,
                        r.Nombre AS RolNombre,
                        s.Nombre AS SeccionalNombre,
                        o.Nombre AS OrganizacionNombre
                    FROM Usuario u
                    LEFT JOIN Rol r ON u.IdRol = r.IdRol
                    LEFT JOIN Organizacion o ON u.IdOrganizacion = o.IdOrganizacion
                    LEFT JOIN Seccional s ON COALESCE(u.IdSeccional, o.IdSeccional) = s.IdSeccional
                    WHERE u.Activo != 3 AND u.IdRol != 1
                    ORDER BY u.IdUsuario DESC
                    """;

            // Try-with-resources: Conectar, preparar y ejecutar consulta SQL de usuarios para el admin
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                // Loop: Iterar sobre cada usuario encontrado
                while (rs.next()) {
                    int idVal = rs.getInt("IdUsuario");
                    int activeVal = rs.getInt("Activo");
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", idVal);
                    user.put("full_name", rs.getString("Nombre"));
                    user.put("document_number", rs.getString("Documento"));
                    user.put("email", rs.getString("Email"));
                    user.put("rol", rs.getString("RolNombre"));
                    user.put("state_user", (activeVal == 1) ? "Activo" : "Desactivado");
                    user.put("state_user_id", activeVal);
                    user.put("sectional", rs.getString("SeccionalNombre") != null ? rs.getString("SeccionalNombre") : "Ninguna");
                    user.put("organization", rs.getString("OrganizacionNombre") != null ? rs.getString("OrganizacionNombre") : "Ninguna");
                    list.add(user);
                }

                // Armar respuesta con paginado simulado
                Map<String, Object> response = new HashMap<>();
                response.put("data", list);
                response.put("paginate", Map.of(
                    "current_page", 1,
                    "per_page", 10,
                    "total", list.size(),
                    "last_page", 1
                ));

                resp.getWriter().write(gson.toJson(response));

            } 
            // Catch: Capturar cualquier error SQL durante la consulta
            catch (SQLException e) {
                // Bloque: Responder con error 500 y registrar el error en la consola
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"success\":false,\"message\":\"Error de base de datos\"}");
                e.printStackTrace();
            }
        } 
        // Condicional: Si no coincide con rutas previas, asumir consulta de ficha de usuario por ID (/api/users/{id})
        else {
            // Bloque try para parsear el ID del usuario de la ruta
            try {
                int id = Integer.parseInt(pathInfo.substring(1));
                // Trae la informacion del usuario conectada con las tablas maestras para así no mostrar un ID en el frontend si no el valor del ID
                String sql = """
                        SELECT 
                            u.IdUsuario, u.Nombre, u.Documento, u.Email, u.Telefono, u.FechaNacimiento, u.Activo, u.IdRol,
                            r.Nombre AS RolNombre,
                            g.Nombre AS GeneroNombre,
                            dt.Nombre AS DocumentoTipoNombre,
                            s.Nombre AS SeccionalNombre,
                            o.Nombre AS OrganizacionNombre
                        FROM Usuario u
                        LEFT JOIN Rol r ON u.IdRol = r.IdRol
                        LEFT JOIN Genero g ON u.IdGenero = g.IdGenero
                        LEFT JOIN DocumentoTipo dt ON u.IdDocumentoTipo = dt.IdDocumentoTipo
                        LEFT JOIN Organizacion o ON u.IdOrganizacion = o.IdOrganizacion
                        LEFT JOIN Seccional s ON COALESCE(u.IdSeccional, o.IdSeccional) = s.IdSeccional
                        WHERE u.IdUsuario = ?
                        """;

                // Try-with-resources: Conectar y preparar consulta SQL con el parámetro de ID de usuario
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {

                    ps.setInt(1, id);
                    // Try-with-resources: Ejecutar y procesar ResultSet
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int activeVal = rs.getInt("Activo");
                            Map<String, Object> user = new HashMap<>();
                            user.put("id", rs.getInt("IdUsuario"));
                            
                            // Separación lógica del campo Nombre completo en nombres y apellidos para la API
                            String fullName = rs.getString("Nombre");
                            String names = "";
                            String lastNames = "";
                            // Condicional: Si el nombre completo no es nulo
                            if (fullName != null) {
                                // Bloque: Limpiar espacios extras y dividir la cadena
                                fullName = fullName.trim();
                                String[] parts = fullName.split("\\s+");
                                // Condicional: Si tiene 4 o más palabras (ej. dos nombres y dos apellidos)
                                if (parts.length >= 4) {
                                    // Bloque: Tomar los dos primeros como nombres y los demás como apellidos
                                    names = parts[0] + " " + parts[1];
                                    StringBuilder sb = new StringBuilder();
                                    // Loop: Unir los fragmentos de apellidos
                                    for (int i = 2; i < parts.length; i++) {
                                        // Condicional: Agregar espacio de separación
                                        if (i > 2) {
                                            sb.append(" ");
                                        }
                                        sb.append(parts[i]);
                                    }
                                    lastNames = sb.toString();
                                } 
                                // Condicional: Si tiene exactamente 3 palabras (ej. un nombre y dos apellidos)
                                else if (parts.length == 3) {
                                    // Bloque: Primer elemento es nombre, los otros dos son apellidos
                                    names = parts[0];
                                    lastNames = parts[1] + " " + parts[2];
                                } 
                                // Condicional: Si tiene exactamente 2 palabras (ej. un nombre y un apellido)
                                else if (parts.length == 2) {
                                    // Bloque: Dividir equitativamente
                                    names = parts[0];
                                    lastNames = parts[1];
                                } 
                                // Condicional: Si es de una sola palabra
                                else {
                                    // Bloque: Colocar todo en nombres
                                    names = fullName;
                                }
                            }
                            user.put("names", names);
                            user.put("last_names", lastNames);
                            user.put("email", rs.getString("Email"));
                            user.put("document_type", rs.getString("DocumentoTipoNombre"));
                            user.put("document_number", rs.getString("Documento"));
                            user.put("birth_date", rs.getDate("FechaNacimiento") != null ? rs.getDate("FechaNacimiento").toString() : "");
                            user.put("gender", rs.getString("GeneroNombre"));
                            user.put("phone", rs.getString("Telefono"));
                            user.put("sectional", rs.getString("SeccionalNombre"));
                            user.put("organization", rs.getString("OrganizacionNombre"));
                            user.put("status", (activeVal == 1) ? "Activo" : (activeVal == 3 ? "Peticion" : "Desactivado"));
                            user.put("state_user_id", activeVal);

                            Map<String, Object> rol = new HashMap<>();
                            rol.put("id", rs.getInt("IdRol"));
                            rol.put("name", rs.getString("RolNombre"));
                            user.put("rol", rol);
                            user.put("rol_id", rs.getInt("IdRol"));

                            Map<String, Object> dataResp = new HashMap<>();
                            dataResp.put("data", user);

                            resp.getWriter().write(gson.toJson(dataResp));
                        } 
                        // Condicional: Si no existe el usuario solicitado
                        else {
                            // Bloque: Retornar código de error 404
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            resp.getWriter().write("{\"success\":false,\"message\":\"Usuario no encontrado\"}");
                        }
                    }
                }
            } 
            // Catch: Capturar error si el ID no es numérico entero
            catch (NumberFormatException e) {
                // Bloque: Responder con código 400 Bad Request
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false,\"message\":\"ID invalido\"}");
            } 
            // Catch: Capturar errores SQL
            catch (SQLException e) {
                // Bloque: Responder con código de error de base de datos 500 e imprimir la traza
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"success\":false,\"message\":\"Error de base de datos\"}");
                e.printStackTrace();
            }
        }
    }

    /**
     * Procesa las solicitudes HTTP POST, principalmente para el registro de nuevos usuarios en el sistema.
     * 
     * Endpoint y Cuerpo JSON:
     * - POST /api/register: Registra una nueva cuenta de usuario en estado pendiente de aprobación (Activo = 3).
     *   Cuerpo JSON representando un RegisterRequest.
     * 
     * @param req Petición HTTP conteniendo los datos de registro en formato JSON.
     * @param resp Respuesta HTTP indicando éxito o error.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Establecer tipo de contenido a JSON y codificación de caracteres a la respuesta
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String servletPath = req.getServletPath();
        
        // Condicional: Validar si la ruta corresponde al registro de usuarios
        if (servletPath != null && (servletPath.contains("register") || servletPath.contains("/register"))) {
            // Bloque try para procesar el cuerpo JSON de registro
            try {
                BufferedReader reader = req.getReader();
                RegisterRequest regReq = gson.fromJson(reader, RegisterRequest.class);

                // Condicional: Validar si los campos obligatorios del registro vienen nulos o vacíos
                if (regReq == null || regReq.email == null || regReq.password == null ||
                    regReq.names == null || regReq.names.trim().isEmpty() ||
                    regReq.document_number == null || regReq.document_number.trim().isEmpty() ||
                    regReq.gender_id == null || regReq.document_type_id == null) {
                    
                    // Bloque: Retornar código de error 400 Bad Request
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("{\"success\":false,\"message\":\"Datos de registro incompletos. Por favor complete todos los campos requeridos.\"}");
                    return;
                }

                // Try-with-resources: Establecer conexión con la base de datos para registrar al usuario
                try (Connection conn = DatabaseConfig.getConnection()) {
                    // Check duplicate email
                    String checkEmailSql = "SELECT IdUsuario FROM Usuario WHERE Email = ?";
                    // Try-with-resources: Preparar consulta de correo duplicado
                    try (PreparedStatement ps = conn.prepareStatement(checkEmailSql)) {
                        ps.setString(1, regReq.email);
                        // Try-with-resources: Ejecutar y evaluar si el correo electrónico ya está registrado
                        try (ResultSet rs = ps.executeQuery()) {
                            // Condicional: Si existe un registro con ese correo
                            if (rs.next()) {
                                // Bloque: Retornar código de error 400 Bad Request indicando duplicidad
                                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                                resp.getWriter().write("{\"success\":false,\"message\":\"El correo ya se encuentra registrado\"}");
                                return;
                            }
                        }
                    }

                    // Check duplicate document number
                    String checkDocSql = "SELECT IdUsuario FROM Usuario WHERE Documento = ?";
                    // Try-with-resources: Preparar consulta de documento duplicado
                    try (PreparedStatement ps = conn.prepareStatement(checkDocSql)) {
                        ps.setString(1, regReq.document_number);
                        // Try-with-resources: Ejecutar y evaluar
                        try (ResultSet rs = ps.executeQuery()) {
                            // Condicional: Si el número de documento ya está registrado
                            if (rs.next()) {
                                // Bloque: Retornar código 400 indicando duplicidad de documento
                                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                                resp.getWriter().write("{\"success\":false,\"message\":\"El documento ya se encuentra registrado\"}");
                                return;
                            }
                        }
                    }

                    java.sql.Date birthDate = null;
                    // Condicional: Si se suministró fecha de nacimiento no vacía
                    if (regReq.birth_date != null && !regReq.birth_date.trim().isEmpty()) {
                        // Bloque try para parsear el formato String a java.sql.Date
                        try {
                            birthDate = java.sql.Date.valueOf(regReq.birth_date.trim());
                        } 
                        // Catch: Ignorar errores de conversión y dejar la fecha en null
                        catch (IllegalArgumentException e) {
                            // ignore or set null
                        }
                    }

                    Integer sectionalId = null;
                    // Condicional: Si se proporcionó organización, consultar su seccional asociada
                    if (regReq.organization_id != null) {
                        String findSecSql = "SELECT IdSeccional FROM Organizacion WHERE IdOrganizacion = ?";
                        // Try-with-resources: Preparar la consulta de búsqueda de seccional
                        try (PreparedStatement psSec = conn.prepareStatement(findSecSql)) {
                            psSec.setInt(1, regReq.organization_id);
                            // Try-with-resources: Ejecutar consulta de seccional
                            try (ResultSet rsSec = psSec.executeQuery()) {
                                // Condicional: Si existe la organización
                                if (rsSec.next()) {
                                    sectionalId = rsSec.getInt("IdSeccional");
                                }
                            }
                        }
                    }

                    String insertSql = """
                        INSERT INTO Usuario (
                            Nombre, Documento, IdRol, IdGenero, IdDocumentoTipo, 
                            IdNacionalidad, Email, Contrasena, Telefono, 
                            FechaNacimiento, IdOrganizacion, IdSeccional, Activo
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """;
                        
                    // Try-with-resources: Preparar la sentencia de inserción de usuario
                    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                        ps.setString(1, regReq.names + (regReq.last_names != null && !regReq.last_names.trim().isEmpty() ? " " + regReq.last_names : ""));
                        ps.setString(2, regReq.document_number);
                        ps.setInt(3, 3); // Voluntario por defecto al registrarse
                        ps.setObject(4, regReq.gender_id, java.sql.Types.INTEGER);
                        ps.setObject(5, regReq.document_type_id, java.sql.Types.INTEGER);
                        ps.setInt(6, 1); // Nacionalidad Colombiana (1)
                        ps.setString(7, regReq.email);
                        ps.setString(8, regReq.password);
                        ps.setString(9, regReq.phone);
                        ps.setDate(10, birthDate);
                        ps.setObject(11, regReq.organization_id, java.sql.Types.INTEGER);
                        ps.setObject(12, sectionalId, java.sql.Types.INTEGER);
                        ps.setInt(13, 3); // Activo = 3 (Petición pendiente de aprobación)

                        int affected = ps.executeUpdate();
                        // Condicional: Si se insertó correctamente al menos una fila
                        if (affected > 0) {
                            // Bloque: Retornar confirmación de registro
                            resp.getWriter().write("{\"success\":true,\"message\":\"Usuario registrado exitosamente. Su solicitud está pendiente de aprobación.\"}");
                        } 
                        // Condicional: Si no afectó ninguna fila
                        else {
                            // Bloque: Retornar código de error interno 500
                            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            resp.getWriter().write("{\"success\":false,\"message\":\"No se pudo registrar el usuario\"}");
                        }
                    }
                }
            } 
            // Catch: Capturar cualquier excepción SQL durante la inserción
            catch (SQLException e) {
                // Bloque: Enviar código de respuesta 500 e imprimir traza de error
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"success\":false,\"message\":\"Error de base de datos\"}");
                e.printStackTrace();
            }
        } 
        // Condicional: Si no es la ruta de registro
        else {
            // Bloque: Retornar error 400 Bad Request
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"Ruta POST no soportada\"}");
        }
    }

    /**
     * Clase interna DTO para recibir los parámetros de registro de un usuario.
     */
    private static class RegisterRequest {
        /** Nombres del usuario. */
        String names;
        /** Apellidos del usuario. */
        String last_names;
        /** Fecha de nacimiento en formato String (yyyy-MM-dd). */
        String birth_date;
        /** ID del tipo de documento de identidad. */
        Integer document_type_id;
        /** Número del documento de identidad. */
        String document_number;
        /** Número de teléfono celular o de contacto. */
        String phone;
        /** ID del género seleccionado. */
        Integer gender_id;
        /** ID de la organización o junta municipal a la que pertenece. */
        Integer organization_id;
        /** Correo electrónico institucional o personal (llave única de sesión). */
        String email;
        /** Contraseña de acceso en texto plano. */
        String password;
    }

    /**
     * Procesa las solicitudes HTTP PATCH para actualizar estados de cuentas de usuarios o reasignar roles.
     * 
     * Endpoints y Parámetros:
     * - PATCH /api/users/role/{id}: Cambia el rol de un usuario. Cuerpo JSON: { "role": String ("Administrador"|"Supervisor"|"Voluntario") }
     * - PATCH /api/users/{id}/change-status: Cambia el estado de actividad del usuario. Cuerpo JSON: { "state_user_id": int (1: Activo, 0: Inactivo, 2: Desactivado, 3: Petición) }
     * 
     * @param req Petición HTTP con JSON en el cuerpo.
     * @param resp Respuesta HTTP.
     * @throws IOException Si ocurre un error de E/S.
     */
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Establecer tipo de contenido y codificación de respuesta a JSON y UTF-8
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        // Condicional: Validar si la ruta posee el ID de usuario requerido
        if (pathInfo == null || pathInfo.equals("/")) {
            // Bloque: Enviar error 400 Bad Request
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"ID o ruta requerida\"}");
            return;
        }

        // Bloque try para capturar y controlar fallos al deserializar o realizar actualizaciones de datos
        try {
            BufferedReader reader = req.getReader();
            Map<String, Object> body = gson.fromJson(reader, Map.class);

            // Condicional: Validar si el cuerpo de la petición viene vacío
            if (body == null) {
                // Bloque: Retornar código de error 400 Bad Request
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false,\"message\":\"Cuerpo vacio\"}");
                return;
            }

            // Condicional: Si la ruta es para cambio de rol de usuario (/role/{id})
            if (pathInfo.startsWith("/role/")) {
                // Bloque: Extraer ID del usuario y determinar el nuevo ID de rol
                int id = Integer.parseInt(pathInfo.substring(6));
                String roleName = (String) body.get("role");
                int roleId = 3; // Voluntario por defecto
                
                // Condicional: Asignar ID de rol según el nombre recibido
                if ("Administrador".equalsIgnoreCase(roleName)) {
                    roleId = 1;
                } 
                // Condicional: Si es Supervisor
                else if ("Supervisor".equalsIgnoreCase(roleName)) {
                    roleId = 2;
                }

                // Definir sentencia de actualización (si se asciende a Supervisor, se pasa a estado aprobación 3)
                String sql = (roleId == 2)
                    ? "UPDATE Usuario SET IdRol = ?, Activo = 3 WHERE IdUsuario = ?"
                    : "UPDATE Usuario SET IdRol = ? WHERE IdUsuario = ?";
                    
                // Try-with-resources: Conectar y preparar sentencia SQL
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {

                    ps.setInt(1, roleId);
                    ps.setInt(2, id);
                    int affected = ps.executeUpdate();

                    // Condicional: Si la actualización afectó al menos una fila
                    if (affected > 0) {
                        // Bloque: Retornar éxito
                        resp.getWriter().write("{\"success\":true,\"message\":\"Rol de usuario actualizado exitosamente\"}");
                    } 
                    // Condicional: Si no se encontró el usuario en la base de datos
                    else {
                        // Bloque: Retornar error 404 de no encontrado
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        resp.getWriter().write("{\"success\":false,\"message\":\"Usuario no encontrado\"}");
                    }
                }

            } 
            // Condicional: Si la ruta es para activar/desactivar al usuario (/{id}/change-status)
            else if (pathInfo.endsWith("/change-status")) {
                // Bloque: Extraer ID del segmento intermedio de la ruta y parsear el estado
                String[] segments = pathInfo.split("/");
                int id = Integer.parseInt(segments[1]);

                Object stateObj = body.get("state_user_id");
                int stateId = 1;
                // Condicional: Si el estado viene en formato numérico directo
                if (stateObj instanceof Number) {
                    stateId = ((Number) stateObj).intValue();
                } 
                // Condicional: Si viene en formato String
                else if (stateObj instanceof String) {
                    stateId = Integer.parseInt((String) stateObj);
                }
                int active = stateId;

                String sql = "UPDATE Usuario SET Activo = ? WHERE IdUsuario = ?";
                // Try-with-resources: Conectar y preparar la actualización del estado de actividad del usuario
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {

                    ps.setInt(1, active);
                    ps.setInt(2, id);
                    int affected = ps.executeUpdate();

                    // Condicional: Si la actualización se realiza correctamente
                    if (affected > 0) {
                        // Bloque: Enviar respuesta de éxito
                        resp.getWriter().write("{\"success\":true,\"message\":\"Estado de usuario actualizado exitosamente\"}");
                    } 
                    // Condicional: Si el usuario no fue hallado en base de datos
                    else {
                        // Bloque: Retornar código de error 404
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        resp.getWriter().write("{\"success\":false,\"message\":\"Usuario no encontrado\"}");
                    }
                }

            } 
            // Condicional: Si la ruta PATCH no se mapea con ningún endpoint soportado
            else {
                // Bloque: Enviar error 400 Bad Request
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false,\"message\":\"Ruta de actualizacion no soportada\"}");
            }

        } 
        // Catch: Capturar excepciones de conversión de números (ID inválido)
        catch (NumberFormatException e) {
            // Bloque: Responder error 400 Bad Request
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"ID invalido\"}");
        } 
        // Catch: Capturar excepciones de base de datos SQL
        catch (SQLException e) {
            // Bloque: Responder con código 500 de error de servidor e imprimir traza
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"Error de base de datos\"}");
            e.printStackTrace();
        }
    }

    /**
     * Procesa las solicitudes HTTP DELETE para eliminar cuentas de usuario.
     * 
     * Endpoint:
     * - DELETE /api/users/{id}: Elimina permanentemente la cuenta de usuario identificada por el ID proporcionado.
     * 
     * @param req Petición HTTP con el ID en la ruta.
     * @param resp Respuesta HTTP en formato JSON.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Indicar codificación y formato JSON de respuesta
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        // Condicional: Validar si la ruta especifica el ID de usuario a eliminar
        if (pathInfo == null || pathInfo.equals("/")) {
            // Bloque: Retornar error 400 indicando requerimiento de ID
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"ID requerido para eliminar\"}");
            return;
        }

        // Bloque try para capturar y controlar fallos al procesar la eliminación del usuario
        try {
            int id = Integer.parseInt(pathInfo.substring(1));
            String sql = "DELETE FROM Usuario WHERE IdUsuario = ?";

            // Try-with-resources: Conectar y preparar sentencia de eliminación de usuario
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, id);
                int affected = ps.executeUpdate();

                // Condicional: Si se eliminó correctamente el usuario
                if (affected > 0) {
                    // Bloque: Enviar confirmación exitosa
                    resp.getWriter().write("{\"success\":true,\"message\":\"Usuario eliminado exitosamente\"}");
                } 
                // Condicional: Si el usuario no existía en la base de datos
                else {
                    // Bloque: Retornar error de no encontrado 404
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{\"success\":false,\"message\":\"Usuario no encontrado\"}");
                }
            }
        } 
        // Catch: Capturar error si el ID provisto no es numérico entero
        catch (NumberFormatException e) {
            // Bloque: Retornar error 400 Bad Request
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"ID invalido\"}");
        } 
        // Catch: Capturar excepciones SQL (incluyendo restricciones de claves foráneas)
        catch (SQLException e) {
            // Bloque: Retornar código de error interno 500 e imprimir traza
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"Error de base de datos o restriccion de clave foranea\"}");
            e.printStackTrace();
        }
    }
}

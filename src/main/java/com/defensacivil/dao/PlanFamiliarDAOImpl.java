package com.defensacivil.dao;

import com.defensacivil.config.DatabaseConfig;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación de la interfaz {@link PlanFamiliarDAO} que gestiona la persistencia
 * de planes familiares mediante consultas SQL en base de datos y memoria caché para campos adicionales.
 */
public class PlanFamiliarDAOImpl implements PlanFamiliarDAO {

    /**
     * Mapa de almacenamiento en memoria para persistir temporalmente información adicional
     * de los planes (ej. tipo de familia).
     */
    private final Map<String, Map<String, Object>> extraData;

    /**
     * Constructor de la clase.
     *
     * @param extraData Mapa de datos adicionales en memoria para campos no persistidos físicamente.
     */
    public PlanFamiliarDAOImpl(Map<String, Map<String, Object>> extraData) {
        // Asignar el mapa recibido como parámetro al atributo local
        this.extraData = extraData;
    }

    /**
     * Método auxiliar privado para traducir un código de estado numérico a su equivalente textual.
     *
     * @param statusId Identificador del estado.
     * @return Cadena con la descripción del estado (ej. "Creado", "En proceso", etc.).
     */
    private String getStatusName(int statusId) {
        // Utilizar una estructura switch expression para mapear el ID del estado a una descripción en texto
        return switch (statusId) {
            case 1 -> "Creado";
            case 3 -> "En proceso";
            case 4 -> "En Revision";
            case 5 -> "Devuelto con observaciones";
            case 6 -> "Rechazado";
            case 7 -> "Completado";
            default -> "Creado";
        };
    }

    /**
     * {@inheritDoc}
     * Obtiene planes familiares aplicando filtros específicos según los roles de Voluntario, Supervisor o Administrador.
     *
     * @param roleId Identificador del rol de usuario que solicita la información (Administrador = 1, Supervisor = 2, Voluntario = 3).
     * @param userId Identificador único del usuario para filtrar voluntarios.
     * @param sectionalId Identificador de la seccional para filtrar supervisores.
     * @return Una lista de mapas con los detalles de cada plan familiar.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    @Override
    public List<Map<String, Object>> getFamilyPlans(int roleId, int userId, int sectionalId) throws SQLException {
        // Inicializar la lista que guardará los planes familiares mapeados
        List<Map<String, Object>> list = new ArrayList<>();
        String sql;
        
        // 1. Elegir la consulta SQL adecuada según el rol del usuario que realiza la solicitud
        if (roleId == 3) { 
            // Rol de Voluntario: filtra planes asociados al voluntario logueado
            sql = """
                SELECT p.IdPlanFamiliar, f.Nombre AS FamiliaNombre, p.Estado, p.Fecha, u.Nombre AS VoluntarioNombre
                FROM PlanFamiliar p
                JOIN Familia f ON p.IdFamilia = f.IdFamilia
                JOIN Usuario u ON p.IdUsuario = u.IdUsuario
                WHERE p.IdUsuario = ?
                ORDER BY p.IdPlanFamiliar DESC
                """;
        } else if (roleId == 2) { 
            // Rol de Supervisor: filtra por la seccional a la que pertenece
            sql = """
                SELECT p.IdPlanFamiliar, f.Nombre AS FamiliaNombre, p.Estado, p.Fecha, u.Nombre AS VoluntarioNombre
                FROM PlanFamiliar p
                JOIN Familia f ON p.IdFamilia = f.IdFamilia
                JOIN Usuario u ON p.IdUsuario = u.IdUsuario
                LEFT JOIN Organizacion org ON u.IdOrganizacion = org.IdOrganizacion
                WHERE org.IdSeccional = ?
                ORDER BY p.IdPlanFamiliar DESC
                """;
        } else { 
            // Rol de Administrador: puede ver todos los planes familiares sin restricciones
            sql = """
                SELECT p.IdPlanFamiliar, f.Nombre AS FamiliaNombre, p.Estado, p.Fecha, u.Nombre AS VoluntarioNombre
                FROM PlanFamiliar p
                JOIN Familia f ON p.IdFamilia = f.IdFamilia
                JOIN Usuario u ON p.IdUsuario = u.IdUsuario
                ORDER BY p.IdPlanFamiliar DESC
                """;
        }

        // Obtener la conexión JDBC y preparar la sentencia SQL
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Asignar dinámicamente los parámetros requeridos según el rol del usuario
            if (roleId == 3) {
                // Voluntario: asignar el ID del usuario
                ps.setInt(1, userId);
            } else if (roleId == 2) {
                // Supervisor: asignar el ID de la seccional
                ps.setInt(1, sectionalId);
            }

            // Ejecutar la consulta SQL y procesar los registros resultantes
            try (ResultSet rs = ps.executeQuery()) {
                // Recorrer los registros de planes familiares recuperados de la BD
                while (rs.next()) {
                    int idVal = rs.getInt("IdPlanFamiliar");
                    String estadoStr = rs.getString("Estado");
                    int statusId = 1;
                    
                    // Intentar parsear el estado a un entero y manejar excepciones de formato
                    try {
                        // Intentar la conversión de texto a número
                        statusId = Integer.parseInt(estadoStr);
                    } catch (Exception ignored) {
                        // Ignorar el error de parseo y mantener el valor por defecto de 1
                    }
                    
                    // Traducir el identificador de estado numérico a texto legible en español
                    String statusText = getStatusName(statusId);

                    // Mapear los datos de cada plan familiar
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", idVal);
                    item.put("last_names", rs.getString("FamiliaNombre"));
                    item.put("status", statusText);
                    item.put("status_id", statusId);
                    item.put("responsable", rs.getString("VoluntarioNombre"));

                    // Recuperar datos complementarios desde el mapa en memoria extraData
                    Map<String, Object> extra = extraData.getOrDefault("plan_" + idVal, Map.of());
                    int familyTypeId = 3;
                    Object ftIdObj = extra.get("family_type_id");
                    
                    // Validar si el tipo de familia en extraData es de tipo numérico o cadena
                    if (ftIdObj instanceof Number) {
                        // Obtener el valor entero del tipo de familia
                        familyTypeId = ((Number) ftIdObj).intValue();
                    } else if (ftIdObj instanceof String) {
                        // Parsear el valor de tipo de familia desde cadena a entero
                        familyTypeId = Integer.parseInt((String) ftIdObj);
                    }

                    String familyType = "Por Definir";
                    
                    // Traducir el ID del tipo de familia a su descripción legible
                    if (familyTypeId == 1) {
                        // Tipo 1: Vulnerable
                        familyType = "Vulnerable";
                    } else if (familyTypeId == 2) {
                        // Tipo 2: No Vulnerable
                        familyType = "No Vulnerable";
                    }

                    item.put("family_type", familyType);
                    item.put("family_type_id", familyTypeId);
                    item.put("department", "Antioquia");
                    item.put("city", "Medellín");
                    item.put("date_create", rs.getDate("Fecha") != null ? rs.getDate("Fecha").toString() : "");
                    
                    // Adicionar el mapa con los datos del plan mapeado a la lista resultante
                    list.add(item);
                }
            }
        }
        // Retornar la lista completa de planes familiares mapeados
        return list;
    }

    /**
     * {@inheritDoc}
     * Cuenta la cantidad de integrantes asociados a un plan para validar si tiene miembros.
     *
     * @param planId Identificador único del plan familiar.
     * @return true si el plan familiar tiene al menos un integrante; false en caso contrario.
     * @throws SQLException Si ocurre un error de base de datos durante la consulta.
     */
    @Override
    public boolean hasMembers(int planId) throws SQLException {
        int count = 0;
        String sql = "SELECT COUNT(*) FROM Integrante WHERE IdPlanFamiliar = ?";
        
        // Conectarse a la base de datos y preparar la consulta de conteo
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Configurar el parámetro de ID de plan familiar
            ps.setInt(1, planId);
            
            // Ejecutar la consulta de conteo y evaluar el ResultSet
            try (ResultSet rs = ps.executeQuery()) {
                // Si la consulta retorna resultados
                if (rs.next()) {
                    // Asignar el valor del conteo obtenido
                    count = rs.getInt(1);
                }
            }
        }
        // Retornar verdadero si el conteo es mayor a cero
        return count > 0;
    }

    /**
     * {@inheritDoc}
     * Realiza un conteo en base de datos para validar si el plan familiar cumple los mínimos de integrantes, riesgos y recursos.
     *
     * @param planId Identificador único del plan familiar.
     * @return Un mapa que contiene los booleanos que validan los requisitos mínimos.
     * @throws SQLException Si ocurre un error de base de datos al realizar las consultas.
     */
    @Override
    public Map<String, Object> validateRequirements(int planId) throws SQLException {
        int memberCount = 0;
        int riskCount = 0;
        int resourceCount = 0;

        // Abrir conexión a la base de datos para ejecutar las diferentes validaciones por separado
        try (Connection conn = DatabaseConfig.getConnection()) {
            
            // Validar la cantidad de integrantes
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM Integrante WHERE IdPlanFamiliar = ?")) {
                ps.setInt(1, planId);
                
                // Ejecutar consulta de integrantes
                try (ResultSet rs = ps.executeQuery()) {
                    // Si se obtiene el resultado
                    if (rs.next()) {
                        // Asignar cantidad de integrantes
                        memberCount = rs.getInt(1);
                    }
                }
            }
            
            // Validar la cantidad de factores de riesgo
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM FactorRiesgo WHERE IdPlanFamiliar = ?")) {
                ps.setInt(1, planId);
                
                // Ejecutar consulta de factores de riesgo
                try (ResultSet rs = ps.executeQuery()) {
                    // Si se obtiene el resultado
                    if (rs.next()) {
                        // Asignar cantidad de factores de riesgo
                        riskCount = rs.getInt(1);
                    }
                }
            }
            
            // Validar la cantidad de recursos comunitarios disponibles registrados
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM RecursoDisponible WHERE IdPlanFamiliar = ?")) {
                ps.setInt(1, planId);
                
                // Ejecutar consulta de recursos disponibles
                try (ResultSet rs = ps.executeQuery()) {
                    // Si se obtiene el resultado
                    if (rs.next()) {
                        // Asignar cantidad de recursos
                        resourceCount = rs.getInt(1);
                    }
                }
            }
        }

        // Estructurar el mapa de resultados con las banderas de cumplimiento
        Map<String, Object> valMap = new HashMap<>();
        valMap.put("is_valid", memberCount >= 2 && riskCount >= 1 && resourceCount >= 1);
        valMap.put("has_min_members", memberCount >= 2);
        valMap.put("has_risk_factors", riskCount >= 1);
        valMap.put("has_resources", resourceCount >= 1);
        valMap.put("has_photos", true); // Simulación de validación de fotos del plan
        valMap.put("has_graphics", true); // Simulación de validación de gráficos del plan
        valMap.put("has_action_before", true); // Simulación de acciones de preparación
        valMap.put("has_action_during", true); // Simulación de acciones durante la emergencia
        valMap.put("has_action_after", true); // Simulación de acciones posteriores

        // Retornar el mapa con la validación de requisitos mínimos
        return valMap;
    }

    /**
     * {@inheritDoc}
     * Busca y obtiene la información detallada del plan familiar, combinando tablas relacionales y extraData en memoria.
     *
     * @param planId Identificador único del plan familiar.
     * @return Un mapa con los detalles completos del plan familiar.
     * @throws SQLException Si ocurre un error al realizar la consulta SQL.
     */
    @Override
    public Map<String, Object> getPlanById(int planId) throws SQLException {
        // Consulta SQL con múltiples LEFT JOINs para obtener toda la información geográfica e identificadores
        String sql = """
            SELECT p.IdPlanFamiliar, f.Nombre AS FamiliaNombre, f.Telefono, f.IdSector, f.IdCalidad, p.Estado,
                   f.Direccion, f.IdDepartamento, f.IdCiudad, d.Nombre AS DepartamentoNombre,
                   s.Nombre AS SectorNombre, cv.Nombre AS CalidadNombre
            FROM PlanFamiliar p
            JOIN Familia f ON p.IdFamilia = f.IdFamilia
            LEFT JOIN Sector s ON f.IdSector = s.IdSector
            LEFT JOIN CalidadVivienda cv ON f.IdCalidad = cv.IdCalidad
            LEFT JOIN Departamento d ON f.IdDepartamento = d.IdDepartamento
            WHERE p.IdPlanFamiliar = ?
            """;
        
        // Inicializar el mapa resultante
        Map<String, Object> item = new HashMap<>();
        
        // Abrir conexión JDBC y preparar la sentencia SQL parametrizada
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Asignar el parámetro de ID de plan familiar
            ps.setInt(1, planId);
            
            // Ejecutar la consulta SQL
            try (ResultSet rs = ps.executeQuery()) {
                // Si el plan familiar existe en la base de datos
                if (rs.next()) {
                    int statusId = 1;
                    
                    // Intentar parsear el estado textual a entero
                    try {
                        statusId = Integer.parseInt(rs.getString("Estado"));
                    } catch (Exception ignored) {
                        // Ignorar cualquier fallo y mantener el estado 1 por defecto
                    }

                    // Recuperar datos complementarios en memoria
                    Map<String, Object> extra = extraData.getOrDefault("plan_" + planId, Map.of());
                    int familyTypeId = 3;
                    Object ftIdObj = extra.get("family_type_id");
                    
                    // Validar e interpretar el formato del ID del tipo de familia
                    if (ftIdObj instanceof Number) {
                        // Obtener el valor entero del tipo de familia
                        familyTypeId = ((Number) ftIdObj).intValue();
                    } else if (ftIdObj instanceof String) {
                        // Parsear a entero en caso de ser un String
                        familyTypeId = Integer.parseInt((String) ftIdObj);
                    }

                    String familyType = "Por Definir";
                    
                    // Asignar la descripción legible del tipo de familia
                    if (familyTypeId == 1) {
                        // 1 = Vulnerable
                        familyType = "Vulnerable";
                    } else if (familyTypeId == 2) {
                        // 2 = No Vulnerable
                        familyType = "No Vulnerable";
                    }

                    String famName = rs.getString("FamiliaNombre");
                    
                    // Limpiar el prefijo "Familia " del nombre si está presente
                    if (famName != null && famName.startsWith("Familia ")) {
                        // Substraer el texto "Familia " (8 caracteres)
                        famName = famName.substring(8);
                    }

                    // Llenar el mapa estructurado
                    item.put("id", rs.getInt("IdPlanFamiliar"));
                    item.put("last_names", famName != null ? famName : "");
                    item.put("family_type", familyType);
                    item.put("family_type_id", familyTypeId);
                    item.put("landline_phone", rs.getString("Telefono") != null ? rs.getString("Telefono") : "");
                    item.put("status", getStatusName(statusId));
                    item.put("status_plan_id", statusId);
                    item.put("status_id", statusId);

                    int dbSectorId = rs.getInt("IdSector");
                    
                    // Manejar valores nulos de base de datos para sector_id y zone_id
                    item.put("sector_id", rs.wasNull() ? 1 : dbSectorId);
                    item.put("zone_id", rs.wasNull() ? 1 : dbSectorId);

                    int dbDeptId = rs.getInt("IdDepartamento");
                    
                    // Manejar valores nulos para departamento
                    item.put("department_id", rs.wasNull() ? 1 : dbDeptId);

                    int dbCityId = rs.getInt("IdCiudad");
                    
                    // Manejar valores nulos para ciudad
                    item.put("city_id", rs.wasNull() ? 1 : dbCityId);

                    item.put("address", rs.getString("Direccion") != null ? rs.getString("Direccion") : "");
                    
                    String dbSectorName = rs.getString("SectorNombre");
                    item.put("sector_name", dbSectorName != null ? dbSectorName : "");
                    
                    int dbCalidadId = rs.getInt("IdCalidad");
                    
                    // Manejar valores nulos para calidad de vivienda
                    item.put("housing_quality_id", rs.wasNull() ? 1 : dbCalidadId);
                    
                    String dbCalidadName = rs.getString("CalidadNombre");
                    item.put("housing_quality", dbCalidadName != null ? dbCalidadName : "Propia");
                    
                    String deptName = rs.getString("DepartamentoNombre");
                    item.put("department", deptName != null ? deptName : "Antioquia");
                    item.put("city", "Medellín");
                    item.put("created_at", LocalDate.now().toString());

                    // Recuperar el último comentario registrado de la tabla ValidacionPlan
                    String commentSql = "SELECT Comentario FROM ValidacionPlan WHERE IdPlanFamiliar = ? ORDER BY IdValidacion DESC LIMIT 1";
                    
                    // Preparar y ejecutar consulta del comentario de validación
                    try (PreparedStatement psC = conn.prepareStatement(commentSql)) {
                        psC.setInt(1, planId);
                        
                        // Obtener ResultSet del comentario
                        try (ResultSet rsC = psC.executeQuery()) {
                            // Si se encuentra un comentario registrado
                            if (rsC.next()) {
                                // Adicionar el comentario al mapa del plan familiar
                                item.put("comentary", rsC.getString("Comentario"));
                            }
                        }
                    }
                }
            }
        }
        // Retornar el mapa del plan familiar
        return item;
    }

    /**
     * {@inheritDoc}
     * Genera estadísticas de planes familiares (pendientes, en revisión, aprobados, rechazados) para el dashboard de supervisor.
     *
     * @return Un mapa con los contadores agrupados por estado de planes familiares.
     * @throws SQLException Si ocurre un error de base de datos durante las consultas.
     */
    @Override
    public Map<String, Object> getSupervisorDashboard() throws SQLException {
        int pendingCount = 0;
        int approvedCount = 0;
        int rejectedCount = 0;
        int inRevisionCount = 0;

        List<Map<String, Object>> latestPlans = new ArrayList<>();
        
        // Abrir conexión a la base de datos para recuperar todos los contadores del dashboard
        try (Connection conn = DatabaseConfig.getConnection()) {
            // 1. Obtener cantidad de planes pendientes (Estado 1 o 3)
            String sqlPending = "SELECT COUNT(*) FROM PlanFamiliar WHERE Estado IN ('1', '3')";
            try (PreparedStatement ps = conn.prepareStatement(sqlPending);
                 ResultSet rs = ps.executeQuery()) {
                // Si la consulta devuelve el conteo
                if (rs.next()) {
                    pendingCount = rs.getInt(1);
                }
            }

            // 2. Obtener cantidad de planes en revisión (Estado 4)
            String sqlRevision = "SELECT COUNT(*) FROM PlanFamiliar WHERE Estado = '4'";
            try (PreparedStatement ps = conn.prepareStatement(sqlRevision);
                 ResultSet rs = ps.executeQuery()) {
                // Si se obtiene el conteo
                if (rs.next()) {
                    inRevisionCount = rs.getInt(1);
                }
            }

            // 3. Obtener cantidad de planes aprobados/completados (Estado 7)
            String sqlApproved = "SELECT COUNT(*) FROM PlanFamiliar WHERE Estado = '7'";
            try (PreparedStatement ps = conn.prepareStatement(sqlApproved);
                 ResultSet rs = ps.executeQuery()) {
                // Si se obtiene el conteo
                if (rs.next()) {
                    approvedCount = rs.getInt(1);
                }
            }

            // 4. Obtener cantidad de planes rechazados/devueltos (Estado 5 o 6)
            String sqlRejected = "SELECT COUNT(*) FROM PlanFamiliar WHERE Estado IN ('5', '6')";
            try (PreparedStatement ps = conn.prepareStatement(sqlRejected);
                 ResultSet rs = ps.executeQuery()) {
                // Si se obtiene el conteo
                if (rs.next()) {
                    rejectedCount = rs.getInt(1);
                }
            }

            // 5. Obtener los 5 planes familiares más recientes del sistema mediante un JOIN
            String sqlLatest = """
                SELECT p.IdPlanFamiliar, f.Nombre AS FamiliaNombre, p.Estado, p.Fecha, 
                       u.Nombre AS VoluntarioNombre, s.Nombre AS SeccionalNombre
                FROM PlanFamiliar p
                JOIN Familia f ON p.IdFamilia = f.IdFamilia
                LEFT JOIN Usuario u ON p.IdUsuario = u.IdUsuario
                LEFT JOIN Organizacion org ON u.IdOrganizacion = org.IdOrganizacion
                LEFT JOIN Seccional s ON org.IdSeccional = s.IdSeccional
                ORDER BY p.IdPlanFamiliar DESC
                LIMIT 5
                """;
            
            // Preparar y ejecutar consulta de planes recientes
            try (PreparedStatement ps = conn.prepareStatement(sqlLatest);
                 ResultSet rs = ps.executeQuery()) {
                
                // Recorrer los registros de los 5 planes más recientes
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    int idVal = rs.getInt("IdPlanFamiliar");
                    String estadoStr = rs.getString("Estado");
                    int statusId = 1;
                    
                    // Parsear el ID de estado
                    try {
                        statusId = Integer.parseInt(estadoStr);
                    } catch (Exception ignored) {
                        // Mantener el estado por defecto si falla
                    }

                    // Guardar los datos de cada plan reciente
                    item.put("id", idVal);
                    item.put("family_name", rs.getString("FamiliaNombre"));
                    item.put("status_id", statusId);
                    item.put("status", getStatusName(statusId));
                    item.put("date", rs.getDate("Fecha") != null ? rs.getDate("Fecha").toString() : "");
                    item.put("volunteer", rs.getString("VoluntarioNombre") != null ? rs.getString("VoluntarioNombre") : "Voluntario Civil");
                    item.put("sectional", rs.getString("SeccionalNombre") != null ? rs.getString("SeccionalNombre") : "Antioquia");
                    
                    // Adicionar el plan reciente a la lista
                    latestPlans.add(item);
                }
            }
        }

        // Estructurar la información de respuesta del dashboard
        Map<String, Object> data = new HashMap<>();
        data.put("pending_plans", pendingCount);
        data.put("in_revision_plans", inRevisionCount);
        data.put("approved_plans", approvedCount);
        data.put("rejected_plans", rejectedCount);
        data.put("time_validation", 120); // Tiempo de validación por defecto simulado
        data.put("latest_plans", latestPlans);

        // Retornar el consolidado
        return data;
    }

    /**
     * {@inheritDoc}
     * Genera datos agregados de usuarios del sistema y sus roles para el dashboard de administrador.
     *
     * @return Un mapa que consolida las métricas globales para el Administrador.
     * @throws SQLException Si ocurre un error de base de datos durante las consultas.
     */
    @Override
    public Map<String, Object> getAdminDashboard() throws SQLException {
        int activeCount = 0;
        int inactiveCount = 0;
        int requestCount = 0;
        int volunteerCount = 0;
        int supervisorCount = 0;

        // Abrir conexión a la base de datos para consultar el estado global de usuarios y roles
        try (Connection conn = DatabaseConfig.getConnection()) {
            
            // 1. Obtener la cantidad de usuarios activos (Activo = 1)
            String sqlActive = "SELECT COUNT(*) FROM Usuario WHERE Activo = 1";
            try (PreparedStatement ps = conn.prepareStatement(sqlActive);
                 ResultSet rs = ps.executeQuery()) {
                // Si retorna resultados
                if (rs.next()) {
                    activeCount = rs.getInt(1);
                }
            }

            // 2. Obtener la cantidad de usuarios inactivos (Activo = 2 o 0)
            String sqlInactive = "SELECT COUNT(*) FROM Usuario WHERE Activo = 2 OR Activo = 0";
            try (PreparedStatement ps = conn.prepareStatement(sqlInactive);
                 ResultSet rs = ps.executeQuery()) {
                // Si retorna resultados
                if (rs.next()) {
                    inactiveCount = rs.getInt(1);
                }
            }

            // 3. Obtener solicitudes de registro pendientes de aprobación (Activo = 3)
            String sqlRequest = "SELECT COUNT(*) FROM Usuario WHERE Activo = 3";
            try (PreparedStatement ps = conn.prepareStatement(sqlRequest);
                 ResultSet rs = ps.executeQuery()) {
                // Si retorna resultados
                if (rs.next()) {
                    requestCount = rs.getInt(1);
                }
            }

            // 4. Obtener cantidad total de usuarios con Rol de Voluntario (IdRol = 3)
            String sqlVolunteer = "SELECT COUNT(*) FROM Usuario WHERE IdRol = 3";
            try (PreparedStatement ps = conn.prepareStatement(sqlVolunteer);
                 ResultSet rs = ps.executeQuery()) {
                // Si retorna resultados
                if (rs.next()) {
                    volunteerCount = rs.getInt(1);
                }
            }

            // 5. Obtener cantidad total de usuarios con Rol de Supervisor (IdRol = 2)
            String sqlSupervisor = "SELECT COUNT(*) FROM Usuario WHERE IdRol = 2";
            try (PreparedStatement ps = conn.prepareStatement(sqlSupervisor);
                 ResultSet rs = ps.executeQuery()) {
                // Si retorna resultados
                if (rs.next()) {
                    supervisorCount = rs.getInt(1);
                }
            }
        }

        // Estructurar los sub-mapas de resumen de usuarios y roles
        Map<String, Object> summary = new HashMap<>();
        summary.put("active", activeCount);
        summary.put("inactive", inactiveCount);
        summary.put("request", requestCount);

        Map<String, Object> rols = new HashMap<>();
        rols.put("volunteer", volunteerCount);
        rols.put("supervisor", supervisorCount);

        // Estructurar datos estáticos/mock de auditoría general e historial de cambios para el frontend
        List<Map<String, Object>> historyGeneral = List.of(
            Map.of(
                "name_model", "Seccional Antioquia",
                "action_execute", "Creación",
                "user_name", "Administrador Sistema",
                "rol", "Administrador",
                "date_time", "2026-06-01 10:15:30",
                "status_old", "Activo",
                "status_new", "Activo"
            ),
            Map.of(
                "name_model", "Organización Cruz Roja 1",
                "action_execute", "Actualización",
                "user_name", "Administrador Sistema",
                "rol", "Administrador",
                "date_time", "2026-06-02 09:30:15",
                "status_old", "Inactivo",
                "status_new", "Activo"
            )
        );

        // Historial simulado de miembros
        List<Map<String, Object>> historyMembers = List.of(
            Map.of(
                "name_model", "Juan Perez",
                "action_execute", "Aprobación",
                "user_name", "Administrador Sistema",
                "rol", "Administrador",
                "date_time", "2026-06-03 14:05:10",
                "status_old", "Peticion",
                "status_new", "Activo"
            ),
            Map.of(
                "name_model", "Voluntario Civil",
                "action_execute", "Creación",
                "user_name", "Administrador Sistema",
                "rol", "Administrador",
                "date_time", "2026-06-02 11:20:00",
                "status_old", "Peticion",
                "status_new", "Activo"
            )
        );

        // Cambios mensuales simulados
        List<Map<String, Object>> monthlyChanges = List.of(
            Map.of("month", "Enero", "total", 15),
            Map.of("month", "Febrero", "total", 20),
            Map.of("month", "Marzo", "total", 8),
            Map.of("month", "Abril", "total", 12),
            Map.of("month", "Mayo", "total", 30),
            Map.of("month", "Junio", "total", 25)
        );

        // Consolidar todos los datos en el mapa de respuesta final
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("summary", summary);
        responseData.put("rols", rols);
        responseData.put("history_general", historyGeneral);
        responseData.put("history_members", historyMembers);
        responseData.put("monthly_changes", monthlyChanges);

        // Retornar la información consolidada
        return responseData;
    }

    /**
     * {@inheritDoc}
     * Crea un registro de familia y el plan familiar correspondiente en una sola transacción.
     *
     * @param lastNames Apellidos de la familia.
     * @param userId Identificador del usuario voluntario creador del plan.
     * @param body Mapa con información opcional del sector, dirección y calidad de la vivienda.
     * @return El identificador autogenerado (clave primaria) del plan familiar insertado, o 0 en caso de error.
     * @throws SQLException Si ocurre un error durante las inserciones SQL.
     */
    @Override
    public int createFamilyPlan(String lastNames, int userId, Map<String, Object> body) throws SQLException {
        int familyId = 0;
        int planId = 0;
        
        // Abrir conexión a la base de datos
        try (Connection conn = DatabaseConfig.getConnection()) {
            // 1. Sentencia SQL para insertar la nueva familia
            String sqlFam = "INSERT INTO Familia (Nombre, IdSector, IdCalidad) VALUES (?, ?, ?)";
            
            // Preparar sentencia y solicitar la recuperación del ID autogenerado de la Familia
            try (PreparedStatement ps = conn.prepareStatement(sqlFam, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, "Familia " + lastNames);
                
                Object sectorIdObj = body.get("sector_id");
                Integer sectorId = null;
                
                // Interpretar el sector_id recibido
                if (sectorIdObj instanceof Number) {
                    sectorId = ((Number) sectorIdObj).intValue();
                } else if (sectorIdObj instanceof String) {
                    sectorId = Integer.parseInt((String) sectorIdObj);
                }
                
                // Configurar el parámetro sectorId o establecer Null
                if (sectorId != null && sectorId > 0) {
                    ps.setInt(2, sectorId);
                } else {
                    ps.setNull(2, Types.INTEGER);
                }

                Object qualityIdObj = body.get("housing_quality_id");
                Integer qualityId = null;
                
                // Interpretar el housing_quality_id recibido
                if (qualityIdObj instanceof Number) {
                    qualityId = ((Number) qualityIdObj).intValue();
                } else if (qualityIdObj instanceof String) {
                    qualityId = Integer.parseInt((String) qualityIdObj);
                }
                
                // Configurar el parámetro qualityId o establecer Null
                if (qualityId != null && qualityId > 0) {
                    ps.setInt(3, qualityId);
                } else {
                    ps.setNull(3, Types.INTEGER);
                }

                // Ejecutar inserción de la Familia
                ps.executeUpdate();
                
                // Recuperar la llave autogenerada de la Familia
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    // Si se generó el ID correctamente
                    if (rs.next()) {
                        familyId = rs.getInt(1);
                    }
                }
            }

            // 2. Si la familia fue registrada correctamente, proceder a insertar el PlanFamiliar correspondiente
            if (familyId > 0) {
                String sqlPlan = "INSERT INTO PlanFamiliar (IdFamilia, IdUsuario, Fecha, Estado) VALUES (?, ?, ?, '1')";
                
                // Preparar inserción de PlanFamiliar y solicitar llave autogenerada
                try (PreparedStatement ps = conn.prepareStatement(sqlPlan, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, familyId);
                    ps.setInt(2, userId);
                    ps.setDate(3, Date.valueOf(LocalDate.now()));
                    ps.executeUpdate();
                    
                    // Recuperar el ID del PlanFamiliar generado
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        // Si se generó la llave
                        if (rs.next()) {
                            planId = rs.getInt(1);
                        }
                    }
                }
            }
        }

        // Si el plan familiar se creó exitosamente, registrar datos adicionales de localización en la caché de memoria
        if (planId > 0) {
            Map<String, Object> extra = new HashMap<>();
            extra.put("zone_id", body.get("zone_id"));
            extra.put("city_id", body.get("city_id"));
            
            // Guardar en el mapa de memoria
            extraData.put("plan_" + planId, extra);
        }

        // Retornar el ID del plan familiar
        return planId;
    }

    /**
     * {@inheritDoc}
     * Actualiza la tabla de Familia física y el almacenamiento de datos adicionales en extraData de memoria.
     *
     * @param planId Identificador único del plan familiar.
     * @param body Mapa con todos los campos de identificación y dirección de la familia.
     * @return true si la actualización física del registro fue exitosa; false en caso contrario.
     * @throws SQLException Si ocurre un error al ejecutar la sentencia SQL de actualización.
     */
    @Override
    public boolean updateIdentification(int planId, Map<String, Object> body) throws SQLException {
        String lastNames = (String) body.get("last_names");
        String landlinePhone = (String) body.get("landline_phone");

        int familyId = 0;
        
        // Abrir conexión a la base de datos
        try (Connection conn = DatabaseConfig.getConnection()) {
            String getFamSql = "SELECT IdFamilia FROM PlanFamiliar WHERE IdPlanFamiliar = ?";
            
            // Recuperar el ID de familia asociado al plan familiar
            try (PreparedStatement ps = conn.prepareStatement(getFamSql)) {
                ps.setInt(1, planId);
                
                // Ejecutar consulta de obtención del ID de familia
                try (ResultSet rs = ps.executeQuery()) {
                    // Si el plan existe y tiene una familia asociada
                    if (rs.next()) {
                        familyId = rs.getInt(1);
                    }
                }
            }

            // Si se encontró un ID de familia válido en la base de datos, proceder a realizar la actualización
            if (familyId > 0) {
                // Sentencia SQL de actualización completa de la tabla Familia
                String sql = "UPDATE Familia SET Nombre = ?, Telefono = ?, IdSector = ?, IdCalidad = ?, Direccion = ?, IdDepartamento = ?, IdCiudad = ? WHERE IdFamilia = ?";
                
                // Preparar y configurar el PreparedStatement para realizar el UPDATE
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, "Familia " + lastNames);
                    ps.setString(2, landlinePhone != null ? landlinePhone : "");
                    
                    Object sectorIdObj = body.get("sector_id");
                    Integer sectorId = null;
                    
                    // Decodificar el sector_id
                    if (sectorIdObj instanceof Number) {
                        sectorId = ((Number) sectorIdObj).intValue();
                    } else if (sectorIdObj instanceof String) {
                        sectorId = Integer.parseInt((String) sectorIdObj);
                    }
                    
                    // Asignar sectorId o configurar Null
                    if (sectorId != null && sectorId > 0) {
                        ps.setInt(3, sectorId);
                    } else {
                        ps.setNull(3, Types.INTEGER);
                    }

                    Object qualityIdObj = body.get("housing_quality_id");
                    Integer qualityId = null;
                    
                    // Decodificar calidad de vivienda
                    if (qualityIdObj instanceof Number) {
                        qualityId = ((Number) qualityIdObj).intValue();
                    } else if (qualityIdObj instanceof String) {
                        qualityId = Integer.parseInt((String) qualityIdObj);
                    }
                    
                    // Asignar qualityId o configurar Null
                    if (qualityId != null && qualityId > 0) {
                        ps.setInt(4, qualityId);
                    } else {
                        ps.setNull(4, Types.INTEGER);
                    }

                    String address = (String) body.get("address");
                    ps.setString(5, address != null ? address : "");

                    Object deptIdObj = body.get("department_id");
                    Integer deptId = null;
                    
                    // Decodificar departamento
                    if (deptIdObj instanceof Number) {
                        deptId = ((Number) deptIdObj).intValue();
                    } else if (deptIdObj instanceof String) {
                        deptId = Integer.parseInt((String) deptIdObj);
                    }
                    
                    // Asignar departamento o configurar Null
                    if (deptId != null && deptId > 0) {
                        ps.setInt(6, deptId);
                    } else {
                        ps.setNull(6, Types.INTEGER);
                    }

                    Object cityIdObj = body.get("city_id");
                    Integer cityId = null;
                    
                    // Decodificar ciudad
                    if (cityIdObj instanceof Number) {
                        cityId = ((Number) cityIdObj).intValue();
                    } else if (cityIdObj instanceof String) {
                        cityId = Integer.parseInt((String) cityIdObj);
                    }
                    
                    // Asignar ciudad o configurar Null
                    if (cityId != null && cityId > 0) {
                        ps.setInt(7, cityId);
                    } else {
                        ps.setNull(7, Types.INTEGER);
                    }

                    ps.setInt(8, familyId);
                    
                    // Ejecutar actualización física
                    ps.executeUpdate();
                }
            } else {
                // Retornar falso si no se encontró la familia asociada al plan familiar
                return false;
            }
        }

        // Retornar verdadero indicando éxito
        return true;
    }

    /**
     * {@inheritDoc}
     * Actualiza el estado del plan familiar e inserta opcionalmente un registro de validación en la tabla ValidacionPlan.
     *
     * @param planId Identificador único del plan familiar.
     * @param statusId Identificador del nuevo estado a asignar.
     * @param commentary Texto explicativo de observaciones del cambio de estado.
     * @return true si el cambio de estado fue exitoso en base de datos; false en caso contrario.
     * @throws SQLException Si ocurre un error al actualizar el plan o insertar la validación.
     */
    @Override
    public boolean changeStatus(int planId, int statusId, String commentary) throws SQLException {
        String sql = "UPDATE PlanFamiliar SET Estado = ? WHERE IdPlanFamiliar = ?";
        
        // Abrir conexión y preparar la actualización de estado del plan familiar
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, String.valueOf(statusId));
            ps.setInt(2, planId);
            
            // Ejecutar la actualización de estado
            int rows = ps.executeUpdate();

            // Si se actualizó el plan familiar y se suministró un comentario de observaciones
            if (rows > 0 && commentary != null) {
                // Sentencia SQL para registrar el historial de validación del supervisor
                String valSql = "INSERT INTO ValidacionPlan (IdPlanFamiliar, IdSupervisor, Fecha, Estado, Comentario) VALUES (?, 2, ?, ?, ?)";
                
                // Preparar e insertar registro de validación del plan familiar
                try (PreparedStatement psV = conn.prepareStatement(valSql)) {
                    psV.setInt(1, planId);
                    psV.setDate(2, Date.valueOf(LocalDate.now()));
                    psV.setString(3, "Rechazado");
                    psV.setString(4, commentary);
                    
                    // Ejecutar inserción de validación
                    psV.executeUpdate();
                }
            }
            // Retornar true si la actualización inicial modificó al menos un registro
            return rows > 0;
        }
    }

    /**
     * {@inheritDoc}
     * Actualiza el tipo de familia/vivienda en la caché de memoria extraData.
     *
     * @param planId Identificador único del plan familiar.
     * @param familyTypeId Identificador del tipo de familia/vivienda a asignar.
     * @return true si la actualización en memoria fue exitosa.
     * @throws SQLException Si ocurre un error al cambiar el tipo de familia.
     */
    @Override
    public boolean changeFamilyType(int planId, int familyTypeId) throws SQLException {
        // Obtener o crear una entrada en la caché en memoria para el plan familiar indicado
        Map<String, Object> extra = extraData.computeIfAbsent("plan_" + planId, k -> new HashMap<>());
        
        // Actualizar el valor del tipo de familia en la caché
        extra.put("family_type_id", familyTypeId);
        
        // Retornar verdadero indicando éxito
        return true;
    }
}

package com.defensacivil.dao;

import com.defensacivil.config.DatabaseConfig;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación de la interfaz {@link VulnerabilidadDAO} que gestiona la persistencia
 * de factores de riesgo, vulnerabilidades estructurales y respuestas del test mediante consultas JDBC directas
 * y el almacenamiento temporal de datos complementarios en memoria.
 */
public class VulnerabilidadDAOImpl implements VulnerabilidadDAO {

    /**
     * Mapa de almacenamiento en memoria para persistir temporalmente información adicional
     * de los factores de riesgo (ej. descripción, distancia).
     */
    private final Map<String, Map<String, Object>> extraData;

    /**
     * Constructor de la clase.
     *
     * @param extraData Mapa para persistir en memoria información adicional.
     */
    public VulnerabilidadDAOImpl(Map<String, Map<String, Object>> extraData) {
        // Asignar la referencia del mapa recibido como parámetro al atributo local
        this.extraData = extraData;
    }

    /**
     * {@inheritDoc}
     * Obtiene una lista simplificada de factores de riesgo con su nombre de amenaza y ubicación concatenados.
     *
     * @param planId Identificador único del plan familiar.
     * @return Una lista de mapas con el ID y el nombre descriptivo de cada factor de riesgo.
     * @throws SQLException Si ocurre un error de base de datos durante la lectura.
     */
    @Override
    public List<Map<String, Object>> getRiskFactorsForSelect(int planId) throws SQLException {
        // Inicializar la lista que guardará los factores de riesgo simplificados
        List<Map<String, Object>> list = new ArrayList<>();
        
        // Sentencia SQL parametrizada con un JOIN para obtener el nombre de la amenaza y la ubicación
        String sql = """
            SELECT f.IdFactorRiesgo, t.Nombre AS AmenazaNombre, f.Ubicacion
            FROM FactorRiesgo f
            JOIN TipoAmenaza t ON f.IdTipoAmenaza = t.IdTipoAmenaza
            WHERE f.IdPlanFamiliar = ?
            """;
        
        // Abrir conexión y preparar la consulta SQL parametrizada
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Asignar el parámetro del ID de plan familiar
            ps.setInt(1, planId);
            
            // Ejecutar la consulta SQL en la base de datos
            try (ResultSet rs = ps.executeQuery()) {
                // Recorrer de forma iterativa los registros devueltos
                while (rs.next()) {
                    Map<String, Object> risk = new HashMap<>();
                    
                    // Llenar la información básica del factor de riesgo para el selector
                    risk.put("id", rs.getInt("IdFactorRiesgo"));
                    risk.put("name", rs.getString("AmenazaNombre") + " - " + rs.getString("Ubicacion"));
                    
                    // Adicionar el mapa a la lista resultante
                    list.add(risk);
                }
            }
        }
        // Retornar la lista
        return list;
    }

    /**
     * {@inheritDoc}
     * Obtiene todos los factores de riesgo de un plan, combinando campos de la base de datos con extraData.
     *
     * @param planId Identificador único del plan familiar.
     * @return Una lista de mapas con los detalles de cada factor de riesgo.
     * @throws SQLException Si ocurre un error de base de datos durante la consulta.
     */
    @Override
    public List<Map<String, Object>> getRiskFactorsByPlan(int planId) throws SQLException {
        // Inicializar la lista que guardará los factores de riesgo obtenidos
        List<Map<String, Object>> list = new ArrayList<>();
        
        // Consulta SQL para traer los detalles de los factores de riesgo filtrando por plan familiar
        String sql = """
            SELECT f.IdFactorRiesgo, f.IdTipoAmenaza, f.Ubicacion, f.AccionReduccion, f.Distancia, t.Nombre AS AmenazaNombre
            FROM FactorRiesgo f
            JOIN TipoAmenaza t ON f.IdTipoAmenaza = t.IdTipoAmenaza
            WHERE f.IdPlanFamiliar = ?
            """;
        
        // Obtener conexión y preparar la sentencia SQL de forma segura
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Asignar el ID del plan familiar en la sentencia SQL
            ps.setInt(1, planId);
            
            // Ejecutar la consulta SQL en base de datos
            try (ResultSet rs = ps.executeQuery()) {
                // Iterar sobre los registros de factores de riesgo recuperados de la BD
                while (rs.next()) {
                    Map<String, Object> risk = new HashMap<>();
                    int riskId = rs.getInt("IdFactorRiesgo");
                    String amenazaNombre = rs.getString("AmenazaNombre");
                    
                    // Mapear los campos obtenidos de la base de datos relacional
                    risk.put("id", riskId);
                    risk.put("threat_type_id", rs.getInt("IdTipoAmenaza"));
                    risk.put("threat_type_name", amenazaNombre);
                    
                    // Crear estructura anidada para encajar con el formato esperado por el frontend
                    risk.put("threat_type", Map.of("id", rs.getInt("IdTipoAmenaza"), "name", amenazaNombre));
                    risk.put("ubication", rs.getString("Ubicacion"));

                    // Recuperar campos complementarios directamente persistidos físicamente en la BD
                    risk.put("description", rs.getString("AccionReduccion") != null ? rs.getString("AccionReduccion") : "");
                    Object distanceVal = rs.getObject("Distancia");
                    risk.put("distance", distanceVal != null ? distanceVal : "");
                    risk.put("family_plan_id", planId);
                    
                    // Añadir el mapa estructurado a la lista final
                    list.add(risk);
                }
            }
        }
        // Retornar la lista de factores de riesgo detallados
        return list;
    }

    /**
     * {@inheritDoc}
     * Obtiene los detalles de un factor de riesgo por su ID, cruzándolo con extraData en memoria.
     *
     * @param riskId Identificador único del factor de riesgo.
     * @return Un mapa con los datos detallados del factor de riesgo, o un mapa vacío si no existe.
     * @throws SQLException Si ocurre un error de base de datos durante la búsqueda.
     */
    @Override
    public Map<String, Object> getRiskFactorById(int riskId) throws SQLException {
        // Consulta SQL con JOIN para buscar un factor de riesgo por su clave primaria
        String sql = """
            SELECT f.IdFactorRiesgo, f.IdTipoAmenaza, f.Ubicacion, f.AccionReduccion, f.Distancia, t.Nombre AS AmenazaNombre
            FROM FactorRiesgo f
            JOIN TipoAmenaza t ON f.IdTipoAmenaza = t.IdTipoAmenaza
            WHERE f.IdFactorRiesgo = ?
            """;
        
        // Inicializar el mapa de respuesta
        Map<String, Object> risk = new HashMap<>();
        
        // Abrir conexión y preparar la consulta SQL
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Asignar el parámetro del ID de factor de riesgo
            ps.setInt(1, riskId);
            
            // Ejecutar la consulta SQL en la base de datos
            try (ResultSet rs = ps.executeQuery()) {
                // Si el factor de riesgo existe
                if (rs.next()) {
                    String amenazaNombre = rs.getString("AmenazaNombre");
                    
                    // Mapear los campos del ResultSet
                    risk.put("id", rs.getInt("IdFactorRiesgo"));
                    risk.put("threat_type_id", rs.getInt("IdTipoAmenaza"));
                    risk.put("threat_type_name", amenazaNombre);
                    risk.put("threat_type", Map.of("id", rs.getInt("IdTipoAmenaza"), "name", amenazaNombre));
                    risk.put("ubication", rs.getString("Ubicacion"));

                    // Recuperar campos complementarios directamente de la BD
                    risk.put("description", rs.getString("AccionReduccion") != null ? rs.getString("AccionReduccion") : "");
                    Object distanceVal = rs.getObject("Distancia");
                    risk.put("distance", distanceVal != null ? distanceVal : "");
                }
            }
        }
        // Retornar el mapa del factor de riesgo
        return risk;
    }

    /**
     * {@inheritDoc}
     * Obtiene todos los factores de riesgo registrados a nivel general en la base de datos.
     *
     * @return Una lista de mapas con la información básica de todos los factores de riesgo.
     * @throws SQLException Si ocurre un error de base de datos durante la lectura.
     */
    @Override
    public List<Map<String, Object>> getAllRiskFactors() throws SQLException {
        // Inicializar la lista resultante
        List<Map<String, Object>> list = new ArrayList<>();
        
        // Sentencia SQL para recuperar todos los registros sin filtro por plan
        String sql = """
            SELECT f.IdFactorRiesgo, f.IdPlanFamiliar, f.IdTipoAmenaza, f.Ubicacion, t.Nombre AS AmenazaNombre
            FROM FactorRiesgo f
            JOIN TipoAmenaza t ON f.IdTipoAmenaza = t.IdTipoAmenaza
            """;
        
        // Abrir conexión, preparar consulta y ejecutarla
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            // Recorrer iterativamente todos los registros devueltos
            while (rs.next()) {
                int threatTypeId = rs.getInt("IdTipoAmenaza");
                String threatTypeName = rs.getString("AmenazaNombre");
                
                // Mapear los datos de cada fila
                Map<String, Object> risk = new HashMap<>();
                risk.put("id", rs.getInt("IdFactorRiesgo"));
                risk.put("family_plan_id", rs.getInt("IdPlanFamiliar"));
                risk.put("threat_type_id", threatTypeId);
                risk.put("threat_type_name", threatTypeName);
                risk.put("threat_type", Map.of("id", threatTypeId, "name", threatTypeName != null ? threatTypeName : ""));
                risk.put("ubication", rs.getString("Ubicacion"));
                
                // Adicionar a la lista global
                list.add(risk);
            }
        }
        // Retornar la lista
        return list;
    }

    /**
     * {@inheritDoc}
     * Inserta un factor de riesgo y almacena sus campos extra (descripción y distancia) en la caché de memoria.
     *
     * @param body Mapa que contiene los parámetros para insertar (threat_type_id, ubication/location, description, distance, family_plan_id).
     * @return El ID autogenerado del nuevo factor de riesgo registrado, o 0 si no se insertó.
     * @throws SQLException Si ocurre un error al ejecutar la inserción SQL.
     */
    @Override
    public int addRiskFactor(Map<String, Object> body) throws SQLException {
        Object threatObj = body.get("threat_type_id");
        int threatId = 1;
        
        // Decodificar el threat_type_id recibido
        if (threatObj instanceof Number) {
            threatId = ((Number) threatObj).intValue();
        } else if (threatObj instanceof String) {
            threatId = Integer.parseInt((String) threatObj);
        }

        // Obtener la ubicación soportando las claves "ubication" y "location"
        String ubicacion = body.containsKey("ubication") ? (String) body.get("ubication") : (String) body.get("location");
        String description = (String) body.get("description");
        
        // Obtener la distancia
        Float distance = null;
        if (body.get("distance") != null) {
            try {
                distance = Float.parseFloat(String.valueOf(body.get("distance")));
            } catch (NumberFormatException ignored) {}
        }

        Object planIdObj = body.get("family_plan_id");
        int planId = 1;
        
        // Decodificar el family_plan_id
        if (planIdObj instanceof Number) {
            planId = ((Number) planIdObj).intValue();
        } else if (planIdObj instanceof String) {
            planId = Integer.parseInt((String) planIdObj);
        }

        // Asignar descripción por defecto en la BD relacional si es nula
        String accionReduccion = description != null && !description.isEmpty() ? description : "Sin descripción";

        // Sentencia SQL parametrizada de inserción
        String sql = "INSERT INTO FactorRiesgo (IdPlanFamiliar, IdTipoAmenaza, Ubicacion, AccionReduccion, Distancia) VALUES (?, ?, ?, ?, ?)";
        
        // Abrir conexión y preparar la inserción indicando retorno de llaves generadas
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            // Configurar los parámetros de inserción
            ps.setInt(1, planId);
            ps.setInt(2, threatId);
            ps.setString(3, ubicacion != null ? ubicacion : "");
            ps.setString(4, accionReduccion);
            if (distance != null) {
                ps.setFloat(5, distance);
            } else {
                ps.setNull(5, java.sql.Types.FLOAT);
            }
            
            // Ejecutar la inserción física
            ps.executeUpdate();

            int generatedId = 0;
            
            // Recuperar la clave autogenerada
            try (ResultSet rs = ps.getGeneratedKeys()) {
                // Si la base de datos generó el ID correctamente
                if (rs.next()) {
                    generatedId = rs.getInt(1);
                }
            }
            
            // Retornar el identificador autogenerado
            return generatedId;
        }
    }

    /**
     * {@inheritDoc}
     * Actualiza el factor de riesgo en la base de datos y sus campos extra en memoria caché.
     *
     * @param riskId Identificador único del factor de riesgo a actualizar.
     * @param body Mapa con los nuevos valores a asignar.
     * @return true si la actualización fue exitosa; false en caso contrario.
     * @throws SQLException Si ocurre un error durante el UPDATE en la base de datos.
     */
    @Override
    public boolean updateRiskFactor(int riskId, Map<String, Object> body) throws SQLException {
        Object threatObj = body.get("threat_type_id");
        int threatId = 1;
        
        // Decodificar el ID del tipo de amenaza
        if (threatObj instanceof Number) {
            threatId = ((Number) threatObj).intValue();
        } else if (threatObj instanceof String) {
            threatId = Integer.parseInt((String) threatObj);
        }

        // Obtener ubicación soportando ambas variantes de clave
        String ubicacion = body.containsKey("ubication") ? (String) body.get("ubication") : (String) body.get("location");
        String description = (String) body.get("description");
        
        // Obtener distancia
        Float distance = null;
        if (body.get("distance") != null) {
            try {
                distance = Float.parseFloat(String.valueOf(body.get("distance")));
            } catch (NumberFormatException ignored) {}
        }

        // Asignar descripción por defecto
        String accionReduccion = description != null && !description.isEmpty() ? description : "Sin descripción";

        // Sentencia SQL de actualización parametrizada
        String sql = "UPDATE FactorRiesgo SET IdTipoAmenaza = ?, Ubicacion = ?, AccionReduccion = ?, Distancia = ? WHERE IdFactorRiesgo = ?";
        
        boolean success = false;
        // Abrir conexión y preparar la actualización
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Configurar parámetros dinámicos del PreparedStatement
            ps.setInt(1, threatId);
            ps.setString(2, ubicacion != null ? ubicacion : "");
            ps.setString(3, accionReduccion);
            if (distance != null) {
                ps.setFloat(4, distance);
            } else {
                ps.setNull(4, java.sql.Types.FLOAT);
            }
            ps.setInt(5, riskId);
            
            // Ejecutar la actualización
            success = ps.executeUpdate() > 0;
        }
        // Retornar verdadero indicando éxito si afectó filas
        return success;
    }

    /**
     * {@inheritDoc}
     * Elimina transaccionalmente un factor de riesgo, removiendo primero sus dependencias en la tabla Vulnerabilidad.
     *
     * @param riskId Identificador único del factor de riesgo a eliminar.
     * @return true si la eliminación del factor de riesgo y dependencias fue exitosa; false en caso contrario.
     * @throws SQLException Si ocurre un error de base de datos durante la transacción o borrado.
     */
    @Override
    public boolean deleteRiskFactor(int riskId) throws SQLException {
        // Abrir conexión a la base de datos para administrar la transacción de eliminación de forma explícita
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Desactivar confirmación automática para controlar la transacción
            conn.setAutoCommit(false);
            
            // Bloque try-catch para revertir cambios ante errores de clave foránea
            try {
                // 1. Eliminar primero las vulnerabilidades asociadas (registros hijos)
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Vulnerabilidad WHERE IdFactorRiesgo = ?")) {
                    ps.setInt(1, riskId);
                    ps.executeUpdate();
                }
                
                // 2. Eliminar el factor de riesgo (registro padre)
                int affectedRows = 0;
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM FactorRiesgo WHERE IdFactorRiesgo = ?")) {
                    ps.setInt(1, riskId);
                    affectedRows = ps.executeUpdate();
                }
                
                // Confirmar transacción al haberse ejecutado ambos borrados correctamente
                conn.commit();
                
                // Retornar verdadero si se borró al menos una fila del factor de riesgo principal
                return affectedRows > 0;
            } catch (SQLException e) {
                // Reversar toda la transacción ante cualquier excepción SQL
                conn.rollback();
                throw e;
            } finally {
                // Garantizar que la confirmación automática vuelva a activarse
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * {@inheritDoc}
     * Obtiene las vulnerabilidades estructurales asociadas a un factor de riesgo mediante un JOIN.
     *
     * @param riskId Identificador único del factor de riesgo.
     * @return Una lista de mapas con las vulnerabilidades y sus grados.
     * @throws SQLException Si ocurre un error de base de datos al realizar la consulta.
     */
    @Override
    public List<Map<String, Object>> getVulnerabilitiesByRiskFactor(int riskId) throws SQLException {
        // Inicializar la lista resultante
        List<Map<String, Object>> list = new ArrayList<>();
        
        // Consulta SQL con JOIN para traer las vulnerabilidades del factor de riesgo indicado
        String sql = """
            SELECT v.IdVulnerabilidad, v.IdTipoVulnerabilidad, v.Grado, vt.Nombre AS VulnNombre
            FROM Vulnerabilidad v
            JOIN VulnerabilidadTipo vt ON v.IdTipoVulnerabilidad = vt.IdTipoVulnerabilidad
            WHERE v.IdFactorRiesgo = ?
            """;
        
        // Abrir conexión JDBC y preparar la sentencia parametrizada
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Asignar el parámetro del ID de factor de riesgo
            ps.setInt(1, riskId);
            
            // Ejecutar la consulta SQL
            try (ResultSet rs = ps.executeQuery()) {
                // Recorrer iterativamente las vulnerabilidades encontradas
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    
                    // Mapear los datos simulando el formato esperado por el frontend
                    item.put("id", rs.getInt("IdVulnerabilidad"));
                    item.put("vulnerability", Map.of("name", rs.getString("VulnNombre")));
                    item.put("vulnerability_grade", Map.of("name", rs.getString("Grado") != null ? rs.getString("Grado") : ""));
                    
                    // Agregar a la lista
                    list.add(item);
                }
            }
        }
        // Retornar la lista
        return list;
    }

    /**
     * {@inheritDoc}
     * Busca y obtiene la información de una vulnerabilidad específica por su ID.
     *
     * @param vulnerabilityId Identificador único del factor de vulnerabilidad.
     * @return Un mapa con los datos de la vulnerabilidad, o un mapa vacío si no existe.
     * @throws SQLException Si ocurre un error de base de datos durante la consulta.
     */
    @Override
    public Map<String, Object> getVulnerabilityById(int vulnerabilityId) throws SQLException {
        // Sentencia SQL parametrizada de búsqueda única
        String sql = """
            SELECT v.IdVulnerabilidad, v.IdFactorRiesgo, v.IdTipoVulnerabilidad, v.Grado, vt.Nombre AS VulnNombre
            FROM Vulnerabilidad v
            JOIN VulnerabilidadTipo vt ON v.IdTipoVulnerabilidad = vt.IdTipoVulnerabilidad
            WHERE v.IdVulnerabilidad = ?
            """;
        
        // Inicializar mapa de respuesta
        Map<String, Object> item = new HashMap<>();
        
        // Abrir conexión y preparar la consulta
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Asignar el parámetro del ID de la vulnerabilidad
            ps.setInt(1, vulnerabilityId);
            
            // Ejecutar y evaluar el ResultSet
            try (ResultSet rs = ps.executeQuery()) {
                // Si la vulnerabilidad existe en la base de datos
                if (rs.next()) {
                    // Mapear los datos de la fila
                    item.put("id", rs.getInt("IdVulnerabilidad"));
                    item.put("vulnerability_id", rs.getInt("IdTipoVulnerabilidad"));
                    item.put("vulnerability", Map.of("name", rs.getString("VulnNombre")));
                    item.put("vulnerability_grade_id", 1); // Mock por defecto para id del grado
                    item.put("vulnerability_grade", Map.of("name", rs.getString("Grado") != null ? rs.getString("Grado") : ""));
                    item.put("risk_factor_id", rs.getInt("IdFactorRiesgo"));
                }
            }
        }
        // Retornar el mapa de la vulnerabilidad
        return item;
    }

    /**
     * {@inheritDoc}
     * Inserta un nuevo registro de vulnerabilidad traduciendo el ID de grado a su descripción textual.
     *
     * @param body Mapa con los parámetros del registro de vulnerabilidad (vulnerability_id, vulnerability_grade_id, risk_factor_id).
     * @return true si la inserción en la base de datos fue exitosa; false en caso contrario.
     * @throws SQLException Si ocurre un error durante el INSERT.
     */
    @Override
    public boolean addVulnerability(Map<String, Object> body) throws SQLException {
        Object vulnIdObj = body.get("vulnerability_id");
        int vulnId = 1;
        
        // Decodificar el ID de tipo de vulnerabilidad
        if (vulnIdObj instanceof Number) {
            vulnId = ((Number) vulnIdObj).intValue();
        } else if (vulnIdObj instanceof String && !((String) vulnIdObj).isEmpty()) {
            vulnId = Integer.parseInt((String) vulnIdObj);
        }

        Object gradeObj = body.get("vulnerability_grade_id");
        int gradeId = 1;
        
        // Decodificar el ID del grado de vulnerabilidad
        if (gradeObj instanceof Number) {
            gradeId = ((Number) gradeObj).intValue();
        } else if (gradeObj instanceof String && !((String) gradeObj).isEmpty()) {
            gradeId = Integer.parseInt((String) gradeObj);
        }
        
        // Traducir el ID del grado a una descripción en texto en español
        String gradeStr = (gradeId == 1) ? "Bajo" : (gradeId == 2 ? "Medio" : "Alto");

        Object riskIdObj = body.get("risk_factor_id");
        int riskId = 1;
        
        // Decodificar el ID del factor de riesgo asociado
        if (riskIdObj instanceof Number) {
            riskId = ((Number) riskIdObj).intValue();
        } else if (riskIdObj instanceof String && !((String) riskIdObj).isEmpty()) {
            riskId = Integer.parseInt((String) riskIdObj);
        }

        // Sentencia SQL parametrizada de inserción física
        String sql = "INSERT INTO Vulnerabilidad (IdFactorRiesgo, IdTipoVulnerabilidad, Grado) VALUES (?, ?, ?)";
        
        // Abrir conexión y preparar la inserción
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Asignar parámetros al PreparedStatement
            ps.setInt(1, riskId);
            ps.setInt(2, vulnId);
            ps.setString(3, gradeStr);
            
            // Ejecutar la inserción y almacenar filas afectadas
            int rows = ps.executeUpdate();
            
            // Retornar verdadero si se insertó el registro
            return rows > 0;
        }
    }

    /**
     * {@inheritDoc}
     * Actualiza el tipo y grado de vulnerabilidad de un registro existente.
     *
     * @param vulnerabilityId Identificador único del factor de vulnerabilidad a actualizar.
     * @param body Mapa con los nuevos valores a asignar.
     * @return true si la actualización en base de datos fue exitosa; false en caso contrario.
     * @throws SQLException Si ocurre un error al ejecutar la actualización.
     */
    @Override
    public boolean updateVulnerability(int vulnerabilityId, Map<String, Object> body) throws SQLException {
        Object vulnIdObj = body.get("vulnerability_id");
        int vulnId = 1;
        
        // Decodificar el ID de tipo de vulnerabilidad
        if (vulnIdObj instanceof Number) {
            vulnId = ((Number) vulnIdObj).intValue();
        }

        Object gradeObj = body.get("vulnerability_grade_id");
        int gradeId = 1;
        
        // Decodificar el grado de vulnerabilidad
        if (gradeObj instanceof Number) {
            gradeId = ((Number) gradeObj).intValue();
        }
        
        // Traducir a texto el grado
        String gradeStr = (gradeId == 1) ? "Bajo" : (gradeId == 2 ? "Medio" : "Alto");

        // Sentencia SQL de actualización parametrizada
        String sql = "UPDATE Vulnerabilidad SET IdTipoVulnerabilidad = ?, Grado = ? WHERE IdVulnerabilidad = ?";
        
        // Abrir conexión y preparar la actualización
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Asignar parámetros dinámicos
            ps.setInt(1, vulnId);
            ps.setString(2, gradeStr);
            ps.setInt(3, vulnerabilityId);
            
            // Ejecutar actualización
            int rows = ps.executeUpdate();
            
            // Retornar verdadero si afectó registros
            return rows > 0;
        }
    }

    /**
     * {@inheritDoc}
     * Elimina físicamente una vulnerabilidad estructural por su ID.
     *
     * @param vulnerabilityId Identificador único de la vulnerabilidad a eliminar.
     * @return true si la eliminación física del registro fue exitosa; false en caso contrario.
     * @throws SQLException Si ocurre un error durante el DELETE en la base de datos.
     */
    @Override
    public boolean deleteVulnerability(int vulnerabilityId) throws SQLException {
        // Sentencia SQL parametrizada de borrado por clave primaria
        String sql = "DELETE FROM Vulnerabilidad WHERE IdVulnerabilidad = ?";
        
        // Abrir conexión y preparar la sentencia
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Asignar parámetro de ID de vulnerabilidad a eliminar
            ps.setInt(1, vulnerabilityId);
            
            // Ejecutar borrado
            int rows = ps.executeUpdate();
            
            // Retornar verdadero si se borró la fila
            return rows > 0;
        }
    }

    /**
     * {@inheritDoc}
     * Registra o actualiza de manera condicional (upsert) la respuesta del test de vulnerabilidad.
     *
     * @param planId Identificador único del plan familiar.
     * @param questionId Identificador único de la pregunta.
     * @param answer Respuesta booleana (true/false).
     * @return true si la inserción o actualización fue exitosa; false en caso contrario.
     * @throws SQLException Si ocurre un error de base de datos durante las consultas.
     */
    @Override
    public boolean saveOrUpdateVulnerableTestAnswer(int planId, int questionId, boolean answer) throws SQLException {
        // Abrir conexión a la base de datos
        try (Connection conn = DatabaseConfig.getConnection()) {
            int existingId = 0;
            String checkSql = "SELECT IdRespuestaPlan FROM RespuestaPlan WHERE IdPregunta = ? AND IdPlanFamiliar = ?";
            
            // 1. Validar si ya existe una respuesta previa registrada para la pregunta y plan familiar
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setInt(1, questionId);
                checkPs.setInt(2, planId);
                
                // Ejecutar la consulta de verificación
                try (ResultSet rs = checkPs.executeQuery()) {
                    // Si ya existe la respuesta
                    if (rs.next()) {
                        // Guardar su clave primaria
                        existingId = rs.getInt(1);
                    }
                }
            }

            // 2. Si existe un registro previo, proceder con una sentencia SQL de tipo UPDATE para modificar el valor
            if (existingId > 0) {
                String updateSql = "UPDATE RespuestaPlan SET Valor = ? WHERE IdRespuestaPlan = ?";
                
                // Preparar y ejecutar la actualización del valor de la respuesta
                try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                    updatePs.setBoolean(1, answer);
                    updatePs.setInt(2, existingId);
                    int rows = updatePs.executeUpdate();
                    
                    // Retornar verdadero si se actualizó el registro
                    return rows > 0;
                }
            } else {
                // En caso contrario, proceder a insertar un nuevo registro de respuesta en la base de datos
                String insertSql = "INSERT INTO RespuestaPlan (IdPregunta, IdPlanFamiliar, Valor) VALUES (?, ?, ?)";
                
                // Preparar y ejecutar la inserción de la respuesta
                try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                    insertPs.setInt(1, questionId);
                    insertPs.setInt(2, planId);
                    insertPs.setBoolean(3, answer);
                    int rows = insertPs.executeUpdate();
                    
                    // Retornar verdadero si se insertó con éxito
                    return rows > 0;
                }
            }
        }
    }
}

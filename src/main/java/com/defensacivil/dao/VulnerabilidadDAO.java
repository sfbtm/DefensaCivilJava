package com.defensacivil.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Interfaz que define las operaciones de acceso a datos (DAO) para la gestión
 * de factores de riesgo, factores de vulnerabilidad física/estructural
 * y el registro de respuestas al test de vulnerabilidad.
 * Sirve como el contrato de persistencia para toda la información de amenazas
 * y susceptibilidades asociadas a los planes de emergencia familiar.
 */
public interface VulnerabilidadDAO {

    // === Factor de Riesgo (FactorRiesgo) ===

    /**
     * Obtiene una lista simplificada de factores de riesgo registrados para un plan familiar,
     * usualmente empleada para llenar componentes de selección (dropdowns/comboboxes) en la interfaz gráfica.
     *
     * @param planId Identificador único del plan familiar del cual se desean obtener los factores de riesgo.
     * @return Una lista de mapas, donde cada mapa contiene el identificador único del factor ("id") y una descripción formateada ("name").
     * @throws SQLException Si ocurre un error de acceso, sintaxis o conexión a la base de datos.
     */
    List<Map<String, Object>> getRiskFactorsForSelect(int planId) throws SQLException;

    /**
     * Obtiene la información detallada de todos los factores de riesgo asociados a un plan familiar en particular.
     *
     * @param planId Identificador único del plan familiar.
     * @return Una lista de mapas que contienen los detalles de cada factor de riesgo (id, threat_type_id, threat_type_name, threat_type, ubication, description, distance, family_plan_id).
     * @throws SQLException Si ocurre un error de base de datos durante la consulta.
     */
    List<Map<String, Object>> getRiskFactorsByPlan(int planId) throws SQLException;

    /**
     * Busca y obtiene la información detallada de un factor de riesgo específico a partir de su identificador único.
     *
     * @param riskId Identificador único del factor de riesgo que se desea consultar.
     * @return Un mapa con los datos del factor de riesgo (claves como "id", "threat_type_id", "ubication", "description", "distance"), o un mapa vacío si no existe.
     * @throws SQLException Si ocurre un error de lectura o conexión con la base de datos.
     */
    Map<String, Object> getRiskFactorById(int riskId) throws SQLException;

    /**
     * Obtiene todos los factores de riesgo registrados globalmente en el sistema, sin aplicar filtros por plan familiar.
     *
     * @return Una lista de mapas con la información básica y de ubicación de todos los factores de riesgo del sistema.
     * @throws SQLException Si ocurre un error al ejecutar la consulta SQL global.
     */
    List<Map<String, Object>> getAllRiskFactors() throws SQLException;

    /**
     * Registra un nuevo factor de riesgo asociado a un plan familiar en la base de datos y memoria.
     *
     * @param body Mapa que contiene los datos del factor de riesgo (claves esperadas: "family_plan_id", "threat_type_id", "ubication" o "location", "description", "distance").
     * @return El identificador autogenerado (clave primaria) del nuevo factor de riesgo insertado, o -1 en caso de fallo.
     * @throws SQLException Si ocurre un error de restricción de integridad o sintaxis SQL al realizar la inserción.
     */
    int addRiskFactor(Map<String, Object> body) throws SQLException;

    /**
     * Actualiza la información (amenaza, ubicación, descripción, distancia) de un factor de riesgo existente.
     *
     * @param riskId Identificador único del factor de riesgo que se va a actualizar.
     * @param body Mapa con los nuevos datos a persistir (claves esperadas: "threat_type_id", "ubication" o "location", "description", "distance").
     * @return true si la actualización del registro fue exitosa en la base de datos; false en caso contrario.
     * @throws SQLException Si ocurre un error durante la actualización del registro.
     */
    boolean updateRiskFactor(int riskId, Map<String, Object> body) throws SQLException;

    /**
     * Elimina físicamente un factor de riesgo de la base de datos, incluyendo la eliminación previa de sus vulnerabilidades dependientes.
     *
     * @param riskId Identificador único del factor de riesgo a eliminar.
     * @return true si el factor de riesgo fue eliminado con éxito; false en caso de que no existiera o fallara la operación.
     * @throws SQLException Si ocurre un error durante el borrado (ej. error transaccional de claves foráneas).
     */
    boolean deleteRiskFactor(int riskId) throws SQLException;


    // === Factores de Vulnerabilidad (Vulnerabilidad) ===

    /**
     * Obtiene los factores de vulnerabilidad física/estructural asociados a un factor de riesgo específico.
     *
     * @param riskId Identificador único del factor de riesgo.
     * @return Una lista de mapas, donde cada mapa contiene información de la vulnerabilidad (claves: "id", "vulnerability", "vulnerability_grade").
     * @throws SQLException Si ocurre un error al consultar las vulnerabilidades en la base de datos.
     */
    List<Map<String, Object>> getVulnerabilitiesByRiskFactor(int riskId) throws SQLException;

    /**
     * Busca y obtiene la información de un factor de vulnerabilidad específico por su identificador único.
     *
     * @param vulnerabilityId Identificador único de la vulnerabilidad.
     * @return Un mapa con los datos detallados de la vulnerabilidad (claves: "id", "vulnerability_id", "vulnerability", "vulnerability_grade_id", "vulnerability_grade", "risk_factor_id"), o un mapa vacío si no existe.
     * @throws SQLException Si ocurre un error en la consulta JDBC.
     */
    Map<String, Object> getVulnerabilityById(int vulnerabilityId) throws SQLException;

    /**
     * Registra una nueva vulnerabilidad física/estructural asociada a un factor de riesgo determinado.
     *
     * @param body Mapa con los campos a registrar (claves esperadas: "risk_factor_id", "vulnerability_id", "vulnerability_grade_id").
     * @return true si el registro de la vulnerabilidad se realizó con éxito; false en caso contrario.
     * @throws SQLException Si ocurre un error al ejecutar la sentencia INSERT de la vulnerabilidad.
     */
    boolean addVulnerability(Map<String, Object> body) throws SQLException;

    /**
     * Actualiza el tipo y/o grado de una vulnerabilidad física/estructural existente.
     *
     * @param vulnerabilityId Identificador único del factor de vulnerabilidad a actualizar.
     * @param body Mapa con los nuevos datos a asignar (claves esperadas: "vulnerability_id", "vulnerability_grade_id").
     * @return true si la actualización de la vulnerabilidad fue exitosa; false en caso contrario.
     * @throws SQLException Si ocurre un error de base de datos durante la actualización.
     */
    boolean updateVulnerability(int vulnerabilityId, Map<String, Object> body) throws SQLException;

    /**
     * Elimina físicamente un factor de vulnerabilidad de la base de datos a partir de su ID.
     *
     * @param vulnerabilityId Identificador único del factor de vulnerabilidad a eliminar.
     * @return true si la eliminación del registro fue exitosa; false en caso contrario.
     * @throws SQLException Si ocurre un error al ejecutar el DELETE en la base de datos.
     */
    boolean deleteVulnerability(int vulnerabilityId) throws SQLException;


    // === Respuestas del Test de Vulnerabilidad (RespuestaPlan) ===

    /**
     * Registra o actualiza la respuesta brindada a una pregunta del test de vulnerabilidad para un plan familiar específico.
     * Implementa lógica de tipo "upsert" (insertar si no existe, actualizar si ya existe).
     *
     * @param planId Identificador único del plan familiar.
     * @param questionId Identificador único de la pregunta de la encuesta/test de vulnerabilidad.
     * @param answer Valor booleano de la respuesta dada (true para "Sí", false para "No").
     * @return true si la operación de inserción o actualización fue completada exitosamente; false en caso contrario.
     * @throws SQLException Si ocurre un error de base de datos al buscar, insertar o actualizar la respuesta.
     */
    boolean saveOrUpdateVulnerableTestAnswer(int planId, int questionId, boolean answer) throws SQLException;

    // === Acciones de Reducción de Riesgo (AccionReduccion) ===

    /**
     * Obtiene la lista de acciones de reducción asociadas a un factor de riesgo.
     */
    List<Map<String, Object>> getReductionActionsByRiskFactor(int riskId) throws SQLException;

    /**
     * Obtiene el detalle de una acción de reducción individual por su ID.
     */
    Map<String, Object> getReductionActionById(int idVal) throws SQLException;

    /**
     * Agrega una nueva acción de reducción de riesgo.
     */
    int addReductionAction(Map<String, Object> body) throws SQLException;

    /**
     * Actualiza una acción de reducción de riesgo existente.
     */
    boolean updateReductionAction(int idVal, Map<String, Object> body) throws SQLException;

    /**
     * Elimina una acción de reducción de riesgo existente.
     */
    boolean deleteReductionAction(int idVal) throws SQLException;
}

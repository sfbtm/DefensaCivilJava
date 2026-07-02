package com.defensacivil.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Interfaz que define las operaciones de acceso a datos (DAO) para la gestión
 * de factores de riesgo, factores de vulnerabilidad física/estructural
 * y registro de respuestas al test de vulnerabilidad.
 */
public interface VulnerabilidadDAO {

    // === Factor de Riesgo (FactorRiesgo) ===

    /**
     * Obtiene una lista simplificada de factores de riesgo registrados para un plan familiar
     * para su uso en componentes de selección.
     *
     * @param planId Identificador único del plan familiar.
     * @return Una lista de mapas con el ID ("id") y la descripción ("name") de cada factor de riesgo.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    List<Map<String, Object>> getRiskFactorsForSelect(int planId) throws SQLException;

    /**
     * Obtiene la información detallada de los factores de riesgo de un plan familiar.
     *
     * @param planId Identificador único del plan familiar.
     * @return Una lista de mapas con los detalles de cada factor de riesgo (ubicación, tipo de amenaza, etc.).
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    List<Map<String, Object>> getRiskFactorsByPlan(int planId) throws SQLException;

    /**
     * Busca y obtiene la información de un factor de riesgo por su identificador único.
     *
     * @param riskId Identificador único del factor de riesgo.
     * @return Un mapa con los datos del factor de riesgo, o null si no se encuentra.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    Map<String, Object> getRiskFactorById(int riskId) throws SQLException;

    /**
     * Obtiene todos los factores de riesgo registrados globalmente en el sistema.
     *
     * @return Una lista de mapas con la información básica de todos los factores de riesgo.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    List<Map<String, Object>> getAllRiskFactors() throws SQLException;

    /**
     * Registra un nuevo factor de riesgo en el sistema.
     *
     * @param body Mapa que contiene los datos del factor de riesgo (ej. plan_id, threat_type_id, location).
     * @return El identificador autogenerado del nuevo factor de riesgo, o -1 en caso de error.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    int addRiskFactor(Map<String, Object> body) throws SQLException;

    /**
     * Actualiza la información de un factor de riesgo existente.
     *
     * @param riskId Identificador único del factor de riesgo a actualizar.
     * @param body Mapa con los nuevos datos.
     * @return true si la actualización fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean updateRiskFactor(int riskId, Map<String, Object> body) throws SQLException;

    /**
     * Elimina físicamente un factor de riesgo de la base de datos.
     *
     * @param riskId Identificador único del factor de riesgo a eliminar.
     * @return true si la eliminación fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean deleteRiskFactor(int riskId) throws SQLException;


    // === Factores de Vulnerabilidad (Vulnerabilidad) ===

    /**
     * Obtiene los factores de vulnerabilidad asociados a un factor de riesgo específico.
     *
     * @param riskId Identificador único del factor de riesgo.
     * @return Una lista de mapas con los detalles de cada vulnerabilidad (tipo, factor, etc.).
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    List<Map<String, Object>> getVulnerabilitiesByRiskFactor(int riskId) throws SQLException;

    /**
     * Busca y obtiene la información de un factor de vulnerabilidad por su identificador único.
     *
     * @param vulnerabilityId Identificador único del factor de vulnerabilidad.
     * @return Un mapa con los datos del factor de vulnerabilidad, o null si no se encuentra.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    Map<String, Object> getVulnerabilityById(int vulnerabilityId) throws SQLException;

    /**
     * Registra una nueva vulnerabilidad física/estructural asociada a un factor de riesgo.
     *
     * @param body Mapa con los campos a registrar (ej. risk_factor_id, vulnerability_type_id, factor).
     * @return true si el registro fue exitoso, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean addVulnerability(Map<String, Object> body) throws SQLException;

    /**
     * Actualiza la información de un factor de vulnerabilidad existente.
     *
     * @param vulnerabilityId Identificador único del factor de vulnerabilidad a actualizar.
     * @param body Mapa con los nuevos datos a asignar.
     * @return true si la actualización fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean updateVulnerability(int vulnerabilityId, Map<String, Object> body) throws SQLException;

    /**
     * Elimina físicamente un factor de vulnerabilidad de la base de datos.
     *
     * @param vulnerabilityId Identificador único del factor de vulnerabilidad a eliminar.
     * @return true si la eliminación fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean deleteVulnerability(int vulnerabilityId) throws SQLException;


    // === Respuestas del Test de Vulnerabilidad (RespuestaPlan) ===

    /**
     * Registra o actualiza la respuesta brindada a una pregunta del test de vulnerabilidad para un plan familiar.
     *
     * @param planId Identificador único del plan familiar.
     * @param questionId Identificador único de la pregunta del test.
     * @param answer Valor booleano de la respuesta (true para Sí, false para No).
     * @return true si el guardado/actualización fue exitoso, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean saveOrUpdateVulnerableTestAnswer(int planId, int questionId, boolean answer) throws SQLException;
}

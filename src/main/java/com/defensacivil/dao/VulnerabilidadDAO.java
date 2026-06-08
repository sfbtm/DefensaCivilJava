package com.defensacivil.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface VulnerabilidadDAO {
    // FactorRiesgo (Factores de Riesgo)
    List<Map<String, Object>> getRiskFactorsForSelect(int planId) throws SQLException;
    List<Map<String, Object>> getRiskFactorsByPlan(int planId) throws SQLException;
    Map<String, Object> getRiskFactorById(int riskId) throws SQLException;
    List<Map<String, Object>> getAllRiskFactors() throws SQLException;
    int addRiskFactor(Map<String, Object> body) throws SQLException;
    boolean updateRiskFactor(int riskId, Map<String, Object> body) throws SQLException;
    boolean deleteRiskFactor(int riskId) throws SQLException;

    // Vulnerabilidad (Factores de Vulnerabilidad)
    List<Map<String, Object>> getVulnerabilitiesByRiskFactor(int riskId) throws SQLException;
    Map<String, Object> getVulnerabilityById(int vulnerabilityId) throws SQLException;
    boolean addVulnerability(Map<String, Object> body) throws SQLException;
    boolean updateVulnerability(int vulnerabilityId, Map<String, Object> body) throws SQLException;
    boolean deleteVulnerability(int vulnerabilityId) throws SQLException;

    // RespuestaPlan (Catálogos de Preguntas / Test)
    boolean saveOrUpdateVulnerableTestAnswer(int planId, int questionId, boolean answer) throws SQLException;
}

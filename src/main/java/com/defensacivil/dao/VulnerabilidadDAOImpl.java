package com.defensacivil.dao;

import com.defensacivil.config.DatabaseConfig;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VulnerabilidadDAOImpl implements VulnerabilidadDAO {

    private final Map<String, Map<String, Object>> extraData;

    public VulnerabilidadDAOImpl(Map<String, Map<String, Object>> extraData) {
        this.extraData = extraData;
    }

    @Override
    public List<Map<String, Object>> getRiskFactorsForSelect(int planId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT f.IdFactorRiesgo, t.Nombre AS AmenazaNombre, f.Ubicacion
            FROM FactorRiesgo f
            JOIN TipoAmenaza t ON f.IdTipoAmenaza = t.IdTipoAmenaza
            WHERE f.IdPlanFamiliar = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> risk = new HashMap<>();
                    risk.put("id", rs.getInt("IdFactorRiesgo"));
                    risk.put("name", rs.getString("AmenazaNombre") + " - " + rs.getString("Ubicacion"));
                    list.add(risk);
                }
            }
        }
        return list;
    }

    @Override
    public List<Map<String, Object>> getRiskFactorsByPlan(int planId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT f.IdFactorRiesgo, f.IdTipoAmenaza, f.Ubicacion, f.AccionReduccion, t.Nombre AS AmenazaNombre
            FROM FactorRiesgo f
            JOIN TipoAmenaza t ON f.IdTipoAmenaza = t.IdTipoAmenaza
            WHERE f.IdPlanFamiliar = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> risk = new HashMap<>();
                    int riskId = rs.getInt("IdFactorRiesgo");
                    String amenazaNombre = rs.getString("AmenazaNombre");
                    risk.put("id", riskId);
                    risk.put("threat_type_id", rs.getInt("IdTipoAmenaza"));
                    risk.put("threat_type_name", amenazaNombre);
                    risk.put("threat_type", Map.of("id", rs.getInt("IdTipoAmenaza"), "name", amenazaNombre));
                    risk.put("ubication", rs.getString("Ubicacion"));

                    Map<String, Object> extra = extraData.getOrDefault("risk_" + riskId, Map.of());
                    risk.put("description", extra.getOrDefault("description", rs.getString("AccionReduccion") != null ? rs.getString("AccionReduccion") : ""));
                    risk.put("distance", extra.getOrDefault("distance", ""));
                    risk.put("family_plan_id", planId);
                    list.add(risk);
                }
            }
        }
        return list;
    }

    @Override
    public Map<String, Object> getRiskFactorById(int riskId) throws SQLException {
        String sql = """
            SELECT f.IdFactorRiesgo, f.IdTipoAmenaza, f.Ubicacion, f.AccionReduccion, t.Nombre AS AmenazaNombre
            FROM FactorRiesgo f
            JOIN TipoAmenaza t ON f.IdTipoAmenaza = t.IdTipoAmenaza
            WHERE f.IdFactorRiesgo = ?
            """;
        Map<String, Object> risk = new HashMap<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, riskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String amenazaNombre = rs.getString("AmenazaNombre");
                    risk.put("id", rs.getInt("IdFactorRiesgo"));
                    risk.put("threat_type_id", rs.getInt("IdTipoAmenaza"));
                    risk.put("threat_type_name", amenazaNombre);
                    risk.put("threat_type", Map.of("id", rs.getInt("IdTipoAmenaza"), "name", amenazaNombre));
                    risk.put("ubication", rs.getString("Ubicacion"));

                    Map<String, Object> extra = extraData.getOrDefault("risk_" + riskId, Map.of());
                    risk.put("description", extra.getOrDefault("description", rs.getString("AccionReduccion") != null ? rs.getString("AccionReduccion") : ""));
                    risk.put("distance", extra.getOrDefault("distance", ""));
                }
            }
        }
        return risk;
    }

    @Override
    public List<Map<String, Object>> getAllRiskFactors() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT f.IdFactorRiesgo, f.IdPlanFamiliar, f.IdTipoAmenaza, f.Ubicacion, t.Nombre AS AmenazaNombre
            FROM FactorRiesgo f
            JOIN TipoAmenaza t ON f.IdTipoAmenaza = t.IdTipoAmenaza
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int threatTypeId = rs.getInt("IdTipoAmenaza");
                String threatTypeName = rs.getString("AmenazaNombre");
                Map<String, Object> risk = new HashMap<>();
                risk.put("id", rs.getInt("IdFactorRiesgo"));
                risk.put("family_plan_id", rs.getInt("IdPlanFamiliar"));
                risk.put("threat_type_id", threatTypeId);
                risk.put("threat_type_name", threatTypeName);
                risk.put("threat_type", Map.of("id", threatTypeId, "name", threatTypeName != null ? threatTypeName : ""));
                risk.put("ubication", rs.getString("Ubicacion"));
                list.add(risk);
            }
        }
        return list;
    }

    @Override
    public int addRiskFactor(Map<String, Object> body) throws SQLException {
        Object threatObj = body.get("threat_type_id");
        int threatId = 1;
        if (threatObj instanceof Number) threatId = ((Number) threatObj).intValue();
        else if (threatObj instanceof String) threatId = Integer.parseInt((String) threatObj);

        String ubicacion = body.containsKey("ubication") ? (String) body.get("ubication") : (String) body.get("location");
        String description = (String) body.get("description");
        String distanceStr = body.get("distance") != null ? String.valueOf(body.get("distance")) : "";

        Object planIdObj = body.get("family_plan_id");
        int planId = 1;
        if (planIdObj instanceof Number) planId = ((Number) planIdObj).intValue();
        else if (planIdObj instanceof String) planId = Integer.parseInt((String) planIdObj);

        String accionReduccion = description != null && !description.isEmpty() ? description : "Sin descripción";

        String sql = "INSERT INTO FactorRiesgo (IdPlanFamiliar, IdTipoAmenaza, Ubicacion, AccionReduccion) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, planId);
            ps.setInt(2, threatId);
            ps.setString(3, ubicacion != null ? ubicacion : "");
            ps.setString(4, accionReduccion);
            ps.executeUpdate();

            int generatedId = 0;
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    generatedId = rs.getInt(1);
                }
            }
            if (generatedId > 0) {
                Map<String, Object> extra = new HashMap<>();
                extra.put("description", description != null ? description : "");
                extra.put("distance", distanceStr);
                extraData.put("risk_" + generatedId, extra);
            }
            return generatedId;
        }
    }

    @Override
    public boolean updateRiskFactor(int riskId, Map<String, Object> body) throws SQLException {
        Object threatObj = body.get("threat_type_id");
        int threatId = 1;
        if (threatObj instanceof Number) threatId = ((Number) threatObj).intValue();
        else if (threatObj instanceof String) threatId = Integer.parseInt((String) threatObj);

        String ubicacion = body.containsKey("ubication") ? (String) body.get("ubication") : (String) body.get("location");
        String description = (String) body.get("description");
        String distanceStr = body.get("distance") != null ? String.valueOf(body.get("distance")) : "";

        String accionReduccion = description != null && !description.isEmpty() ? description : "Sin descripción";

        String sql = "UPDATE FactorRiesgo SET IdTipoAmenaza = ?, Ubicacion = ?, AccionReduccion = ? WHERE IdFactorRiesgo = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, threatId);
            ps.setString(2, ubicacion != null ? ubicacion : "");
            ps.setString(3, accionReduccion);
            ps.setInt(4, riskId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                Map<String, Object> extra = extraData.computeIfAbsent("risk_" + riskId, k -> new HashMap<>());
                extra.put("description", description != null ? description : "");
                extra.put("distance", distanceStr);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean deleteRiskFactor(int riskId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Delete child vulnerabilities first
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Vulnerabilidad WHERE IdFactorRiesgo = ?")) {
                    ps.setInt(1, riskId);
                    ps.executeUpdate();
                }
                // 2. Delete the risk factor itself
                int affectedRows = 0;
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM FactorRiesgo WHERE IdFactorRiesgo = ?")) {
                    ps.setInt(1, riskId);
                    affectedRows = ps.executeUpdate();
                }
                conn.commit();
                extraData.remove("risk_" + riskId);
                return affectedRows > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public List<Map<String, Object>> getVulnerabilitiesByRiskFactor(int riskId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT v.IdVulnerabilidad, v.IdTipoVulnerabilidad, v.Grado, vt.Nombre AS VulnNombre
            FROM Vulnerabilidad v
            JOIN VulnerabilidadTipo vt ON v.IdTipoVulnerabilidad = vt.IdTipoVulnerabilidad
            WHERE v.IdFactorRiesgo = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, riskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getInt("IdVulnerabilidad"));
                    item.put("vulnerability", Map.of("name", rs.getString("VulnNombre")));
                    item.put("vulnerability_grade", Map.of("name", rs.getString("Grado") != null ? rs.getString("Grado") : ""));
                    list.add(item);
                }
            }
        }
        return list;
    }

    @Override
    public Map<String, Object> getVulnerabilityById(int vulnerabilityId) throws SQLException {
        String sql = """
            SELECT v.IdVulnerabilidad, v.IdFactorRiesgo, v.IdTipoVulnerabilidad, v.Grado, vt.Nombre AS VulnNombre
            FROM Vulnerabilidad v
            JOIN VulnerabilidadTipo vt ON v.IdTipoVulnerabilidad = vt.IdTipoVulnerabilidad
            WHERE v.IdVulnerabilidad = ?
            """;
        Map<String, Object> item = new HashMap<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vulnerabilityId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    item.put("id", rs.getInt("IdVulnerabilidad"));
                    item.put("vulnerability_id", rs.getInt("IdTipoVulnerabilidad"));
                    item.put("vulnerability", Map.of("name", rs.getString("VulnNombre")));
                    item.put("vulnerability_grade_id", 1);
                    item.put("vulnerability_grade", Map.of("name", rs.getString("Grado") != null ? rs.getString("Grado") : ""));
                    item.put("risk_factor_id", rs.getInt("IdFactorRiesgo"));
                }
            }
        }
        return item;
    }

    @Override
    public boolean addVulnerability(Map<String, Object> body) throws SQLException {
        Object vulnIdObj = body.get("vulnerability_id");
        int vulnId = 1;
        if (vulnIdObj instanceof Number) vulnId = ((Number) vulnIdObj).intValue();
        else if (vulnIdObj instanceof String && !((String) vulnIdObj).isEmpty()) vulnId = Integer.parseInt((String) vulnIdObj);

        Object gradeObj = body.get("vulnerability_grade_id");
        int gradeId = 1;
        if (gradeObj instanceof Number) gradeId = ((Number) gradeObj).intValue();
        else if (gradeObj instanceof String && !((String) gradeObj).isEmpty()) gradeId = Integer.parseInt((String) gradeObj);
        String gradeStr = (gradeId == 1) ? "Bajo" : (gradeId == 2 ? "Medio" : "Alto");

        Object riskIdObj = body.get("risk_factor_id");
        int riskId = 1;
        if (riskIdObj instanceof Number) riskId = ((Number) riskIdObj).intValue();
        else if (riskIdObj instanceof String && !((String) riskIdObj).isEmpty()) riskId = Integer.parseInt((String) riskIdObj);

        String sql = "INSERT INTO Vulnerabilidad (IdFactorRiesgo, IdTipoVulnerabilidad, Grado) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, riskId);
            ps.setInt(2, vulnId);
            ps.setString(3, gradeStr);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }

    @Override
    public boolean updateVulnerability(int vulnerabilityId, Map<String, Object> body) throws SQLException {
        Object vulnIdObj = body.get("vulnerability_id");
        int vulnId = 1;
        if (vulnIdObj instanceof Number) vulnId = ((Number) vulnIdObj).intValue();

        Object gradeObj = body.get("vulnerability_grade_id");
        int gradeId = 1;
        if (gradeObj instanceof Number) gradeId = ((Number) gradeObj).intValue();
        String gradeStr = (gradeId == 1) ? "Bajo" : (gradeId == 2 ? "Medio" : "Alto");

        String sql = "UPDATE Vulnerabilidad SET IdTipoVulnerabilidad = ?, Grado = ? WHERE IdVulnerabilidad = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vulnId);
            ps.setString(2, gradeStr);
            ps.setInt(3, vulnerabilityId);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }

    @Override
    public boolean deleteVulnerability(int vulnerabilityId) throws SQLException {
        String sql = "DELETE FROM Vulnerabilidad WHERE IdVulnerabilidad = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vulnerabilityId);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }

    @Override
    public boolean saveOrUpdateVulnerableTestAnswer(int planId, int questionId, boolean answer) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            int existingId = 0;
            String checkSql = "SELECT IdRespuestaPlan FROM RespuestaPlan WHERE IdPregunta = ? AND IdPlanFamiliar = ?";
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setInt(1, questionId);
                checkPs.setInt(2, planId);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        existingId = rs.getInt(1);
                    }
                }
            }

            if (existingId > 0) {
                String updateSql = "UPDATE RespuestaPlan SET Valor = ? WHERE IdRespuestaPlan = ?";
                try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                    updatePs.setBoolean(1, answer);
                    updatePs.setInt(2, existingId);
                    int rows = updatePs.executeUpdate();
                    return rows > 0;
                }
            } else {
                String insertSql = "INSERT INTO RespuestaPlan (IdPregunta, IdPlanFamiliar, Valor) VALUES (?, ?, ?)";
                try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                    insertPs.setInt(1, questionId);
                    insertPs.setInt(2, planId);
                    insertPs.setBoolean(3, answer);
                    int rows = insertPs.executeUpdate();
                    return rows > 0;
                }
            }
        }
    }
}

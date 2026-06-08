package com.defensacivil.dao;

import com.defensacivil.config.DatabaseConfig;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlanComplementarioDAOImpl implements PlanComplementarioDAO {

    private final Map<String, Map<String, Object>> extraData;

    public PlanComplementarioDAOImpl(Map<String, Map<String, Object>> extraData) {
        this.extraData = extraData;
    }

    // --- HOUSING INFO & GRAPHICS ---

    @Override
    public Map<String, Object> getHousingInfo(int planId, int typeId) throws SQLException {
        int esEntornoVal = (typeId == 2) ? 1 : 0;
        String sql = "SELECT IdGrafico, RutaImagen FROM GraficoVivienda WHERE IdPlanFamiliar = ? AND EsEntorno = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, planId);
            ps.setInt(2, esEntornoVal);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getInt("IdGrafico"));
                    item.put("path", rs.getString("RutaImagen"));
                    item.put("family_plan_id", planId);
                    return item;
                }
            }
        }
        return null;
    }

    @Override
    public boolean saveOrUpdateHousingGraphic(int planId, String savedFileName, String description, int esEntornoVal) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            boolean exists = false;
            if (esEntornoVal == 1) {
                String checkSql = "SELECT IdGrafico FROM GraficoVivienda WHERE IdPlanFamiliar = ? AND EsEntorno = ?";
                try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                    checkPs.setInt(1, planId);
                    checkPs.setInt(2, esEntornoVal);
                    try (ResultSet rs = checkPs.executeQuery()) {
                        if (rs.next()) {
                            exists = true;
                        }
                    }
                }
            }

            if (exists) {
                String updateSql = "UPDATE GraficoVivienda SET RutaImagen = ?, Descripcion = ? WHERE IdPlanFamiliar = ? AND EsEntorno = ?";
                try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                    updatePs.setString(1, savedFileName);
                    updatePs.setString(2, description);
                    updatePs.setInt(3, planId);
                    updatePs.setInt(4, esEntornoVal);
                    return updatePs.executeUpdate() > 0;
                }
            } else {
                String insertSql = "INSERT INTO GraficoVivienda (IdPlanFamiliar, RutaImagen, Descripcion, EsEntorno) VALUES (?, ?, ?, ?)";
                try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                    insertPs.setInt(1, planId);
                    insertPs.setString(2, savedFileName);
                    insertPs.setString(3, description);
                    insertPs.setInt(4, esEntornoVal);
                    return insertPs.executeUpdate() > 0;
                }
            }
        }
    }

    @Override
    public List<Map<String, Object>> getHousingGraphicsByPlan(int planId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT IdGrafico, RutaImagen, Descripcion FROM GraficoVivienda WHERE IdPlanFamiliar = ? AND EsEntorno = 0";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getInt("IdGrafico"));
                    item.put("path", rs.getString("RutaImagen"));
                    item.put("description", rs.getString("Descripcion") != null ? rs.getString("Descripcion") : "");
                    list.add(item);
                }
            }
        }
        return list;
    }

    @Override
    public Map<String, Object> getHousingGraphicById(int id) throws SQLException {
        String sql = "SELECT IdGrafico, RutaImagen, Descripcion, IdPlanFamiliar FROM GraficoVivienda WHERE IdGrafico = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getInt("IdGrafico"));
                    item.put("path", rs.getString("RutaImagen"));
                    item.put("description", rs.getString("Descripcion") != null ? rs.getString("Descripcion") : "");
                    item.put("family_plan_id", rs.getInt("IdPlanFamiliar"));
                    return item;
                }
            }
        }
        return null;
    }

    @Override
    public boolean updateHousingGraphicDescription(int id, String description) throws SQLException {
        String sql = "UPDATE GraficoVivienda SET Descripcion = ? WHERE IdGrafico = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, description != null ? description : "");
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deleteHousingGraphic(int id) throws SQLException {
        String sql = "DELETE FROM GraficoVivienda WHERE IdGrafico = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // --- ACTION PLANS & ACTIONS ---

    @Override
    public boolean hasActionPlan(int planId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM PlanAccion pa JOIN FactorRiesgo fr ON pa.IdFactorRiesgo = fr.IdFactorRiesgo WHERE fr.IdPlanFamiliar = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    @Override
    public Map<String, Object> getActionPlanByPlan(int planId) throws SQLException {
        String sql = "SELECT pa.IdPlanAccion, pa.IdCoordinador FROM PlanAccion pa JOIN FactorRiesgo fr ON pa.IdFactorRiesgo = fr.IdFactorRiesgo WHERE fr.IdPlanFamiliar = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getInt("IdPlanAccion"));
                    item.put("family_plan_id", planId);
                    item.put("coordinator_id", rs.getInt("IdCoordinador"));
                    return item;
                }
            }
        }
        return null;
    }

    @Override
    public boolean createActionPlan(int planId, int coordinatorId) throws SQLException {
        String sql = "INSERT INTO PlanAccion (IdFactorRiesgo, IdCoordinador) VALUES (?, ?)";
        try (Connection conn = DatabaseConfig.getConnection()) {
            int riskId = 0;
            try (PreparedStatement psR = conn.prepareStatement("SELECT IdFactorRiesgo FROM FactorRiesgo WHERE IdPlanFamiliar = ? LIMIT 1")) {
                psR.setInt(1, planId);
                try (ResultSet rsR = psR.executeQuery()) {
                    if (rsR.next()) riskId = rsR.getInt(1);
                }
            }

            if (riskId == 0) {
                try (PreparedStatement psR = conn.prepareStatement("INSERT INTO FactorRiesgo (IdPlanFamiliar, IdTipoAmenaza, Ubicacion) VALUES (?, 1, 'General')", Statement.RETURN_GENERATED_KEYS)) {
                    psR.setInt(1, planId);
                    psR.executeUpdate();
                    try (ResultSet rsR = psR.getGeneratedKeys()) {
                        if (rsR.next()) riskId = rsR.getInt(1);
                    }
                }
            }

            int coordId = coordinatorId;
            if (coordId == 0) {
                try (PreparedStatement psM = conn.prepareStatement("SELECT IdIntegrante FROM Integrante WHERE IdPlanFamiliar = ? LIMIT 1")) {
                    psM.setInt(1, planId);
                    try (ResultSet rsM = psM.executeQuery()) {
                        if (rsM.next()) coordId = rsM.getInt(1);
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, riskId);
                ps.setInt(2, coordId);
                return ps.executeUpdate() > 0;
            }
        }
    }

    @Override
    public List<Map<String, Object>> getActionsByActionPlan(int actionPlanId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT a.IdAccion, a.IdResponsable, a.Etapa, a.Descripcion, i.Nombre, i.Apellido FROM Accion a JOIN Integrante i ON a.IdResponsable = i.IdIntegrante WHERE a.IdPlanAccion = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, actionPlanId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getInt("IdAccion"));
                    item.put("description", rs.getString("Descripcion"));
                    item.put("stage", rs.getString("Etapa"));
                    item.put("member_id", rs.getInt("IdResponsable"));
                    item.put("member", Map.of(
                        "names", rs.getString("Nombre"),
                        "last_names", rs.getString("Apellido")
                    ));
                    list.add(item);
                }
            }
        }
        return list;
    }

    @Override
    public Map<String, Object> getActionById(int id) throws SQLException {
        String sql = "SELECT a.IdAccion, a.IdResponsable, a.Etapa, a.Descripcion, i.Nombre, i.Apellido FROM Accion a JOIN Integrante i ON a.IdResponsable = i.IdIntegrante WHERE a.IdAccion = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getInt("IdAccion"));
                    item.put("description", rs.getString("Descripcion"));
                    item.put("stage", rs.getString("Etapa"));
                    item.put("member_id", rs.getInt("IdResponsable"));
                    item.put("member", Map.of(
                        "names", rs.getString("Nombre"),
                        "last_names", rs.getString("Apellido")
                    ));
                    return item;
                }
            }
        }
        return null;
    }

    @Override
    public boolean insertAction(int actionPlanId, int memberId, String stage, String description) throws SQLException {
        String sql = "INSERT INTO Accion (IdPlanAccion, IdResponsable, Etapa, Descripcion) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, actionPlanId);
            ps.setInt(2, memberId);
            ps.setString(3, stage);
            ps.setString(4, description);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean updateAction(int id, int memberId, String description) throws SQLException {
        String sql = "UPDATE Accion SET IdResponsable = ?, Descripcion = ? WHERE IdAccion = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            ps.setString(2, description);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deleteAction(int id) throws SQLException {
        String sql = "DELETE FROM Accion WHERE IdAccion = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // --- PET VACCINES ---

    @Override
    public List<Map<String, Object>> getVaccinesByPet(int petId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT IdVacuna, Nombre FROM Vacunas WHERE IdMascota = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, petId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int vacId = rs.getInt("IdVacuna");
                    Map<String, Object> extra = extraData.getOrDefault("vaccine_" + vacId, Map.of());
                    String dateVal = (String) extra.getOrDefault("date", java.time.LocalDate.now().toString());
                    Map<String, Object> vaccine = new HashMap<>();
                    vaccine.put("id", vacId);
                    vaccine.put("name", rs.getString("Nombre") != null ? rs.getString("Nombre") : "");
                    vaccine.put("date", dateVal);
                    list.add(vaccine);
                }
            }
        }
        return list;
    }

    @Override
    public Map<String, Object> getVaccineById(int vaccineId) throws SQLException {
        String sql = "SELECT IdVacuna, Nombre, IdMascota FROM Vacunas WHERE IdVacuna = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vaccineId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int vacId = rs.getInt("IdVacuna");
                    Map<String, Object> vac = new HashMap<>();
                    vac.put("id", vacId);
                    vac.put("name", rs.getString("Nombre"));
                    vac.put("pet_id", rs.getInt("IdMascota"));
                    Map<String, Object> extra = extraData.getOrDefault("vaccine_" + vacId, Map.of());
                    String dateVal = (String) extra.getOrDefault("date", java.time.LocalDate.now().toString());
                    vac.put("date", dateVal);
                    return vac;
                }
            }
        }
        return null;
    }

    @Override
    public int insertVaccine(Map<String, Object> body) throws SQLException {
        String name = (String) body.get("name");
        String dateStr = (String) body.get("date");
        Object petIdObj = body.get("pet_id");
        int petId = 1;
        if (petIdObj instanceof Number) petId = ((Number) petIdObj).intValue();
        else if (petIdObj instanceof String) petId = Integer.parseInt((String) petIdObj);

        String sql = "INSERT INTO Vacunas (Nombre, IdMascota) VALUES (?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, petId);
            ps.executeUpdate();

            int generatedId = 0;
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    generatedId = rs.getInt(1);
                }
            }

            if (generatedId > 0) {
                Map<String, Object> extra = new HashMap<>();
                extra.put("date", dateStr != null ? dateStr : java.time.LocalDate.now().toString());
                extraData.put("vaccine_" + generatedId, extra);
            }
            return generatedId;
        }
    }

    @Override
    public boolean updateVaccine(int vaccineId, Map<String, Object> body) throws SQLException {
        String name = (String) body.get("name");
        String dateStr = (String) body.get("date");

        String sql = "UPDATE Vacunas SET Nombre = ? WHERE IdVacuna = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, vaccineId);
            boolean updated = ps.executeUpdate() > 0;

            if (updated) {
                Map<String, Object> extra = extraData.computeIfAbsent("vaccine_" + vaccineId, k -> new HashMap<>());
                extra.put("date", dateStr != null ? dateStr : java.time.LocalDate.now().toString());
            }
            return updated;
          }
      }

    @Override
    public boolean deleteVaccine(int vaccineId) throws SQLException {
        String sql = "DELETE FROM Vacunas WHERE IdVacuna = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vaccineId);
            boolean deleted = ps.executeUpdate() > 0;
            if (deleted) {
                extraData.remove("vaccine_" + vaccineId);
            }
            return deleted;
        }
    }
}

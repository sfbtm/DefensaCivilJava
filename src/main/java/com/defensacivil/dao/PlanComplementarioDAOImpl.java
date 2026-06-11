package com.defensacivil.dao;

import com.defensacivil.config.DatabaseConfig;
import com.defensacivil.dto.HousingInfoDTO;
import com.defensacivil.dto.ActionPlanDTO;
import com.defensacivil.dto.ActionDTO;
import com.defensacivil.dto.VaccineDTO;
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
    public HousingInfoDTO getHousingInfo(int planId, int typeId) throws SQLException {
        int esEntornoVal = (typeId == 2) ? 1 : 0;
        String sql = "SELECT IdGrafico, RutaImagen, Descripcion FROM GraficoVivienda WHERE IdPlanFamiliar = ? AND EsEntorno = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, planId);
            ps.setInt(2, esEntornoVal);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    HousingInfoDTO dto = new HousingInfoDTO();
                    dto.setId(rs.getInt("IdGrafico"));
                    dto.setPath(rs.getString("RutaImagen"));
                    dto.setDescription(rs.getString("Descripcion"));
                    dto.setFamily_plan_id(planId);
                    return dto;
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
    public List<HousingInfoDTO> getHousingGraphicsByPlan(int planId) throws SQLException {
        List<HousingInfoDTO> list = new ArrayList<>();
        String sql = "SELECT IdGrafico, RutaImagen, Descripcion FROM GraficoVivienda WHERE IdPlanFamiliar = ? AND EsEntorno = 0";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HousingInfoDTO dto = new HousingInfoDTO();
                    dto.setId(rs.getInt("IdGrafico"));
                    dto.setPath(rs.getString("RutaImagen"));
                    dto.setDescription(rs.getString("Descripcion") != null ? rs.getString("Descripcion") : "");
                    dto.setFamily_plan_id(planId);
                    list.add(dto);
                }
            }
        }
        return list;
    }

    @Override
    public HousingInfoDTO getHousingGraphicById(int id) throws SQLException {
        String sql = "SELECT IdGrafico, RutaImagen, Descripcion, IdPlanFamiliar FROM GraficoVivienda WHERE IdGrafico = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    HousingInfoDTO dto = new HousingInfoDTO();
                    dto.setId(rs.getInt("IdGrafico"));
                    dto.setPath(rs.getString("RutaImagen"));
                    dto.setDescription(rs.getString("Descripcion") != null ? rs.getString("Descripcion") : "");
                    dto.setFamily_plan_id(rs.getInt("IdPlanFamiliar"));
                    return dto;
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
    public ActionPlanDTO getActionPlanByPlan(int planId) throws SQLException {
        String sql = "SELECT pa.IdPlanAccion, pa.IdCoordinador FROM PlanAccion pa JOIN FactorRiesgo fr ON pa.IdFactorRiesgo = fr.IdFactorRiesgo WHERE fr.IdPlanFamiliar = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ActionPlanDTO dto = new ActionPlanDTO();
                    dto.setId(rs.getInt("IdPlanAccion"));
                    dto.setFamily_plan_id(planId);
                    dto.setCoordinator_id(rs.getInt("IdCoordinador"));
                    return dto;
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
    public List<ActionDTO> getActionsByActionPlan(int actionPlanId) throws SQLException {
        List<ActionDTO> list = new ArrayList<>();
        String sql = "SELECT a.IdAccion, a.IdResponsable, a.Etapa, a.Descripcion, i.Nombre, i.Apellido " +
                     "FROM Accion a " +
                     "JOIN Integrante i ON a.IdResponsable = i.IdIntegrante " +
                     "WHERE a.IdPlanAccion = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, actionPlanId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ActionDTO dto = new ActionDTO();
                    dto.setId(rs.getInt("IdAccion"));
                    dto.setDescription(rs.getString("Descripcion"));
                    dto.setStage(rs.getString("Etapa"));
                    dto.setMember_id(rs.getInt("IdResponsable"));

                    ActionDTO.MemberInfo member = new ActionDTO.MemberInfo();
                    member.setNames(rs.getString("Nombre"));
                    member.setLast_names(rs.getString("Apellido"));
                    dto.setMember(member);

                    list.add(dto);
                }
            }
        }
        return list;
    }

    @Override
    public ActionDTO getActionById(int id) throws SQLException {
        String sql = "SELECT a.IdAccion, a.IdResponsable, a.Etapa, a.Descripcion, i.Nombre, i.Apellido " +
                     "FROM Accion a " +
                     "JOIN Integrante i ON a.IdResponsable = i.IdIntegrante " +
                     "WHERE a.IdAccion = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ActionDTO dto = new ActionDTO();
                    dto.setId(rs.getInt("IdAccion"));
                    dto.setDescription(rs.getString("Descripcion"));
                    dto.setStage(rs.getString("Etapa"));
                    dto.setMember_id(rs.getInt("IdResponsable"));

                    ActionDTO.MemberInfo member = new ActionDTO.MemberInfo();
                    member.setNames(rs.getString("Nombre"));
                    member.setLast_names(rs.getString("Apellido"));
                    dto.setMember(member);

                    return dto;
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
    public List<VaccineDTO> getVaccinesByPet(int petId) throws SQLException {
        List<VaccineDTO> list = new ArrayList<>();
        String sql = "SELECT IdVacuna, Nombre FROM Vacunas WHERE IdMascota = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, petId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int vacId = rs.getInt("IdVacuna");
                    Map<String, Object> extra = extraData.getOrDefault("vaccine_" + vacId, Map.of());
                    String dateVal = (String) extra.getOrDefault("date", java.time.LocalDate.now().toString());

                    VaccineDTO dto = new VaccineDTO();
                    dto.setId(vacId);
                    dto.setName(rs.getString("Nombre") != null ? rs.getString("Nombre") : "");
                    dto.setDate(dateVal);
                    dto.setPet_id(petId);
                    list.add(dto);
                }
            }
        }
        return list;
    }

    @Override
    public VaccineDTO getVaccineById(int vaccineId) throws SQLException {
        String sql = "SELECT IdVacuna, Nombre, IdMascota FROM Vacunas WHERE IdVacuna = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vaccineId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int vacId = rs.getInt("IdVacuna");
                    Map<String, Object> extra = extraData.getOrDefault("vaccine_" + vacId, Map.of());
                    String dateVal = (String) extra.getOrDefault("date", java.time.LocalDate.now().toString());

                    VaccineDTO dto = new VaccineDTO();
                    dto.setId(vacId);
                    dto.setName(rs.getString("Nombre"));
                    dto.setPet_id(rs.getInt("IdMascota"));
                    dto.setDate(dateVal);
                    return dto;
                }
            }
        }
        return null;
    }

    @Override
    public int insertVaccine(VaccineDTO dto) throws SQLException {
        String name = dto.getName();
        String dateStr = dto.getDate();
        Integer petId = dto.getPet_id();

        String sql = "INSERT INTO Vacunas (Nombre, IdMascota) VALUES (?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
           ps.setString(1, name);
           ps.setInt(2, petId != null ? petId : 1);
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
    public boolean updateVaccine(int vaccineId, VaccineDTO dto) throws SQLException {
        String name = dto.getName();
        String dateStr = dto.getDate();

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

    // --- AVAILABLE RESOURCES ---

    @Override
    public List<Map<String, Object>> getAvailableResourcesByPlan(int planId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT rd.IdRecurso, rd.Ubicacion, rd.Distancia, rd.Telefono, rt.Nombre AS RecursoNombre, s.Nombre AS ServicioNombre
            FROM RecursoDisponible rd
            JOIN RecursoTipo rt ON rd.IdRecursoTipo = rt.IdRecursoTipo
            JOIN Servicio s ON rd.IdServicio = s.IdServicio
            WHERE rd.IdPlanFamiliar = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    int resourceId = rs.getInt("IdRecurso");
                    item.put("id", resourceId);
                    item.put("resource_name", rs.getString("RecursoNombre"));
                    item.put("location", rs.getString("Ubicacion") != null ? rs.getString("Ubicacion") : "");
                    item.put("distance", rs.getFloat("Distancia"));
                    item.put("service", rs.getString("ServicioNombre") != null ? rs.getString("ServicioNombre") : "");
                    
                    // Load description from extraData
                    Map<String, Object> extra = extraData.getOrDefault("resource_" + resourceId, Map.of());
                    item.put("description", extra.getOrDefault("description", ""));
                    item.put("phone", rs.getString("Telefono") != null ? rs.getString("Telefono") : "");
                    list.add(item);
                }
            }
        }
        return list;
    }

    @Override
    public Map<String, Object> getAvailableResourceById(int idVal) throws SQLException {
        Map<String, Object> item = new HashMap<>();
        String sql = """
            SELECT rd.IdRecurso, rd.IdRecursoTipo, rd.IdServicio, rd.Ubicacion, rd.Distancia, rd.Telefono, rt.Nombre AS RecursoNombre
            FROM RecursoDisponible rd
            JOIN RecursoTipo rt ON rd.IdRecursoTipo = rt.IdRecursoTipo
            WHERE rd.IdRecurso = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVal);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    item.put("id", rs.getInt("IdRecurso"));
                    item.put("phone", rs.getString("Telefono") != null ? rs.getString("Telefono") : "");
                    item.put("distance", rs.getFloat("Distancia"));
                    item.put("location", rs.getString("Ubicacion") != null ? rs.getString("Ubicacion") : "");
                    item.put("resource_id", rs.getInt("IdRecursoTipo"));
                    item.put("resource_name", rs.getString("RecursoNombre"));
                    
                    Map<String, Object> extra = extraData.getOrDefault("resource_" + idVal, Map.of());
                    item.put("description", extra.getOrDefault("description", ""));
                }
            }
        }
        return item;
    }

    @Override
    public boolean insertAvailableResource(int planId, int resourceId, String description, String location, float distance, String phone) throws SQLException {
        int serviceId = 1;
        String serviceSql = "SELECT s.IdServicio FROM Servicio s WHERE s.Nombre = (SELECT r.Servicio FROM Recurso r WHERE r.IdRecurso = ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement psService = conn.prepareStatement(serviceSql)) {
            psService.setInt(1, resourceId);
            try (ResultSet rs = psService.executeQuery()) {
                if (rs.next()) {
                    serviceId = rs.getInt("IdServicio");
                }
            }
        } catch (Exception e) {
            // Fallback to default service ID 1
        }

        String sql = "INSERT INTO RecursoDisponible (IdPlanFamiliar, IdRecursoTipo, IdServicio, Ubicacion, Distancia, Telefono) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, planId);
            ps.setInt(2, resourceId); // Using resourceId directly as IdRecursoTipo
            ps.setInt(3, serviceId);
            ps.setString(4, location != null ? location : "");
            ps.setFloat(5, distance);
            ps.setString(6, phone != null ? phone : "");
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                return false;
            }

            int generatedId = 0;
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    generatedId = rs.getInt(1);
                }
            }
            if (generatedId > 0) {
                Map<String, Object> extra = new HashMap<>();
                extra.put("description", description != null ? description : "");
                extraData.put("resource_" + generatedId, extra);
            }
            return true;
        }
    }

    @Override
    public boolean updateAvailableResource(int idVal, int resourceId, String description, String location, float distance, String phone) throws SQLException {
        int serviceId = 1;
        String serviceSql = "SELECT s.IdServicio FROM Servicio s WHERE s.Nombre = (SELECT r.Servicio FROM Recurso r WHERE r.IdRecurso = ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement psService = conn.prepareStatement(serviceSql)) {
            psService.setInt(1, resourceId);
            try (ResultSet rs = psService.executeQuery()) {
                if (rs.next()) {
                    serviceId = rs.getInt("IdServicio");
                }
            }
        } catch (Exception e) {
            // Fallback to default service ID 1
        }

        String sql = "UPDATE RecursoDisponible SET IdRecursoTipo = ?, IdServicio = ?, Ubicacion = ?, Distancia = ?, Telefono = ? WHERE IdRecurso = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            ps.setInt(2, serviceId);
            ps.setString(3, location != null ? location : "");
            ps.setFloat(4, distance);
            ps.setString(5, phone != null ? phone : "");
            ps.setInt(6, idVal);
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                // Update extraData cache
                Map<String, Object> extra = extraData.computeIfAbsent("resource_" + idVal, k -> new HashMap<>());
                extra.put("description", description != null ? description : "");
            }
            return updated;
        }
    }

    @Override
    public boolean deleteAvailableResource(int idVal) throws SQLException {
        String sql = "DELETE FROM RecursoDisponible WHERE IdRecurso = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVal);
            boolean deleted = ps.executeUpdate() > 0;
            if (deleted) {
                extraData.remove("resource_" + idVal);
            }
            return deleted;
        }
    }
}

package com.defensacivil.dao;

import com.defensacivil.dto.HousingInfoDTO;
import com.defensacivil.dto.ActionPlanDTO;
import com.defensacivil.dto.ActionDTO;
import com.defensacivil.dto.VaccineDTO;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface PlanComplementarioDAO {
    // Housing Info & Graphics
    HousingInfoDTO getHousingInfo(int planId, int typeId) throws SQLException;
    boolean saveOrUpdateHousingGraphic(int planId, String savedFileName, String description, int esEntornoVal) throws SQLException;
    List<HousingInfoDTO> getHousingGraphicsByPlan(int planId) throws SQLException;
    HousingInfoDTO getHousingGraphicById(int id) throws SQLException;
    boolean updateHousingGraphicDescription(int id, String description) throws SQLException;
    boolean deleteHousingGraphic(int id) throws SQLException;

    // Action Plans & Actions
    boolean hasActionPlan(int planId) throws SQLException;
    ActionPlanDTO getActionPlanByPlan(int planId) throws SQLException;
    boolean createActionPlan(int planId, int coordinatorId) throws SQLException;
    List<ActionDTO> getActionsByActionPlan(int actionPlanId) throws SQLException;
    ActionDTO getActionById(int id) throws SQLException;
    boolean insertAction(int actionPlanId, int memberId, String stage, String description) throws SQLException;
    boolean updateAction(int id, int memberId, String description) throws SQLException;
    boolean deleteAction(int id) throws SQLException;

    // Pet Vaccines
    List<VaccineDTO> getVaccinesByPet(int petId) throws SQLException;
    VaccineDTO getVaccineById(int vaccineId) throws SQLException;
    int insertVaccine(VaccineDTO dto) throws SQLException;
    boolean updateVaccine(int vaccineId, VaccineDTO dto) throws SQLException;
    boolean deleteVaccine(int vaccineId) throws SQLException;

    // Available Resources
    List<Map<String, Object>> getAvailableResourcesByPlan(int planId) throws SQLException;
    Map<String, Object> getAvailableResourceById(int idVal) throws SQLException;
    boolean insertAvailableResource(int planId, int resourceId, String description, String location, float distance, String phone) throws SQLException;
    boolean updateAvailableResource(int idVal, int resourceId, String description, String location, float distance, String phone) throws SQLException;
    boolean deleteAvailableResource(int idVal) throws SQLException;
}

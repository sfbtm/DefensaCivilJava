package com.defensacivil.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface PlanComplementarioDAO {
    // Housing Info & Graphics
    Map<String, Object> getHousingInfo(int planId, int typeId) throws SQLException;
    boolean saveOrUpdateHousingGraphic(int planId, String savedFileName, String description, int esEntornoVal) throws SQLException;
    List<Map<String, Object>> getHousingGraphicsByPlan(int planId) throws SQLException;
    Map<String, Object> getHousingGraphicById(int id) throws SQLException;
    boolean updateHousingGraphicDescription(int id, String description) throws SQLException;
    boolean deleteHousingGraphic(int id) throws SQLException;

    // Action Plans & Actions
    boolean hasActionPlan(int planId) throws SQLException;
    Map<String, Object> getActionPlanByPlan(int planId) throws SQLException;
    boolean createActionPlan(int planId, int coordinatorId) throws SQLException;
    List<Map<String, Object>> getActionsByActionPlan(int actionPlanId) throws SQLException;
    Map<String, Object> getActionById(int id) throws SQLException;
    boolean insertAction(int actionPlanId, int memberId, String stage, String description) throws SQLException;
    boolean updateAction(int id, int memberId, String description) throws SQLException;
    boolean deleteAction(int id) throws SQLException;

    // Pet Vaccines
    List<Map<String, Object>> getVaccinesByPet(int petId) throws SQLException;
    Map<String, Object> getVaccineById(int vaccineId) throws SQLException;
    int insertVaccine(Map<String, Object> body) throws SQLException;
    boolean updateVaccine(int vaccineId, Map<String, Object> body) throws SQLException;
    boolean deleteVaccine(int vaccineId) throws SQLException;
}

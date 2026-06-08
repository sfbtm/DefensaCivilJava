package com.defensacivil.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface PlanFamiliarDAO {
    List<Map<String, Object>> getFamilyPlans(int roleId, int userId, int sectionalId) throws SQLException;
    boolean hasMembers(int planId) throws SQLException;
    Map<String, Object> validateRequirements(int planId) throws SQLException;
    Map<String, Object> getPlanById(int planId) throws SQLException;
    Map<String, Object> getSupervisorDashboard() throws SQLException;
    Map<String, Object> getAdminDashboard() throws SQLException;

    int createFamilyPlan(String lastNames, int userId, Map<String, Object> body) throws SQLException;
    boolean updateIdentification(int planId, Map<String, Object> body) throws SQLException;
    boolean changeStatus(int planId, int statusId, String commentary) throws SQLException;
    boolean changeFamilyType(int planId, int familyTypeId) throws SQLException;
}

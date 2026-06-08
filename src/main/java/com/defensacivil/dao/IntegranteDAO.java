package com.defensacivil.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface IntegranteDAO {
    List<Map<String, Object>> getMembersForSelect(int familyPlanId) throws SQLException;
    List<Map<String, Object>> getMembersByFamilyPlan(int familyPlanId) throws SQLException;
    Map<String, Object> getMemberById(int memberId) throws SQLException;
    List<Map<String, Object>> getAllFamilyMembers() throws SQLException;
    int addMember(int familyPlanId, Map<String, Object> body) throws SQLException;
    boolean updateMember(int memberId, Map<String, Object> body) throws SQLException;
    boolean deleteMember(int memberId) throws SQLException;
}

package com.defensacivil.dao;

import com.defensacivil.dto.PetDTO;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface MascotaDAO {
    List<Map<String, Object>> getPetsByFamilyPlan(int planId) throws SQLException;
    PetDTO getPetById(int petId) throws SQLException;
    List<Map<String, Object>> getAllPets() throws SQLException;
    int insertPet(PetDTO dto) throws SQLException;
    boolean updatePet(int petId, PetDTO dto) throws SQLException;
    boolean deletePet(int petId) throws SQLException;

    List<Map<String, Object>> getVaccinesByPet(int petId) throws SQLException;
    Map<String, Object> getVaccineById(int vaccineId) throws SQLException;
    int insertVaccine(Map<String, Object> body) throws SQLException;
    boolean updateVaccine(int vaccineId, Map<String, Object> body) throws SQLException;
    boolean deleteVaccine(int vaccineId) throws SQLException;
}

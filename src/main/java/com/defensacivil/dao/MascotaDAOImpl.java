package com.defensacivil.dao;

import com.defensacivil.config.DatabaseConfig;
import com.defensacivil.dto.PetDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MascotaDAOImpl implements MascotaDAO {

    private final Map<String, Map<String, Object>> extraData;

    public MascotaDAOImpl(Map<String, Map<String, Object>> extraData) {
        this.extraData = extraData;
    }

    private Map<String, Object> mapResultSetToPet(ResultSet rs) throws SQLException {
        Map<String, Object> pet = new HashMap<>();
        pet.put("id", rs.getInt("IdMascota"));
        pet.put("name", rs.getString("Nombre"));
        pet.put("breed", rs.getString("Raza"));
        
        Date birthDateVal = rs.getDate("FechaNacimiento");
        pet.put("birth_date", birthDateVal != null ? birthDateVal.toString() : "");

        int speciesId = rs.getInt("IdEspecie");
        String especieStr = rs.getString("EspecieNombre");
        if (especieStr == null) {
            especieStr = "Perro";
        }
        pet.put("species_id", speciesId);
        pet.put("species_name", especieStr);
        pet.put("species", Map.of("id", speciesId, "name", especieStr));

        int genderId = rs.getInt("IdGenero");
        String generoStr = rs.getString("GeneroNombre");
        int finalGenderId = (genderId == 0) ? 1 : genderId;
        String genderName = generoStr != null ? generoStr : "Macho";
        pet.put("animal_gender_id", finalGenderId);
        pet.put("animal_gender_name", genderName);
        pet.put("animal_gender", Map.of("id", finalGenderId, "name", genderName));

        int age = 0;
        if (birthDateVal != null) {
            java.time.LocalDate birthDate = birthDateVal.toLocalDate();
            java.time.LocalDate now = java.time.LocalDate.now();
            age = java.time.Period.between(birthDate, now).getYears();
        }
        pet.put("age", age);
        pet.put("family_plan_id", rs.getInt("IdPlanFamiliar"));
        return pet;
    }

    @Override
    public List<Map<String, Object>> getPetsByFamilyPlan(int planId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT m.IdMascota, m.Nombre, m.IdGenero, g.Nombre AS GeneroNombre, m.Raza, 
                   m.IdEspecie, e.Nombre AS EspecieNombre, m.FechaNacimiento, m.IdPlanFamiliar 
            FROM Mascotas m 
            LEFT JOIN Genero g ON m.IdGenero = g.IdGenero 
            LEFT JOIN Especie e ON m.IdEspecie = e.IdEspecie 
            WHERE m.IdPlanFamiliar = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPet(rs));
                }
            }
        }
        return list;
    }

    @Override
    public PetDTO getPetById(int petId) throws SQLException {
        String sql = """
            SELECT m.IdMascota, m.Nombre, m.IdGenero, m.Raza, m.IdEspecie, e.Nombre AS EspecieNombre, m.FechaNacimiento, m.IdPlanFamiliar 
            FROM Mascotas m 
            LEFT JOIN Especie e ON m.IdEspecie = e.IdEspecie
            WHERE m.IdMascota = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, petId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PetDTO dto = new PetDTO();
                    dto.setId(rs.getInt("IdMascota"));
                    dto.setName(rs.getString("Nombre"));
                    dto.setBreed(rs.getString("Raza"));
                    
                    Date birthDateVal = rs.getDate("FechaNacimiento");
                    dto.setBirthDate(birthDateVal != null ? birthDateVal.toString() : "");

                    dto.setSpeciesId(rs.getInt("IdEspecie"));
                    dto.setAnimalGenderId(rs.getInt("IdGenero"));
                    dto.setFamilyPlanId(rs.getInt("IdPlanFamiliar"));

                    int age = 0;
                    if (birthDateVal != null) {
                        java.time.LocalDate birthDate = birthDateVal.toLocalDate();
                        java.time.LocalDate now = java.time.LocalDate.now();
                        age = java.time.Period.between(birthDate, now).getYears();
                    }
                    dto.setAge(age);
                    return dto;
                }
            }
        }
        return null;
    }

    @Override
    public List<Map<String, Object>> getAllPets() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT m.IdMascota, m.Nombre, m.IdGenero, g.Nombre AS GeneroNombre, m.Raza, 
                   m.IdEspecie, e.Nombre AS EspecieNombre, m.FechaNacimiento, m.IdPlanFamiliar 
            FROM Mascotas m 
            LEFT JOIN Genero g ON m.IdGenero = g.IdGenero 
            LEFT JOIN Especie e ON m.IdEspecie = e.IdEspecie
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToPet(rs));
            }
        }
        return list;
    }

    @Override
    public int insertPet(PetDTO dto) throws SQLException {
        String name = dto.getName();
        String breed = dto.getBreed();
        String birthDateStr = dto.getBirthDate();
        int genderId = dto.getAnimalGenderId() == 0 ? 1 : dto.getAnimalGenderId();
        int speciesId = dto.getSpeciesId() == 0 ? 1 : dto.getSpeciesId();
        int planId = dto.getFamilyPlanId() == 0 ? 1 : dto.getFamilyPlanId();

        String sql = "INSERT INTO Mascotas (Nombre, IdGenero, Raza, IdEspecie, FechaNacimiento, IdPlanFamiliar) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, genderId);
            ps.setString(3, breed);
            ps.setInt(4, speciesId);
            ps.setDate(5, birthDateStr != null && !birthDateStr.isEmpty() ? Date.valueOf(birthDateStr) : Date.valueOf(java.time.LocalDate.now()));
            ps.setInt(6, planId);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public boolean updatePet(int petId, PetDTO dto) throws SQLException {
        String name = dto.getName();
        String breed = dto.getBreed();
        String birthDateStr = dto.getBirthDate();
        int genderId = dto.getAnimalGenderId() == 0 ? 1 : dto.getAnimalGenderId();
        int speciesId = dto.getSpeciesId() == 0 ? 1 : dto.getSpeciesId();

        String sql = "UPDATE Mascotas SET Nombre = ?, IdGenero = ?, Raza = ?, IdEspecie = ?, FechaNacimiento = ? WHERE IdMascota = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, genderId);
            ps.setString(3, breed);
            ps.setInt(4, speciesId);
            ps.setDate(5, birthDateStr != null && !birthDateStr.isEmpty() ? Date.valueOf(birthDateStr) : Date.valueOf(java.time.LocalDate.now()));
            ps.setInt(6, petId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deletePet(int petId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Delete related vaccines first in the intermediate table
                try (PreparedStatement psV = conn.prepareStatement("DELETE FROM MascotaVacuna WHERE IdMascota = ?")) {
                    psV.setInt(1, petId);
                    psV.executeUpdate();
                }
                int affectedRows = 0;
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Mascotas WHERE IdMascota = ?")) {
                    ps.setInt(1, petId);
                    affectedRows = ps.executeUpdate();
                }
                conn.commit();
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
    public List<Map<String, Object>> getVaccinesByPet(int petId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT mv.IdVacuna, v.Nombre, mv.FechaAplicacion " +
                     "FROM MascotaVacuna mv " +
                     "JOIN Vacuna v ON mv.IdVacuna = v.IdVacuna " +
                     "WHERE mv.IdMascota = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, petId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int vacId = rs.getInt("IdVacuna");
                    Date fechaDate = rs.getDate("FechaAplicacion");
                    String dateVal = fechaDate != null ? fechaDate.toString() : java.time.LocalDate.now().toString();

                    int relationId = petId * 100000 + vacId;

                    Map<String, Object> vaccine = new HashMap<>();
                    vaccine.put("id", relationId);
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
        int petId = vaccineId / 100000;
        int vacCatalogId = vaccineId % 100000;
        String sql = "SELECT mv.IdVacuna, v.Nombre, mv.FechaAplicacion " +
                     "FROM MascotaVacuna mv " +
                     "JOIN Vacuna v ON mv.IdVacuna = v.IdVacuna " +
                     "WHERE mv.IdMascota = ? AND mv.IdVacuna = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, petId);
            ps.setInt(2, vacCatalogId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Date fechaDate = rs.getDate("FechaAplicacion");
                    String dateVal = fechaDate != null ? fechaDate.toString() : java.time.LocalDate.now().toString();

                    Map<String, Object> vac = new HashMap<>();
                    vac.put("id", vaccineId);
                    vac.put("name", rs.getString("Nombre"));
                    vac.put("pet_id", petId);
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

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int vaccineCatalogId = 0;
                // 1. Find or create in Vacuna catalog
                String findSql = "SELECT IdVacuna FROM Vacuna WHERE Nombre = ?";
                try (PreparedStatement findPs = conn.prepareStatement(findSql)) {
                    findPs.setString(1, name);
                    try (ResultSet rs = findPs.executeQuery()) {
                        if (rs.next()) {
                            vaccineCatalogId = rs.getInt("IdVacuna");
                        }
                    }
                }
                if (vaccineCatalogId == 0) {
                    String insertCatalogSql = "INSERT INTO Vacuna (Nombre) VALUES (?)";
                    try (PreparedStatement insertCatalogPs = conn.prepareStatement(insertCatalogSql, Statement.RETURN_GENERATED_KEYS)) {
                        insertCatalogPs.setString(1, name);
                        insertCatalogPs.executeUpdate();
                        try (ResultSet rs = insertCatalogPs.getGeneratedKeys()) {
                            if (rs.next()) {
                                vaccineCatalogId = rs.getInt(1);
                            }
                        }
                    }
                }
                if (vaccineCatalogId == 0) {
                    conn.rollback();
                    return 0;
                }

                // 2. Insert into MascotaVacuna
                String insertLinkSql = "INSERT INTO MascotaVacuna (IdMascota, IdVacuna, FechaAplicacion) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertLinkSql)) {
                    ps.setInt(1, petId);
                    ps.setInt(2, vaccineCatalogId);
                    ps.setDate(3, dateStr != null && !dateStr.isEmpty() ? Date.valueOf(dateStr) : Date.valueOf(java.time.LocalDate.now()));
                    int rows = ps.executeUpdate();
                    if (rows > 0) {
                        conn.commit();
                        return petId * 100000 + vaccineCatalogId;
                    }
                }
                conn.rollback();
                return 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public boolean updateVaccine(int vaccineId, Map<String, Object> body) throws SQLException {
        int oldPetId = vaccineId / 100000;
        int oldVacCatalogId = vaccineId % 100000;
        String name = (String) body.get("name");
        String dateStr = (String) body.get("date");

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int newVaccineCatalogId = 0;
                // 1. Find or create in Vacuna catalog
                String findSql = "SELECT IdVacuna FROM Vacuna WHERE Nombre = ?";
                try (PreparedStatement findPs = conn.prepareStatement(findSql)) {
                    findPs.setString(1, name);
                    try (ResultSet rs = findPs.executeQuery()) {
                        if (rs.next()) {
                            newVaccineCatalogId = rs.getInt("IdVacuna");
                        }
                    }
                }
                if (newVaccineCatalogId == 0) {
                    String insertCatalogSql = "INSERT INTO Vacuna (Nombre) VALUES (?)";
                    try (PreparedStatement insertCatalogPs = conn.prepareStatement(insertCatalogSql, Statement.RETURN_GENERATED_KEYS)) {
                        insertCatalogPs.setString(1, name);
                        insertCatalogPs.executeUpdate();
                        try (ResultSet rs = insertCatalogPs.getGeneratedKeys()) {
                            if (rs.next()) {
                                newVaccineCatalogId = rs.getInt(1);
                            }
                        }
                    }
                }
                if (newVaccineCatalogId == 0) {
                    conn.rollback();
                    return false;
                }

                // 2. Update MascotaVacuna
                String sql = "UPDATE MascotaVacuna SET IdVacuna = ?, FechaAplicacion = ? WHERE IdMascota = ? AND IdVacuna = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, newVaccineCatalogId);
                    ps.setDate(2, dateStr != null && !dateStr.isEmpty() ? Date.valueOf(dateStr) : Date.valueOf(java.time.LocalDate.now()));
                    ps.setInt(3, oldPetId);
                    ps.setInt(4, oldVacCatalogId);
                    int rows = ps.executeUpdate();
                    conn.commit();
                    return rows > 0;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public boolean deleteVaccine(int vaccineId) throws SQLException {
        int petId = vaccineId / 100000;
        int vacCatalogId = vaccineId % 100000;
        String sql = "DELETE FROM MascotaVacuna WHERE IdMascota = ? AND IdVacuna = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, petId);
            ps.setInt(2, vacCatalogId);
            return ps.executeUpdate() > 0;
        }
    }
}

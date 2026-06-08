package com.defensacivil.dao;

import com.defensacivil.config.DatabaseConfig;
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
        
        Date edadDate = rs.getDate("Edad");
        pet.put("birth_date", edadDate != null ? edadDate.toString() : "");

        String especieStr = rs.getString("Especie");
        int speciesId = 1;
        if (especieStr != null) {
            if (especieStr.equalsIgnoreCase("Perro")) speciesId = 1;
            else if (especieStr.equalsIgnoreCase("Gato")) speciesId = 2;
            else if (especieStr.equalsIgnoreCase("Ave")) speciesId = 3;
        }
        pet.put("species_id", speciesId);
        pet.put("species_name", especieStr != null ? especieStr : "Perro");
        pet.put("species", Map.of("id", speciesId, "name", especieStr != null ? especieStr : "Perro"));

        int genderId = rs.getInt("IdGenero");
        String generoStr = rs.getString("GeneroNombre");
        int finalGenderId = (genderId == 0) ? 1 : genderId;
        String genderName = generoStr != null ? generoStr : "Macho";
        pet.put("animal_gender_id", finalGenderId);
        pet.put("animal_gender_name", genderName);
        pet.put("animal_gender", Map.of("id", finalGenderId, "name", genderName));

        int age = 0;
        if (edadDate != null) {
            java.time.LocalDate birthDate = edadDate.toLocalDate();
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
        String sql = "SELECT m.IdMascota, m.Nombre, m.IdGenero, g.Nombre AS GeneroNombre, m.Raza, m.Especie, m.Edad, m.IdPlanFamiliar FROM Mascotas m LEFT JOIN Genero g ON m.IdGenero = g.IdGenero WHERE m.IdPlanFamiliar = ?";
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
    public Map<String, Object> getPetById(int petId) throws SQLException {
        String sql = "SELECT m.IdMascota, m.Nombre, m.IdGenero, g.Nombre AS GeneroNombre, m.Raza, m.Especie, m.Edad, m.IdPlanFamiliar FROM Mascotas m LEFT JOIN Genero g ON m.IdGenero = g.IdGenero WHERE m.IdMascota = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, petId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPet(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<Map<String, Object>> getAllPets() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT m.IdMascota, m.Nombre, m.IdGenero, g.Nombre AS GeneroNombre, m.Raza, m.Especie, m.Edad, m.IdPlanFamiliar FROM Mascotas m LEFT JOIN Genero g ON m.IdGenero = g.IdGenero";
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
    public int insertPet(Map<String, Object> body) throws SQLException {
        String name = (String) body.get("name");
        String breed = (String) body.get("breed");
        String birthDateStr = (String) body.get("birth_date");

        Object genderIdObj = body.get("animal_gender_id");
        int genderId = 1;
        if (genderIdObj instanceof Number) genderId = ((Number) genderIdObj).intValue();
        else if (genderIdObj instanceof String) genderId = Integer.parseInt((String) genderIdObj);

        Object speciesIdObj = body.get("species_id");
        int speciesId = 1;
        if (speciesIdObj instanceof Number) speciesId = ((Number) speciesIdObj).intValue();
        else if (speciesIdObj instanceof String) speciesId = Integer.parseInt((String) speciesIdObj);
        String species = switch (speciesId) {
            case 1 -> "Perro";
            case 2 -> "Gato";
            case 3 -> "Ave";
            default -> "Perro";
        };

        Object planIdObj = body.get("family_plan_id");
        int planId = 1;
        if (planIdObj instanceof Number) planId = ((Number) planIdObj).intValue();
        else if (planIdObj instanceof String) planId = Integer.parseInt((String) planIdObj);

        String sql = "INSERT INTO Mascotas (Nombre, IdGenero, Raza, Especie, Edad, IdPlanFamiliar) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, genderId);
            ps.setString(3, breed);
            ps.setString(4, species);
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
    public boolean updatePet(int petId, Map<String, Object> body) throws SQLException {
        String name = (String) body.get("name");
        String breed = (String) body.get("breed");
        String birthDateStr = (String) body.get("birth_date");

        Object genderIdObj = body.get("animal_gender_id");
        int genderId = 1;
        if (genderIdObj instanceof Number) genderId = ((Number) genderIdObj).intValue();
        else if (genderIdObj instanceof String) genderId = Integer.parseInt((String) genderIdObj);

        Object speciesIdObj = body.get("species_id");
        int speciesId = 1;
        if (speciesIdObj instanceof Number) speciesId = ((Number) speciesIdObj).intValue();
        else if (speciesIdObj instanceof String) speciesId = Integer.parseInt((String) speciesIdObj);
        String species = switch (speciesId) {
            case 1 -> "Perro";
            case 2 -> "Gato";
            case 3 -> "Ave";
            default -> "Perro";
        };

        String sql = "UPDATE Mascotas SET Nombre = ?, IdGenero = ?, Raza = ?, Especie = ?, Edad = ? WHERE IdMascota = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, genderId);
            ps.setString(3, breed);
            ps.setString(4, species);
            ps.setDate(5, birthDateStr != null && !birthDateStr.isEmpty() ? Date.valueOf(birthDateStr) : Date.valueOf(java.time.LocalDate.now()));
            ps.setInt(6, petId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deletePet(int petId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Delete related vaccines first to respect foreign keys
            try (PreparedStatement psV = conn.prepareStatement("DELETE FROM Vacunas WHERE IdMascota = ?")) {
                psV.setInt(1, petId);
                psV.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Mascotas WHERE IdMascota = ?")) {
                ps.setInt(1, petId);
                return ps.executeUpdate() > 0;
            }
        }
    }

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

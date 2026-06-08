package com.defensacivil.dao;

import com.defensacivil.config.DatabaseConfig;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IntegranteDAOImpl implements IntegranteDAO {

    private final Map<String, Map<String, Object>> extraData;

    public IntegranteDAOImpl(Map<String, Map<String, Object>> extraData) {
        this.extraData = extraData;
    }

    @Override
    public List<Map<String, Object>> getMembersForSelect(int familyPlanId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT IdIntegrante, Nombre, Apellido FROM Integrante WHERE IdPlanFamiliar = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, familyPlanId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> member = new HashMap<>();
                    member.put("id", rs.getInt("IdIntegrante"));
                    member.put("full_name", rs.getString("Nombre") + " " + rs.getString("Apellido"));
                    list.add(member);
                }
            }
        }
        return list;
    }

    @Override
    public List<Map<String, Object>> getMembersByFamilyPlan(int familyPlanId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT i.IdIntegrante, i.Nombre, i.Apellido, i.Parentesco, i.Telefono, i.IdGenero, i.IdDocumentoTipo, i.IdNacionalidad,
                   g.Nombre AS GeneroNombre, dt.Nombre AS DocumentoNombre, n.Nombre AS NacionalidadNombre
            FROM Integrante i
            LEFT JOIN Genero g ON i.IdGenero = g.IdGenero
            LEFT JOIN DocumentoTipo dt ON i.IdDocumentoTipo = dt.IdDocumentoTipo
            LEFT JOIN Nacionalidad n ON i.IdNacionalidad = n.IdNacionalidad
            WHERE i.IdPlanFamiliar = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, familyPlanId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int memberId = rs.getInt("IdIntegrante");
                    Map<String, Object> member = new HashMap<>();
                    member.put("id", memberId);
                    member.put("names", rs.getString("Nombre"));
                    member.put("last_names", rs.getString("Apellido"));
                    member.put("full_name", rs.getString("Nombre") + " " + rs.getString("Apellido"));
                    member.put("relationship", rs.getString("Parentesco"));
                    member.put("phone", rs.getString("Telefono"));
                    member.put("gender", Map.of("name", rs.getString("GeneroNombre") != null ? rs.getString("GeneroNombre") : ""));
                    member.put("document_type", Map.of("acronym", rs.getString("DocumentoNombre") != null ? rs.getString("DocumentoNombre") : ""));
                    member.put("nationality", Map.of("name", rs.getString("NacionalidadNombre") != null ? rs.getString("NacionalidadNombre") : ""));
                    
                    Map<String, Object> extra = extraData.getOrDefault("member_" + memberId, Map.of());
                    member.put("blood_group", extra.getOrDefault("blood_group", "O+"));
                    member.put("eps", extra.getOrDefault("eps", "Compensar"));
                    member.put("birth_date", extra.getOrDefault("birth_date", "1990-01-01"));
                    member.put("document_number", extra.getOrDefault("document_number", "1000000" + memberId));
                    member.put("kinship", rs.getString("Parentesco"));
                    list.add(member);
                }
            }
        }
        return list;
    }

    @Override
    public Map<String, Object> getMemberById(int memberId) throws SQLException {
        String sql = """
            SELECT i.IdIntegrante, i.Nombre, i.Apellido, i.Parentesco, i.Telefono, i.IdGenero, i.IdDocumentoTipo, i.IdNacionalidad,
                   g.Nombre AS GeneroNombre, dt.Nombre AS DocumentoNombre, n.Nombre AS NacionalidadNombre
            FROM Integrante i
            LEFT JOIN Genero g ON i.IdGenero = g.IdGenero
            LEFT JOIN DocumentoTipo dt ON i.IdDocumentoTipo = dt.IdDocumentoTipo
            LEFT JOIN Nacionalidad n ON i.IdNacionalidad = n.IdNacionalidad
            WHERE i.IdIntegrante = ?
            """;
        Map<String, Object> member = new HashMap<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    member.put("id", rs.getInt("IdIntegrante"));
                    member.put("names", rs.getString("Nombre"));
                    member.put("last_names", rs.getString("Apellido"));
                    member.put("relationship", rs.getString("Parentesco"));
                    member.put("phone", rs.getString("Telefono"));
                    member.put("gender_id", rs.getInt("IdGenero"));
                    member.put("document_type_id", rs.getInt("IdDocumentoTipo"));
                    member.put("nationality_id", rs.getInt("IdNacionalidad"));
                    member.put("gender", Map.of("name", rs.getString("GeneroNombre") != null ? rs.getString("GeneroNombre") : ""));
                    member.put("document_type", Map.of("acronym", rs.getString("DocumentoNombre") != null ? rs.getString("DocumentoNombre") : ""));
                    member.put("nationality", Map.of("name", rs.getString("NacionalidadNombre") != null ? rs.getString("NacionalidadNombre") : ""));
                    member.put("kinship", Map.of("name", rs.getString("Parentesco") != null ? rs.getString("Parentesco") : ""));
                    
                    Map<String, Object> extra = extraData.getOrDefault("member_" + memberId, Map.of());
                    int bloodGroupId = 1;
                    Object bgIdObj = extra.get("blood_group_id");
                    if (bgIdObj instanceof Number) bloodGroupId = ((Number) bgIdObj).intValue();
                    else if (bgIdObj instanceof String) bloodGroupId = Integer.parseInt((String) bgIdObj);

                    member.put("blood_group_id", bloodGroupId);
                    member.put("blood_group", Map.of("name", extra.getOrDefault("blood_group", "O+")));
                    member.put("eps", extra.getOrDefault("eps", "Compensar"));
                    member.put("birth_date", extra.getOrDefault("birth_date", "1990-01-01"));
                    member.put("document_number", extra.getOrDefault("document_number", "1000000" + memberId));
                    member.put("kinship_id", 1);
                }
            }
        }
        return member;
    }

    @Override
    public List<Map<String, Object>> getAllFamilyMembers() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT IdIntegrante, IdPlanFamiliar FROM Integrante";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(Map.of(
                    "member_id", rs.getInt("IdIntegrante"),
                    "family_plan_id", rs.getInt("IdPlanFamiliar")
                ));
            }
        }
        return list;
    }

    @Override
    public int addMember(int familyPlanId, Map<String, Object> body) throws SQLException {
        String names = (String) body.get("names");
        String lastNames = (String) body.get("last_names");
        String relationship = (String) body.get("relationship");
        String phone = (String) body.get("phone");

        Object genderObj = body.get("gender_id");
        int genderId = 1;
        if (genderObj instanceof Number) genderId = ((Number) genderObj).intValue();

        Object docTypeObj = body.get("document_type_id");
        int docTypeId = 1;
        if (docTypeObj instanceof Number) docTypeId = ((Number) docTypeObj).intValue();

        Object natObj = body.get("nationality_id");
        int natId = 1;
        if (natObj instanceof Number) natId = ((Number) natObj).intValue();

        String sql = "INSERT INTO Integrante (IdPlanFamiliar, Nombre, Apellido, Parentesco, Telefono, IdGenero, IdDocumentoTipo, IdNacionalidad) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, familyPlanId);
            ps.setString(2, names);
            ps.setString(3, lastNames);
            ps.setString(4, relationship != null ? relationship : "Familiar");
            ps.setString(5, phone != null ? phone : "");
            ps.setInt(6, genderId);
            ps.setInt(7, docTypeId);
            ps.setInt(8, natId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int memberId = rs.getInt(1);

                    // Save extra unmapped fields to memory
                    Map<String, Object> extra = new HashMap<>();
                    extra.put("eps", body.get("eps"));
                    extra.put("birth_date", body.get("birth_date"));
                    extra.put("document_number", body.get("document_number"));

                    Object bgIdObj = body.get("blood_group_id");
                    int bgId = 1;
                    if (bgIdObj instanceof Number) bgId = ((Number) bgIdObj).intValue();
                    else if (bgIdObj instanceof String) bgId = Integer.parseInt((String) bgIdObj);

                    String bgName = "O+";
                    if (bgId == 1) bgName = "O+";
                    else if (bgId == 2) bgName = "O-";
                    else if (bgId == 3) bgName = "A+";
                    else if (bgId == 4) bgName = "A-";
                    else if (bgId == 5) bgName = "B+";
                    else if (bgId == 6) bgName = "B-";
                    else if (bgId == 7) bgName = "AB+";
                    else if (bgId == 8) bgName = "AB-";
                    extra.put("blood_group", bgName);
                    extra.put("blood_group_id", bgId);

                    extraData.put("member_" + memberId, extra);
                    return memberId;
                }
            }
        }
        return -1;
    }

    @Override
    public boolean updateMember(int memberId, Map<String, Object> body) throws SQLException {
        String names = (String) body.get("names");
        String lastNames = (String) body.get("last_names");
        String relationship = (String) body.get("relationship");
        String phone = (String) body.get("phone");

        Object genderObj = body.get("gender_id");
        int genderId = 1;
        if (genderObj instanceof Number) genderId = ((Number) genderObj).intValue();

        Object docTypeObj = body.get("document_type_id");
        int docTypeId = 1;
        if (docTypeObj instanceof Number) docTypeId = ((Number) docTypeObj).intValue();

        Object natObj = body.get("nationality_id");
        int natId = 1;
        if (natObj instanceof Number) natId = ((Number) natObj).intValue();

        String sql = "UPDATE Integrante SET Nombre = ?, Apellido = ?, Parentesco = ?, Telefono = ?, IdGenero = ?, IdDocumentoTipo = ?, IdNacionalidad = ? WHERE IdIntegrante = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, names);
            ps.setString(2, lastNames);
            ps.setString(3, relationship != null ? relationship : "Familiar");
            ps.setString(4, phone != null ? phone : "");
            ps.setInt(5, genderId);
            ps.setInt(6, docTypeId);
            ps.setInt(7, natId);
            ps.setInt(8, memberId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                // Save extra unmapped fields to memory
                Map<String, Object> extra = extraData.computeIfAbsent("member_" + memberId, k -> new HashMap<>());
                extra.put("eps", body.get("eps"));
                extra.put("birth_date", body.get("birth_date"));
                extra.put("document_number", body.get("document_number"));

                Object bgIdObj = body.get("blood_group_id");
                int bgId = 1;
                if (bgIdObj instanceof Number) bgId = ((Number) bgIdObj).intValue();
                else if (bgIdObj instanceof String) bgId = Integer.parseInt((String) bgIdObj);

                String bgName = "O+";
                if (bgId == 1) bgName = "O+";
                else if (bgId == 2) bgName = "O-";
                else if (bgId == 3) bgName = "A+";
                else if (bgId == 4) bgName = "A-";
                else if (bgId == 5) bgName = "B+";
                else if (bgId == 6) bgName = "B-";
                else if (bgId == 7) bgName = "AB+";
                else if (bgId == 8) bgName = "AB-";
                extra.put("blood_group", bgName);
                extra.put("blood_group_id", bgId);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean deleteMember(int memberId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Delete related diseases first to respect foreign key constraints
                try (PreparedStatement psD = conn.prepareStatement("DELETE FROM Enfermedad WHERE IdIntegrante = ?")) {
                    psD.setInt(1, memberId);
                    psD.executeUpdate();
                }
                int affectedRows = 0;
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Integrante WHERE IdIntegrante = ?")) {
                    ps.setInt(1, memberId);
                    affectedRows = ps.executeUpdate();
                }
                conn.commit();
                extraData.remove("member_" + memberId);
                return affectedRows > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}

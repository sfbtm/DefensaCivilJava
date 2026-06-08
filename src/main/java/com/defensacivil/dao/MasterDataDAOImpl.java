package com.defensacivil.dao;

import com.defensacivil.config.DatabaseConfig;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MasterDataDAOImpl implements MasterDataDAO {

    private Map<String, Object> mapRow(ResultSet rs, EntityConfig cfg) throws SQLException {
        Map<String, Object> item = new HashMap<>();
        item.put("id", rs.getInt(cfg.idCol));
        item.put(cfg.jsonNameKey, rs.getString(cfg.nameCol));
        item.put("is_active", true);

        for (Map.Entry<String, String> entry : cfg.extraColMap.entrySet()) {
            String dbCol = entry.getKey();
            String jsonKey = entry.getValue();
            Object value = rs.getObject(dbCol);

            if (value instanceof Boolean) {
                item.put(jsonKey, value);
            } else if (dbCol.equalsIgnoreCase("Activo") || dbCol.equalsIgnoreCase("Activa") || dbCol.equalsIgnoreCase("Precaucion")) {
                item.put(jsonKey, rs.getBoolean(dbCol));
            } else {
                item.put(jsonKey, value);
            }
        }
        return item;
    }

    @Override
    public List<Map<String, Object>> getAll(EntityConfig cfg) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = String.format("SELECT * FROM %s ORDER BY %s DESC", cfg.tableName, cfg.idCol);

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs, cfg));
            }
        }
        return list;
    }

    @Override
    public Map<String, Object> getById(EntityConfig cfg, int id) throws SQLException {
        String sql = String.format("SELECT * FROM %s WHERE %s = ?", cfg.tableName, cfg.idCol);
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs, cfg);
                }
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> getPaginated(EntityConfig cfg, int page, int perPage) throws SQLException {
        int offset = (page - 1) * perPage;
        int total = 0;
        List<Map<String, Object>> list = new ArrayList<>();

        boolean hasActiveCol = cfg.extraColMap.containsKey("Activa") || cfg.extraColMap.containsKey("Activo");
        String activeColName = cfg.extraColMap.containsKey("Activa") ? "Activa" : "Activo";

        String countSql = hasActiveCol 
            ? String.format("SELECT COUNT(*) FROM %s WHERE %s = 1", cfg.tableName, activeColName)
            : String.format("SELECT COUNT(*) FROM %s", cfg.tableName);

        String selectSql = hasActiveCol 
            ? String.format("SELECT * FROM %s WHERE %s = 1 LIMIT ? OFFSET ?", cfg.tableName, activeColName)
            : String.format("SELECT * FROM %s LIMIT ? OFFSET ?", cfg.tableName);

        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(countSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) total = rs.getInt(1);
            }

            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setInt(1, perPage);
                ps.setInt(2, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs, cfg));
                    }
                }
            }
        }

        int lastPage = (int) Math.ceil((double) total / perPage);
        if (lastPage < 1) lastPage = 1;

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("data", list);

        Map<String, Object> paginateMap = new HashMap<>();
        paginateMap.put("current_page", page);
        paginateMap.put("last_page", lastPage);
        paginateMap.put("per_page", perPage);
        paginateMap.put("total", total);
        responseMap.put("paginate", paginateMap);

        return responseMap;
    }

    @Override
    public boolean insert(EntityConfig cfg, Map<String, Object> body) throws SQLException {
        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        columns.add(cfg.nameCol);
        values.add(body.get(cfg.jsonNameKey));

        for (Map.Entry<String, String> entry : cfg.extraColMap.entrySet()) {
            String dbCol = entry.getKey();
            String jsonKey = entry.getValue();

            Object val = body.get(jsonKey);
            if (val == null) {
                if (dbCol.equalsIgnoreCase("Activo") || dbCol.equalsIgnoreCase("Activa")) {
                    val = 1;
                } else if (dbCol.equalsIgnoreCase("Precaucion")) {
                    val = 0;
                }
            } else {
                if (val instanceof Boolean) {
                    val = (Boolean) val ? 1 : 0;
                } else if (val instanceof Number) {
                    val = ((Number) val).intValue();
                } else if (val instanceof String) {
                    try {
                        val = Integer.parseInt((String) val);
                    } catch (NumberFormatException e) {
                        // Keep as string
                    }
                }
            }
            columns.add(dbCol);
            values.add(val);
        }

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO ").append(cfg.tableName).append(" (");
        for (int i = 0; i < columns.size(); i++) {
            sqlBuilder.append(columns.get(i));
            if (i < columns.size() - 1) sqlBuilder.append(", ");
        }
        sqlBuilder.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            sqlBuilder.append("?");
            if (i < columns.size() - 1) sqlBuilder.append(", ");
        }
        sqlBuilder.append(")");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlBuilder.toString())) {

            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, values.get(i));
            }

            int affected = ps.executeUpdate();
            return affected > 0;
        }
    }

    @Override
    public boolean update(EntityConfig cfg, int id, Map<String, Object> body) throws SQLException {
        List<String> setClauses = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        if (body.containsKey(cfg.jsonNameKey)) {
            setClauses.add(cfg.nameCol + " = ?");
            values.add(body.get(cfg.jsonNameKey));
        }

        for (Map.Entry<String, String> entry : cfg.extraColMap.entrySet()) {
            String dbCol = entry.getKey();
            String jsonKey = entry.getValue();

            if (body.containsKey(jsonKey)) {
                setClauses.add(dbCol + " = ?");
                Object val = body.get(jsonKey);
                if (val instanceof Boolean) {
                    val = (Boolean) val ? 1 : 0;
                } else if (val instanceof Number) {
                    val = ((Number) val).intValue();
                } else if (val instanceof String) {
                    try {
                        val = Integer.parseInt((String) val);
                    } catch (NumberFormatException e) {
                        // Keep as string
                    }
                }
                values.add(val);
            }
        }

        if (setClauses.isEmpty()) {
            return false;
        }

        String sql = String.format("UPDATE %s SET %s WHERE %s = ?", cfg.tableName, String.join(", ", setClauses), cfg.idCol);
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, values.get(i));
            }
            ps.setInt(values.size() + 1, id);

            int affected = ps.executeUpdate();
            return affected > 0;
        }
    }

    @Override
    public boolean updateStatus(EntityConfig cfg, int id, int active) throws SQLException {
        String activeCol = null;
        for (Map.Entry<String, String> entry : cfg.extraColMap.entrySet()) {
            if (entry.getValue().equals("is_active")) {
                activeCol = entry.getKey();
                break;
            }
        }

        if (activeCol == null) {
            return true;
        }

        String sql = String.format("UPDATE %s SET %s = ? WHERE %s = ?", cfg.tableName, activeCol, cfg.idCol);
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, active);
            ps.setInt(2, id);
            int affected = ps.executeUpdate();
            return affected > 0;
        }
    }

    @Override
    public boolean delete(EntityConfig cfg, int id) throws SQLException {
        String sql = String.format("DELETE FROM %s WHERE %s = ?", cfg.tableName, cfg.idCol);
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int affected = ps.executeUpdate();
            return affected > 0;
        }
    }
}

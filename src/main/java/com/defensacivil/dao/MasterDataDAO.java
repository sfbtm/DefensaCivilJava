package com.defensacivil.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface MasterDataDAO {

    class EntityConfig {
        public final String tableName;
        public final String idCol;
        public final String nameCol;
        public final String jsonNameKey;
        public final Map<String, String> extraColMap;

        public EntityConfig(String tableName, String idCol, String nameCol) {
            this(tableName, idCol, nameCol, "name", Map.of());
        }

        public EntityConfig(String tableName, String idCol, String nameCol, String jsonNameKey, Map<String, String> extraColMap) {
            this.tableName = tableName;
            this.idCol = idCol;
            this.nameCol = nameCol;
            this.jsonNameKey = jsonNameKey;
            this.extraColMap = extraColMap;
        }
    }

    List<Map<String, Object>> getAll(EntityConfig cfg) throws SQLException;
    Map<String, Object> getById(EntityConfig cfg, int id) throws SQLException;
    Map<String, Object> getPaginated(EntityConfig cfg, int page, int perPage) throws SQLException;
    boolean insert(EntityConfig cfg, Map<String, Object> body) throws SQLException;
    boolean update(EntityConfig cfg, int id, Map<String, Object> body) throws SQLException;
    boolean updateStatus(EntityConfig cfg, int id, int active) throws SQLException;
    boolean delete(EntityConfig cfg, int id) throws SQLException;
}

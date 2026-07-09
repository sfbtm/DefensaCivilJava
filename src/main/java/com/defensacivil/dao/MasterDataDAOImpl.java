package com.defensacivil.dao;

import com.defensacivil.config.DatabaseConfig;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación de la interfaz {@link MasterDataDAO} que realiza operaciones
 * CRUD genéricas sobre tablas de catálogo y maestras utilizando sentencias SQL estructuradas dinámicamente.
 */
public class MasterDataDAOImpl implements MasterDataDAO {

    /**
     * Mapea una fila del {@link ResultSet} a un mapa de datos, utilizando la configuración de columnas
     * y tipos provistos en {@link EntityConfig}.
     *
     * @param rs El {@link ResultSet} con el cursor en la fila actual.
     * @param cfg Configuración de mapeo de la entidad.
     * @return Un mapa con las claves JSON configuradas y sus respectivos valores mapeados.
     * @throws SQLException Si ocurre un error al leer el {@link ResultSet}.
     */
    private Map<String, Object> mapRow(ResultSet rs, EntityConfig cfg) throws SQLException {
        // Inicializar el mapa de retorno de datos del registro
        Map<String, Object> item = new HashMap<>();
        item.put("id", rs.getInt(cfg.idCol));
        item.put(cfg.jsonNameKey, rs.getString(cfg.nameCol));
        item.put("is_active", true);

        // Iterar sobre las columnas adicionales configuradas en el mapa (bucle loop)
        for (Map.Entry<String, String> entry : cfg.extraColMap.entrySet()) {
            String dbCol = entry.getKey();
            String jsonKey = entry.getValue();
            Object value = rs.getObject(dbCol);

            // Validar si el valor obtenido es una instancia de Boolean (bloque condicional if)
            if (value instanceof Boolean) {
                item.put(jsonKey, value);
            // Verificar si el nombre de la columna física indica estado activo o precaución (bloque condicional else-if)
            } else if (dbCol.equalsIgnoreCase("Activo") || dbCol.equalsIgnoreCase("Activa") || dbCol.equalsIgnoreCase("Precaucion")) {
                item.put(jsonKey, rs.getBoolean(dbCol));
            // Mapear el valor directamente para cualquier otro caso (bloque condicional else)
            } else {
                item.put(jsonKey, value);
            }
        }
        return item;
    }

    /**
     * {@inheritDoc}
     * Realiza una consulta SELECT dinamizada con el nombre de la tabla y ordena por ID de forma descendente.
     */
    @Override
    public List<Map<String, Object>> getAll(EntityConfig cfg) throws SQLException {
        // Inicializar la lista que guardará todos los elementos de la tabla maestra
        List<Map<String, Object>> list = new ArrayList<>();
        // Construir la consulta SQL dinámica
        String sql = String.format("SELECT * FROM %s ORDER BY %s DESC", cfg.tableName, cfg.idCol);

        // Intentar conectar, preparar y ejecutar la consulta usando try-with-resources
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // Iterar sobre todos los registros recuperados de la tabla (bucle loop while)
            while (rs.next()) {
                list.add(mapRow(rs, cfg));
            }
        }
        return list;
    }

    /**
     * {@inheritDoc}
     * Realiza un SELECT dinamizado por el nombre de la tabla y columna ID filtrando por un parámetro.
     */
    @Override
    public Map<String, Object> getById(EntityConfig cfg, int id) throws SQLException {
        // Estructurar consulta SQL con filtro por ID
        String sql = String.format("SELECT * FROM %s WHERE %s = ?", cfg.tableName, cfg.idCol);
        // Intentar conectar y preparar consulta en try-with-resources
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            // Ejecutar la consulta en try-with-resources
            try (ResultSet rs = ps.executeQuery()) {
                // Verificar si existe el registro filtrado (bloque condicional if)
                if (rs.next()) {
                    return mapRow(rs, cfg);
                }
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * Ejecuta una consulta de conteo y otra paginada con LIMIT y OFFSET, filtrando opcionalmente por registros activos.
     */
    @Override
    public Map<String, Object> getPaginated(EntityConfig cfg, int page, int perPage) throws SQLException {
        // Calcular el desplazamiento (OFFSET) para la consulta SQL paginada
        int offset = (page - 1) * perPage;
        int total = 0;
        List<Map<String, Object>> list = new ArrayList<>();

        // Detectar si la tabla configurada contiene una columna física que controle si está activo el registro
        boolean hasActiveCol = cfg.extraColMap.containsKey("Activa") || cfg.extraColMap.containsKey("Activo");
        // Operador condicional ternario para definir el nombre físico de la columna de estado
        String activeColName = cfg.extraColMap.containsKey("Activa") ? "Activa" : "Activo";

        // Estructurar la consulta de conteo (COUNT) dependiendo de si se filtran activos (operador condicional ternario)
        String countSql = hasActiveCol 
            ? String.format("SELECT COUNT(*) FROM %s WHERE %s = 1", cfg.tableName, activeColName)
            : String.format("SELECT COUNT(*) FROM %s", cfg.tableName);

        // Estructurar la consulta de selección con LIMIT y OFFSET para la paginación (operador condicional ternario)
        String selectSql = hasActiveCol 
            ? String.format("SELECT * FROM %s WHERE %s = 1 LIMIT ? OFFSET ?", cfg.tableName, activeColName)
            : String.format("SELECT * FROM %s LIMIT ? OFFSET ?", cfg.tableName);

        // Conectar a la base de datos usando try-with-resources
        try (Connection conn = DatabaseConfig.getConnection()) {
            // 1. Obtener la cantidad total de registros para el cálculo de páginas usando try-with-resources
            try (PreparedStatement ps = conn.prepareStatement(countSql);
                 ResultSet rs = ps.executeQuery()) {
                // Leer el total de registros (bloque condicional if)
                if (rs.next()) {
                    total = rs.getInt(1);
                }
            }

            // 2. Recuperar la lista de registros limitados por la página actual usando try-with-resources
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setInt(1, perPage);
                ps.setInt(2, offset);
                // Ejecutar consulta y recorrer resultados en try-with-resources
                try (ResultSet rs = ps.executeQuery()) {
                    // Iterar sobre las filas de la página (bucle loop while)
                    while (rs.next()) {
                        list.add(mapRow(rs, cfg));
                    }
                }
            }
        }

        // Calcular dinámicamente el número de la última página
        int lastPage = (int) Math.ceil((double) total / perPage);
        // Validar que el número de última página no sea menor a 1 (bloque condicional if)
        if (lastPage < 1) {
            lastPage = 1;
        }

        // Construir la respuesta final estructurada con los datos de paginación
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("data", list);

        // Mapear los metadatos de la paginación para consumo de APIs frontend
        Map<String, Object> paginateMap = new HashMap<>();
        paginateMap.put("current_page", page);
        paginateMap.put("last_page", lastPage);
        paginateMap.put("per_page", perPage);
        paginateMap.put("total", total);
        responseMap.put("paginate", paginateMap);

        return responseMap;
    }

    /**
     * {@inheritDoc}
     * Construye dinámicamente una sentencia INSERT INTO con los campos descriptivos y extra configurados.
     */
    @Override
    public boolean insert(EntityConfig cfg, Map<String, Object> body) throws SQLException {
        // Inicializar listas de columnas físicas y valores a insertar
        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        columns.add(cfg.nameCol);
        values.add(body.get(cfg.jsonNameKey));

        // Iterar sobre las columnas extra configuradas (bucle loop)
        for (Map.Entry<String, String> entry : cfg.extraColMap.entrySet()) {
            String dbCol = entry.getKey();
            String jsonKey = entry.getValue();

            Object val = body.get(jsonKey);
            // Validar si el valor de la columna es nulo para asignar defaults (bloque condicional if-else)
            if (val == null) {
                // Verificar si la columna corresponde a control de estado activo (bloque condicional if-else)
                if (dbCol.equalsIgnoreCase("Activo") || dbCol.equalsIgnoreCase("Activa")) {
                    val = 1;
                } else if (dbCol.equalsIgnoreCase("Precaucion")) {
                    val = 0;
                }
            } else {
                // Formatear tipos booleanos o numéricos según corresponda (bloque condicional if-else)
                if (val instanceof Boolean) {
                    val = (Boolean) val ? 1 : 0;
                } else if (val instanceof Number) {
                    val = ((Number) val).intValue();
                } else if (val instanceof String) {
                    // Intentar parsear el String a entero por si es un código (bloque try-catch)
                    try {
                        val = Integer.parseInt((String) val);
                    } catch (NumberFormatException e) {
                        // Conservar el valor original si no se puede convertir a número (bloque catch)
                    }
                }
            }
            columns.add(dbCol);
            values.add(val);
        }

        // Construir la consulta de inserción SQL
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO ").append(cfg.tableName).append(" (");
        // Iterar para concatenar los nombres de columna (bucle loop)
        for (int i = 0; i < columns.size(); i++) {
            sqlBuilder.append(columns.get(i));
            // Concatenar coma si no es la última columna (bloque condicional if)
            if (i < columns.size() - 1) {
                sqlBuilder.append(", ");
            }
        }
        sqlBuilder.append(") VALUES (");
        // Iterar para colocar los marcadores de parámetro '?' (bucle loop)
        for (int i = 0; i < columns.size(); i++) {
            sqlBuilder.append("?");
            // Concatenar coma si no es el último parámetro (bloque condicional if)
            if (i < columns.size() - 1) {
                sqlBuilder.append(", ");
            }
        }
        sqlBuilder.append(")");

        // Intentar conectar y preparar la sentencia dinámica en try-with-resources
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlBuilder.toString())) {

            // Asignar dinámicamente los parámetros de la consulta (bucle loop)
            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, values.get(i));
            }

            int affected = ps.executeUpdate();
            // Retornar verdadero si se insertó al menos una fila (bloque condicional)
            return affected > 0;
        }
    }

    /**
     * {@inheritDoc}
     * Construye dinámicamente una sentencia UPDATE con las cláusulas SET correspondientes a las claves presentes en el cuerpo.
     */
    @Override
    public boolean update(EntityConfig cfg, int id, Map<String, Object> body) throws SQLException {
        // Inicializar listas para las cláusulas de actualización y los valores de parámetros
        List<String> setClauses = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        // Validar si el cuerpo de datos contiene la clave de nombre/descripción (bloque condicional if)
        if (body.containsKey(cfg.jsonNameKey)) {
            setClauses.add(cfg.nameCol + " = ?");
            values.add(body.get(cfg.jsonNameKey));
        }

        // Iterar sobre las columnas adicionales configuradas (bucle loop)
        for (Map.Entry<String, String> entry : cfg.extraColMap.entrySet()) {
            String dbCol = entry.getKey();
            String jsonKey = entry.getValue();

            // Validar si los nuevos datos contienen la clave de la columna extra (bloque condicional if)
            if (body.containsKey(jsonKey)) {
                setClauses.add(dbCol + " = ?");
                Object val = body.get(jsonKey);
                // Convertir tipos de datos del valor a actualizar (bloque condicional if-else)
                if (val instanceof Boolean) {
                    val = (Boolean) val ? 1 : 0;
                } else if (val instanceof Number) {
                    val = ((Number) val).intValue();
                } else if (val instanceof String) {
                    // Intentar parsear el String a entero si es compatible (bloque try-catch)
                    try {
                        val = Integer.parseInt((String) val);
                    } catch (NumberFormatException e) {
                        // Conservar el valor original si falla la conversión (bloque catch)
                    }
                }
                values.add(val);
            }
        }

        // Si no hay campos para actualizar, retornar falso inmediatamente (bloque condicional if)
        if (setClauses.isEmpty()) {
            return false;
        }

        // Construir la consulta de actualización SQL dinámica
        String sql = String.format("UPDATE %s SET %s WHERE %s = ?", cfg.tableName, String.join(", ", setClauses), cfg.idCol);
        // Intentar conectar y preparar la sentencia en try-with-resources
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Asignar parámetros dinámicos a la consulta (bucle loop)
            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, values.get(i));
            }
            ps.setInt(values.size() + 1, id);

            int affected = ps.executeUpdate();
            // Retornar verdadero si se modificó al menos una fila (bloque condicional)
            return affected > 0;
        }
    }

    /**
     * {@inheritDoc}
     * Actualiza el estado de activación en la base de datos identificando cuál es la columna 'Activo'/'Activa'.
     */
    @Override
    public boolean updateStatus(EntityConfig cfg, int id, int active) throws SQLException {
        String activeCol = null;
        // Buscar cuál columna extra mapea al estado activo (bucle loop)
        for (Map.Entry<String, String> entry : cfg.extraColMap.entrySet()) {
            // Verificar si el valor del mapa coincide con "is_active" (bloque condicional if)
            if (entry.getValue().equals("is_active")) {
                activeCol = entry.getKey();
                break;
            }
        }

        // Si no se encontró ninguna columna de estado de activación, asumir éxito inmediato (bloque condicional if)
        if (activeCol == null) {
            return true;
        }

        // Construir consulta SQL para actualizar el estado
        String sql = String.format("UPDATE %s SET %s = ? WHERE %s = ?", cfg.tableName, activeCol, cfg.idCol);
        // Intentar conectar y preparar la sentencia en try-with-resources
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, active);
            ps.setInt(2, id);
            int affected = ps.executeUpdate();
            // Retornar verdadero si se actualizó el registro (bloque condicional)
            return affected > 0;
        }
    }

    /**
     * {@inheritDoc}
     * Ejecuta una sentencia DELETE FROM dinámica para eliminar físicamente un registro.
     */
    @Override
    public boolean delete(EntityConfig cfg, int id) throws SQLException {
        // Construir la consulta de eliminación SQL
        String sql = String.format("DELETE FROM %s WHERE %s = ?", cfg.tableName, cfg.idCol);
        // Intentar conectar y preparar la sentencia en try-with-resources
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int affected = ps.executeUpdate();
            // Retornar verdadero si se eliminó el registro (bloque condicional)
            return affected > 0;
        }
    }
}

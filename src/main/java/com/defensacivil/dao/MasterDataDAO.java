package com.defensacivil.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Interfaz que define las operaciones genéricas de acceso a datos (DAO) para el mantenimiento
 * de tablas maestras o catálogos del sistema (ej. Genero, DocumentoTipo, Nacionalidad, etc.).
 */
public interface MasterDataDAO {

    /**
     * Clase de configuración interna que define el mapeo físico de una entidad maestra
     * de la base de datos hacia las claves de respuesta JSON.
     */
    class EntityConfig {
        /** Nombre físico de la tabla en la base de datos. */
        public final String tableName;
        /** Nombre físico de la columna que actúa como identificador primario. */
        public final String idCol;
        /** Nombre físico de la columna principal (ej. Nombre/Descripción). */
        public final String nameCol;
        /** Clave JSON que se utilizará para el nombre en las respuestas del API (generalmente "name"). */
        public final String jsonNameKey;
        /** Mapeo adicional de columnas físicas de la tabla y sus correspondientes claves JSON. */
        public final Map<String, String> extraColMap;

        /**
         * Constructor simplificado para entidades maestras básicas.
         *
         * @param tableName Nombre físico de la tabla.
         * @param idCol Nombre de la columna identificadora.
         * @param nameCol Nombre de la columna descriptiva.
         */
        public EntityConfig(String tableName, String idCol, String nameCol) {
            this(tableName, idCol, nameCol, "name", Map.of());
        }

        /**
         * Constructor completo para configurar tablas maestras complejas con columnas adicionales.
         *
         * @param tableName Nombre físico de la tabla.
         * @param idCol Nombre de la columna identificadora.
         * @param nameCol Nombre de la columna descriptiva.
         * @param jsonNameKey Clave JSON de salida para la columna descriptiva.
         * @param extraColMap Mapa de otras columnas físicas a sus claves JSON de salida.
         */
        public EntityConfig(String tableName, String idCol, String nameCol, String jsonNameKey, Map<String, String> extraColMap) {
            this.tableName = tableName;
            this.idCol = idCol;
            this.nameCol = nameCol;
            this.jsonNameKey = jsonNameKey;
            this.extraColMap = extraColMap;
        }
    }

    /**
     * Obtiene todos los registros de una entidad maestra configurada.
     *
     * @param cfg Configuración de la entidad {@link EntityConfig}.
     * @return Lista de mapas con los registros y sus campos estructurados.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    List<Map<String, Object>> getAll(EntityConfig cfg) throws SQLException;

    /**
     * Obtiene un registro específico de la entidad maestra por su ID.
     *
     * @param cfg Configuración de la entidad {@link EntityConfig}.
     * @param id Identificador único del registro.
     * @return Un mapa con los datos del registro, o null si no se encuentra.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    Map<String, Object> getById(EntityConfig cfg, int id) throws SQLException;

    /**
     * Obtiene registros paginados de la entidad maestra configurada.
     *
     * @param cfg Configuración de la entidad {@link EntityConfig}.
     * @param page Número de página a recuperar (1-based).
     * @param perPage Cantidad de registros por página.
     * @return Un mapa que contiene la lista de elementos ("items") y la cantidad total de páginas/registros.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    Map<String, Object> getPaginated(EntityConfig cfg, int page, int perPage) throws SQLException;

    /**
     * Inserta un nuevo registro en la tabla maestra configurada.
     *
     * @param cfg Configuración de la entidad {@link EntityConfig}.
     * @param body Mapa que contiene los valores a insertar.
     * @return true si la inserción fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean insert(EntityConfig cfg, Map<String, Object> body) throws SQLException;

    /**
     * Actualiza un registro existente de la tabla maestra configurada.
     *
     * @param cfg Configuración de la entidad {@link EntityConfig}.
     * @param id Identificador único del registro a actualizar.
     * @param body Mapa con los nuevos valores a asignar.
     * @return true si la actualización fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean update(EntityConfig cfg, int id, Map<String, Object> body) throws SQLException;

    /**
     * Actualiza el estado de activación (columna 'Activo' u homóloga) de un registro.
     *
     * @param cfg Configuración de la entidad {@link EntityConfig}.
     * @param id Identificador único del registro.
     * @param active Estado de activación (1 para activo, 0 para inactivo).
     * @return true si la actualización de estado fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean updateStatus(EntityConfig cfg, int id, int active) throws SQLException;

    /**
     * Elimina físicamente un registro de la tabla maestra por su ID.
     *
     * @param cfg Configuración de la entidad {@link EntityConfig}.
     * @param id Identificador único del registro a eliminar.
     * @return true si la eliminación fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean delete(EntityConfig cfg, int id) throws SQLException;
}

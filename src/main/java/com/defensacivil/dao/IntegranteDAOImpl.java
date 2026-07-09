package com.defensacivil.dao;

import com.defensacivil.config.DatabaseConfig;
import com.defensacivil.dto.MemberDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación de la interfaz {@link IntegranteDAO} que gestiona la persistencia
 * de integrantes y sus condiciones médicas mediante consultas SQL directas y almacenamiento en memoria
 * para datos no mapeados en la base de datos relacional.
 */
public class IntegranteDAOImpl implements IntegranteDAO {

    private final Map<String, Map<String, Object>> extraData;

    /**
     * Constructor de la clase que recibe un mapa para persistir temporalmente en memoria
     * campos adicionales que no tienen correspondencia directa en las columnas físicas de la tabla Integrante.
     *
     * @param extraData Mapa de almacenamiento para información adicional (ej. eps, birth_date, document_number).
     */
    public IntegranteDAOImpl(Map<String, Map<String, Object>> extraData) {
        // Inicializar el mapa de datos adicionales de memoria
        this.extraData = extraData;
    }

    /**
     * {@inheritDoc}
     * Obtiene una lista simplificada de integrantes (ID y nombre completo) asociados a un plan familiar.
     */
    @Override
    public List<Map<String, Object>> getMembersForSelect(int familyPlanId) throws SQLException {
        // Inicializar la lista que contendrá los mapas de integrantes simplificados
        List<Map<String, Object>> list = new ArrayList<>();
        // Definir la consulta SQL para seleccionar el ID, nombre y apellido de los integrantes del plan familiar
        String sql = "SELECT IdIntegrante, Nombre, Apellido FROM Integrante WHERE IdPlanFamiliar = ?";
        // Obtener conexión y preparar la sentencia SQL de forma segura usando try-with-resources
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // Asignar el parámetro del ID del plan familiar en la consulta SQL
            ps.setInt(1, familyPlanId);
            // Ejecutar la consulta SQL y procesar los resultados obtenidos en el ResultSet
            try (ResultSet rs = ps.executeQuery()) {
                // Iterar sobre cada registro obtenido en el ResultSet (bucle loop)
                while (rs.next()) {
                    // Crear un mapa para almacenar la información estructurada del integrante
                    Map<String, Object> member = new HashMap<>();
                    // Almacenar el identificador único del integrante en el mapa
                    member.put("id", rs.getInt("IdIntegrante"));
                    // Concatenar nombre y apellido para formar el nombre completo
                    member.put("full_name", rs.getString("Nombre") + " " + rs.getString("Apellido"));
                    // Añadir el mapa a la lista final
                    list.add(member);
                }
            }
        }
        // Retornar la lista de mapas conteniendo la información simplificada
        return list;
    }

    /**
     * {@inheritDoc}
     * Obtiene todos los integrantes de un plan familiar, combinando columnas de la base de datos
     * con la información adicional almacenada en memoria (extraData).
     */
    @Override
    public List<Map<String, Object>> getMembersByFamilyPlan(int familyPlanId) throws SQLException {
        // Inicializar la lista que guardará los detalles completos de los integrantes
        List<Map<String, Object>> list = new ArrayList<>();
        // Consulta SQL con múltiples INNER/LEFT JOINs para obtener descripciones de género, tipo de documento y nacionalidad
        String sql = """
            SELECT i.IdIntegrante, i.Nombre, i.Apellido, i.Parentesco, i.Telefono, i.IdGenero, i.IdDocumentoTipo, i.IdNacionalidad,
                   i.Documento, i.FechaNacimiento, i.GrupoSanguineo, i.Eps,
                   g.Nombre AS GeneroNombre, dt.Nombre AS DocumentoNombre, n.Nombre AS NacionalidadNombre
            FROM Integrante i
            LEFT JOIN Genero g ON i.IdGenero = g.IdGenero
            LEFT JOIN DocumentoTipo dt ON i.IdDocumentoTipo = dt.IdDocumentoTipo
            LEFT JOIN Nacionalidad n ON i.IdNacionalidad = n.IdNacionalidad
            WHERE i.IdPlanFamiliar = ?
            """;
        // Obtener conexión y preparar sentencia SQL usando try-with-resources
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // Establecer el parámetro del plan familiar
            ps.setInt(1, familyPlanId);
            // Ejecutar consulta SQL en try-with-resources
            try (ResultSet rs = ps.executeQuery()) {
                // Iterar a través de todos los integrantes encontrados (bucle loop)
                while (rs.next()) {
                    // Obtener el identificador único del integrante
                    int memberId = rs.getInt("IdIntegrante");
                    // Crear el mapa para agrupar los datos relacionales del integrante
                    Map<String, Object> member = new HashMap<>();
                    member.put("id", memberId);
                    member.put("names", rs.getString("Nombre"));
                    member.put("last_names", rs.getString("Apellido"));
                    member.put("full_name", rs.getString("Nombre") + " " + rs.getString("Apellido"));
                    member.put("relationship", rs.getString("Parentesco"));
                    member.put("phone", rs.getString("Telefono"));
                    // Mapear los datos embebidos requeridos por la vista o API usando condicionales ternarios
                    // Verificar si GeneroNombre es no nulo para asignar su valor, de lo contrario asignar un string vacío
                    member.put("gender", Map.of("name", rs.getString("GeneroNombre") != null ? rs.getString("GeneroNombre") : ""));
                    // Verificar si DocumentoNombre es no nulo para asignar su valor, de lo contrario asignar un string vacío
                    member.put("document_type", Map.of("acronym", rs.getString("DocumentoNombre") != null ? rs.getString("DocumentoNombre") : ""));
                    // Verificar si NacionalidadNombre es no nulo para asignar su valor, de lo contrario asignar un string vacío
                    member.put("nationality", Map.of("name", rs.getString("NacionalidadNombre") != null ? rs.getString("NacionalidadNombre") : ""));
                    
                    // Asignar el grupo sanguíneo, utilizando 'O+' por defecto si es nulo (condicional ternario)
                    member.put("blood_group", rs.getString("GrupoSanguineo") != null ? rs.getString("GrupoSanguineo") : "O+");
                    // Asignar la EPS, utilizando 'Compensar' por defecto si es nula (condicional ternario)
                    member.put("eps", rs.getString("Eps") != null ? rs.getString("Eps") : "Compensar");
                    // Asignar la fecha de nacimiento como string, usando '1990-01-01' por defecto si es nula (condicional ternario)
                    member.put("birth_date", rs.getDate("FechaNacimiento") != null ? rs.getDate("FechaNacimiento").toString() : "1990-01-01");
                    // Asignar el número de documento, generando uno ficticio si es nulo (condicional ternario)
                    member.put("document_number", rs.getString("Documento") != null ? rs.getString("Documento") : ("1000000" + memberId));
                    member.put("kinship", rs.getString("Parentesco"));
                    // Añadir el integrante estructurado a la lista
                    list.add(member);
                }
            }
        }
        // Retornar la lista con la información detallada de los integrantes
        return list;
    }

    /**
     * {@inheritDoc}
     * Recupera un integrante específico por su identificador, mapeando sus datos a un DTO
     * y cargando la información adicional desde el mapa en memoria.
     */
    @Override
    public MemberDTO getMemberById(int memberId) throws SQLException {
        // Consulta SQL para obtener los detalles de un integrante por su ID
        String sql = """
            SELECT i.IdIntegrante, i.Nombre, i.Apellido, i.Parentesco, i.Telefono, i.IdGenero, i.IdDocumentoTipo, i.IdNacionalidad,
                   i.Documento, i.FechaNacimiento, i.GrupoSanguineo, i.Eps
            FROM Integrante i
            WHERE i.IdIntegrante = ?
            """;
        // Intentar establecer conexión y preparar la consulta en un bloque try-with-resources
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // Asignar el parámetro del ID del integrante
            ps.setInt(1, memberId);
            // Ejecutar la consulta en un bloque try-with-resources
            try (ResultSet rs = ps.executeQuery()) {
                // Evaluar si existe al menos un registro resultante (bloque condicional if)
                if (rs.next()) {
                    // Instanciar un nuevo DTO de integrante
                    MemberDTO dto = new MemberDTO();
                    dto.setId(rs.getInt("IdIntegrante"));
                    dto.setNames(rs.getString("Nombre"));
                    dto.setLastNames(rs.getString("Apellido"));
                    dto.setRelationship(rs.getString("Parentesco"));
                    dto.setPhone(rs.getString("Telefono"));
                    dto.setGenderId(rs.getInt("IdGenero"));
                    dto.setDocumentTypeId(rs.getInt("IdDocumentoTipo"));
                    dto.setNationalityId(rs.getInt("IdNacionalidad"));
                    
                    // Obtener el nombre del grupo sanguíneo
                    String bgName = rs.getString("GrupoSanguineo");
                    // Resolver el ID numérico del grupo sanguíneo a partir de su nombre (usando "O+" por defecto si es nulo - condicional ternario)
                    int bloodGroupId = getBloodGroupIdByName(bgName != null ? bgName : "O+");

                    dto.setBloodGroupId(bloodGroupId);
                    // Resolver la EPS del integrante, usando "Compensar" por defecto si es nula (condicional ternario)
                    dto.setEps(rs.getString("Eps") != null ? rs.getString("Eps") : "Compensar");
                    // Resolver la fecha de nacimiento del integrante, usando "1990-01-01" por defecto si es nula (condicional ternario)
                    dto.setBirthDate(rs.getDate("FechaNacimiento") != null ? rs.getDate("FechaNacimiento").toString() : "1990-01-01");
                    // Resolver el número de documento, usando uno generado por defecto si es nulo (condicional ternario)
                    dto.setDocumentNumber(rs.getString("Documento") != null ? rs.getString("Documento") : ("1000000" + memberId));
                    // Retornar el objeto DTO del integrante
                    return dto;
                }
            }
        }
        // Retornar null si no se encontró ningún registro para el ID provisto
        return null;
    }

    /**
     * {@inheritDoc}
     * Consulta general de todos los integrantes registrados y sus respectivos planes familiares.
     */
    @Override
    public List<Map<String, Object>> getAllFamilyMembers() throws SQLException {
        // Inicializar la lista que guardará las relaciones de integrantes y planes familiares
        List<Map<String, Object>> list = new ArrayList<>();
        // Sentencia SQL para seleccionar el identificador del integrante y de su plan familiar
        String sql = "SELECT IdIntegrante, IdPlanFamiliar FROM Integrante";
        // Intentar establecer la conexión, preparar y ejecutar la consulta en un bloque try-with-resources
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            // Recorrer todos los registros resultantes (bucle loop)
            while (rs.next()) {
                // Añadir un mapa inmutable con las claves member_id y family_plan_id a la lista
                list.add(Map.of(
                    "member_id", rs.getInt("IdIntegrante"),
                    "family_plan_id", rs.getInt("IdPlanFamiliar")
                ));
            }
        }
        // Retornar la lista de relaciones
        return list;
    }

    /**
     * {@inheritDoc}
     * Inserta un nuevo integrante en la base de datos y almacena sus campos adicionales
     * (EPS, fecha de nacimiento, documento, tipo de sangre) en memoria (extraData).
     */
    @Override
    public int addMember(int familyPlanId, MemberDTO dto) throws SQLException {
        // Obtener los datos del integrante desde el DTO
        String names = dto.getNames();
        String lastNames = dto.getLastNames();
        String relationship = dto.getRelationship();
        String phone = dto.getPhone();
        // Asignar 1 por defecto al ID de género si es 0 (condicional ternario)
        int genderId = dto.getGenderId() == 0 ? 1 : dto.getGenderId();
        // Asignar 1 por defecto al ID de tipo de documento si es 0 (condicional ternario)
        int docTypeId = dto.getDocumentTypeId() == 0 ? 1 : dto.getDocumentTypeId();
        // Asignar 1 por defecto al ID de nacionalidad si es 0 (condicional ternario)
        int natId = dto.getNationalityId() == 0 ? 1 : dto.getNationalityId();

        // Consulta SQL para insertar el integrante en la tabla física de la base de datos
        String sql = "INSERT INTO Integrante (IdPlanFamiliar, Nombre, Apellido, Parentesco, Telefono, IdGenero, IdDocumentoTipo, IdNacionalidad, Documento, FechaNacimiento, GrupoSanguineo, Eps) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        // Intentar obtener conexión y preparar la sentencia solicitando el retorno de las llaves generadas
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // Mapear los parámetros correspondientes en la sentencia preparada
            ps.setInt(1, familyPlanId);
            ps.setString(2, names);
            ps.setString(3, lastNames);
            // Asignar "Familiar" por defecto si el parentesco es nulo (condicional ternario)
            ps.setString(4, relationship != null ? relationship : "Familiar");
            // Asignar un string vacío por defecto si el teléfono es nulo (condicional ternario)
            ps.setString(5, phone != null ? phone : "");
            ps.setInt(6, genderId);
            ps.setInt(7, docTypeId);
            ps.setInt(8, natId);
            ps.setString(9, dto.getDocumentNumber());
            // Validar si la fecha de nacimiento en el DTO es no nula y no vacía para realizar la conversión a java.sql.Date, de lo contrario asignar null (condicional ternario)
            ps.setDate(10, dto.getBirthDate() != null && !dto.getBirthDate().isEmpty() ? Date.valueOf(dto.getBirthDate()) : null);
            // Obtener el nombre del grupo sanguíneo a partir de su ID (por defecto ID 1 si es 0 - condicional ternario)
            ps.setString(11, getBloodGroupNameById(dto.getBloodGroupId() == 0 ? 1 : dto.getBloodGroupId()));
            ps.setString(12, dto.getEps());
            // Ejecutar la inserción
            ps.executeUpdate();
            // Intentar recuperar el ID autogenerado del integrante en un bloque try-with-resources
            try (ResultSet rs = ps.getGeneratedKeys()) {
                // Verificar si se generó y retornó alguna llave (bloque condicional if)
                if (rs.next()) {
                    // Obtener la llave autogenerada
                    int memberId = rs.getInt(1);
                    // Retornar el ID del integrante recién creado
                    return memberId;
                }
            }
        }
        // Retornar -1 en caso de que no se haya completado la creación o no se generara la llave
        return -1;
    }

    /**
     * {@inheritDoc}
     * Actualiza la información básica del integrante en la base de datos y sus campos adicionales
     * (EPS, fecha de nacimiento, documento, tipo de sangre) en memoria (extraData).
     */
    @Override
    public boolean updateMember(int memberId, MemberDTO dto) throws SQLException {
        // Extraer los valores actualizados del DTO
        String names = dto.getNames();
        String lastNames = dto.getLastNames();
        String relationship = dto.getRelationship();
        String phone = dto.getPhone();
        // Asignar 1 por defecto si el ID de género es 0 (condicional ternario)
        int genderId = dto.getGenderId() == 0 ? 1 : dto.getGenderId();
        // Asignar 1 por defecto si el ID de tipo de documento es 0 (condicional ternario)
        int docTypeId = dto.getDocumentTypeId() == 0 ? 1 : dto.getDocumentTypeId();
        // Asignar 1 por defecto si el ID de nacionalidad es 0 (condicional ternario)
        int natId = dto.getNationalityId() == 0 ? 1 : dto.getNationalityId();

        // Sentencia SQL para actualizar todas las columnas asociadas al integrante
        String sql = "UPDATE Integrante SET Nombre = ?, Apellido = ?, Parentesco = ?, Telefono = ?, IdGenero = ?, IdDocumentoTipo = ?, IdNacionalidad = ?, Documento = ?, FechaNacimiento = ?, GrupoSanguineo = ?, Eps = ? WHERE IdIntegrante = ?";
        // Intentar conectar y preparar la sentencia en un try-with-resources
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // Asignar los parámetros para la actualización
            ps.setString(1, names);
            ps.setString(2, lastNames);
            // Asignar "Familiar" si es nulo (condicional ternario)
            ps.setString(3, relationship != null ? relationship : "Familiar");
            // Asignar vacío si es nulo (condicional ternario)
            ps.setString(4, phone != null ? phone : "");
            ps.setInt(5, genderId);
            ps.setInt(6, docTypeId);
            ps.setInt(7, natId);
            ps.setString(8, dto.getDocumentNumber());
            // Convertir la fecha si es no nula y no vacía, de lo contrario establecer null (condicional ternario)
            ps.setDate(9, dto.getBirthDate() != null && !dto.getBirthDate().isEmpty() ? Date.valueOf(dto.getBirthDate()) : null);
            // Obtener el nombre del grupo sanguíneo (ID 1 por defecto si es 0 - condicional ternario)
            ps.setString(10, getBloodGroupNameById(dto.getBloodGroupId() == 0 ? 1 : dto.getBloodGroupId()));
            ps.setString(11, dto.getEps());
            ps.setInt(12, memberId);
            // Retornar verdadero si la cantidad de filas afectadas por la actualización es mayor a 0 (condicional)
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * {@inheritDoc}
     * Elimina al integrante de la base de datos de manera transaccional, removiendo primero
     * las dependencias en la tabla IntegranteEnfermedad y luego el registro del integrante y sus datos de memoria.
     */
    @Override
    public boolean deleteMember(int memberId) throws SQLException {
        // Obtener conexión a la base de datos usando try-with-resources
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Desactivar el auto-commit para iniciar una transacción manual y asegurar atomicidad
            conn.setAutoCommit(false);
            // Intentar ejecutar las operaciones transaccionales en un bloque try
            try {
                // 1. Eliminar primero las condiciones médicas asociadas del integrante para respetar la clave foránea en try-with-resources
                try (PreparedStatement psD = conn.prepareStatement("DELETE FROM IntegranteEnfermedad WHERE IdIntegrante = ?")) {
                    psD.setInt(1, memberId);
                    psD.executeUpdate();
                }
                int affectedRows = 0;
                // 2. Eliminar el registro principal del integrante de la tabla Integrante en try-with-resources
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Integrante WHERE IdIntegrante = ?")) {
                    ps.setInt(1, memberId);
                    affectedRows = ps.executeUpdate();
                }
                // Si todo fue exitoso, confirmar los cambios de la transacción en la base de datos
                conn.commit();
                // Remover la información adicional del integrante de la memoria caché virtual
                extraData.remove("member_" + memberId);
                // Retornar verdadero si se eliminó al menos una fila del integrante
                return affectedRows > 0;
            } catch (SQLException e) {
                // En caso de cualquier error de SQL, revertir todos los cambios realizados en la transacción (bloque catch)
                conn.rollback();
                // Relanzar la excepción para que sea manejada en capas superiores
                throw e;
            } finally {
                // Asegurar que el comportamiento de auto-commit se restablezca a verdadero para la conexión (bloque finally)
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * {@inheritDoc}
     * Obtiene las enfermedades asociadas a un integrante realizando un JOIN entre IntegranteEnfermedad
     * y Enfermedad. Adicionalmente, genera un ID único combinando memberId y diseaseId.
     */
    @Override
    public List<Map<String, Object>> getConditionsByMember(int memberId) throws SQLException {
        // Inicializar la lista para contener las condiciones/enfermedades del integrante
        List<Map<String, Object>> list = new ArrayList<>();
        // Sentencia SQL con JOIN para asociar la relación con el nombre del catálogo de Enfermedad
        String sql = "SELECT ie.IdEnfermedad, e.Nombre, ie.Medicina, ie.Dosis " +
                     "FROM IntegranteEnfermedad ie " +
                     "JOIN Enfermedad e ON ie.IdEnfermedad = e.IdEnfermedad " +
                     "WHERE ie.IdIntegrante = ?";
        // Intentar conectar y preparar la consulta en un try-with-resources
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // Asignar el parámetro del ID del integrante
            ps.setInt(1, memberId);
            // Ejecutar la consulta en un try-with-resources
            try (ResultSet rs = ps.executeQuery()) {
                // Iterar sobre cada registro resultante (bucle loop while)
                while (rs.next()) {
                    // Crear un mapa para contener los atributos de la condición médica
                    Map<String, Object> cond = new HashMap<>();
                    int diseaseId = rs.getInt("IdEnfermedad");
                    // Generar un ID relacional ficticio combinando el ID de integrante y de enfermedad
                    int relationId = memberId * 100000 + diseaseId;
                    cond.put("id", relationId);
                    cond.put("name", rs.getString("Nombre"));
                    cond.put("dose", rs.getString("Dosis"));
                    cond.put("medicine", rs.getString("Medicina"));

                    // Mapear el mock type para acoplar con tu frontend
                    cond.put("condition_type_id", 1);
                    cond.put("condition_type", Map.of("id", 1, "name", "Diabetes"));
                    // Añadir la condición médica a la lista
                    list.add(cond);
                }
            }
        }
        // Retornar la lista de condiciones médicas
        return list;
    }

    /**
     * {@inheritDoc}
     * Obtiene los detalles de una condición médica específica por su ID compuesto (memberId * 100000 + diseaseId).
     */
    @Override
    public Map<String, Object> getConditionById(int conditionId) throws SQLException {
        // Descomponer el ID compuesto para obtener el ID de integrante original
        int memberId = conditionId / 100000;
        // Descomponer el ID compuesto para obtener el ID de enfermedad original
        int diseaseId = conditionId % 100000;
        // Consulta SQL para obtener los datos de la relación integrante-enfermedad
        String sql = "SELECT ie.IdIntegrante, ie.IdEnfermedad, e.Nombre, ie.Medicina, ie.Dosis " +
                     "FROM IntegranteEnfermedad ie " +
                     "JOIN Enfermedad e ON ie.IdEnfermedad = e.IdEnfermedad " +
                     "WHERE ie.IdIntegrante = ? AND ie.IdEnfermedad = ?";
        // Intentar conectar y preparar la sentencia usando try-with-resources
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // Mapear los parámetros del integrante y de la enfermedad
            ps.setInt(1, memberId);
            ps.setInt(2, diseaseId);
            // Ejecutar la consulta en try-with-resources
            try (ResultSet rs = ps.executeQuery()) {
                // Validar si existe el registro (bloque condicional if)
                if (rs.next()) {
                    // Crear el mapa con los detalles de la condición
                    Map<String, Object> cond = new HashMap<>();
                    cond.put("id", conditionId);
                    cond.put("name", rs.getString("Nombre"));
                    cond.put("dose", rs.getString("Dosis"));
                    cond.put("medicine", rs.getString("Medicina"));
                    cond.put("condition_type_id", 1);
                    cond.put("condition_type", Map.of("id", 1, "name", "Diabetes"));
                    cond.put("member_id", rs.getInt("IdIntegrante"));
                    // Retornar el mapa del registro
                    return cond;
                }
            }
        }
        // Retornar null si no se encuentra la relación
        return null;
    }

    /**
     * {@inheritDoc}
     * Asocia una condición médica a un integrante. Si la enfermedad no existe en el catálogo
     * (tabla Enfermedad), se crea previamente de forma transaccional.
     */
    @Override
    public boolean addCondition(int memberId, String name, String dose) throws SQLException {
        // Intentar conectar a la base de datos usando try-with-resources
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Deshabilitar auto-commit para manejar manualmente la transacción
            conn.setAutoCommit(false);
            // Intentar operaciones transaccionales en un bloque try
            try {
                int diseaseId = 0;
                // 1. Find or create in Enfermedad catalog
                String findSql = "SELECT IdEnfermedad FROM Enfermedad WHERE Nombre = ?";
                // Preparar y ejecutar consulta para buscar si la enfermedad ya está registrada en el catálogo en try-with-resources
                try (PreparedStatement findPs = conn.prepareStatement(findSql)) {
                    findPs.setString(1, name);
                    try (ResultSet rs = findPs.executeQuery()) {
                        // Si existe la enfermedad, obtener su ID correspondiente (bloque condicional if)
                        if (rs.next()) {
                            diseaseId = rs.getInt("IdEnfermedad");
                        }
                    }
                }
                // Si la enfermedad no existe en el catálogo, proceder a insertarla (bloque condicional if)
                if (diseaseId == 0) {
                    String insertCatalogSql = "INSERT INTO Enfermedad (Nombre) VALUES (?)";
                    // Preparar la inserción en Enfermedad solicitando la llave generada en try-with-resources
                    try (PreparedStatement insertCatalogPs = conn.prepareStatement(insertCatalogSql, Statement.RETURN_GENERATED_KEYS)) {
                        insertCatalogPs.setString(1, name);
                        insertCatalogPs.executeUpdate();
                        // Obtener la llave autogenerada en try-with-resources
                        try (ResultSet rs = insertCatalogPs.getGeneratedKeys()) {
                            // Validar si existe la llave autogenerada (bloque condicional if)
                            if (rs.next()) {
                                diseaseId = rs.getInt(1);
                            }
                        }
                    }
                }
                // Si tras buscar/crear no se obtuvo un ID válido de enfermedad, hacer rollback y salir (bloque condicional if)
                if (diseaseId == 0) {
                    conn.rollback();
                    return false;
                }
                // 2. Insert link into IntegranteEnfermedad
                String insertLinkSql = "INSERT INTO IntegranteEnfermedad (IdIntegrante, IdEnfermedad, Medicina, Dosis) VALUES (?, ?, ?, ?)";
                // Preparar la inserción del enlace integrante-enfermedad en try-with-resources
                try (PreparedStatement ps = conn.prepareStatement(insertLinkSql)) {
                    ps.setInt(1, memberId);
                    ps.setInt(2, diseaseId);
                    ps.setString(3, name);
                    // Asignar una dosis vacía si es nula (condicional ternario)
                    ps.setString(4, dose != null ? dose : "");
                    // Ejecutar la consulta de inserción
                    int rows = ps.executeUpdate();
                    // Confirmar transacción
                    conn.commit();
                    // Retornar verdadero si se insertó el registro exitosamente
                    return rows > 0;
                }
            } catch (SQLException e) {
                // Hacer rollback ante cualquier excepción de SQL (bloque catch)
                conn.rollback();
                // Propagar la excepción
                throw e;
            } finally {
                // Restablecer el estado de auto-commit (bloque finally)
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * {@inheritDoc}
     * Actualiza la condición médica de un integrante usando el ID compuesto para identificar la relación anterior,
     * permitiendo cambiar la enfermedad (creándola si no existe en el catálogo) y la dosis de forma transaccional.
     */
    @Override
    public boolean updateCondition(int conditionId, String name, String dose) throws SQLException {
        // Descomponer el ID compuesto
        int memberId = conditionId / 100000;
        int oldDiseaseId = conditionId % 100000;
        // Intentar obtener conexión usando try-with-resources
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Iniciar transacción desactivando el auto-commit
            conn.setAutoCommit(false);
            // Intentar operaciones transaccionales en un bloque try
            try {
                int diseaseId = 0;
                // 1. Find or create in Enfermedad catalog
                String findSql = "SELECT IdEnfermedad FROM Enfermedad WHERE Nombre = ?";
                // Buscar si la nueva enfermedad existe en el catálogo en try-with-resources
                try (PreparedStatement findPs = conn.prepareStatement(findSql)) {
                    findPs.setString(1, name);
                    try (ResultSet rs = findPs.executeQuery()) {
                        // Validar si existe en ResultSet (bloque condicional if)
                        if (rs.next()) {
                            diseaseId = rs.getInt("IdEnfermedad");
                        }
                    }
                }
                // Si no existe, crear la enfermedad en el catálogo (bloque condicional if)
                if (diseaseId == 0) {
                    String insertCatalogSql = "INSERT INTO Enfermedad (Nombre) VALUES (?)";
                    try (PreparedStatement insertCatalogPs = conn.prepareStatement(insertCatalogSql, Statement.RETURN_GENERATED_KEYS)) {
                        insertCatalogPs.setString(1, name);
                        insertCatalogPs.executeUpdate();
                        try (ResultSet rs = insertCatalogPs.getGeneratedKeys()) {
                            // Validar si existe la clave generada (bloque condicional if)
                            if (rs.next()) {
                                diseaseId = rs.getInt(1);
                            }
                        }
                    }
                }
                // Si falla la obtención del ID de enfermedad, revertir transacción (bloque condicional if)
                if (diseaseId == 0) {
                    conn.rollback();
                    return false;
                }
                // 2. Update link in IntegranteEnfermedad
                String updateLinkSql = "UPDATE IntegranteEnfermedad SET IdEnfermedad = ?, Medicina = ?, Dosis = ? WHERE IdIntegrante = ? AND IdEnfermedad = ?";
                // Actualizar la relación integrante-enfermedad con la nueva enfermedad y dosis en try-with-resources
                try (PreparedStatement ps = conn.prepareStatement(updateLinkSql)) {
                    ps.setInt(1, diseaseId);
                    ps.setString(2, name);
                    // Asignar dosis vacía si es nula (condicional ternario)
                    ps.setString(3, dose != null ? dose : "");
                    ps.setInt(4, memberId);
                    ps.setInt(5, oldDiseaseId);
                    // Ejecutar actualización
                    int rows = ps.executeUpdate();
                    // Confirmar transacción
                    conn.commit();
                    // Retornar verdadero si se modificó alguna fila
                    return rows > 0;
                }
            } catch (SQLException e) {
                // En caso de error, revertir cambios (bloque catch)
                conn.rollback();
                // Relanzar la excepción
                throw e;
            } finally {
                // Restablecer el comportamiento de auto-commit (bloque finally)
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * {@inheritDoc}
     * Elimina el enlace entre un integrante y una enfermedad de la tabla IntegranteEnfermedad
     * decodificando el ID compuesto.
     */
    @Override
    public boolean deleteCondition(int conditionId) throws SQLException {
        // Descomponer el ID compuesto
        int memberId = conditionId / 100000;
        int diseaseId = conditionId % 100000;
        // Sentencia SQL para eliminar la relación específica de la base de datos
        String sql = "DELETE FROM IntegranteEnfermedad WHERE IdIntegrante = ? AND IdEnfermedad = ?";
        // Intentar conectar y preparar la sentencia usando try-with-resources
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            ps.setInt(2, diseaseId);
            // Retornar verdadero si se eliminó la fila (condicional)
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Mapea el nombre del grupo sanguíneo a su ID correspondiente en base de datos.
     *
     * @param bgName Nombre del grupo sanguíneo (ej. "O+").
     * @return El ID entero del grupo sanguíneo.
     */
    private int getBloodGroupIdByName(String bgName) {
        // Estructura de control condicional if para verificar coincidencia con el grupo sanguíneo O+
        if ("O+".equalsIgnoreCase(bgName)) return 1;
        // Estructura de control condicional if para verificar coincidencia con el grupo sanguíneo O-
        if ("O-".equalsIgnoreCase(bgName)) return 2;
        // Estructura de control condicional if para verificar coincidencia con el grupo sanguíneo A+
        if ("A+".equalsIgnoreCase(bgName)) return 3;
        // Estructura de control condicional if para verificar coincidencia con el grupo sanguíneo A-
        if ("A-".equalsIgnoreCase(bgName)) return 4;
        // Estructura de control condicional if para verificar coincidencia con el grupo sanguíneo B+
        if ("B+".equalsIgnoreCase(bgName)) return 5;
        // Estructura de control condicional if para verificar coincidencia con el grupo sanguíneo B-
        if ("B-".equalsIgnoreCase(bgName)) return 6;
        // Estructura de control condicional if para verificar coincidencia con el grupo sanguíneo AB+
        if ("AB+".equalsIgnoreCase(bgName)) return 7;
        // Estructura de control condicional if para verificar coincidencia con el grupo sanguíneo AB-
        if ("AB-".equalsIgnoreCase(bgName)) return 8;
        // Retornar 1 (O+) por defecto si no coincide con ninguno
        return 1;
    }

    /**
     * Mapea el ID del grupo sanguíneo a su nombre correspondiente en base de datos.
     *
     * @param bgId ID del grupo sanguíneo.
     * @return El nombre string del grupo sanguíneo.
     */
    private String getBloodGroupNameById(int bgId) {
        // Estructura de control switch-case para resolver el nombre del grupo sanguíneo según el ID
        switch (bgId) {
            case 1: return "O+";
            case 2: return "O-";
            case 3: return "A+";
            case 4: return "A-";
            case 5: return "B+";
            case 6: return "B-";
            case 7: return "AB+";
            case 8: return "AB-";
            // Retornar O+ por defecto en caso de ID no registrado
            default: return "O+";
        }
    }
}

package com.defensacivil.dao;

import com.defensacivil.config.DatabaseConfig;
import com.defensacivil.dto.PetDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación de la interfaz {@link MascotaDAO} que gestiona la persistencia
 * de mascotas y sus vacunas utilizando consultas JDBC.
 */
public class MascotaDAOImpl implements MascotaDAO {

    private final Map<String, Map<String, Object>> extraData;

    /**
     * Constructor de la clase MascotaDAOImpl.
     *
     * @param extraData Mapa para el almacenamiento de datos adicionales en memoria.
     */
    public MascotaDAOImpl(Map<String, Map<String, Object>> extraData) {
        // Inicializar el mapa de datos adicionales provisto
        this.extraData = extraData;
    }

    /**
     * Método auxiliar privado para mapear una fila del {@link ResultSet} a un mapa de datos de mascota.
     *
     * @param rs El {@link ResultSet} posicionado en la fila actual.
     * @return Un mapa que representa a la mascota con claves estructuradas (id, name, breed, birth_date, age, etc.).
     * @throws SQLException Si ocurre un error al leer del {@link ResultSet}.
     */
    private Map<String, Object> mapResultSetToPet(ResultSet rs) throws SQLException {
        // Inicializar el mapa contenedor de los datos de la mascota
        Map<String, Object> pet = new HashMap<>();
        pet.put("id", rs.getInt("IdMascota"));
        pet.put("name", rs.getString("Nombre"));
        pet.put("breed", rs.getString("Raza"));
        
        // Obtener la fecha de nacimiento como java.sql.Date
        Date birthDateVal = rs.getDate("FechaNacimiento");
        // Operador condicional ternario para validar si la fecha es no nula y convertirla a string
        pet.put("birth_date", birthDateVal != null ? birthDateVal.toString() : "");

        // Cargar los datos y nombres de la especie, asignando un valor predeterminado si es nulo
        int speciesId = rs.getInt("IdEspecie");
        String especieStr = rs.getString("EspecieNombre");
        // Bloque condicional if para asignar especie por defecto si es nula
        if (especieStr == null) {
            especieStr = "Perro";
        }
        pet.put("species_id", speciesId);
        pet.put("species_name", especieStr);
        // Formar el sub-mapa para simular un objeto relacional anidado (Especie)
        pet.put("species", Map.of("id", speciesId, "name", especieStr));

        // Cargar y mapear el género de la mascota con valores por defecto
        int genderId = rs.getInt("IdGenero");
        String generoStr = rs.getString("GeneroNombre");
        // Operador condicional ternario para establecer ID de género por defecto si es cero
        int finalGenderId = (genderId == 0) ? 1 : genderId;
        String genderName = "Macho";
        // Bloque condicional if para verificar si el género devuelto no es nulo
        if (generoStr != null) {
            // Bloque condicional if anidado para verificar si corresponde a un género femenino
            if ("Femenino".equalsIgnoreCase(generoStr) || "Hembra".equalsIgnoreCase(generoStr)) {
                genderName = "Hembra";
            }
        }
        pet.put("animal_gender_id", finalGenderId);
        pet.put("animal_gender_name", genderName);
        // Formar el sub-mapa anidado del género de la mascota
        pet.put("animal_gender", Map.of("id", finalGenderId, "name", genderName));

        // Calcular la edad actual de la mascota en años a partir de la fecha de nacimiento
        int age = 0;
        // Bloque condicional if para calcular la edad solo si existe la fecha de nacimiento
        if (birthDateVal != null) {
            java.time.LocalDate birthDate = birthDateVal.toLocalDate();
            java.time.LocalDate now = java.time.LocalDate.now();
            age = java.time.Period.between(birthDate, now).getYears();
        }
        pet.put("age", age);
        pet.put("family_plan_id", rs.getInt("IdPlanFamiliar"));
        return pet;
    }

    /**
     * {@inheritDoc}
     * Obtiene la lista de mascotas registradas en un plan familiar específico mediante un SELECT con JOINs.
     */
    @Override
    public List<Map<String, Object>> getPetsByFamilyPlan(int planId) throws SQLException {
        // Inicializar la lista de mapas para los registros de las mascotas
        List<Map<String, Object>> list = new ArrayList<>();
        // Sentencia SQL para obtener mascotas e información de género y especie asociadas a un plan familiar
        String sql = """
            SELECT m.IdMascota, m.Nombre, m.IdGenero, g.Nombre AS GeneroNombre, m.Raza, 
                   m.IdEspecie, e.Nombre AS EspecieNombre, m.FechaNacimiento, m.IdPlanFamiliar 
            FROM Mascotas m 
            LEFT JOIN Genero g ON m.IdGenero = g.IdGenero 
            LEFT JOIN Especie e ON m.IdEspecie = e.IdEspecie 
            WHERE m.IdPlanFamiliar = ?
            """;
        // Intentar conectar y preparar la sentencia usando try-with-resources
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, planId);
            // Ejecutar la consulta en try-with-resources
            try (ResultSet rs = ps.executeQuery()) {
                // Iterar sobre cada fila obtenida (bucle loop)
                while (rs.next()) {
                    list.add(mapResultSetToPet(rs));
                }
            }
        }
        return list;
    }

    /**
     * {@inheritDoc}
     * Recupera una mascota específica por su ID mapeando los datos de la base de datos a un {@link PetDTO}.
     */
    @Override
    public PetDTO getPetById(int petId) throws SQLException {
        // Consulta SQL para obtener los datos de la mascota filtrada por su ID
        String sql = """
            SELECT m.IdMascota, m.Nombre, m.IdGenero, m.Raza, m.IdEspecie, e.Nombre AS EspecieNombre, m.FechaNacimiento, m.IdPlanFamiliar 
            FROM Mascotas m 
            LEFT JOIN Especie e ON m.IdEspecie = e.IdEspecie
            WHERE m.IdMascota = ?
            """;
        // Intentar conectar y preparar la sentencia en try-with-resources
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, petId);
            // Ejecutar la consulta en try-with-resources
            try (ResultSet rs = ps.executeQuery()) {
                // Validar si existe el registro de la mascota (bloque condicional if)
                if (rs.next()) {
                    PetDTO dto = new PetDTO();
                    dto.setId(rs.getInt("IdMascota"));
                    dto.setName(rs.getString("Nombre"));
                    dto.setBreed(rs.getString("Raza"));
                    
                    Date birthDateVal = rs.getDate("FechaNacimiento");
                    // Operador condicional ternario para mapear la fecha a String
                    dto.setBirthDate(birthDateVal != null ? birthDateVal.toString() : "");

                    dto.setSpeciesId(rs.getInt("IdEspecie"));
                    dto.setAnimalGenderId(rs.getInt("IdGenero"));
                    dto.setFamilyPlanId(rs.getInt("IdPlanFamiliar"));

                    int age = 0;
                    // Bloque condicional if para calcular la edad si la fecha es no nula
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

    /**
     * {@inheritDoc}
     * Obtiene una lista con todas las mascotas registradas en la base de datos.
     */
    @Override
    public List<Map<String, Object>> getAllPets() throws SQLException {
        // Inicializar la lista que guardará los mapas de todas las mascotas
        List<Map<String, Object>> list = new ArrayList<>();
        // Sentencia SQL para consultar todos los registros de mascotas con su género y especie
        String sql = """
            SELECT m.IdMascota, m.Nombre, m.IdGenero, g.Nombre AS GeneroNombre, m.Raza, 
                   m.IdEspecie, e.Nombre AS EspecieNombre, m.FechaNacimiento, m.IdPlanFamiliar 
            FROM Mascotas m 
            LEFT JOIN Genero g ON m.IdGenero = g.IdGenero 
            LEFT JOIN Especie e ON m.IdEspecie = e.IdEspecie
            """;
        // Intentar conectar, preparar y ejecutar consulta en try-with-resources
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            // Recorrer los registros (bucle loop)
            while (rs.next()) {
                list.add(mapResultSetToPet(rs));
            }
        }
        return list;
    }

    /**
     * {@inheritDoc}
     * Inserta un nuevo registro de mascota y retorna la clave autogenerada.
     */
    @Override
    public int insertPet(PetDTO dto) throws SQLException {
        // Extraer atributos del DTO de mascota
        String name = dto.getName();
        String breed = dto.getBreed();
        String birthDateStr = dto.getBirthDate();
        // Operador condicional ternario para asegurar valor predeterminado si es 0
        int genderId = dto.getAnimalGenderId() == 0 ? 1 : dto.getAnimalGenderId();
        // Operador condicional ternario para asegurar valor predeterminado si es 0
        int speciesId = dto.getSpeciesId() == 0 ? 1 : dto.getSpeciesId();
        // Operador condicional ternario para asegurar valor predeterminado si es 0
        int planId = dto.getFamilyPlanId() == 0 ? 1 : dto.getFamilyPlanId();

        // Consulta SQL para insertar la mascota
        String sql = "INSERT INTO Mascotas (Nombre, IdGenero, Raza, IdEspecie, FechaNacimiento, IdPlanFamiliar) VALUES (?, ?, ?, ?, ?, ?)";
        // Intentar conectar y preparar la sentencia solicitando el retorno de llaves autogeneradas
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, genderId);
            ps.setString(3, breed);
            ps.setInt(4, speciesId);
            // Operador condicional ternario para validar si se proporciona fecha o asignar la actual
            ps.setDate(5, birthDateStr != null && !birthDateStr.isEmpty() ? Date.valueOf(birthDateStr) : Date.valueOf(java.time.LocalDate.now()));
            ps.setInt(6, planId);
            ps.executeUpdate();

            // Intentar recuperar el ID generado en try-with-resources
            try (ResultSet rs = ps.getGeneratedKeys()) {
                // Verificar si se obtuvo la clave (bloque condicional if)
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     * Actualiza un registro de mascota existente en la base de datos con los datos del DTO.
     */
    @Override
    public boolean updatePet(int petId, PetDTO dto) throws SQLException {
        // Extraer atributos del DTO
        String name = dto.getName();
        String breed = dto.getBreed();
        String birthDateStr = dto.getBirthDate();
        // Operadores condicionales ternarios para asegurar IDs correctos
        int genderId = dto.getAnimalGenderId() == 0 ? 1 : dto.getAnimalGenderId();
        int speciesId = dto.getSpeciesId() == 0 ? 1 : dto.getSpeciesId();

        // Consulta SQL para realizar la actualización
        String sql = "UPDATE Mascotas SET Nombre = ?, IdGenero = ?, Raza = ?, IdEspecie = ?, FechaNacimiento = ? WHERE IdMascota = ?";
        // Intentar conectar y preparar la sentencia en try-with-resources
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, genderId);
            ps.setString(3, breed);
            ps.setInt(4, speciesId);
            // Operador condicional ternario para validar la fecha de nacimiento
            ps.setDate(5, birthDateStr != null && !birthDateStr.isEmpty() ? Date.valueOf(birthDateStr) : Date.valueOf(java.time.LocalDate.now()));
            ps.setInt(6, petId);
            // Retornar verdadero si se modificó alguna fila (condicional)
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * {@inheritDoc}
     * Elimina una mascota de manera transaccional, eliminando primero sus vacunas
     * registradas en la tabla intermedia MascotaVacuna para mantener la integridad referencial.
     */
    @Override
    public boolean deletePet(int petId) throws SQLException {
        // Intentar conectar usando try-with-resources
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Iniciar transacción manual desactivando auto-commit
            conn.setAutoCommit(false);
            // Ejecutar operaciones de borrado en un bloque try
            try {
                // 1. Eliminar vacunas relacionadas en la tabla intermedia usando try-with-resources
                try (PreparedStatement psV = conn.prepareStatement("DELETE FROM MascotaVacuna WHERE IdMascota = ?")) {
                    psV.setInt(1, petId);
                    psV.executeUpdate();
                }
                int affectedRows = 0;
                // 2. Eliminar el registro principal de la mascota en try-with-resources
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Mascotas WHERE IdMascota = ?")) {
                    ps.setInt(1, petId);
                    affectedRows = ps.executeUpdate();
                }
                // Confirmar transacción
                conn.commit();
                // Retornar verdadero si se eliminó la mascota (condicional)
                return affectedRows > 0;
            } catch (SQLException e) {
                // Revertir cambios en caso de error (bloque catch)
                conn.rollback();
                // Propagar la excepción
                throw e;
            } finally {
                // Restablecer auto-commit (bloque finally)
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * {@inheritDoc}
     * Obtiene el listado de vacunas aplicadas a una mascota realizando un JOIN.
     **/
    @Override
    public List<Map<String, Object>> getVaccinesByPet(int petId) throws SQLException {
        // Inicializar la lista de mapas para las vacunas de la mascota
        List<Map<String, Object>> list = new ArrayList<>();
        // Sentencia SQL con JOIN para asociar la vacuna y su fecha de aplicación
        String sql = "SELECT mv.IdVacuna, v.Nombre, mv.FechaAplicacion " +
                     "FROM MascotaVacuna mv " +
                     "JOIN Vacuna v ON mv.IdVacuna = v.IdVacuna " +
                     "WHERE mv.IdMascota = ?";
        // Intentar conectar y preparar consulta en try-with-resources
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, petId);
            // Ejecutar la consulta en try-with-resources
            try (ResultSet rs = ps.executeQuery()) {
                // Iterar sobre las filas obtenidas (bucle loop)
                while (rs.next()) {
                    int vacId = rs.getInt("IdVacuna");
                    Date fechaDate = rs.getDate("FechaAplicacion");
                    // Operador condicional ternario para validar si la fecha es no nula, de lo contrario usar hoy
                    String dateVal = fechaDate != null ? fechaDate.toString() : java.time.LocalDate.now().toString();

                    // Generar un ID relacional ficticio combinando el ID de mascota y el de vacuna
                    int relationId = petId * 100000 + vacId;

                    Map<String, Object> vaccine = new HashMap<>();
                    vaccine.put("id", relationId);
                    // Operador condicional ternario para validar nombre no nulo
                    vaccine.put("name", rs.getString("Nombre") != null ? rs.getString("Nombre") : "");
                    vaccine.put("date", dateVal);
                    list.add(vaccine);
                }
            }
        }
        return list;
    }

    /**
     * {@inheritDoc}
     * Obtiene una vacuna registrada descodificando el ID compuesto (petId * 100000 + vacCatalogId).
     */
    @Override
    public Map<String, Object> getVaccineById(int vaccineId) throws SQLException {
        // Descomponer el ID compuesto para extraer el ID de la mascota
        int petId = vaccineId / 100000;
        // Descomponer el ID compuesto para extraer el ID de la vacuna
        int vacCatalogId = vaccineId % 100000;
        // Consulta SQL para buscar la aplicación de la vacuna
        String sql = "SELECT mv.IdVacuna, v.Nombre, mv.FechaAplicacion " +
                     "FROM MascotaVacuna mv " +
                     "JOIN Vacuna v ON mv.IdVacuna = v.IdVacuna " +
                     "WHERE mv.IdMascota = ? AND mv.IdVacuna = ?";
        // Intentar conectar y preparar sentencia en try-with-resources
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, petId);
            ps.setInt(2, vacCatalogId);
            // Ejecutar la consulta en try-with-resources
            try (ResultSet rs = ps.executeQuery()) {
                // Validar si existe el registro de vacuna (bloque condicional if)
                if (rs.next()) {
                    Date fechaDate = rs.getDate("FechaAplicacion");
                    // Operador condicional ternario para validar fecha
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

    /**
     * {@inheritDoc}
     * Registra transaccionalmente una vacuna para una mascota. Si el tipo de vacuna no existe
     * en el catálogo (tabla Vacuna), lo crea primero, y luego inserta el registro en MascotaVacuna.
     */
    @Override
    public int insertVaccine(Map<String, Object> body) throws SQLException {
        // Obtener datos del cuerpo de la petición
        String name = (String) body.get("name");
        String dateStr = (String) body.get("date");
        Object petIdObj = body.get("pet_id");
        int petId = 1;
        // Bloque condicional if-else para resolver el ID de la mascota según el tipo de objeto recibido
        if (petIdObj instanceof Number) {
            petId = ((Number) petIdObj).intValue();
        } else if (petIdObj instanceof String) {
            petId = Integer.parseInt((String) petIdObj);
        }

        // Intentar conectar en try-with-resources
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Desactivar auto-commit para control transaccional
            conn.setAutoCommit(false);
            // Ejecutar consultas en bloque try
            try {
                int vaccineCatalogId = 0;
                // 1. Find or create in Vacuna catalog
                String findSql = "SELECT IdVacuna FROM Vacuna WHERE Nombre = ?";
                // Buscar si la vacuna ya existe en el catálogo en try-with-resources
                try (PreparedStatement findPs = conn.prepareStatement(findSql)) {
                    findPs.setString(1, name);
                    try (ResultSet rs = findPs.executeQuery()) {
                        // Si existe, obtener su ID (bloque condicional if)
                        if (rs.next()) {
                            vaccineCatalogId = rs.getInt("IdVacuna");
                        }
                    }
                }
                // Si la vacuna no existe en el catálogo, insertarla (bloque condicional if)
                if (vaccineCatalogId == 0) {
                    String insertCatalogSql = "INSERT INTO Vacuna (Nombre) VALUES (?)";
                    try (PreparedStatement insertCatalogPs = conn.prepareStatement(insertCatalogSql, Statement.RETURN_GENERATED_KEYS)) {
                        insertCatalogPs.setString(1, name);
                        insertCatalogPs.executeUpdate();
                        try (ResultSet rs = insertCatalogPs.getGeneratedKeys()) {
                            // Obtener llave autogenerada (bloque condicional if)
                            if (rs.next()) {
                                vaccineCatalogId = rs.getInt(1);
                            }
                        }
                    }
                }
                // Si tras la creación o búsqueda no se tiene ID válido, revertir y retornar 0 (bloque condicional if)
                if (vaccineCatalogId == 0) {
                    conn.rollback();
                    return 0;
                }

                // 2. Insert into MascotaVacuna
                String insertLinkSql = "INSERT INTO MascotaVacuna (IdMascota, IdVacuna, FechaAplicacion) VALUES (?, ?, ?)";
                // Insertar enlace en try-with-resources
                try (PreparedStatement ps = conn.prepareStatement(insertLinkSql)) {
                    ps.setInt(1, petId);
                    ps.setInt(2, vaccineCatalogId);
                    // Operador condicional ternario para resolver la fecha a registrar
                    ps.setDate(3, dateStr != null && !dateStr.isEmpty() ? Date.valueOf(dateStr) : Date.valueOf(java.time.LocalDate.now()));
                    int rows = ps.executeUpdate();
                    // Si se insertó correctamente el enlace, confirmar y retornar el ID compuesto (bloque condicional if)
                    if (rows > 0) {
                        conn.commit();
                        return petId * 100000 + vaccineCatalogId;
                    }
                }
                // Revertir si falla
                conn.rollback();
                return 0;
            } catch (SQLException e) {
                // Revertir transacción en caso de error (bloque catch)
                conn.rollback();
                // Propagar la excepción
                throw e;
            } finally {
                // Asegurar restablecimiento de auto-commit (bloque finally)
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * {@inheritDoc}
     * Actualiza transaccionalmente la vacuna de una mascota identificada por su ID compuesto,
     * permitiendo cambiar la fecha o el tipo de vacuna (creándolo en el catálogo si es necesario).
     */
    @Override
    public boolean updateVaccine(int vaccineId, Map<String, Object> body) throws SQLException {
        // Descomponer ID compuesto
        int oldPetId = vaccineId / 100000;
        int oldVacCatalogId = vaccineId % 100000;
        String name = (String) body.get("name");
        String dateStr = (String) body.get("date");

        // Intentar conectar en try-with-resources
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Desactivar auto-commit para iniciar transacción
            conn.setAutoCommit(false);
            // Realizar operaciones en bloque try
            try {
                int newVaccineCatalogId = 0;
                // 1. Find or create in Vacuna catalog
                String findSql = "SELECT IdVacuna FROM Vacuna WHERE Nombre = ?";
                // Buscar si la vacuna ya existe en el catálogo en try-with-resources
                try (PreparedStatement findPs = conn.prepareStatement(findSql)) {
                    findPs.setString(1, name);
                    try (ResultSet rs = findPs.executeQuery()) {
                        // Si existe, obtener el ID (bloque condicional if)
                        if (rs.next()) {
                            newVaccineCatalogId = rs.getInt("IdVacuna");
                        }
                    }
                }
                // Si la vacuna no existe en el catálogo, insertarla (bloque condicional if)
                if (newVaccineCatalogId == 0) {
                    String insertCatalogSql = "INSERT INTO Vacuna (Nombre) VALUES (?)";
                    try (PreparedStatement insertCatalogPs = conn.prepareStatement(insertCatalogSql, Statement.RETURN_GENERATED_KEYS)) {
                        insertCatalogPs.setString(1, name);
                        insertCatalogPs.executeUpdate();
                        try (ResultSet rs = insertCatalogPs.getGeneratedKeys()) {
                            // Obtener ID generado (bloque condicional if)
                            if (rs.next()) {
                                newVaccineCatalogId = rs.getInt(1);
                            }
                        }
                    }
                }
                // Si falla la obtención de ID válido, revertir y retornar falso (bloque condicional if)
                if (newVaccineCatalogId == 0) {
                    conn.rollback();
                    return false;
                }

                // 2. Update MascotaVacuna
                String sql = "UPDATE MascotaVacuna SET IdVacuna = ?, FechaAplicacion = ? WHERE IdMascota = ? AND IdVacuna = ?";
                // Actualizar la tabla intermedia en try-with-resources
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, newVaccineCatalogId);
                    // Operador condicional ternario para la fecha
                    ps.setDate(2, dateStr != null && !dateStr.isEmpty() ? Date.valueOf(dateStr) : Date.valueOf(java.time.LocalDate.now()));
                    ps.setInt(3, oldPetId);
                    ps.setInt(4, oldVacCatalogId);
                    int rows = ps.executeUpdate();
                    // Confirmar transacción
                    conn.commit();
                    // Retornar verdadero si se actualizó el registro (condicional)
                    return rows > 0;
                }
            } catch (SQLException e) {
                // Revertir cambios en caso de error (bloque catch)
                conn.rollback();
                // Propagar excepción
                throw e;
            } finally {
                // Asegurar restablecimiento de auto-commit (bloque finally)
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * {@inheritDoc}
     * Elimina el registro de aplicación de vacuna de la tabla MascotaVacuna decodificando su ID compuesto.
     */
    @Override
    public boolean deleteVaccine(int vaccineId) throws SQLException {
        // Descomponer el ID compuesto
        int petId = vaccineId / 100000;
        int vacCatalogId = vaccineId % 100000;
        // Sentencia SQL para eliminar la fila de aplicación de vacuna
        String sql = "DELETE FROM MascotaVacuna WHERE IdMascota = ? AND IdVacuna = ?";
        // Intentar conectar y preparar la consulta en try-with-resources
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, petId);
            ps.setInt(2, vacCatalogId);
            // Retornar verdadero si se borró la fila (condicional)
            return ps.executeUpdate() > 0;
        }
    }
}

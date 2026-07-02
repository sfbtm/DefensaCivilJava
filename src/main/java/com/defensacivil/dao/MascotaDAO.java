package com.defensacivil.dao;

import com.defensacivil.dto.PetDTO;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Interfaz que define las operaciones de acceso a datos (DAO) para la gestión
 * de mascotas asociadas a los planes familiares y el control de sus vacunas.
 */
public interface MascotaDAO {

    /**
     * Obtiene la lista de mascotas registradas en un plan familiar específico.
     *
     * @param planId Identificador único del plan familiar.
     * @return Una lista de mapas con los detalles de cada mascota (id, nombre, tipo, etc.).
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    List<Map<String, Object>> getPetsByFamilyPlan(int planId) throws SQLException;

    /**
     * Obtiene la información de una mascota específica a partir de su identificador único.
     *
     * @param petId Identificador único de la mascota.
     * @return Un objeto {@link PetDTO} con los datos de la mascota, o null si no se encuentra.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    PetDTO getPetById(int petId) throws SQLException;

    /**
     * Obtiene todas las mascotas registradas en el sistema.
     *
     * @return Una lista de mapas con la información básica de todas las mascotas.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    List<Map<String, Object>> getAllPets() throws SQLException;

    /**
     * Registra una nueva mascota en la base de datos.
     *
     * @param dto Objeto {@link PetDTO} que contiene los datos de la mascota a registrar.
     * @return El identificador autogenerado de la nueva mascota, o -1 en caso de error.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    int insertPet(PetDTO dto) throws SQLException;

    /**
     * Actualiza la información de una mascota existente en la base de datos.
     *
     * @param petId Identificador único de la mascota a actualizar.
     * @param dto Objeto {@link PetDTO} con los nuevos datos de la mascota.
     * @return true si la actualización fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean updatePet(int petId, PetDTO dto) throws SQLException;

    /**
     * Elimina una mascota de la base de datos por su identificador.
     *
     * @param petId Identificador único de la mascota a eliminar.
     * @return true si la eliminación fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean deletePet(int petId) throws SQLException;

    /**
     * Obtiene las vacunas aplicadas a una mascota específica.
     *
     * @param petId Identificador único de la mascota.
     * @return Una lista de mapas con los detalles de las vacunas (id, tipo, fecha, etc.).
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    List<Map<String, Object>> getVaccinesByPet(int petId) throws SQLException;

    /**
     * Obtiene la información detallada de una vacuna por su identificador único.
     *
     * @param vaccineId Identificador único de la vacuna (relación mascota-vacuna).
     * @return Un mapa con los detalles de la vacuna registrada, o null si no se encuentra.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    Map<String, Object> getVaccineById(int vaccineId) throws SQLException;

    /**
     * Registra una nueva vacuna aplicada a una mascota.
     *
     * @param body Mapa que contiene los datos necesarios para registrar la vacuna (ej. pet_id, vaccine_type_id, date).
     * @return El identificador único de la vacuna registrada, o -1 en caso de error.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    int insertVaccine(Map<String, Object> body) throws SQLException;

    /**
     * Actualiza la información de una vacuna existente.
     *
     * @param vaccineId Identificador único de la vacuna a actualizar.
     * @param body Mapa con los nuevos datos de la vacuna.
     * @return true si la actualización fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean updateVaccine(int vaccineId, Map<String, Object> body) throws SQLException;

    /**
     * Elimina el registro de una vacuna aplicada de la base de datos.
     *
     * @param vaccineId Identificador único de la vacuna a eliminar.
     * @return true si la eliminación fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean deleteVaccine(int vaccineId) throws SQLException;
}

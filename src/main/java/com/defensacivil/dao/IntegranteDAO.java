package com.defensacivil.dao;

import com.defensacivil.dto.MemberDTO;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Interfaz que define las operaciones de acceso a datos (DAO) para la gestión
 * de integrantes de los planes familiares y sus condiciones médicas o enfermedades.
 */
public interface IntegranteDAO {

    /**
     * Obtiene una lista simplificada de los integrantes asociados a un plan familiar
     * para ser utilizada en componentes de selección (select/dropdown).
     *
     * @param familyPlanId Identificador único del plan familiar.
     * @return Una lista de mapas que contienen el id ("id") y el nombre completo ("full_name") del integrante.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    List<Map<String, Object>> getMembersForSelect(int familyPlanId) throws SQLException;

    /**
     * Obtiene la información detallada de todos los integrantes pertenecientes a un plan familiar específico.
     *
     * @param familyPlanId Identificador único del plan familiar.
     * @return Una lista de mapas con los detalles de cada integrante (nombre, parentesco, género, documento, etc.).
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    List<Map<String, Object>> getMembersByFamilyPlan(int familyPlanId) throws SQLException;

    /**
     * Busca y obtiene la información de un integrante por su identificador único.
     *
     * @param memberId Identificador único del integrante.
     * @return Un objeto {@link MemberDTO} con los datos del integrante, o null si no se encuentra.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    MemberDTO getMemberById(int memberId) throws SQLException;

    /**
     * Obtiene una lista de todos los integrantes registrados con sus respectivos planes familiares.
     *
     * @return Una lista de mapas con las relaciones de "member_id" y "family_plan_id".
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    List<Map<String, Object>> getAllFamilyMembers() throws SQLException;

    /**
     * Registra un nuevo integrante en un plan familiar.
     *
     * @param familyPlanId Identificador único del plan familiar al que se asociará el integrante.
     * @param dto Objeto {@link MemberDTO} que contiene los datos del integrante a registrar.
     * @return El identificador autogenerado del nuevo integrante, o -1 en caso de error.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    int addMember(int familyPlanId, MemberDTO dto) throws SQLException;

    /**
     * Actualiza la información de un integrante existente.
     *
     * @param memberId Identificador único del integrante a actualizar.
     * @param dto Objeto {@link MemberDTO} con los nuevos datos del integrante.
     * @return true si la actualización fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean updateMember(int memberId, MemberDTO dto) throws SQLException;

    /**
     * Elimina un integrante de la base de datos por su identificador.
     *
     * @param memberId Identificador único del integrante a eliminar.
     * @return true si la eliminación fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean deleteMember(int memberId) throws SQLException;

    /**
     * Obtiene las condiciones médicas o enfermedades asociadas a un integrante específico.
     *
     * @param memberId Identificador único del integrante.
     * @return Una lista de mapas con los detalles de las enfermedades (id, nombre, medicina, dosis, etc.).
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    List<Map<String, Object>> getConditionsByMember(int memberId) throws SQLException;

    /**
     * Obtiene los detalles de una condición médica o enfermedad por su identificador único.
     *
     * @param conditionId Identificador único de la condición (relación integrante-enfermedad).
     * @return Un mapa con los detalles de la condición médica, o null si no se encuentra.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    Map<String, Object> getConditionById(int conditionId) throws SQLException;

    /**
     * Registra una nueva condición médica o enfermedad para un integrante.
     *
     * @param memberId Identificador único del integrante.
     * @param name Nombre de la enfermedad o condición médica.
     * @param dose Dosis del medicamento recetado.
     * @return true si el registro fue exitoso, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean addCondition(int memberId, String name, String dose) throws SQLException;

    /**
     * Actualiza una condición médica o enfermedad existente.
     *
     * @param conditionId Identificador único de la condición a actualizar.
     * @param name Nuevo nombre de la enfermedad o condición médica.
     * @param dose Nueva dosis del medicamento.
     * @return true si la actualización fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean updateCondition(int conditionId, String name, String dose) throws SQLException;

    /**
     * Elimina una condición médica o enfermedad asociada a un integrante.
     *
     * @param conditionId Identificador único de la condición a eliminar.
     * @return true si la eliminación fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean deleteCondition(int conditionId) throws SQLException;

}

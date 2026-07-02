package com.defensacivil.dao;

import com.defensacivil.dto.HousingInfoDTO;
import com.defensacivil.dto.ActionPlanDTO;
import com.defensacivil.dto.ActionDTO;
import com.defensacivil.dto.VaccineDTO;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Interfaz que define las operaciones de acceso a datos (DAO) para la gestión
 * del Plan Complementario, el cual incluye información de la vivienda/gráficos,
 * planes de acción y sus tareas asignadas a integrantes, control de vacunas de mascotas,
 * y recursos disponibles (recursos comunitarios cercanos).
 */
public interface PlanComplementarioDAO {

    // === Vivienda e Información de Gráficos (Housing Info & Graphics) ===

    /**
     * Obtiene la información o gráfico de la vivienda asociado a un plan y a un tipo de gráfico específico.
     *
     * @param planId Identificador único del plan familiar.
     * @param typeId Identificador del tipo de gráfico/información de vivienda.
     * @return Objeto {@link HousingInfoDTO} con la información del gráfico, o null si no se encuentra.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    HousingInfoDTO getHousingInfo(int planId, int typeId) throws SQLException;

    /**
     * Registra o actualiza un gráfico de vivienda (croquis, entorno, etc.) para un plan familiar.
     *
     * @param planId Identificador único del plan familiar.
     * @param savedFileName Nombre del archivo físico guardado en el servidor.
     * @param description Descripción textual del gráfico.
     * @param esEntornoVal Valor indicador de si corresponde al entorno (1) o al croquis interno (0).
     * @return true si la operación de guardado/actualización fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean saveOrUpdateHousingGraphic(int planId, String savedFileName, String description, int esEntornoVal) throws SQLException;

    /**
     * Obtiene todos los gráficos de vivienda (croquis/entorno) registrados para un plan familiar específico.
     *
     * @param planId Identificador único del plan familiar.
     * @return Lista de objetos {@link HousingInfoDTO} con los gráficos del plan.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    List<HousingInfoDTO> getHousingGraphicsByPlan(int planId) throws SQLException;

    /**
     * Busca y obtiene la información de un gráfico de vivienda por su identificador único.
     *
     * @param id Identificador único del gráfico de vivienda.
     * @return Objeto {@link HousingInfoDTO} con la información del gráfico, o null si no se encuentra.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    HousingInfoDTO getHousingGraphicById(int id) throws SQLException;

    /**
     * Actualiza la descripción de un gráfico de vivienda existente.
     *
     * @param id Identificador único del gráfico de vivienda.
     * @param description Nueva descripción para el gráfico.
     * @return true si la actualización fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean updateHousingGraphicDescription(int id, String description) throws SQLException;

    /**
     * Elimina físicamente un gráfico de vivienda (croquis/entorno) de la base de datos.
     *
     * @param id Identificador único del gráfico a eliminar.
     * @return true si la eliminación fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean deleteHousingGraphic(int id) throws SQLException;


    // === Planes de Acción y Acciones (Action Plans & Actions) ===

    /**
     * Verifica si un plan familiar ya cuenta con un plan de acción registrado.
     *
     * @param planId Identificador único del plan familiar.
     * @return true si ya existe un plan de acción, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean hasActionPlan(int planId) throws SQLException;

    /**
     * Obtiene el plan de acción asociado a un plan familiar específico.
     *
     * @param planId Identificador único del plan familiar.
     * @return Objeto {@link ActionPlanDTO} con la información del plan de acción, o null si no existe.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    ActionPlanDTO getActionPlanByPlan(int planId) throws SQLException;

    /**
     * Registra un nuevo plan de acción para un plan familiar asignando un coordinador.
     *
     * @param planId Identificador único del plan familiar.
     * @param coordinatorId Identificador único del integrante que actuará como coordinador.
     * @return true si el registro fue exitoso, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean createActionPlan(int planId, int coordinatorId) throws SQLException;

    /**
     * Obtiene el listado de acciones/tareas detalladas registradas en un plan de acción específico.
     *
     * @param actionPlanId Identificador único del plan de acción.
     * @return Lista de objetos {@link ActionDTO} con las tareas asignadas.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    List<ActionDTO> getActionsByActionPlan(int actionPlanId) throws SQLException;

    /**
     * Obtiene una acción/tarea específica por su identificador único.
     *
     * @param id Identificador único de la acción.
     * @return Objeto {@link ActionDTO} con el detalle de la acción, o null si no se encuentra.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    ActionDTO getActionById(int id) throws SQLException;

    /**
     * Inserta una nueva acción/tarea en un plan de acción para un integrante en una etapa determinada.
     *
     * @param actionPlanId Identificador del plan de acción.
     * @param memberId Identificador único del integrante responsable.
     * @param stage Etapa del plan de acción (ej. "Antes", "Durante", "Después").
     * @param description Descripción textual de la tarea o acción a ejecutar.
     * @return true si la inserción fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean insertAction(int actionPlanId, int memberId, String stage, String description) throws SQLException;

    /**
     * Actualiza el integrante asignado y la descripción de una acción/tarea específica.
     *
     * @param id Identificador único de la acción.
     * @param memberId Identificador único del nuevo integrante responsable.
     * @param description Nueva descripción de la acción.
     * @return true si la actualización fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean updateAction(int id, int memberId, String description) throws SQLException;

    /**
     * Elimina físicamente una acción/tarea del plan de acción de la base de datos.
     *
     * @param id Identificador único de la acción a eliminar.
     * @return true si la eliminación fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean deleteAction(int id) throws SQLException;


    // === Vacunas de Mascotas (Pet Vaccines) ===

    /**
     * Obtiene el historial de vacunas aplicadas a una mascota específica.
     *
     * @param petId Identificador único de la mascota.
     * @return Lista de objetos {@link VaccineDTO} que representan las vacunas aplicadas.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    List<VaccineDTO> getVaccinesByPet(int petId) throws SQLException;

    /**
     * Obtiene la información detallada de una aplicación de vacuna específica.
     *
     * @param vaccineId Identificador único de la vacuna (relación mascota-vacuna).
     * @return Objeto {@link VaccineDTO} con la información registrada, o null si no se encuentra.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    VaccineDTO getVaccineById(int vaccineId) throws SQLException;

    /**
     * Registra una nueva aplicación de vacuna a una mascota.
     *
     * @param dto Objeto {@link VaccineDTO} con los datos de la vacuna y la fecha de aplicación.
     * @return El identificador de la vacuna registrada, o -1 en caso de error.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    int insertVaccine(VaccineDTO dto) throws SQLException;

    /**
     * Actualiza la información de aplicación de una vacuna existente.
     *
     * @param vaccineId Identificador único del registro de vacuna.
     * @param dto Objeto {@link VaccineDTO} con los nuevos datos.
     * @return true si la actualización fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean updateVaccine(int vaccineId, VaccineDTO dto) throws SQLException;

    /**
     * Elimina físicamente la aplicación de una vacuna de la base de datos.
     *
     * @param vaccineId Identificador único del registro de vacuna a eliminar.
     * @return true si la eliminación fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean deleteVaccine(int vaccineId) throws SQLException;


    // === Recursos Disponibles (Available Resources) ===

    /**
     * Obtiene todos los recursos comunitarios externos registrados en el plan familiar.
     *
     * @param planId Identificador único del plan familiar.
     * @return Lista de mapas con los detalles de cada recurso comunitario cercano.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    List<Map<String, Object>> getAvailableResourcesByPlan(int planId) throws SQLException;

    /**
     * Busca y obtiene la información de un recurso comunitario por su identificador único.
     *
     * @param idVal Identificador del recurso comunitario registrado en el plan familiar.
     * @return Mapa con la información del recurso, o null si no existe.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    Map<String, Object> getAvailableResourceById(int idVal) throws SQLException;

    /**
     * Registra un recurso comunitario disponible en el plan familiar.
     *
     * @param planId Identificador único del plan familiar.
     * @param resourceId Identificador único del tipo de recurso (ej. Hospital, Estación de Bomberos).
     * @param description Descripción textual o nombre del recurso.
     * @param location Ubicación o dirección del recurso.
     * @param distance Distancia aproximada en kilómetros o metros desde la vivienda.
     * @param phone Teléfono de contacto del recurso.
     * @return true si la inserción fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean insertAvailableResource(int planId, int resourceId, String description, String location, float distance, String phone) throws SQLException;

    /**
     * Actualiza la información de un recurso comunitario registrado en el plan familiar.
     *
     * @param idVal Identificador de la relación de recurso registrado.
     * @param resourceId Nuevo identificador único del tipo de recurso.
     * @param description Nueva descripción o nombre del recurso.
     * @param location Nueva ubicación o dirección.
     * @param distance Nueva distancia aproximada.
     * @param phone Nuevo teléfono de contacto.
     * @return true si la actualización fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean updateAvailableResource(int idVal, int resourceId, String description, String location, float distance, String phone) throws SQLException;

    /**
     * Elimina el registro de un recurso comunitario disponible asociado al plan familiar.
     *
     * @param idVal Identificador único del registro de recurso a eliminar.
     * @return true si la eliminación fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean deleteAvailableResource(int idVal) throws SQLException;
}

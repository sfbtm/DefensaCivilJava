package com.defensacivil.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Interfaz que define las operaciones de acceso a datos (DAO) para la gestión
 * de planes familiares de emergencia, incluyendo tableros de control (dashboards),
 * validación de requisitos mínimos, identificación de viviendas y cambios de estado del plan.
 */
public interface PlanFamiliarDAO {

    /**
     * Obtiene los planes familiares registrados filtrándolos según el rol, identificador de usuario y seccional correspondientes.
     *
     * @param roleId Identificador del rol de usuario que solicita la información (ej. Administrador, Supervisor, Líder).
     * @param userId Identificador único del usuario.
     * @param sectionalId Identificador de la seccional a la que pertenece.
     * @return Una lista de mapas con los detalles de cada plan familiar.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    List<Map<String, Object>> getFamilyPlans(int roleId, int userId, int sectionalId) throws SQLException;

    /**
     * Verifica si un plan familiar posee integrantes registrados.
     *
     * @param planId Identificador único del plan familiar.
     * @return true si el plan familiar tiene al menos un integrante, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean hasMembers(int planId) throws SQLException;

    /**
     * Valida que el plan familiar cumpla con los requisitos mínimos establecidos (ej. croquis, integrantes, recursos).
     *
     * @param planId Identificador único del plan familiar.
     * @return Un mapa que contiene indicadores de cumplimiento para cada tipo de requisito evaluado.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    Map<String, Object> validateRequirements(int planId) throws SQLException;

    /**
     * Busca y obtiene la información detallada de un plan familiar específico.
     *
     * @param planId Identificador único del plan familiar.
     * @return Un mapa con los detalles del plan familiar, o null si no se encuentra.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    Map<String, Object> getPlanById(int planId) throws SQLException;

    /**
     * Obtiene las métricas generales de planes familiares diseñadas para el tablero de control del Supervisor.
     *
     * @return Un mapa con contadores agrupados por estado (ej. pendientes, aprobados, rechazados).
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    Map<String, Object> getSupervisorDashboard() throws SQLException;

    /**
     * Obtiene las métricas globales del sistema diseñadas para el tablero de control del Administrador.
     *
     * @return Un mapa con las estadísticas generales y métricas del sistema.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    Map<String, Object> getAdminDashboard() throws SQLException;

    /**
     * Registra un nuevo plan familiar en el sistema asociado a un usuario líder.
     *
     * @param lastNames Apellidos de la familia.
     * @param userId Identificador único del usuario líder creador del plan.
     * @param body Mapa con información opcional de la dirección de vivienda u otros parámetros del plan.
     * @return El identificador único del nuevo plan familiar generado, o -1 en caso de error.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    int createFamilyPlan(String lastNames, int userId, Map<String, Object> body) throws SQLException;

    /**
     * Actualiza la información geográfica y de localización (dirección, coordenadas, barrio, etc.) de un plan familiar.
     *
     * @param planId Identificador único del plan familiar.
     * @param body Mapa con los campos a actualizar (latitud, longitud, dirección, teléfono, etc.).
     * @return true si la actualización fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean updateIdentification(int planId, Map<String, Object> body) throws SQLException;

    /**
     * Modifica el estado de aprobación de un plan familiar y registra las observaciones o comentarios pertinentes.
     *
     * @param planId Identificador único del plan familiar.
     * @param statusId Identificador del nuevo estado a asignar.
     * @param commentary Texto explicativo de observaciones del cambio de estado.
     * @return true si el cambio de estado fue exitoso, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean changeStatus(int planId, int statusId, String commentary) throws SQLException;

    /**
     * Modifica el tipo de vivienda/familia asociado al plan familiar.
     *
     * @param planId Identificador único del plan familiar.
     * @param familyTypeId Identificador único del tipo de familia/vivienda a asignar.
     * @return true si la actualización fue exitosa, false en caso contrario.
     * @throws SQLException Si ocurre un error al acceder a la base de datos.
     */
    boolean changeFamilyType(int planId, int familyTypeId) throws SQLException;
}

package com.defensacivil.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Interfaz que define las operaciones de acceso a datos (DAO) para la gestión
 * de planes familiares de emergencia, incluyendo tableros de control (dashboards),
 * validación de requisitos mínimos, identificación de viviendas y cambios de estado del plan.
 * Proporciona el contrato para interactuar con el origen de datos (base de datos y/o memoria).
 */
public interface PlanFamiliarDAO {

    /**
     * Obtiene los planes familiares registrados filtrándolos según el rol, identificador de usuario y seccional correspondientes.
     *
     * @param roleId Identificador del rol de usuario que solicita la información (ej. Administrador = 1, Supervisor = 2, Voluntario = 3).
     * @param userId Identificador único del usuario para filtrar en caso de voluntarios.
     * @param sectionalId Identificador de la seccional a la que pertenece para filtrar en caso de supervisores.
     * @return Una lista de mapas con los detalles de cada plan familiar, conteniendo claves como "id", "last_names", "status", etc.
     * @throws SQLException Si ocurre un error de sintaxis SQL, de conexión o de acceso a la base de datos.
     */
    List<Map<String, Object>> getFamilyPlans(int roleId, int userId, int sectionalId) throws SQLException;

    /**
     * Verifica si un plan familiar posee integrantes registrados en su núcleo familiar.
     *
     * @param planId Identificador único del plan familiar a consultar.
     * @return true si el plan familiar tiene al menos un integrante registrado; false si no tiene miembros o no existe.
     * @throws SQLException Si ocurre un error de comunicación, de lectura o de conexión en la base de datos.
     */
    boolean hasMembers(int planId) throws SQLException;

    /**
     * Valida que el plan familiar cumpla con los requisitos mínimos establecidos (ej. número mínimo de integrantes, riesgos y recursos).
     *
     * @param planId Identificador único del plan familiar que se desea validar.
     * @return Un mapa que contiene los resultados de la validación (claves como "is_valid", "has_min_members", "has_risk_factors", etc.).
     * @throws SQLException Si ocurre un error al ejecutar las consultas de conteo en la base de datos.
     */
    Map<String, Object> validateRequirements(int planId) throws SQLException;

    /**
     * Busca y obtiene la información detallada de un plan familiar específico por su identificador.
     *
     * @param planId Identificador único del plan familiar.
     * @return Un mapa con los detalles completos del plan familiar y la familia asociada, o un mapa vacío/null si no se encuentra.
     * @throws SQLException Si ocurre un error de base de datos durante la consulta.
     */
    Map<String, Object> getPlanById(int planId) throws SQLException;

    /**
     * Obtiene las métricas generales de planes familiares diseñadas para el tablero de control (dashboard) del Supervisor.
     *
     * @return Un mapa con contadores agrupados por estado (pendientes, en revisión, aprobados, rechazados) y la lista de planes recientes.
     * @throws SQLException Si ocurre un error de base de datos al realizar los conteos y consultas.
     */
    Map<String, Object> getSupervisorDashboard() throws SQLException;

    /**
     * Obtiene las métricas globales del sistema diseñadas para el tablero de control (dashboard) del Administrador.
     *
     * @return Un mapa con las estadísticas generales de usuarios (activos, inactivos, solicitudes), roles (voluntarios, supervisores) e históricos de cambios.
     * @throws SQLException Si ocurre un error en la base de datos al recuperar las métricas de usuarios y auditorías.
     */
    Map<String, Object> getAdminDashboard() throws SQLException;

    /**
     * Registra un nuevo plan familiar en el sistema asociado a un usuario líder (voluntario).
     *
     * @param lastNames Apellidos de la familia para nombrar el registro (se le antepone "Familia ").
     * @param userId Identificador único del usuario líder creador/responsable del plan.
     * @param body Mapa con información opcional de la dirección de vivienda u otros parámetros del plan (sector_id, housing_quality_id, etc.).
     * @return El identificador único del nuevo plan familiar generado (clave primaria autogenerada), o -1 en caso de error.
     * @throws SQLException Si ocurre un error al realizar las inserciones de la Familia o del PlanFamiliar en la base de datos.
     */
    int createFamilyPlan(String lastNames, int userId, Map<String, Object> body) throws SQLException;

    /**
     * Actualiza la información geográfica y de localización (dirección, teléfono, sector, calidad, etc.) de la familia asociada a un plan.
     *
     * @param planId Identificador único del plan familiar.
     * @param body Mapa con los campos a actualizar (last_names, landline_phone, sector_id, housing_quality_id, address, department_id, city_id).
     * @return true si la actualización del registro de la familia fue exitosa; false en caso contrario.
     * @throws SQLException Si ocurre un error al ejecutar la sentencia SQL de actualización.
     */
    boolean updateIdentification(int planId, Map<String, Object> body) throws SQLException;

    /**
     * Modifica el estado de aprobación de un plan familiar y registra las observaciones o comentarios de validación.
     *
     * @param planId Identificador único del plan familiar.
     * @param statusId Identificador del nuevo estado numérico a asignar.
     * @param commentary Texto explicativo de observaciones del cambio de estado (insertado en la tabla de validación).
     * @return true si el cambio de estado fue exitoso; false en caso contrario.
     * @throws SQLException Si ocurre un error al actualizar el plan o al insertar la validación.
     */
    boolean changeStatus(int planId, int statusId, String commentary) throws SQLException;

    /**
     * Modifica el tipo de vivienda/familia asociado al plan familiar (ej. Vulnerable, No Vulnerable).
     *
     * @param planId Identificador único del plan familiar.
     * @param familyTypeId Identificador único del tipo de familia/vivienda a asignar (1 para Vulnerable, 2 para No Vulnerable, 3 para Por Definir).
     * @return true si la actualización en la memoria o base de datos fue exitosa; false en caso contrario.
     * @throws SQLException Si ocurre un error durante el cambio de tipo de familia.
     */
    boolean changeFamilyType(int planId, int familyTypeId) throws SQLException;
}

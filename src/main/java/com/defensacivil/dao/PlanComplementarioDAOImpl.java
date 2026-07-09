package com.defensacivil.dao;

import com.defensacivil.config.DatabaseConfig;
import com.defensacivil.dto.HousingInfoDTO;
import com.defensacivil.dto.ActionPlanDTO;
import com.defensacivil.dto.ActionDTO;
import com.defensacivil.dto.VaccineDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación de la interfaz {@link PlanComplementarioDAO} que gestiona la persistencia
 * de gráficos de vivienda, planes de acción, vacunas y recursos disponibles mediante consultas JDBC directas
 * y el almacenamiento temporal en memoria para campos no estructurados en la base de datos.
 */
public class PlanComplementarioDAOImpl implements PlanComplementarioDAO {

    /**
     * Mapa de almacenamiento en memoria para persistir temporalmente información adicional
     * que no se mapea directamente en las tablas relacionales físicas.
     */
    private final Map<String, Map<String, Object>> extraData;

    /**
     * Constructor que inicializa el mapa de almacenamiento de datos complementarios en memoria.
     *
     * @param extraData Mapa de almacenamiento para información adicional.
     */
    public PlanComplementarioDAOImpl(Map<String, Map<String, Object>> extraData) {
        // Asignar la referencia del mapa recibido al atributo local
        this.extraData = extraData;
    }

    // --- HOUSING INFO & GRAPHICS ---

    /**
     * {@inheritDoc}
     * Recupera el croquis (EsEntorno=0) o plano del entorno (EsEntorno=1) según el typeId provisto.
     *
     * @param planId Identificador único del plan familiar.
     * @param typeId Identificador del tipo de gráfico (1 para croquis, 2 para plano de entorno).
     * @return El DTO {@link HousingInfoDTO} con la información del gráfico, o null si no se encuentra.
     * @throws SQLException Si ocurre un error al ejecutar la consulta SQL o de conexión.
     */
    @Override
    public HousingInfoDTO getHousingInfo(int planId, int typeId) throws SQLException {
        // Determinar si corresponde al entorno (1) o al croquis de la vivienda (0)
        int esEntornoVal = (typeId == 2) ? 1 : 0;
        
        // Sentencia SQL parametrizada para buscar el gráfico del plan familiar
        String sql = "SELECT IdGrafico, RutaImagen, Descripcion FROM GraficoVivienda WHERE IdPlanFamiliar = ? AND EsEntorno = ? LIMIT 1";
        
        // Abrir la conexión y preparar el PreparedStatement de forma segura
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Asignar los parámetros a la consulta SQL
            ps.setInt(1, planId);
            ps.setInt(2, esEntornoVal);
            
            // Ejecutar la consulta y gestionar el ciclo de vida del ResultSet
            try (ResultSet rs = ps.executeQuery()) {
                // Si existe un registro coincidente, mapear los datos al DTO
                if (rs.next()) {
                    // Instanciar el DTO y establecer sus valores correspondientes
                    HousingInfoDTO dto = new HousingInfoDTO();
                    dto.setId(rs.getInt("IdGrafico"));
                    dto.setPath(rs.getString("RutaImagen"));
                    dto.setDescription(rs.getString("Descripcion"));
                    dto.setFamily_plan_id(planId);
                    
                    // Retornar el DTO completamente poblado
                    return dto;
                }
            }
        }
        // Retornar null si no se encontró ningún gráfico para los parámetros provistos
        return null;
    }

    /**
     * {@inheritDoc}
     * Guarda un nuevo gráfico o actualiza el existente si corresponde al plano del entorno.
     *
     * @param planId Identificador único del plan familiar.
     * @param savedFileName Nombre o ruta física del archivo de imagen guardado.
     * @param description Descripción asociada al gráfico de la vivienda.
     * @param esEntornoVal Indicador de si el gráfico corresponde al plano del entorno (1) o al croquis (0).
     * @return true si la inserción o actualización fue exitosa; false en caso contrario.
     * @throws SQLException Si ocurre un error de base de datos durante las transacciones o consultas.
     */
    @Override
    public boolean saveOrUpdateHousingGraphic(int planId, String savedFileName, String description, int esEntornoVal) throws SQLException {
        // Establecer conexión con la base de datos de manera segura
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Bandera para determinar si el registro de gráfico del entorno ya existe
            boolean exists = false;
            
            // Si el gráfico corresponde al plano del entorno (esEntornoVal == 1), comprobar si ya existe un registro previo
            if (esEntornoVal == 1) {
                // Sentencia SQL para validar la existencia del gráfico
                String checkSql = "SELECT IdGrafico FROM GraficoVivienda WHERE IdPlanFamiliar = ? AND EsEntorno = ?";
                
                // Preparar y ejecutar la consulta de verificación de existencia
                try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                    checkPs.setInt(1, planId);
                    checkPs.setInt(2, esEntornoVal);
                    
                    // Ejecutar la consulta y analizar los resultados del ResultSet
                    try (ResultSet rs = checkPs.executeQuery()) {
                        // Si existe al menos un registro en la base de datos
                        if (rs.next()) {
                            // Cambiar bandera indicando que el registro ya existe
                            exists = true;
                        }
                    }
                }
            }

            // Si el registro ya existe, proceder con una sentencia SQL de tipo UPDATE para modificar la ruta de imagen y descripción
            if (exists) {
                // Sentencia SQL de actualización
                String updateSql = "UPDATE GraficoVivienda SET RutaImagen = ?, Descripcion = ? WHERE IdPlanFamiliar = ? AND EsEntorno = ?";
                
                // Preparar y ejecutar la actualización del gráfico existente
                try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                    updatePs.setString(1, savedFileName);
                    updatePs.setString(2, description);
                    updatePs.setInt(3, planId);
                    updatePs.setInt(4, esEntornoVal);
                    
                    // Retornar true si la actualización afectó al menos una fila
                    return updatePs.executeUpdate() > 0;
                }
            } else {
                // En caso contrario, proceder a insertar un nuevo registro de gráfico en la base de datos
                String insertSql = "INSERT INTO GraficoVivienda (IdPlanFamiliar, RutaImagen, Descripcion, EsEntorno) VALUES (?, ?, ?, ?)";
                
                // Preparar y ejecutar la inserción del nuevo gráfico
                try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                    insertPs.setInt(1, planId);
                    insertPs.setString(2, savedFileName);
                    insertPs.setString(3, description);
                    insertPs.setInt(4, esEntornoVal);
                    
                    // Retornar true si la inserción afectó al menos una fila
                    return insertPs.executeUpdate() > 0;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * Recupera todos los gráficos asociados al plan familiar que no correspondan al entorno (EsEntorno=0).
     *
     * @param planId Identificador único del plan familiar.
     * @return Una lista de DTOs {@link HousingInfoDTO} correspondientes a los croquis guardados.
     * @throws SQLException Si ocurre un error de base de datos durante la lectura.
     */
    @Override
    public List<HousingInfoDTO> getHousingGraphicsByPlan(int planId) throws SQLException {
        // Inicializar la lista que contendrá los gráficos del plan familiar
        List<HousingInfoDTO> list = new ArrayList<>();
        
        // Sentencia SQL parametrizada para buscar gráficos con EsEntorno = 0
        String sql = "SELECT IdGrafico, RutaImagen, Descripcion FROM GraficoVivienda WHERE IdPlanFamiliar = ? AND EsEntorno = 0";
        
        // Abrir conexión JDBC y preparar la consulta de forma segura
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Asignar el parámetro del identificador del plan familiar
            ps.setInt(1, planId);
            
            // Ejecutar la consulta SQL y recuperar el ResultSet
            try (ResultSet rs = ps.executeQuery()) {
                // Recorrer iterativamente todos los registros retornados
                while (rs.next()) {
                    // Crear un nuevo DTO para mapear las columnas del registro actual
                    HousingInfoDTO dto = new HousingInfoDTO();
                    dto.setId(rs.getInt("IdGrafico"));
                    dto.setPath(rs.getString("RutaImagen"));
                    dto.setDescription(rs.getString("Descripcion") != null ? rs.getString("Descripcion") : "");
                    dto.setFamily_plan_id(planId);
                    
                    // Agregar el DTO a la lista de gráficos
                    list.add(dto);
                }
            }
        }
        // Retornar la lista de gráficos del plan familiar
        return list;
    }

    /**
     * {@inheritDoc}
     * Recupera un gráfico específico a partir de su identificador único de base de datos.
     *
     * @param id Identificador único del gráfico.
     * @return El DTO {@link HousingInfoDTO} correspondiente, o null si no se encuentra.
     * @throws SQLException Si ocurre un error de acceso a la base de datos.
     */
    @Override
    public HousingInfoDTO getHousingGraphicById(int id) throws SQLException {
        // Sentencia SQL parametrizada para buscar un gráfico por su identificador primario
        String sql = "SELECT IdGrafico, RutaImagen, Descripcion, IdPlanFamiliar FROM GraficoVivienda WHERE IdGrafico = ?";
        
        // Abrir conexión y preparar consulta SQL
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Asignar el ID del gráfico a la consulta
            ps.setInt(1, id);
            
            // Ejecutar consulta y gestionar el ciclo del ResultSet
            try (ResultSet rs = ps.executeQuery()) {
                // Si el gráfico con el ID suministrado existe en la base de datos
                if (rs.next()) {
                    // Instanciar y llenar el DTO con la información de la fila
                    HousingInfoDTO dto = new HousingInfoDTO();
                    dto.setId(rs.getInt("IdGrafico"));
                    dto.setPath(rs.getString("RutaImagen"));
                    dto.setDescription(rs.getString("Descripcion") != null ? rs.getString("Descripcion") : "");
                    dto.setFamily_plan_id(rs.getInt("IdPlanFamiliar"));
                    
                    // Retornar el DTO mapeado
                    return dto;
                }
            }
        }
        // Retornar null si no se encontró ningún registro para el ID indicado
        return null;
    }

    /**
     * {@inheritDoc}
     * Actualiza la descripción asociada a una imagen o croquis de la vivienda.
     *
     * @param id Identificador único del gráfico.
     * @param description Nueva descripción a asignar al gráfico.
     * @return true si la descripción fue actualizada correctamente; false en caso contrario.
     * @throws SQLException Si ocurre un error durante el UPDATE en la base de datos.
     */
    @Override
    public boolean updateHousingGraphicDescription(int id, String description) throws SQLException {
        // Sentencia SQL para actualizar el campo descripción por ID de gráfico
        String sql = "UPDATE GraficoVivienda SET Descripcion = ? WHERE IdGrafico = ?";
        
        // Abrir conexión y preparar la actualización
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Asignar los parámetros dinámicos de forma segura
            ps.setString(1, description != null ? description : "");
            ps.setInt(2, id);
            
            // Retornar true si la actualización afectó al menos a una fila
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * {@inheritDoc}
     * Elimina físicamente el croquis de vivienda por su identificador único.
     *
     * @param id Identificador único del gráfico a eliminar.
     * @return true si el registro fue eliminado correctamente; false en caso contrario.
     * @throws SQLException Si ocurre un error al ejecutar la sentencia de eliminación.
     */
    @Override
    public boolean deleteHousingGraphic(int id) throws SQLException {
        // Sentencia SQL para eliminar físicamente el registro de gráfico
        String sql = "DELETE FROM GraficoVivienda WHERE IdGrafico = ?";
        
        // Abrir conexión y preparar la eliminación
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Asignar el parámetro del ID del gráfico a eliminar
            ps.setInt(1, id);
            
            // Retornar true si el borrado afectó al menos a una fila
            return ps.executeUpdate() > 0;
        }
    }

    // --- ACTION PLANS & ACTIONS ---

    /**
     * {@inheritDoc}
     * Verifica la existencia de un plan de acción realizando un JOIN con FactorRiesgo para filtrar por plan familiar.
     *
     * @param planId Identificador único del plan familiar.
     * @return true si el plan de acción existe para el plan familiar dado; false en caso contrario.
     * @throws SQLException Si ocurre un error de base de datos durante el conteo.
     */
    @Override
    public boolean hasActionPlan(int planId) throws SQLException {
        // Consulta SQL para realizar un conteo cruzado de planes de acción por plan familiar
        String sql = "SELECT COUNT(*) FROM PlanAccion pa JOIN FactorRiesgo fr ON pa.IdFactorRiesgo = fr.IdFactorRiesgo WHERE fr.IdPlanFamiliar = ?";
        
        // Abrir conexión y preparar la sentencia SQL
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Asignar el identificador del plan familiar a la consulta
            ps.setInt(1, planId);
            
            // Ejecutar la consulta SQL y validar la respuesta del ResultSet
            try (ResultSet rs = ps.executeQuery()) {
                // Si el ResultSet devuelve datos del conteo
                if (rs.next()) {
                    // Retornar true si la cuenta de registros es mayor a cero
                    return rs.getInt(1) > 0;
                }
            }
        }
        // Retornar false si ocurre alguna inconsistencia o no se encontraron registros
        return false;
    }

    /**
     * {@inheritDoc}
     * Obtiene el plan de acción (ID y coordinador) cruzando información con FactorRiesgo.
     *
     * @param planId Identificador único del plan familiar.
     * @return El DTO {@link ActionPlanDTO} con la información del plan de acción, o null si no se encuentra.
     * @throws SQLException Si ocurre un error de lectura en la base de datos.
     */
    @Override
    public ActionPlanDTO getActionPlanByPlan(int planId) throws SQLException {
        // Consulta SQL para obtener la información del plan de acción cruzando con FactorRiesgo
        String sql = "SELECT pa.IdPlanAccion, pa.IdCoordinador FROM PlanAccion pa JOIN FactorRiesgo fr ON pa.IdFactorRiesgo = fr.IdFactorRiesgo WHERE fr.IdPlanFamiliar = ? LIMIT 1";
        
        // Abrir la conexión y preparar el PreparedStatement de forma segura
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Asignar el ID del plan familiar en la consulta SQL
            ps.setInt(1, planId);
            
            // Ejecutar la consulta SQL parametrizada
            try (ResultSet rs = ps.executeQuery()) {
                // Si existe el plan de acción para el plan familiar indicado
                if (rs.next()) {
                    // Instanciar y poblar el DTO con la información de la fila
                    ActionPlanDTO dto = new ActionPlanDTO();
                    dto.setId(rs.getInt("IdPlanAccion"));
                    dto.setFamily_plan_id(planId);
                    dto.setCoordinator_id(rs.getInt("IdCoordinador"));
                    
                    // Retornar el DTO mapeado
                    return dto;
                }
            }
        }
        // Retornar null si no se encontró ningún plan de acción coincidente
        return null;
    }

    /**
     * {@inheritDoc}
     * Crea transaccionalmente un plan de acción. Si el plan familiar no posee un factor de riesgo
     * registrado, lo crea automáticamente antes de insertar el plan de acción.
     *
     * @param planId Identificador único del plan familiar.
     * @param coordinatorId Identificador único del integrante coordinador.
     * @return true si el plan de acción se creó con éxito; false en caso contrario.
     * @throws SQLException Si ocurre un error de acceso a datos o de restricción de clave.
     */
    @Override
    public boolean createActionPlan(int planId, int coordinatorId) throws SQLException {
        // Consulta SQL de inserción principal de PlanAccion
        String sql = "INSERT INTO PlanAccion (IdPlanFamiliar, IdFactorRiesgo, IdCoordinador) VALUES (?, ?, ?)";
        
        // Abrir conexión física a la base de datos
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Variable para almacenar el ID del factor de riesgo
            int riskId = 0;
            
            // Sentencia SQL para validar si ya existe un factor de riesgo para el plan familiar
            try (PreparedStatement psR = conn.prepareStatement("SELECT IdFactorRiesgo FROM FactorRiesgo WHERE IdPlanFamiliar = ? LIMIT 1")) {
                psR.setInt(1, planId);
                
                // Ejecutar la consulta de búsqueda de factor de riesgo
                try (ResultSet rsR = psR.executeQuery()) {
                    // Si se encuentra un factor de riesgo existente
                    if (rsR.next()) {
                        // Asignar el ID del factor de riesgo encontrado
                        riskId = rsR.getInt(1);
                    }
                }
            }

            // Si no se encuentra ningún factor de riesgo previo para el plan familiar (riskId == 0), se procede a crearlo
            if (riskId == 0) {
                // Sentencia SQL para insertar un factor de riesgo por defecto y recuperar su clave autogenerada
                try (PreparedStatement psR = conn.prepareStatement("INSERT INTO FactorRiesgo (IdPlanFamiliar, IdTipoAmenaza, Ubicacion) VALUES (?, 1, 'General')", Statement.RETURN_GENERATED_KEYS)) {
                    psR.setInt(1, planId);
                    psR.executeUpdate();
                    
                    // Recuperar la clave autogenerada del factor de riesgo recién insertado
                    try (ResultSet rsR = psR.getGeneratedKeys()) {
                        // Si se generó la clave correctamente
                        if (rsR.next()) {
                            // Asignar la clave autogenerada
                            riskId = rsR.getInt(1);
                        }
                    }
                }
            }

            // Variable para almacenar el ID final del coordinador del plan de acción
            int coordId = coordinatorId;
            
            // Si el ID del coordinador provisto es 0, buscar un integrante del plan familiar para asignarlo por defecto
            if (coordId == 0) {
                // Sentencia SQL para recuperar el primer integrante asociado al plan familiar
                try (PreparedStatement psM = conn.prepareStatement("SELECT IdIntegrante FROM Integrante WHERE IdPlanFamiliar = ? LIMIT 1")) {
                    psM.setInt(1, planId);
                    
                    // Ejecutar la consulta de obtención del integrante
                    try (ResultSet rsM = psM.executeQuery()) {
                        // Si existe algún integrante registrado
                        if (rsM.next()) {
                            // Asignar el ID de dicho integrante como coordinador por defecto
                            coordId = rsM.getInt(1);
                        }
                    }
                }
            }

            // Ejecutar la inserción final del PlanAccion vinculando el plan familiar, el factor de riesgo y el coordinador
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, planId);
                ps.setInt(2, riskId);
                ps.setInt(3, coordId);
                
                // Retornar true si la inserción afectó al menos a una fila
                return ps.executeUpdate() > 0;
            }
        }
    }

    /**
     * {@inheritDoc}
     * Obtiene la lista de acciones asignadas en el plan de acción, incluyendo datos del integrante responsable.
     *
     * @param actionPlanId Identificador único del plan de acción.
     * @return Una lista de DTOs {@link ActionDTO} con la información detallada de cada acción.
     * @throws SQLException Si ocurre un error de base de datos al realizar la consulta.
     */
    @Override
    public List<ActionDTO> getActionsByActionPlan(int actionPlanId) throws SQLException {
        // Inicializar la lista que almacenará las acciones
        List<ActionDTO> list = new ArrayList<>();
        
        // Consulta SQL con JOIN para traer las acciones con la información del integrante responsable
        String sql = "SELECT a.IdAccion, a.IdResponsable, a.Etapa, a.Descripcion, i.Nombre, i.Apellido " +
                     "FROM Accion a " +
                     "JOIN Integrante i ON a.IdResponsable = i.IdIntegrante " +
                     "WHERE a.IdPlanAccion = ?";
        
        // Abrir conexión y preparar la consulta
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Asignar el parámetro del ID del plan de acción
            ps.setInt(1, actionPlanId);
            
            // Ejecutar la consulta y iterar sobre el ResultSet
            try (ResultSet rs = ps.executeQuery()) {
                // Recorrer recursivamente las filas de acciones encontradas
                while (rs.next()) {
                    // Crear y poblar el DTO de la Acción
                    ActionDTO dto = new ActionDTO();
                    dto.setId(rs.getInt("IdAccion"));
                    dto.setDescription(rs.getString("Descripcion"));
                    dto.setStage(rs.getString("Etapa"));
                    dto.setMember_id(rs.getInt("IdResponsable"));

                    // Crear y poblar la clase interna de información del integrante responsable
                    ActionDTO.MemberInfo member = new ActionDTO.MemberInfo();
                    member.setNames(rs.getString("Nombre"));
                    member.setLast_names(rs.getString("Apellido"));
                    dto.setMember(member);

                    // Adicionar la acción mapeada a la lista
                    list.add(dto);
                }
            }
        }
        // Retornar la lista completa de acciones del plan
        return list;
    }

    /**
     * {@inheritDoc}
     * Recupera una acción específica por su identificador, incluyendo los nombres del integrante responsable.
     *
     * @param id Identificador único de la acción.
     * @return El DTO {@link ActionDTO} con la información de la acción, o null si no se encuentra.
     * @throws SQLException Si ocurre un error de base de datos durante la búsqueda.
     */
    @Override
    public ActionDTO getActionById(int id) throws SQLException {
        // Consulta SQL parametrizada para buscar una acción por su identificador primario
        String sql = "SELECT a.IdAccion, a.IdResponsable, a.Etapa, a.Descripcion, i.Nombre, i.Apellido " +
                     "FROM Accion a " +
                     "JOIN Integrante i ON a.IdResponsable = i.IdIntegrante " +
                     "WHERE a.IdAccion = ?";
        
        // Abrir conexión y preparar la consulta de búsqueda
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Asignar el ID de la acción a la consulta
            ps.setInt(1, id);
            
            // Ejecutar y obtener el ResultSet
            try (ResultSet rs = ps.executeQuery()) {
                // Si la acción existe
                if (rs.next()) {
                    // Crear y llenar el DTO de la Acción
                    ActionDTO dto = new ActionDTO();
                    dto.setId(rs.getInt("IdAccion"));
                    dto.setDescription(rs.getString("Descripcion"));
                    dto.setStage(rs.getString("Etapa"));
                    dto.setMember_id(rs.getInt("IdResponsable"));

                    // Mapear la información del integrante responsable asignado
                    ActionDTO.MemberInfo member = new ActionDTO.MemberInfo();
                    member.setNames(rs.getString("Nombre"));
                    member.setLast_names(rs.getString("Apellido"));
                    dto.setMember(member);

                    // Retornar la acción completamente poblada
                    return dto;
                }
            }
        }
        // Retornar null si no se encontró ningún registro para el ID indicado
        return null;
    }

    /**
     * {@inheritDoc}
     * Registra una nueva acción o tarea a realizar dentro del plan de acción.
     *
     * @param actionPlanId Identificador único del plan de acción.
     * @param memberId Identificador del integrante responsable de ejecutar la acción.
     * @param stage Etapa en la cual se ejecuta la acción (ej. Antes, Durante, Después).
     * @param description Descripción de la tarea o acción a ejecutar.
     * @return true si la inserción fue exitosa; false en caso contrario.
     * @throws SQLException Si ocurre un error al ejecutar la sentencia SQL de inserción.
     */
    @Override
    public boolean insertAction(int actionPlanId, int memberId, String stage, String description) throws SQLException {
        // Sentencia SQL parametrizada para insertar una nueva acción
        String sql = "INSERT INTO Accion (IdPlanAccion, IdResponsable, Etapa, Descripcion) VALUES (?, ?, ?, ?)";
        
        // Abrir conexión y preparar el INSERT
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Configurar los parámetros de inserción
            ps.setInt(1, actionPlanId);
            ps.setInt(2, memberId);
            ps.setString(3, stage);
            ps.setString(4, description);
            
            // Retornar true si la inserción afectó filas
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * {@inheritDoc}
     * Actualiza el responsable y la descripción de la acción.
     *
     * @param id Identificador único de la acción a actualizar.
     * @param memberId Nuevo integrante responsable de la acción.
     * @param description Nueva descripción de la tarea a realizar.
     * @return true si el registro fue modificado con éxito; false en caso contrario.
     * @throws SQLException Si ocurre un error al ejecutar la actualización del registro.
     */
    @Override
    public boolean updateAction(int id, int memberId, String description) throws SQLException {
        // Sentencia SQL parametrizada para actualizar una acción por su identificador primario
        String sql = "UPDATE Accion SET IdResponsable = ?, Descripcion = ? WHERE IdAccion = ?";
        
        // Abrir conexión y preparar la actualización SQL
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Configurar parámetros dinámicos
            ps.setInt(1, memberId);
            ps.setString(2, description);
            ps.setInt(3, id);
            
            // Retornar true si se actualizó el registro
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * {@inheritDoc}
     * Elimina una acción/tarea del plan de acción por su identificador.
     *
     * @param id Identificador único de la acción a eliminar.
     * @return true si el registro de la acción fue eliminado correctamente; false en caso contrario.
     * @throws SQLException Si ocurre un error al ejecutar el DELETE en la base de datos.
     */
    @Override
    public boolean deleteAction(int id) throws SQLException {
        // Sentencia SQL parametrizada para eliminar una acción por su clave primaria
        String sql = "DELETE FROM Accion WHERE IdAccion = ?";
        
        // Abrir conexión y preparar la eliminación SQL
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Configurar el parámetro de ID de la acción a eliminar
            ps.setInt(1, id);
            
            // Retornar true si la eliminación afectó filas
            return ps.executeUpdate() > 0;
        }
    }

    // --- PET VACCINES ---

    /**
     * Recupera el historial de vacunas de la mascota.
     *
     * @param petId Identificador único de la mascota.
     * @return Una lista de DTOs con la información de vacunas.
     * @throws SQLException Si ocurre un error de base de datos durante la lectura.
     */
    @Override
    public List<VaccineDTO> getVaccinesByPet(int petId) throws SQLException {
        // Inicializar la lista que guardará las vacunas
        List<VaccineDTO> list = new ArrayList<>();
        
        // Sentencia SQL para consultar las vacunas aplicadas a la mascota usando un JOIN
        String sql = "SELECT mv.IdMascotaVacuna, mv.IdVacuna, v.Nombre, mv.FechaAplicacion " +
                     "FROM MascotaVacuna mv " +
                     "JOIN Vacuna v ON mv.IdVacuna = v.IdVacuna " +
                     "WHERE mv.IdMascota = ?";
        
        // Abrir conexión y preparar la consulta SQL
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Configurar el parámetro de ID de mascota
            ps.setInt(1, petId);
            
            // Ejecutar la consulta SQL
            try (ResultSet rs = ps.executeQuery()) {
                // Iterar a través de los resultados encontrados
                while (rs.next()) {
                    int relationId = rs.getInt("IdMascotaVacuna");
                    Date fechaDate = rs.getDate("FechaAplicacion");
                    
                    // Formatear la fecha a cadena, utilizando la fecha actual como fallback
                    String dateVal = fechaDate != null ? fechaDate.toString() : java.time.LocalDate.now().toString();
 
                    // Crear y mapear los campos en el DTO de Vacuna
                    VaccineDTO dto = new VaccineDTO();
                    dto.setId(relationId);
                    dto.setName(rs.getString("Nombre") != null ? rs.getString("Nombre") : "");
                    dto.setDate(dateVal);
                    dto.setPet_id(petId);
                    
                    // Agregar el DTO mapeado a la lista
                    list.add(dto);
                }
            }
        }
        // Retornar la lista de vacunas mapeadas
        return list;
    }
 
    /**
     * {@inheritDoc}
     * Obtiene una vacuna específica por su ID físico único (IdMascotaVacuna).
     *
     * @param vaccineId Identificador físico de la relación vacuna-mascota.
     * @return El DTO {@link VaccineDTO} con la información de la vacuna, o null si no se encuentra.
     * @throws SQLException Si ocurre un error de base de datos durante la lectura.
     */
    @Override
    public VaccineDTO getVaccineById(int vaccineId) throws SQLException {
        // Sentencia SQL parametrizada para buscar un registro único de vacuna de mascota
        String sql = "SELECT mv.IdMascotaVacuna, mv.IdMascota, v.Nombre, mv.FechaAplicacion " +
                     "FROM MascotaVacuna mv " +
                     "JOIN Vacuna v ON mv.IdVacuna = v.IdVacuna " +
                     "WHERE mv.IdMascotaVacuna = ?";
        
        // Abrir conexión y preparar la sentencia de búsqueda por ID único
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, vaccineId);
            
            // Ejecutar la consulta y procesar el ResultSet
            try (ResultSet rs = ps.executeQuery()) {
                // Si existe el registro de relación vacuna-mascota
                if (rs.next()) {
                    int petId = rs.getInt("IdMascota");
                    Date fechaDate = rs.getDate("FechaAplicacion");
                    
                    // Formatear la fecha a cadena, utilizando la fecha actual como fallback
                    String dateVal = fechaDate != null ? fechaDate.toString() : java.time.LocalDate.now().toString();
 
                    // Instanciar y llenar el DTO resultante
                    VaccineDTO dto = new VaccineDTO();
                    dto.setId(vaccineId);
                    dto.setName(rs.getString("Nombre"));
                    dto.setPet_id(petId);
                    dto.setDate(dateVal);
                    return dto;
                }
            }
        }
        // Retornar null si no se encontró ningún registro coincidente
        return null;
    }

    /**
     * {@inheritDoc}
     * Inserta transaccionalmente una vacuna para la mascota, creándola en el catálogo de vacunas previamente si no existe.
     *
     * @param dto El DTO {@link VaccineDTO} con la información de la vacuna a insertar.
     * @return El identificador compuesto generado si la inserción fue exitosa, o 0 en caso contrario.
     * @throws SQLException Si ocurre un error durante las operaciones SQL o la transacción falla.
     */
    @Override
    public int insertVaccine(VaccineDTO dto) throws SQLException {
        String name = dto.getName();
        String dateStr = dto.getDate();
        Integer petId = dto.getPet_id();
        
        // Validar y aplicar fallback para el ID de mascota si es nulo
        if (petId == null) {
            petId = 1;
        }

        // Obtener conexión física para iniciar la gestión transaccional manual
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Desactivar confirmación automática para el manejo explícito de transacciones
            conn.setAutoCommit(false);
            
            // Bloque try-catch interno para gestionar de forma segura transacciones y rollback
            try {
                int vaccineCatalogId = 0;
                // 1. Buscar o crear la vacuna en la tabla de catálogo (Vacuna)
                String findSql = "SELECT IdVacuna FROM Vacuna WHERE Nombre = ?";
                
                // Preparar y ejecutar consulta para validar si la vacuna ya está en el catálogo
                try (PreparedStatement findPs = conn.prepareStatement(findSql)) {
                    findPs.setString(1, name);
                    
                    // Obtener ResultSet del catálogo
                    try (ResultSet rs = findPs.executeQuery()) {
                        // Si ya existe la vacuna en el catálogo
                        if (rs.next()) {
                            // Asignar el ID recuperado del catálogo
                            vaccineCatalogId = rs.getInt("IdVacuna");
                        }
                    }
                }
                
                // Si la vacuna no existe en el catálogo, se procede a insertarla
                if (vaccineCatalogId == 0) {
                    String insertCatalogSql = "INSERT INTO Vacuna (Nombre) VALUES (?)";
                    
                    // Insertar la vacuna en el catálogo y solicitar llaves autogeneradas
                    try (PreparedStatement insertCatalogPs = conn.prepareStatement(insertCatalogSql, Statement.RETURN_GENERATED_KEYS)) {
                        insertCatalogPs.setString(1, name);
                        insertCatalogPs.executeUpdate();
                        
                        // Obtener la clave autogenerada de la vacuna insertada
                        try (ResultSet rs = insertCatalogPs.getGeneratedKeys()) {
                            // Si se generó el ID correctamente
                            if (rs.next()) {
                                // Asignar el ID autogenerado del catálogo
                                vaccineCatalogId = rs.getInt(1);
                            }
                        }
                    }
                }
                
                // Si no se pudo obtener un ID válido de catálogo, hacer rollback y abortar
                if (vaccineCatalogId == 0) {
                    conn.rollback();
                    return 0;
                }

                // 2. Insertar la relación MascotaVacuna
                String insertLinkSql = "INSERT INTO MascotaVacuna (IdMascota, IdVacuna, FechaAplicacion) VALUES (?, ?, ?)";
                
                // Preparar la inserción de la relación vacuna-mascota
                try (PreparedStatement ps = conn.prepareStatement(insertLinkSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, petId);
                    ps.setInt(2, vaccineCatalogId);
                    
                    // Configurar la fecha de aplicación, utilizando la fecha de hoy si no se provee
                    ps.setDate(3, dateStr != null && !dateStr.isEmpty() ? Date.valueOf(dateStr) : Date.valueOf(java.time.LocalDate.now()));
                    int rows = ps.executeUpdate();
                    
                    // Si el registro se creó exitosamente en la relación MascotaVacuna
                    if (rows > 0) {
                        int generatedKey = 0;
                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            if (rs.next()) {
                                generatedKey = rs.getInt(1);
                            }
                        }
                        // Confirmar los cambios transaccionales en la base de datos
                        conn.commit();
                        
                        // Retornar el identificador físico generado
                        return generatedKey;
                    }
                }
                
                // Si la inserción falló, realizar rollback de la transacción
                conn.rollback();
                return 0;
            } catch (SQLException e) {
                // Ante cualquier error SQL, reversar la transacción y propagar la excepción
                conn.rollback();
                throw e;
            } finally {
                // Garantizar que la confirmación automática vuelva a activarse en la conexión
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public boolean updateVaccine(int vaccineId, VaccineDTO dto) throws SQLException {
        String name = dto.getName();
        String dateStr = dto.getDate();
 
        // Obtener conexión para administrar la transacción de actualización de forma explícita
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Desactivar auto commit
            conn.setAutoCommit(false);
            
            // Bloque try-catch para control y reversión segura de transacciones
            try {
                int newVaccineCatalogId = 0;
                // 1. Buscar o crear la nueva vacuna en el catálogo (Vacuna)
                String findSql = "SELECT IdVacuna FROM Vacuna WHERE Nombre = ?";
                
                // Buscar si la nueva vacuna ya existe en el catálogo por nombre
                try (PreparedStatement findPs = conn.prepareStatement(findSql)) {
                    findPs.setString(1, name);
                    
                    // Ejecutar búsqueda en catálogo
                    try (ResultSet rs = findPs.executeQuery()) {
                        // Si existe en el catálogo
                        if (rs.next()) {
                            // Asignar el ID recuperado de la vacuna en catálogo
                            newVaccineCatalogId = rs.getInt("IdVacuna");
                        }
                    }
                }
                
                // Si la vacuna no existe en el catálogo, insertarla
                if (newVaccineCatalogId == 0) {
                    String insertCatalogSql = "INSERT INTO Vacuna (Nombre) VALUES (?)";
                    
                    // Insertar nuevo registro en catálogo y recuperar la llave generada
                    try (PreparedStatement insertCatalogPs = conn.prepareStatement(insertCatalogSql, Statement.RETURN_GENERATED_KEYS)) {
                        insertCatalogPs.setString(1, name);
                        insertCatalogPs.executeUpdate();
                        
                        // Recuperar el ID de catálogo generado
                        try (ResultSet rs = insertCatalogPs.getGeneratedKeys()) {
                            // Si se generó el ID correctamente
                            if (rs.next()) {
                                // Asignar el ID generado
                                newVaccineCatalogId = rs.getInt(1);
                            }
                        }
                    }
                }
                
                // Si no se obtuvo una llave válida de catálogo, hacer rollback y retornar false
                if (newVaccineCatalogId == 0) {
                    conn.rollback();
                    return false;
                }
 
                // 2. Modificar la relación en MascotaVacuna
                String sql = "UPDATE MascotaVacuna SET IdVacuna = ?, FechaAplicacion = ? WHERE IdMascotaVacuna = ?";
                
                // Preparar y ejecutar la actualización del registro en la relación MascotaVacuna
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, newVaccineCatalogId);
                    ps.setDate(2, dateStr != null && !dateStr.isEmpty() ? Date.valueOf(dateStr) : Date.valueOf(java.time.LocalDate.now()));
                    ps.setInt(3, vaccineId);
                    int rows = ps.executeUpdate();
                    
                    // Confirmar transacción
                    conn.commit();
                    
                    // Retornar true si se actualizó el registro
                    return rows > 0;
                }
            } catch (SQLException e) {
                // Ante fallas en la transacción, reversar los cambios y lanzar excepción
                conn.rollback();
                throw e;
            } finally {
                // Asegurar que la confirmación automática vuelva a activarse en la conexión
                conn.setAutoCommit(true);
            }
        }
    }
 
    /**
     * {@inheritDoc}
     * Elimina el registro de aplicación de vacuna por su ID físico único.
     *
     * @param vaccineId Identificador físico de la relación vacuna-mascota.
     * @return true si la eliminación del registro de relación fue exitosa; false en caso contrario.
     * @throws SQLException Si ocurre un error al ejecutar la sentencia SQL.
     */
    @Override
    public boolean deleteVaccine(int vaccineId) throws SQLException {
        // Sentencia SQL parametrizada de borrado
        String sql = "DELETE FROM MascotaVacuna WHERE IdMascotaVacuna = ?";
        
        // Abrir conexión y preparar la eliminación física
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, vaccineId);
            
            // Retornar true si el DELETE afectó filas
            return ps.executeUpdate() > 0;
        }
    }

    // --- AVAILABLE RESOURCES ---

    /**
     * {@inheritDoc}
     * Obtiene todos los recursos comunitarios externos, recuperando la descripción desde el extraData de memoria.
     *
     * @param planId Identificador único del plan familiar.
     * @return Una lista de mapas con los detalles de cada recurso disponible.
     * @throws SQLException Si ocurre un error al ejecutar la consulta SQL.
     */
    @Override
    public List<Map<String, Object>> getAvailableResourcesByPlan(int planId) throws SQLException {
        // Inicializar la lista para retornar los recursos
        List<Map<String, Object>> list = new ArrayList<>();
        
        // Consulta SQL con múltiples JOINs para consolidar los recursos del plan familiar
        String sql = """
            SELECT rd.IdRecurso, rd.Ubicacion, rd.Distancia, rd.Telefono, rd.Descripcion, rt.Nombre AS RecursoNombre, s.Nombre AS ServicioNombre
            FROM RecursoDisponible rd
            JOIN RecursoTipo rt ON rd.IdRecursoTipo = rt.IdRecursoTipo
            JOIN Servicio s ON rd.IdServicio = s.IdServicio
            WHERE rd.IdPlanFamiliar = ?
            """;
        
        // Abrir conexión y preparar la consulta parametrizada
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Configurar el identificador de plan familiar
            ps.setInt(1, planId);
            
            // Ejecutar la consulta SQL
            try (ResultSet rs = ps.executeQuery()) {
                // Iterar recursivamente sobre los registros de recursos comunitarios
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    int resourceId = rs.getInt("IdRecurso");
                    
                    // Rellenar el mapa de datos del recurso con las columnas correspondientes
                    item.put("id", resourceId);
                    item.put("resource_name", rs.getString("RecursoNombre"));
                    item.put("location", rs.getString("Ubicacion") != null ? rs.getString("Ubicacion") : "");
                    item.put("distance", rs.getFloat("Distancia"));
                    item.put("service", rs.getString("ServicioNombre") != null ? rs.getString("ServicioNombre") : "");
                    item.put("description", rs.getString("Descripcion") != null ? rs.getString("Descripcion") : "");
                    item.put("phone", rs.getString("Telefono") != null ? rs.getString("Telefono") : "");
                    
                    // Agregar el mapa de recurso a la lista final
                    list.add(item);
                }
            }
        }
        // Retornar la lista de recursos disponibles
        return list;
    }

    /**
     * {@inheritDoc}
     * Busca un recurso comunitario cercano por su identificador y asocia la descripción desde el extraData.
     *
     * @param idVal Identificador único del recurso disponible.
     * @return Un mapa con los detalles del recurso encontrado, o un mapa vacío si no existe.
     * @throws SQLException Si ocurre un error al consultar la base de datos.
     */
    @Override
    public Map<String, Object> getAvailableResourceById(int idVal) throws SQLException {
        // Inicializar el mapa de retorno
        Map<String, Object> item = new HashMap<>();
        
        // Consulta SQL con JOIN para buscar el recurso disponible por su clave primaria
        String sql = """
            SELECT rd.IdRecurso, rd.IdRecursoTipo, rd.IdServicio, rd.Ubicacion, rd.Distancia, rd.Telefono, rd.Descripcion, rt.Nombre AS RecursoNombre
            FROM RecursoDisponible rd
            JOIN RecursoTipo rt ON rd.IdRecursoTipo = rt.IdRecursoTipo
            WHERE rd.IdRecurso = ?
            """;
        
        // Abrir conexión y preparar consulta SQL
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Asignar el parámetro del ID de recurso disponible
            ps.setInt(1, idVal);
            
            // Ejecutar la consulta y gestionar el ciclo del ResultSet
            try (ResultSet rs = ps.executeQuery()) {
                // Si el recurso existe
                if (rs.next()) {
                    // Mapear los datos de la fila del ResultSet
                    item.put("id", rs.getInt("IdRecurso"));
                    item.put("phone", rs.getString("Telefono") != null ? rs.getString("Telefono") : "");
                    item.put("distance", rs.getFloat("Distancia"));
                    item.put("location", rs.getString("Ubicacion") != null ? rs.getString("Ubicacion") : "");
                    item.put("resource_id", rs.getInt("IdRecursoTipo"));
                    item.put("resource_name", rs.getString("RecursoNombre"));
                    item.put("description", rs.getString("Descripcion") != null ? rs.getString("Descripcion") : "");
                }
            }
        }
        // Retornar el mapa del recurso disponible
        return item;
    }

    /**
     * {@inheritDoc}
     * Inserta un recurso comunitario cercano en la base de datos y almacena su descripción en memoria (extraData).
     *
     * @param planId Identificador único del plan familiar.
     * @param resourceId Identificador único del tipo de recurso.
     * @param description Descripción asociada al recurso disponible.
     * @param location Ubicación o dirección del recurso.
     * @param distance Distancia aproximada en kilómetros o metros.
     * @param phone Teléfono de contacto del recurso.
     * @return true si la inserción fue exitosa; false en caso contrario.
     * @throws SQLException Si ocurre un error al ejecutar la sentencia INSERT de la base de datos.
     */
    @Override
    public boolean insertAvailableResource(int planId, int resourceId, String description, String location, float distance, String phone) throws SQLException {
        // Variable para guardar el ID de servicio asociado al tipo de recurso
        int serviceId = 1;
        
        // Sentencia SQL para obtener el ID de servicio basado en el tipo de recurso seleccionado
        String serviceSql = "SELECT s.IdServicio FROM Servicio s WHERE s.Nombre = (SELECT r.Servicio FROM RecursoTipo r WHERE r.IdRecursoTipo = ?)";
        
        // Abrir conexión y preparar la consulta de servicio de forma aislada
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement psService = conn.prepareStatement(serviceSql)) {
            
            // Asignar el parámetro del ID de recurso a la consulta
            psService.setInt(1, resourceId);
            
            // Ejecutar la consulta de servicio
            try (ResultSet rs = psService.executeQuery()) {
                // Si se encuentra una correspondencia de servicio
                if (rs.next()) {
                    // Obtener el ID de servicio correspondiente
                    serviceId = rs.getInt("IdServicio");
                }
            }
        } catch (Exception e) {
            // Bloque catch para capturar cualquier excepción y usar el ID de servicio 1 por defecto (fallback)
        }

        // Sentencia SQL parametrizada para realizar la inserción física del recurso disponible
        String sql = "INSERT INTO RecursoDisponible (IdPlanFamiliar, IdRecursoTipo, IdServicio, Ubicacion, Distancia, Telefono, Descripcion) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        // Abrir conexión y preparar inserción SQL
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            // Configurar los parámetros de inserción
            ps.setInt(1, planId);
            ps.setInt(2, resourceId);
            ps.setInt(3, serviceId);
            ps.setString(4, location != null ? location : "");
            ps.setFloat(5, distance);
            ps.setString(6, phone != null ? phone : "");
            ps.setString(7, description != null ? description : "");
            
            // Ejecutar inserción y guardar el número de filas afectadas
            int affectedRows = ps.executeUpdate();
            
            // Si no se insertó ninguna fila
            if (affectedRows == 0) {
                // Retornar false indicando fallo en la inserción
                return false;
            }
            
            // Retornar true indicando éxito en el registro
            return true;
        }
    }

    /**
     * {@inheritDoc}
     * Actualiza la información física del recurso y su descripción asociada en el extraData.
     *
     * @param idVal Identificador único del recurso disponible a actualizar.
     * @param resourceId Identificador del tipo de recurso.
     * @param description Nueva descripción del recurso.
     * @param location Nueva dirección/ubicación del recurso.
     * @param distance Nueva distancia al recurso.
     * @param phone Nuevo teléfono de contacto.
     * @return true si la actualización en base de datos fue exitosa; false en caso contrario.
     * @throws SQLException Si ocurre un error al ejecutar la sentencia SQL de actualización.
     */
    @Override
    public boolean updateAvailableResource(int idVal, int resourceId, String description, String location, float distance, String phone) throws SQLException {
        // Variable para guardar el ID de servicio asociado al tipo de recurso
        int serviceId = 1;
        
        // Sentencia SQL para obtener el ID de servicio basado en el tipo de recurso
        String serviceSql = "SELECT s.IdServicio FROM Servicio s WHERE s.Nombre = (SELECT r.Servicio FROM RecursoTipo r WHERE r.IdRecursoTipo = ?)";
        
        // Abrir conexión y preparar consulta de servicio de forma aislada
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement psService = conn.prepareStatement(serviceSql)) {
            
            // Asignar parámetro de tipo de recurso
            psService.setInt(1, resourceId);
            
            // Ejecutar la consulta del servicio
            try (ResultSet rs = psService.executeQuery()) {
                // Si se encuentra una correspondencia
                if (rs.next()) {
                    // Asignar el ID del servicio
                    serviceId = rs.getInt("IdServicio");
                }
            }
        } catch (Exception e) {
            // Bloque catch para capturar cualquier excepción y usar el ID de servicio 1 por defecto (fallback)
        }

        // Sentencia SQL parametrizada de actualización de RecursoDisponible
        String sql = "UPDATE RecursoDisponible SET IdRecursoTipo = ?, IdServicio = ?, Ubicacion = ?, Distancia = ?, Telefono = ?, Descripcion = ? WHERE IdRecurso = ?";
        
        // Abrir conexión y preparar la sentencia UPDATE
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Configurar los parámetros de actualización
            ps.setInt(1, resourceId);
            ps.setInt(2, serviceId);
            ps.setString(3, location != null ? location : "");
            ps.setFloat(4, distance);
            ps.setString(5, phone != null ? phone : "");
            ps.setString(6, description != null ? description : "");
            ps.setInt(7, idVal);
            
            // Retornar true si la actualización afectó al menos a un registro
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * {@inheritDoc}
     * Elimina el recurso de la base de datos y remueve su descripción de la caché extraData.
     *
     * @param idVal Identificador único del recurso disponible a eliminar.
     * @return true si la eliminación en base de datos fue exitosa; false en caso contrario.
     * @throws SQLException Si ocurre un error al ejecutar la sentencia de borrado SQL.
     */
    @Override
    public boolean deleteAvailableResource(int idVal) throws SQLException {
        // Sentencia SQL de eliminación física
        String sql = "DELETE FROM RecursoDisponible WHERE IdRecurso = ?";
        
        // Abrir conexión y preparar la eliminación
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Asignar el parámetro del ID de recurso a eliminar
            ps.setInt(1, idVal);
            
            // Retornar el resultado de la operación de borrado
            return ps.executeUpdate() > 0;
        }
    }
}

package com.defensacivil.dao;

import com.defensacivil.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementación de la interfaz {@link UsuarioDAO} que interactúa con la base de datos
 * para realizar la autenticación de usuarios y recuperación de sus atributos de sesión.
 */
public class UsuarioDAOImpl implements UsuarioDAO {

    /**
     * Constructor por defecto de la clase.
     */
    public UsuarioDAOImpl() {
        // Constructor sin argumentos
    }

    /**
     * {@inheritDoc}
     * Realiza una consulta SELECT filtrando por correo electrónico y compara la contraseña
     * ingresada con la almacenada.
     *
     * @param email El correo electrónico del usuario que intenta iniciar sesión.
     * @param password La contraseña en texto plano para comparar con la contraseña almacenada en la base de datos.
     * @return Un mapa con los detalles del usuario o null si no se encuentra.
     * @throws SQLException Si ocurre un error de acceso a la base de datos.
     */
    @Override
    public Map<String, Object> autenticarUsuario(String email, String password) throws SQLException {
        // Sentencia SQL parametrizada para buscar un usuario por su email e incluir su seccional a través de Organizacion
        String sql = """
            SELECT u.IdUsuario, u.Nombre, u.IdRol, COALESCE(u.IdSeccional, o.IdSeccional) AS IdSeccional, u.IdGenero, u.Email, u.Contrasena, u.Activo 
            FROM Usuario u 
            LEFT JOIN Organizacion o ON u.IdOrganizacion = o.IdOrganizacion 
            WHERE u.Email = ?
            """;
        
        // Obtener conexión JDBC y preparar consulta parametrizada de manera segura
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Asignar el parámetro del correo electrónico a la consulta
            ps.setString(1, email);
            
            // Ejecutar la consulta en la base de datos y gestionar el ciclo de vida del ResultSet
            try (ResultSet rs = ps.executeQuery()) {
                // Si el usuario con el correo especificado existe
                if (rs.next()) {
                    // Inicializar el mapa para almacenar los datos del usuario
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("IdUsuario", rs.getInt("IdUsuario"));
                    userMap.put("Nombre", rs.getString("Nombre"));
                    userMap.put("IdRol", rs.getInt("IdRol"));
                    userMap.put("IdSeccional", rs.getInt("IdSeccional"));
                    userMap.put("IdGenero", rs.getInt("IdGenero"));
                    userMap.put("Email", rs.getString("Email"));
                    userMap.put("Contrasena", rs.getString("Contrasena"));
                    userMap.put("Activo", rs.getInt("Activo"));
                    
                    // Verificar si la contraseña provista coincide exactamente con la guardada en la base de datos (texto plano)
                    boolean passwordCorrect = password != null && password.equals(rs.getString("Contrasena"));
                    
                    // Almacenar el indicador de verificación en el mapa
                    userMap.put("password_correct", passwordCorrect);
                    
                    // Retornar el mapa con los datos cargados del usuario
                    return userMap;
                }
            }
        }
        // Retornar nulo si no se encontró ningún registro para el email ingresado
        return null;
    }
}

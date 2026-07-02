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
 * para realizar la autenticación de usuarios.
 */
public class UsuarioDAOImpl implements UsuarioDAO {

    /**
     * {@inheritDoc}
     * Realiza una consulta SELECT filtrando por correo electrónico y compara la contraseña
     * ingresada con la almacenada.
     */
    @Override
    public Map<String, Object> autenticarUsuario(String email, String password) throws SQLException {
        String sql = """
            SELECT u.IdUsuario, u.Nombre, u.IdRol, o.IdSeccional, u.IdGenero, u.Email, u.Contrasena, u.Activo 
            FROM Usuario u 
            LEFT JOIN Organizacion o ON u.IdOrganizacion = o.IdOrganizacion 
            WHERE u.Email = ?
            """;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, email);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("IdUsuario", rs.getInt("IdUsuario"));
                    userMap.put("Nombre", rs.getString("Nombre"));
                    userMap.put("IdRol", rs.getInt("IdRol"));
                    userMap.put("IdSeccional", rs.getInt("IdSeccional"));
                    userMap.put("IdGenero", rs.getInt("IdGenero"));
                    userMap.put("Email", rs.getString("Email"));
                    userMap.put("Contrasena", rs.getString("Contrasena"));
                    userMap.put("Activo", rs.getInt("Activo"));
                    
                    boolean passwordCorrect = password != null && password.equals(rs.getString("Contrasena"));
                    userMap.put("password_correct", passwordCorrect);
                    
                    return userMap;
                }
            }
        }
        return null;
    }
}

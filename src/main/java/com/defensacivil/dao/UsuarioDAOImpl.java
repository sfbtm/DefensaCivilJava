package com.defensacivil.dao;

import com.defensacivil.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class UsuarioDAOImpl implements UsuarioDAO {

    @Override
    public Map<String, Object> autenticarUsuario(String email, String password) throws SQLException {
        String sql = "SELECT IdUsuario, Nombre, IdRol, IdSeccional, IdGenero, Email, Contrasena, Activo FROM Usuario WHERE Email = ?";
        
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

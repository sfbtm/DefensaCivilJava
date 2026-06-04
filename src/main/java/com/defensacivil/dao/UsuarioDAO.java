package com.defensacivil.dao;

import java.sql.SQLException;
import java.util.Map;

public interface UsuarioDAO {
    /**
     * Authenticates a user by email and checks if the provided password is correct.
     * Returns a map with user details and a "password_correct" boolean flag.
     * Returns null if no user is found with the given email.
     *
     * @param email The user's email.
     * @param password The raw password to verify.
     * @return A map with user details or null if not found.
     * @throws SQLException If a database access error occurs.
     */
    Map<String, Object> autenticarUsuario(String email, String password) throws SQLException;
}

package com.defensacivil.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Clase de configuración para la conexión a la base de datos MySQL.
 * Proporciona el controlador JDBC necesario y expone un método estático
 * para obtener una conexión activa.
 */
public class DatabaseConfig {

    private static final String URL =
            "jdbc:mysql://localhost:3306/DefensaCivilDB";

    private static final String USER = "root";

    private static final String PASSWORD = "admin123";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found in classpath", e);
        }
    }

    /**
     * Establece y retorna una conexión a la base de datos MySQL configurada.
     *
     * @return Una instancia de {@link Connection} conectada a la base de datos.
     * @throws SQLException Si ocurre un error de acceso a la base de datos o la URL es inválida.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
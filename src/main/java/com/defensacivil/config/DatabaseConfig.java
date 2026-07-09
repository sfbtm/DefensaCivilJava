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

    // URL de conexión JDBC apuntando al servidor MySQL local y al esquema DefensaCivilDB
    private static final String URL =
            "jdbc:mysql://127.0.0.1:3306/DefensaCivilDB";

    // Nombre de usuario de la base de datos
    private static final String USER = "root";

    // Contraseña de acceso para el usuario root de MySQL
    private static final String PASSWORD = "admin123";

    // Bloque estático que se ejecuta al cargar la clase en memoria para asegurar la carga del controlador JDBC
    static {
        // Bloque de inicialización estática
        try {
            // Bloque try: intentar cargar dinámicamente la clase del controlador JDBC de MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // Bloque catch: capturar la excepción si el driver no está presente en el classpath y lanzar una excepción de ejecución
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
        // Bloque del método de obtención de conexión
        
        // Establece la conexión utilizando el Administrador de Controladores con las credenciales definidas
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
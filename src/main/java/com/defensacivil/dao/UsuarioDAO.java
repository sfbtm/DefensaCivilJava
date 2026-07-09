package com.defensacivil.dao;

import java.sql.SQLException;
import java.util.Map;

/**
 * Interfaz que define las operaciones de acceso a datos (DAO) para la gestión
 * y autenticación de usuarios en el sistema de Defensa Civil.
 * Proporciona el contrato necesario para buscar usuarios y verificar credenciales.
 */
public interface UsuarioDAO {
    /**
     * Autentica un usuario por su correo electrónico y verifica si la contraseña proporcionada es correcta.
     * Retorna un mapa con los detalles del usuario (como ID, Nombre, Rol, Seccional, etc.) y una bandera booleana "password_correct".
     * Retorna null si no se encuentra ningún usuario con el correo electrónico proporcionado.
     *
     * @param email El correo electrónico del usuario que intenta iniciar sesión.
     * @param password La contraseña en texto plano para comparar con la contraseña almacenada en la base de datos.
     * @return Un mapa con los detalles del usuario (IdUsuario, Nombre, IdRol, IdSeccional, IdGenero, Email, Contrasena, Activo, password_correct) o null si no se encuentra el correo electrónico.
     * @throws SQLException Si ocurre un error de acceso a la base de datos o de comunicación de red con el servidor SQL.
     */
    Map<String, Object> autenticarUsuario(String email, String password) throws SQLException;
}

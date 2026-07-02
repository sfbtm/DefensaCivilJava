package com.defensacivil.dao;

import java.sql.SQLException;
import java.util.Map;

/**
 * Interfaz que define las operaciones de acceso a datos (DAO) para la gestión
 * y autenticación de usuarios en el sistema.
 */
public interface UsuarioDAO {
    /**
     * Autentica un usuario por su correo electrónico y verifica si la contraseña proporcionada es correcta.
     * Retorna un mapa con los detalles del usuario y una bandera booleana "password_correct".
     * Retorna null si no se encuentra ningún usuario con el correo electrónico proporcionado.
     *
     * @param email El correo electrónico del usuario.
     * @param password La contraseña en texto plano para verificar.
     * @return Un mapa con los detalles del usuario o null si no se encuentra.
     * @throws SQLException Si ocurre un error de acceso a la base de datos.
     */
    Map<String, Object> autenticarUsuario(String email, String password) throws SQLException;
}

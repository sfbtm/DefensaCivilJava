package com.defensacivil.controller;

import com.defensacivil.config.ResponseUtil;
import com.defensacivil.dao.UsuarioDAO;
import com.defensacivil.dao.UsuarioDAOImpl;
import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet que maneja el proceso de autenticación de usuarios (Inicio de sesión).
 * Valida las credenciales contra la base de datos, comprueba el estado activo del usuario,
 * e inicia la sesión simulando tokens de seguridad para propósitos académicos.
 * 
 * Endpoint:
 * - /api/login
 */
@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {

    /**
     * Instancia de Gson utilizada para deserializar los datos JSON de la solicitud de login.
     */
    private final Gson gson = new Gson();

    /**
     * Objeto de acceso a datos (DAO) para realizar operaciones y validación de usuarios en la base de datos.
     */
    private final UsuarioDAO usuarioDAO = new UsuarioDAOImpl();

    /**
     * Procesa la solicitud HTTP POST para autenticar un usuario.
     * 
     * Cuerpo JSON recibido:
     * - { "email": "correo@dominio.com", "password": "clave" }
     * 
     * Respuestas del Servidor:
     * - 200 OK (Éxito): Retorna datos del usuario y tokens simulados.
     * - 200 OK (Error de lógica): Mensajes sobre credenciales incorrectas, usuario inactivo o pendiente de aprobación.
     * - 400 Bad Request: Si los datos de entrada son inválidos o nulos.
     * - 500 Internal Server Error: Si ocurre un fallo en el servidor o base de datos.
     * 
     * @param req Petición HTTP conteniendo el JSON en el cuerpo.
     * @param resp Respuesta HTTP.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Bloque try para capturar y procesar excepciones durante todo el flujo de autenticación
        try {
            // Leer el cuerpo de la petición HTTP leyendo línea por línea
            StringBuilder sb = new StringBuilder();
            String line;
            // Bloque try-with-resources para asegurar el cierre automático del lector de flujo de entrada
            try (BufferedReader reader = req.getReader()) {
                // Bucle de lectura para extraer todas las líneas del cuerpo de la petición HTTP
                while ((line = reader.readLine()) != null) {
                    // Bloque ejecutado para concatenar cada línea leída
                    sb.append(line);
                }
            }

            // Deserializar la cadena JSON de entrada a un objeto Java LoginRequest usando Gson
            LoginRequest loginReq = gson.fromJson(sb.toString(), LoginRequest.class);

            // Validar que la solicitud y el correo no sean nulos
            if (loginReq == null || loginReq.email == null) {
                // Bloque ejecutado si la solicitud o el email son nulos
                ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Datos invalidos");
                return;
            }

            // Validar credenciales consultando a la base de datos a través de UsuarioDAO
            Map<String, Object> dbUser = usuarioDAO.autenticarUsuario(loginReq.email, loginReq.password);
            // Condicional para evaluar si se encontró un usuario con ese correo electrónico
            if (dbUser == null) {
                // Bloque ejecutado si el usuario no está registrado en la base de datos
                ResponseUtil.sendError(resp, HttpServletResponse.SC_OK, "Credenciales incorrectas o usuario no registrado");
                return;
            }

            // Comprobar si la contraseña coincide con el registro encontrado
            boolean passwordCorrect = (boolean) dbUser.get("password_correct");
            // Condicional para evaluar si la contraseña es incorrecta
            if (!passwordCorrect) {
                // Bloque ejecutado si la contraseña no coincide
                ResponseUtil.sendError(resp, HttpServletResponse.SC_OK, "Credenciales incorrectas");
                return;
            }

            // Validar el estado activo del usuario: 3 = Pendiente de Aprobación, 0 o 2 = Inactivo
            int activeVal = (int) dbUser.get("Activo");
            // Estructura condicional múltiple para verificar el estado de activación del usuario
            if (activeVal == 3) {
                // Bloque ejecutado si el usuario está pendiente de aprobación
                ResponseUtil.sendError(resp, HttpServletResponse.SC_OK, "Su solicitud de registro no se ha aprobado, por favor contacte al supervisor de su seccional o al administrador.");
                return;
            } else if (activeVal == 2 || activeVal == 0) {
                // Bloque ejecutado si el usuario está inactivo o suspendido
                ResponseUtil.sendError(resp, HttpServletResponse.SC_OK, "El usuario se encuentra inactivo, por favor contacte al supervisor de su seccional o administrador.");
                return;
            }

            // Establecer variables estáticas de sesión simulada para acoplar con los servlets de la app
            int userId = (int) dbUser.get("IdUsuario");
            int sectionalId = (int) dbUser.get("IdSeccional");
            int roleId = (int) dbUser.get("IdRol");

            UserServlet.loggedInUserId = userId;
            UserServlet.loggedInSectionalId = sectionalId;
            UserServlet.loggedInRoleId = roleId;

            // Almacenar los mismos datos de sesión en la sesión HttpSession del contenedor web
            jakarta.servlet.http.HttpSession session = req.getSession(true);
            session.setAttribute("userId", userId);
            session.setAttribute("sectionalId", sectionalId);
            session.setAttribute("roleId", roleId);

            String fullName = (String) dbUser.get("Nombre");
            int genderId = (int) dbUser.get("IdGenero");

            // Construir el mapa de respuesta JSON requerido por la SPA del frontend
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("full_name", fullName);
            responseData.put("id", userId);
            responseData.put("permissions", "[]");
            responseData.put("role_id", roleId);
            responseData.put("sectional_id", sectionalId);
            responseData.put("gender", genderId);
            // Simulación de tokens JWT para cumplir con la arquitectura desacoplada de la aplicación
            responseData.put("token", "dummy-jwt-token-for-academic-purposes");
            responseData.put("refresh_token", "dummy-refresh-token-for-academic-purposes");

            // Responder con un objeto de éxito y el respectivo mensaje de bienvenida
            ResponseUtil.sendSuccess(resp, HttpServletResponse.SC_OK, responseData, "Bienvenido, " + fullName);

        } catch (Exception e) {
            // Bloque catch para capturar fallos técnicos imprevistos o errores SQL
            // Imprimir la traza del error en la salida de consola del servidor
            e.printStackTrace();
            // Retornar un error 500 estándar en caso de fallo del servidor
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno en el servidor");
        }
    }

    /**
     * Clase interna DTO que representa la estructura de la solicitud de login.
     */
    private static class LoginRequest {
        /**
         * Correo electrónico suministrado por el usuario para identificarse.
         */
        String email;

        /**
         * Contraseña provista para validar la identidad del usuario.
         */
        String password;
    }
}

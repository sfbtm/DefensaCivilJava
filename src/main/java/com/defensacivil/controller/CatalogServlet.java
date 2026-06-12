package com.defensacivil.controller;

import com.defensacivil.config.DatabaseConfig;
import com.defensacivil.config.ResponseUtil;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {
    "/api/statusPlans",
    "/api/zones",
    "/api/cities/*",
    "/api/kinships/*",
    "/api/bloodGroups/*",
    "/api/animalGenders/*"
})
public class CatalogServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            if (servletPath.contains("statusPlans")) {
                List<Map<String, Object>> list = List.of(
                    Map.of("id", 1, "name", "Creado"),
                    Map.of("id", 3, "name", "En proceso"),
                    Map.of("id", 4, "name", "En Revision"),
                    Map.of("id", 5, "name", "Devuelto con observaciones"),
                    Map.of("id", 6, "name", "Rechazado"),
                    Map.of("id", 7, "name", "Completado")
                );
                resp.getWriter().write(gson.toJson(Map.of("data", list)));
                return;
            }

            if (servletPath.contains("zones")) {
                List<Map<String, Object>> list = List.of(
                    Map.of("id", 1, "name", "Urbana"),
                    Map.of("id", 2, "name", "Rural")
                );
                resp.getWriter().write(gson.toJson(Map.of("data", list)));
                return;
            }

            if (servletPath.contains("cities")) {
                if (pathInfo != null && pathInfo.startsWith("/department/")) {
                    int deptId = 1;
                    try {
                        deptId = Integer.parseInt(pathInfo.substring(12));
                    } catch (NumberFormatException ignored) {}
                    
                    List<Map<String, Object>> list = List.of(
                        Map.of("id", 1, "name", "Medellín"),
                        Map.of("id", 2, "name", "Bello"),
                        Map.of("id", 3, "name", "Itagüí")
                    );
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                } else {
                    List<Map<String, Object>> list = List.of(
                        Map.of("id", 1, "name", "Medellín"),
                        Map.of("id", 2, "name", "Bello"),
                        Map.of("id", 3, "name", "Itagüí")
                    );
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                }
                return;
            }

            if (servletPath.contains("kinships")) {
                if (pathInfo != null && !pathInfo.equals("/")) {
                    int id = Integer.parseInt(pathInfo.substring(1));
                    String name = switch (id) {
                        case 1 -> "Padre";
                        case 2 -> "Madre";
                        case 3 -> "Hijo/a";
                        default -> "Otro";
                    };
                    resp.getWriter().write(gson.toJson(Map.of("data", Map.of("id", id, "name", name))));
                } else {
                    List<Map<String, Object>> list = List.of(
                        Map.of("id", 1, "name", "Padre"),
                        Map.of("id", 2, "name", "Madre"),
                        Map.of("id", 3, "name", "Hijo/a"),
                        Map.of("id", 4, "name", "Otro")
                    );
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                }
                return;
            }

            if (servletPath.contains("bloodGroups")) {
                if (pathInfo != null && !pathInfo.equals("/")) {
                    int id = Integer.parseInt(pathInfo.substring(1));
                    String name = switch (id) {
                        case 1 -> "O+";
                        case 2 -> "O-";
                        case 3 -> "A+";
                        case 4 -> "A-";
                        case 5 -> "B+";
                        case 6 -> "B-";
                        case 7 -> "AB+";
                        case 8 -> "AB-";
                        default -> "O+";
                    };
                    resp.getWriter().write(gson.toJson(Map.of("data", Map.of("id", id, "name", name))));
                } else {
                    List<Map<String, Object>> list = List.of(
                        Map.of("id", 1, "name", "O+"),
                        Map.of("id", 2, "name", "O-"),
                        Map.of("id", 3, "name", "A+"),
                        Map.of("id", 4, "name", "A-"),
                        Map.of("id", 5, "name", "B+"),
                        Map.of("id", 6, "name", "B-"),
                        Map.of("id", 7, "name", "AB+"),
                        Map.of("id", 8, "name", "AB-")
                    );
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                }
                return;
            }

            if (servletPath.contains("animalGenders")) {
                if (pathInfo != null && pathInfo.startsWith("/pet/")) {
                    int petId = Integer.parseInt(pathInfo.substring(5));
                    String sql = "SELECT m.IdGenero, g.Nombre AS GeneroNombre FROM Mascotas m LEFT JOIN Genero g ON m.IdGenero = g.IdGenero WHERE m.IdMascota = ?";
                    Map<String, Object> gender = Map.of("id", 1, "name", "Macho");
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, petId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                int genderId = rs.getInt("IdGenero");
                                String genderName = rs.getString("GeneroNombre");
                                gender = Map.of("id", genderId, "name", genderName != null ? genderName : "Macho");
                            }
                        }
                    }
                    resp.getWriter().write(gson.toJson(Map.of("data", gender)));
                    return;
                } else if (pathInfo != null && !pathInfo.equals("/")) {
                    int id = Integer.parseInt(pathInfo.substring(1));
                    String name = (id == 1) ? "Macho" : "Hembra";
                    resp.getWriter().write(gson.toJson(Map.of("data", Map.of("id", id, "name", name))));
                } else {
                    List<Map<String, Object>> list = List.of(
                        Map.of("id", 1, "name", "Macho"),
                        Map.of("id", 2, "name", "Hembra")
                    );
                    resp.getWriter().write(gson.toJson(Map.of("data", list)));
                }
                return;
            }

            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta de catálogo no soportada");
        } catch (Exception e) {
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}

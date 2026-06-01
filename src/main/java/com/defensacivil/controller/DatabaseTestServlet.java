package com.defensacivil.controller;

import com.defensacivil.config.DatabaseConfig;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;

@WebServlet("/api/db-test")
public class DatabaseTestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json");

        try (Connection conn = DatabaseConfig.getConnection()) {

            resp.getWriter().write("""
                    {
                        "data": "Conexion exitosa"
                    }
                    """);

        } catch (Exception e) {

            resp.setStatus(500);

            resp.getWriter().write("""
                    {
                        "error": "Error de conexion"
                    }
                    """);

            e.printStackTrace();
        }
    }
}
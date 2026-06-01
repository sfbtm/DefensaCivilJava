package com.defensacivil.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/notifications/*")
public class NotificationServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();

        if (pathInfo != null && pathInfo.contains("/count/")) {
            // GET /api/notifications/user/count/{userId}
            resp.getWriter().write("""
                    {
                        "data": {
                            "unread_notifications": 0
                        }
                    }
                    """);
        } else {
            // GET /api/notifications/user/{userId} o general
            resp.getWriter().write("""
                    {
                        "data": []
                    }
                    """);
        }
    }
}

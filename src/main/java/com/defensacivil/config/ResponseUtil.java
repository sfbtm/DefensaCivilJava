package com.defensacivil.config;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ResponseUtil {

    private static final Gson gson = new Gson();

    public static void sendSuccess(HttpServletResponse resp, int status, Object data, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        if (message != null) {
            responseMap.put("message", message);
        }
        if (data != null) {
            responseMap.put("data", data);
        }

        resp.getWriter().write(gson.toJson(responseMap));
    }

    public static void sendSuccess(HttpServletResponse resp, Object data) throws IOException {
        sendSuccess(resp, HttpServletResponse.SC_OK, data, null);
    }

    public static void sendSuccess(HttpServletResponse resp, String message) throws IOException {
        sendSuccess(resp, HttpServletResponse.SC_OK, null, message);
    }

    public static void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", false);
        responseMap.put("message", message);

        resp.getWriter().write(gson.toJson(responseMap));
    }
}

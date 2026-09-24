package com.pulseroute.servlet;

import com.pulseroute.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Returns JSON containing currently authenticated Citizen Session.
 */
@WebServlet(name = "SessionServlet", urlPatterns = {"/api/session"})
public class SessionServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Prevent browser caching of session endpoint
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");
            StringBuilder json = new StringBuilder();
            json.append("{")
                .append("\"authenticated\":true,")
                .append("\"user\":{")
                .append("\"id\":").append(user.getId()).append(",")
                .append("\"fullName\":\"").append(escapeJson(user.getFullName())).append("\",")
                .append("\"citizenId\":\"").append(escapeJson(user.getCitizenId())).append("\",")
                .append("\"email\":\"").append(escapeJson(user.getEmail())).append("\",")
                .append("\"mobile\":\"").append(escapeJson(user.getMobile())).append("\",")
                .append("\"emergencyContact\":\"").append(escapeJson(user.getEmergencyContact())).append("\",")
                .append("\"address\":\"").append(escapeJson(user.getAddress() != null ? user.getAddress() : "")).append("\"")
                .append("}")
                .append("}");
            response.getWriter().write(json.toString());
        } else {
            response.getWriter().write("{\"authenticated\":false}");
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

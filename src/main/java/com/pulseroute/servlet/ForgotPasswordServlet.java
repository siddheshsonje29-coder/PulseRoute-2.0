package com.pulseroute.servlet;

import com.pulseroute.dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Handles Password Reset with Citizen verification.
 */
@WebServlet(name = "ForgotPasswordServlet", urlPatterns = {"/forgot-password"})
public class ForgotPasswordServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private UserDAO userDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        this.userDAO = new UserDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/index.html?auth=forgot");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String citizenId = request.getParameter("citizenId");
        String email = request.getParameter("email");
        String emergencyContact = request.getParameter("emergencyContact");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        String acceptHeader = request.getHeader("Accept");
        String requestedWith = request.getHeader("X-Requested-With");
        boolean isAjax = (acceptHeader != null && acceptHeader.contains("application/json")) ||
                         "XMLHttpRequest".equalsIgnoreCase(requestedWith);

        if (citizenId == null || email == null || emergencyContact == null || newPassword == null ||
            citizenId.trim().isEmpty() || email.trim().isEmpty() || emergencyContact.trim().isEmpty() || newPassword.trim().isEmpty()) {
            sendError(request, response, isAjax, "Please fill in all verification fields.", "missing_fields");
            return;
        }

        citizenId = citizenId.trim();
        email = email.trim().toLowerCase();
        emergencyContact = emergencyContact.trim().replaceAll("[\\s-]", "");

        if (newPassword.length() < 6) {
            sendError(request, response, isAjax, "Password must be at least 6 characters.", "length");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            sendError(request, response, isAjax, "Passwords do not match.", "mismatch");
            return;
        }

        boolean resetSuccess = userDAO.resetPasswordWithVerification(citizenId, email, emergencyContact, newPassword);

        if (resetSuccess) {
            String successMsg = "Password updated successfully. Please login with your new credentials.";
            if (isAjax) {
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":true,\"message\":\"" + successMsg + "\",\"redirect\":\"" + request.getContextPath() + "/index.html?auth=login&msg=password_reset\"}");
            } else {
                String encoded = URLEncoder.encode(successMsg, StandardCharsets.UTF_8);
                response.sendRedirect(request.getContextPath() + "/index.html?auth=login&msg=password_reset&text=" + encoded);
            }
        } else {
            sendError(request, response, isAjax, "Identity verification failed. Please check Citizen ID, registered email, and emergency contact.", "verification_failed");
        }
    }

    private void sendError(HttpServletRequest req, HttpServletResponse resp, boolean isAjax, String msg, String code) throws IOException {
        if (isAjax) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"success\":false,\"error\":\"" + code + "\",\"message\":\"" + msg + "\"}");
        } else {
            String encoded = URLEncoder.encode(msg, StandardCharsets.UTF_8);
            resp.sendRedirect(req.getContextPath() + "/index.html?auth=forgot&error=" + code + "&msg=" + encoded);
        }
    }
}

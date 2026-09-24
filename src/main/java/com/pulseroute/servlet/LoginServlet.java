package com.pulseroute.servlet;

import com.pulseroute.dao.UserDAO;
import com.pulseroute.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Handles Citizen Authentication and Session initialization.
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {
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
        // Redirect to single unified index.html
        response.sendRedirect(request.getContextPath() + "/index.html");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String identifier = request.getParameter("identifier");
        if (identifier == null || identifier.trim().isEmpty()) {
            identifier = request.getParameter("loginId");
        }
        String password = request.getParameter("password");

        // Basic input validation
        if (identifier == null || identifier.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            String errorMsg = URLEncoder.encode("Please enter both Citizen ID/Email and Password.", StandardCharsets.UTF_8);
            response.sendRedirect(request.getContextPath() + "/index.html?error=missing_fields&msg=" + errorMsg);
            return;
        }

        identifier = identifier.trim();
        password = password.trim();

        // Verify credentials with BCrypt via UserDAO
        User authenticatedUser = userDAO.verifyCredentials(identifier, password);

        if (authenticatedUser != null) {
            // Success: establish session
            HttpSession session = request.getSession(true);
            session.setMaxInactiveInterval(30 * 60); // 30 minutes session timeout
            session.setAttribute("user", authenticatedUser);
            session.setAttribute("citizenId", authenticatedUser.getCitizenId());
            session.setAttribute("citizenName", authenticatedUser.getFullName());
            session.setAttribute("citizenEmail", authenticatedUser.getEmail());

            // Handle AJAX vs standard Form submission
            String acceptHeader = request.getHeader("Accept");
            String requestedWith = request.getHeader("X-Requested-With");
            boolean isAjax = (acceptHeader != null && acceptHeader.contains("application/json")) ||
                             "XMLHttpRequest".equalsIgnoreCase(requestedWith);

            if (isAjax) {
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":true,\"user\":{\"id\":" + authenticatedUser.getId() +
                    ",\"fullName\":\"" + authenticatedUser.getFullName() +
                    "\",\"email\":\"" + authenticatedUser.getEmail() +
                    "\",\"citizenId\":\"" + authenticatedUser.getCitizenId() +
                    "\",\"mobile\":\"" + authenticatedUser.getMobile() +
                    "\",\"emergencyContact\":\"" + (authenticatedUser.getEmergencyContact() != null ? authenticatedUser.getEmergencyContact() : "") +
                    "\",\"address\":\"" + (authenticatedUser.getAddress() != null ? authenticatedUser.getAddress().replace("\"", "\\\"") : "") +
                    "\"},\"redirect\":\"" + request.getContextPath() + "/index.html\"}");
            } else {
                response.sendRedirect(request.getContextPath() + "/index.html");
            }
        } else {
            // Failure: return unauthorized error
            String acceptHeader = request.getHeader("Accept");
            String requestedWith = request.getHeader("X-Requested-With");
            boolean isAjax = (acceptHeader != null && acceptHeader.contains("application/json")) ||
                             "XMLHttpRequest".equalsIgnoreCase(requestedWith);

            if (isAjax) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Invalid Citizen ID/Email or Password.\"}");
            } else {
                response.sendRedirect(request.getContextPath() + "/index.html?error=invalid");
            }
        }
    }
}

package com.pulseroute.servlet;

import com.pulseroute.dao.UserDAO;
import com.pulseroute.model.User;
import com.pulseroute.util.BCrypt;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Handles Citizen Registration with BCrypt hashing and duplicate checks.
 */
@WebServlet(name = "SignupServlet", urlPatterns = {"/signup"})
public class SignupServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final Pattern EMAIL_REGEX =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern MOBILE_REGEX =
            Pattern.compile("^[0-9]{10,15}$");

    private UserDAO userDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        this.userDAO = new UserDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/index.html?auth=signup");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String mobile = request.getParameter("mobile");
        String citizenId = request.getParameter("citizenId");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        String emergencyContact = request.getParameter("emergencyContact");
        String address = request.getParameter("address");

        String acceptHeader = request.getHeader("Accept");
        String requestedWith = request.getHeader("X-Requested-With");
        boolean isAjax = (acceptHeader != null && acceptHeader.contains("application/json")) ||
                         "XMLHttpRequest".equalsIgnoreCase(requestedWith);

        // 1. Validate required fields
        if (isEmpty(fullName) || isEmpty(email) || isEmpty(mobile) ||
            isEmpty(citizenId) || isEmpty(password) || isEmpty(emergencyContact)) {
            sendError(request, response, isAjax, "Please fill in all required fields.", "missing_fields");
            return;
        }

        fullName = fullName.trim();
        email = email.trim().toLowerCase();
        mobile = mobile.trim().replaceAll("[\\s-]", "");
        citizenId = citizenId.trim().toUpperCase();
        emergencyContact = emergencyContact.trim().replaceAll("[\\s-]", "");
        if (address != null) address = address.trim();

        // 2. Validate email format
        if (!EMAIL_REGEX.matcher(email).matches()) {
            sendError(request, response, isAjax, "Invalid email address format.", "invalid_email");
            return;
        }

        // 3. Validate mobile numbers
        if (!MOBILE_REGEX.matcher(mobile).matches()) {
            sendError(request, response, isAjax, "Mobile number must be 10 to 15 digits.", "invalid_mobile");
            return;
        }
        if (!MOBILE_REGEX.matcher(emergencyContact).matches()) {
            sendError(request, response, isAjax, "Emergency contact must be 10 to 15 digits.", "invalid_emergency_contact");
            return;
        }

        // 4. Validate password complexity and confirmation
        if (password.length() < 6) {
            sendError(request, response, isAjax, "Password must be at least 6 characters long.", "password_too_short");
            return;
        }
        if (!password.equals(confirmPassword)) {
            sendError(request, response, isAjax, "Passwords do not match.", "password_mismatch");
            return;
        }

        // 5. Prevent duplicate email registration
        if (userDAO.findByEmail(email) != null) {
            sendError(request, response, isAjax, "An account with this email already exists.", "duplicate_email");
            return;
        }

        // 6. Prevent duplicate citizen ID registration
        if (userDAO.findByCitizenId(citizenId) != null) {
            sendError(request, response, isAjax, "Citizen ID already registered in the system.", "duplicate_citizen_id");
            return;
        }

        // 7. Hash password using BCrypt
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));

        // 8. Construct user model and save
        User newUser = new User(fullName, email, mobile, citizenId, hashedPassword, emergencyContact, address);
        boolean saved = userDAO.save(newUser);

        if (saved) {
            if (isAjax) {
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":true,\"message\":\"Account created successfully. Please login.\",\"redirect\":\"" + request.getContextPath() + "/index.html?auth=login&msg=registered\"}");
            } else {
                response.sendRedirect(request.getContextPath() + "/index.html?auth=login&msg=registered");
            }
        } else {
            sendError(request, response, isAjax, "Unable to register citizen account due to a database error. Please try again.", "db_error");
        }
    }

    private void sendError(HttpServletRequest req, HttpServletResponse resp, boolean isAjax, String msg, String code) throws IOException {
        if (isAjax) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"success\":false,\"error\":\"" + code + "\",\"message\":\"" + msg + "\"}");
        } else {
            String encoded = URLEncoder.encode(msg, StandardCharsets.UTF_8);
            resp.sendRedirect(req.getContextPath() + "/index.html?auth=signup&error=" + code + "&msg=" + encoded);
        }
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}

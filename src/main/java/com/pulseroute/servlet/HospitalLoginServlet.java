package com.pulseroute.servlet;

import com.pulseroute.dao.HospitalDAO;
import com.pulseroute.model.Hospital;
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
 * Handles Hospital Admin Authentication and Session initialization.
 */
@WebServlet(name = "HospitalLoginServlet", urlPatterns = {"/hospital/login"})
public class HospitalLoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private HospitalDAO hospitalDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        this.hospitalDAO = new HospitalDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/hospital-login.html");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String identifier = request.getParameter("identifier");
        if (identifier == null || identifier.trim().isEmpty()) {
            identifier = request.getParameter("hospitalCode");
        }
        if (identifier == null || identifier.trim().isEmpty()) {
            identifier = request.getParameter("email");
        }
        String password = request.getParameter("password");

        String acceptHeader = request.getHeader("Accept");
        String requestedWith = request.getHeader("X-Requested-With");
        boolean isAjax = (acceptHeader != null && acceptHeader.contains("application/json")) ||
                         "XMLHttpRequest".equalsIgnoreCase(requestedWith);

        if (identifier == null || identifier.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            if (isAjax) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Please enter Hospital Code/Email and Password.\"}");
            } else {
                String errorMsg = URLEncoder.encode("Please enter Hospital Code/Email and Password.", StandardCharsets.UTF_8);
                response.sendRedirect(request.getContextPath() + "/hospital-login.html?error=missing&msg=" + errorMsg);
            }
            return;
        }

        identifier = identifier.trim();
        password = password.trim();

        Hospital hospital = hospitalDAO.verifyCredentials(identifier, password);

        if (hospital != null) {
            HttpSession session = request.getSession(true);
            session.setMaxInactiveInterval(60 * 60); // 1 hour session
            session.setAttribute("hospital", hospital);
            session.setAttribute("hospitalId", hospital.getHospitalId());
            session.setAttribute("hospitalCode", hospital.getHospitalCode());
            session.setAttribute("hospitalName", hospital.getHospitalName());

            if (isAjax) {
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":true,\"redirect\":\"" + request.getContextPath() + "/hospital-dashboard.html\",\"hospital\":{" +
                    "\"hospitalId\":" + hospital.getHospitalId() + "," +
                    "\"hospitalCode\":\"" + escapeJson(hospital.getHospitalCode()) + "\"," +
                    "\"hospitalName\":\"" + escapeJson(hospital.getHospitalName()) + "\"," +
                    "\"email\":\"" + escapeJson(hospital.getEmail()) + "\"," +
                    "\"location\":\"" + escapeJson(hospital.getLocation()) + "\"," +
                    "\"contact\":\"" + escapeJson(hospital.getContact()) + "\"" +
                    "}}");
            } else {
                response.sendRedirect(request.getContextPath() + "/hospital-dashboard.html");
            }
        } else {
            if (isAjax) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Invalid Hospital Code/Email or Password.\"}");
            } else {
                String errorMsg = URLEncoder.encode("Invalid Hospital Code/Email or Password.", StandardCharsets.UTF_8);
                response.sendRedirect(request.getContextPath() + "/hospital-login.html?error=auth&msg=" + errorMsg);
            }
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}

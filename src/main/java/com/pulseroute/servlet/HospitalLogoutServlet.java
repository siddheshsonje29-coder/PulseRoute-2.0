package com.pulseroute.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Handles Hospital Admin Logout and session termination.
 */
@WebServlet(name = "HospitalLogoutServlet", urlPatterns = {"/hospital/logout"})
public class HospitalLogoutServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processLogout(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processLogout(request, response);
    }

    private void processLogout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute("hospital");
            session.removeAttribute("hospitalId");
            session.removeAttribute("hospitalCode");
            session.removeAttribute("hospitalName");
            session.invalidate();
        }

        String acceptHeader = request.getHeader("Accept");
        String requestedWith = request.getHeader("X-Requested-With");
        boolean isAjax = (acceptHeader != null && acceptHeader.contains("application/json")) ||
                         "XMLHttpRequest".equalsIgnoreCase(requestedWith);

        if (isAjax) {
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":true,\"redirect\":\"" + request.getContextPath() + "/hospital-login.html\"}");
        } else {
            response.sendRedirect(request.getContextPath() + "/hospital-login.html?logged_out=1");
        }
    }
}

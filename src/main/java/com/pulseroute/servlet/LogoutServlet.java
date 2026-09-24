package com.pulseroute.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Handles Session Invalidation and Citizen Logout.
 */
@WebServlet(name = "LogoutServlet", urlPatterns = {"/logout"})
public class LogoutServlet extends HttpServlet {
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
            session.removeAttribute("user");
            session.removeAttribute("citizenId");
            session.removeAttribute("citizenName");
            session.removeAttribute("citizenEmail");
            session.invalidate();
        }

        String acceptHeader = request.getHeader("Accept");
        String requestedWith = request.getHeader("X-Requested-With");
        boolean isAjax = (acceptHeader != null && acceptHeader.contains("application/json")) ||
                         "XMLHttpRequest".equalsIgnoreCase(requestedWith);

        if (isAjax) {
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":true,\"redirect\":\"" + request.getContextPath() + "/index.html?auth=login&msg=logged_out\"}");
        } else {
            response.sendRedirect(request.getContextPath() + "/index.html?auth=login&msg=logged_out");
        }
    }
}

package com.pulseroute.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Filter that protects citizen portal and hospital administration portals
 * with strict role-based access control.
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = {"/*"})
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String path = uri.substring(contextPath.length());

        // Normalize path
        if (path.isEmpty()) {
            path = "/";
        }

        // 1. Allow public paths and static assets
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);

        // 2. Hospital Admin Routes Protection
        if (isHospitalAdminPath(path)) {
            boolean isHospitalAdmin = (session != null && session.getAttribute("hospital") != null);
            if (isHospitalAdmin) {
                chain.doFilter(request, response);
            } else {
                // Deny access to normal users and unauthenticated users
                if (path.startsWith("/hospital/")) {
                    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    httpResponse.setContentType("application/json");
                    httpResponse.getWriter().write("{\"success\":false,\"message\":\"Unauthorized: Hospital administrator access required.\"}");
                } else {
                    httpResponse.sendRedirect(contextPath + "/hospital-login.html?error=unauthorized");
                }
            }
            return;
        }

        // 3. Citizen Emergency API Access - Emergency SOS calls must never be blocked
        if (path.startsWith("/emergency/")) {
            chain.doFilter(request, response);
            return;
        }

        // 4. Default Protected Citizen Pages
        boolean isCitizenLoggedIn = (session != null && session.getAttribute("user") != null);
        if (isCitizenLoggedIn) {
            chain.doFilter(request, response);
        } else {
            httpResponse.sendRedirect(contextPath + "/index.html");
        }
    }

    private boolean isPublicPath(String path) {
        String lower = path.toLowerCase();
        return lower.equals("/") ||
               lower.equals("/index.html") ||
               lower.equals("/login.html") ||
               lower.equals("/signup.html") ||
               lower.equals("/forgot-password.html") ||
               lower.equals("/hospital-login.html") ||
               lower.equals("/hospital/login") ||
               lower.equals("/hospital/logout") ||
               lower.startsWith("/login") ||
               lower.startsWith("/signup") ||
               lower.startsWith("/logout") ||
               lower.startsWith("/forgot-password") ||
               lower.startsWith("/emergency/") ||
               lower.startsWith("/api/session") ||
               lower.startsWith("/hospital/session") ||
               lower.endsWith(".css") ||
               lower.endsWith(".js") ||
               lower.endsWith(".png") ||
               lower.endsWith(".jpg") ||
               lower.endsWith(".jpeg") ||
               lower.endsWith(".svg") ||
               lower.endsWith(".ico") ||
               lower.endsWith(".woff") ||
               lower.endsWith(".woff2") ||
               lower.endsWith(".ttf") ||
               lower.startsWith("/css/") ||
               lower.startsWith("/js/") ||
               lower.startsWith("/images/");
    }

    private boolean isHospitalAdminPath(String path) {
        String lower = path.toLowerCase();
        return lower.equals("/hospital-dashboard.html") ||
               (lower.startsWith("/hospital/") && 
                !lower.startsWith("/hospital/login") && 
                !lower.startsWith("/hospital/logout") && 
                !lower.startsWith("/hospital/session"));
    }

    @Override
    public void destroy() {
    }
}

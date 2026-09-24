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
import java.util.Map;

/**
 * Returns Hospital Admin session state and stats.
 */
@WebServlet(name = "HospitalSessionServlet", urlPatterns = {"/hospital/session"})
public class HospitalSessionServlet extends HttpServlet {
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
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("hospital") != null) {
            Hospital hospital = (Hospital) session.getAttribute("hospital");
            // refresh data from DB
            Hospital fresh = hospitalDAO.findById(hospital.getHospitalId());
            if (fresh != null) hospital = fresh;

            Map<String, Integer> stats = hospitalDAO.getHospitalStats(hospital.getHospitalId());

            StringBuilder json = new StringBuilder();
            json.append("{")
                .append("\"authenticated\":true,")
                .append("\"hospital\":{")
                .append("\"hospitalId\":").append(hospital.getHospitalId()).append(",")
                .append("\"hospitalCode\":\"").append(escapeJson(hospital.getHospitalCode())).append("\",")
                .append("\"hospitalName\":\"").append(escapeJson(hospital.getHospitalName())).append("\",")
                .append("\"email\":\"").append(escapeJson(hospital.getEmail())).append("\",")
                .append("\"location\":\"").append(escapeJson(hospital.getLocation())).append("\",")
                .append("\"contact\":\"").append(escapeJson(hospital.getContact())).append("\",")
                .append("\"status\":\"").append(escapeJson(hospital.getStatus())).append("\"")
                .append("},")
                .append("\"stats\":{")
                .append("\"totalAmbulances\":").append(stats.getOrDefault("totalAmbulances", 0)).append(",")
                .append("\"availableAmbulances\":").append(stats.getOrDefault("availableAmbulances", 0)).append(",")
                .append("\"assignedAmbulances\":").append(stats.getOrDefault("assignedAmbulances", 0)).append(",")
                .append("\"onTripAmbulances\":").append(stats.getOrDefault("onTripAmbulances", 0)).append(",")
                .append("\"offlineAmbulances\":").append(stats.getOrDefault("offlineAmbulances", 0)).append(",")
                .append("\"authorizedAmbulances\":").append(stats.getOrDefault("authorizedAmbulances", 0)).append(",")
                .append("\"pendingRequests\":").append(stats.getOrDefault("pendingRequests", 0)).append(",")
                .append("\"activeDispatches\":").append(stats.getOrDefault("activeDispatches", 0))
                .append("}")
                .append("}");

            response.getWriter().write(json.toString());
        } else {
            response.getWriter().write("{\"authenticated\":false}");
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}

package com.pulseroute.servlet;

import com.pulseroute.dao.AmbulanceDAO;
import com.pulseroute.model.Ambulance;
import com.pulseroute.model.Hospital;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

/**
 * Handles Hospital Ambulance fleet operations:
 * - /hospital/ambulances (GET)
 * - /hospital/ambulances/add (POST)
 * - /hospital/ambulances/update (POST)
 * - /hospital/ambulances/authorize (POST)
 */
@WebServlet(name = "HospitalAmbulanceServlet", urlPatterns = {
    "/hospital/ambulances",
    "/hospital/ambulances/add",
    "/hospital/ambulances/update",
    "/hospital/ambulances/authorize"
})
public class HospitalAmbulanceServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private AmbulanceDAO ambulanceDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        this.ambulanceDAO = new AmbulanceDAO();
    }

    private Hospital getAuthenticatedHospital(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("hospital") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Hospital admin authentication required.\"}");
            return null;
        }
        return (Hospital) session.getAttribute("hospital");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Hospital hospital = getAuthenticatedHospital(request, response);
        if (hospital == null) return;

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        boolean dispatchableOnly = "true".equalsIgnoreCase(request.getParameter("dispatchable"));
        List<Ambulance> list = dispatchableOnly ? 
                ambulanceDAO.findDispatchableAmbulances(hospital.getHospitalId()) : 
                ambulanceDAO.findByHospitalId(hospital.getHospitalId());

        StringBuilder json = new StringBuilder();
        json.append("{\"success\":true,\"ambulances\":[");
        for (int i = 0; i < list.size(); i++) {
            Ambulance a = list.get(i);
            if (i > 0) json.append(",");
            json.append("{")
                .append("\"ambulanceId\":").append(a.getAmbulanceId()).append(",")
                .append("\"hospitalId\":").append(a.getHospitalId()).append(",")
                .append("\"vehicleNumber\":\"").append(escapeJson(a.getVehicleNumber())).append("\",")
                .append("\"driverName\":\"").append(escapeJson(a.getDriverName())).append("\",")
                .append("\"driverContact\":\"").append(escapeJson(a.getDriverContact())).append("\",")
                .append("\"status\":\"").append(escapeJson(a.getStatus())).append("\",")
                .append("\"authorizationStatus\":\"").append(escapeJson(a.getAuthorizationStatus())).append("\",")
                .append("\"currentLocation\":\"").append(escapeJson(a.getCurrentLocation())).append("\",")
                .append("\"isDispatchable\":").append(a.isDispatchable())
                .append("}");
        }
        json.append("]}");
        response.getWriter().write(json.toString());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Hospital hospital = getAuthenticatedHospital(request, response);
        if (hospital == null) return;

        String path = request.getServletPath();

        if ("/hospital/ambulances/add".equals(path)) {
            handleAddAmbulance(request, response, hospital);
        } else if ("/hospital/ambulances/update".equals(path)) {
            handleUpdateStatus(request, response, hospital);
        } else if ("/hospital/ambulances/authorize".equals(path)) {
            handleAuthorize(request, response, hospital);
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Unknown action.\"}");
        }
    }

    private void handleAddAmbulance(HttpServletRequest request, HttpServletResponse response, Hospital hospital) throws IOException {
        String vehicleNumber = request.getParameter("vehicleNumber");
        String driverName = request.getParameter("driverName");
        String driverContact = request.getParameter("driverContact");
        String currentLocation = request.getParameter("currentLocation");
        String status = request.getParameter("status");
        String authorizationStatus = request.getParameter("authorizationStatus");

        if (vehicleNumber == null || vehicleNumber.trim().isEmpty() ||
            driverName == null || driverName.trim().isEmpty() ||
            driverContact == null || driverContact.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Vehicle number, driver name, and driver contact are required.\"}");
            return;
        }

        Ambulance amb = new Ambulance();
        amb.setHospitalId(hospital.getHospitalId());
        amb.setVehicleNumber(vehicleNumber.trim().toUpperCase());
        amb.setDriverName(driverName.trim());
        amb.setDriverContact(driverContact.trim());
        amb.setCurrentLocation(currentLocation != null && !currentLocation.trim().isEmpty() ? currentLocation.trim() : hospital.getLocation());
        amb.setStatus(status != null && !status.trim().isEmpty() ? status.trim().toUpperCase() : "AVAILABLE");
        amb.setAuthorizationStatus(authorizationStatus != null && !authorizationStatus.trim().isEmpty() ? authorizationStatus.trim().toUpperCase() : "AUTHORIZED");

        boolean created = ambulanceDAO.addAmbulance(amb);
        if (created) {
            response.getWriter().write("{\"success\":true,\"message\":\"Ambulance registered successfully.\",\"ambulanceId\":" + amb.getAmbulanceId() + "}");
        } else {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.getWriter().write("{\"success\":false,\"message\":\"Could not register ambulance. Vehicle number may already exist.\"}");
        }
    }

    private void handleUpdateStatus(HttpServletRequest request, HttpServletResponse response, Hospital hospital) throws IOException {
        String ambIdStr = request.getParameter("ambulanceId");
        String status = request.getParameter("status");

        if (ambIdStr == null || status == null || ambIdStr.trim().isEmpty() || status.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Ambulance ID and new status are required.\"}");
            return;
        }

        try {
            int ambId = Integer.parseInt(ambIdStr.trim());
            String newStatus = status.trim().toUpperCase();
            if (!newStatus.equals("AVAILABLE") && !newStatus.equals("ASSIGNED") && !newStatus.equals("ON_TRIP") && !newStatus.equals("OFFLINE")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false,\"message\":\"Invalid status. Must be AVAILABLE, ASSIGNED, ON_TRIP, or OFFLINE.\"}");
                return;
            }

            boolean updated = ambulanceDAO.updateStatus(ambId, hospital.getHospitalId(), newStatus);
            if (updated) {
                response.getWriter().write("{\"success\":true,\"message\":\"Ambulance status updated to " + newStatus + ".\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"success\":false,\"message\":\"Ambulance not found or not owned by your hospital.\"}");
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Invalid Ambulance ID format.\"}");
        }
    }

    private void handleAuthorize(HttpServletRequest request, HttpServletResponse response, Hospital hospital) throws IOException {
        String ambIdStr = request.getParameter("ambulanceId");
        String authStatus = request.getParameter("authorizationStatus");

        if (ambIdStr == null || authStatus == null || ambIdStr.trim().isEmpty() || authStatus.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Ambulance ID and authorization status are required.\"}");
            return;
        }

        try {
            int ambId = Integer.parseInt(ambIdStr.trim());
            String newAuth = authStatus.trim().toUpperCase();
            if (!newAuth.equals("AUTHORIZED") && !newAuth.equals("PENDING") && !newAuth.equals("REVOKED")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false,\"message\":\"Invalid authorization status. Must be AUTHORIZED, PENDING, or REVOKED.\"}");
                return;
            }

            boolean updated = ambulanceDAO.updateAuthorization(ambId, hospital.getHospitalId(), newAuth);
            if (updated) {
                response.getWriter().write("{\"success\":true,\"message\":\"Ambulance authorization updated to " + newAuth + ".\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"success\":false,\"message\":\"Ambulance not found or not owned by your hospital.\"}");
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Invalid Ambulance ID format.\"}");
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}

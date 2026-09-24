package com.pulseroute.servlet;

import com.pulseroute.dao.AmbulanceDAO;
import com.pulseroute.dao.EmergencyRequestDAO;
import com.pulseroute.model.Ambulance;
import com.pulseroute.model.EmergencyRequest;
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
 * Handles Hospital Emergency Request Management:
 * - /hospital/emergencies (GET)
 * - /hospital/emergency/approve (POST)
 * - /hospital/emergency/reject (POST)
 * - /hospital/emergency/assign (POST)
 * - /hospital/emergency/status-update (POST)
 */
@WebServlet(name = "HospitalEmergencyServlet", urlPatterns = {
    "/hospital/emergencies",
    "/hospital/emergency/approve",
    "/hospital/emergency/reject",
    "/hospital/emergency/assign",
    "/hospital/emergency/status-update"
})
public class HospitalEmergencyServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private EmergencyRequestDAO emergencyDAO;
    private AmbulanceDAO ambulanceDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        this.emergencyDAO = new EmergencyRequestDAO();
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

        List<EmergencyRequest> requests = emergencyDAO.getRequestsForHospital(hospital.getHospitalId());

        StringBuilder listJson = new StringBuilder();
        listJson.append("[");
        for (int i = 0; i < requests.size(); i++) {
            EmergencyRequest r = requests.get(i);
            if (i > 0) listJson.append(",");
            listJson.append("{")
                .append("\"requestId\":").append(r.getRequestId()).append(",")
                .append("\"userId\":").append(r.getUserId() != null ? r.getUserId() : "null").append(",")
                .append("\"userName\":\"").append(escapeJson(r.getUserName() != null ? r.getUserName() : "Anonymous Citizen")).append("\",")
                .append("\"userMobile\":\"").append(escapeJson(r.getUserMobile() != null ? r.getUserMobile() : "N/A")).append("\",")
                .append("\"userEmail\":\"").append(escapeJson(r.getUserEmail() != null ? r.getUserEmail() : "")).append("\",")
                .append("\"userEmergencyContact\":\"").append(escapeJson(r.getUserEmergencyContact() != null ? r.getUserEmergencyContact() : "")).append("\",")
                .append("\"emergencyType\":\"").append(escapeJson(r.getEmergencyType())).append("\",")
                .append("\"userLocation\":\"").append(escapeJson(r.getUserLocation())).append("\",")
                .append("\"status\":\"").append(escapeJson(r.getStatus())).append("\",")
                .append("\"createdAt\":\"").append(r.getCreatedAt() != null ? r.getCreatedAt().toString() : "").append("\",")
                .append("\"hospitalId\":").append(r.getHospitalId() != null ? r.getHospitalId() : "null").append(",")
                .append("\"hospitalName\":\"").append(escapeJson(r.getHospitalName() != null ? r.getHospitalName() : "")).append("\",")
                .append("\"ambulanceId\":").append(r.getAmbulanceId() != null ? r.getAmbulanceId() : "null").append(",")
                .append("\"vehicleNumber\":\"").append(escapeJson(r.getVehicleNumber() != null ? r.getVehicleNumber() : "")).append("\",")
                .append("\"driverName\":\"").append(escapeJson(r.getDriverName() != null ? r.getDriverName() : "")).append("\",")
                .append("\"driverContact\":\"").append(escapeJson(r.getDriverContact() != null ? r.getDriverContact() : "")).append("\",")
                .append("\"assignedAt\":\"").append(r.getAssignedAt() != null ? r.getAssignedAt().toString() : "").append("\",")
                .append("\"completedAt\":\"").append(r.getCompletedAt() != null ? r.getCompletedAt().toString() : "").append("\",")
                .append("\"rejectionReason\":\"").append(escapeJson(r.getRejectionReason() != null ? r.getRejectionReason() : "")).append("\"")
                .append("}");
        }
        listJson.append("]");

        response.getWriter().write("{\"success\":true,\"requests\":" + listJson.toString() + ",\"emergencies\":" + listJson.toString() + "}");
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

        if ("/hospital/emergency/approve".equals(path)) {
            handleApprove(request, response, hospital);
        } else if ("/hospital/emergency/reject".equals(path)) {
            handleReject(request, response, hospital);
        } else if ("/hospital/emergency/assign".equals(path)) {
            handleAssign(request, response, hospital);
        } else if ("/hospital/emergency/status-update".equals(path)) {
            handleStatusUpdate(request, response, hospital);
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Unknown emergency action.\"}");
        }
    }

    private void handleApprove(HttpServletRequest request, HttpServletResponse response, Hospital hospital) throws IOException {
        String reqIdStr = request.getParameter("requestId");
        if (reqIdStr == null || reqIdStr.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Request ID is required.\"}");
            return;
        }

        try {
            int requestId = Integer.parseInt(reqIdStr.trim());
            boolean approved = emergencyDAO.approveRequest(requestId, hospital.getHospitalId());
            if (approved) {
                response.getWriter().write("{\"success\":true,\"message\":\"Emergency request approved by " + escapeJson(hospital.getHospitalName()) + ".\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false,\"message\":\"Request could not be approved. It may already be processed or cancelled.\"}");
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Invalid Request ID format.\"}");
        }
    }

    private void handleReject(HttpServletRequest request, HttpServletResponse response, Hospital hospital) throws IOException {
        String reqIdStr = request.getParameter("requestId");
        String reason = request.getParameter("reason");

        if (reqIdStr == null || reqIdStr.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Request ID is required.\"}");
            return;
        }

        try {
            int requestId = Integer.parseInt(reqIdStr.trim());
            boolean rejected = emergencyDAO.rejectRequest(requestId, hospital.getHospitalId(), reason);
            if (rejected) {
                response.getWriter().write("{\"success\":true,\"message\":\"Emergency request rejected.\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false,\"message\":\"Request could not be rejected.\"}");
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Invalid Request ID format.\"}");
        }
    }

    private void handleAssign(HttpServletRequest request, HttpServletResponse response, Hospital hospital) throws IOException {
        String reqIdStr = request.getParameter("requestId");
        String ambIdStr = request.getParameter("ambulanceId");

        if (reqIdStr == null || ambIdStr == null || reqIdStr.trim().isEmpty() || ambIdStr.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Both Request ID and Ambulance ID are required.\"}");
            return;
        }

        try {
            int requestId = Integer.parseInt(reqIdStr.trim());
            int ambulanceId = Integer.parseInt(ambIdStr.trim());

            // Server-side validation check
            Ambulance amb = ambulanceDAO.findById(ambulanceId);
            if (amb == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"success\":false,\"message\":\"Ambulance not found.\"}");
                return;
            }

            if (amb.getHospitalId() != hospital.getHospitalId()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"success\":false,\"message\":\"Security Violation: Ambulance does not belong to your hospital.\"}");
                return;
            }

            if (!"AUTHORIZED".equalsIgnoreCase(amb.getAuthorizationStatus())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"success\":false,\"message\":\"Ambulance authorization is " + amb.getAuthorizationStatus() + ". Only AUTHORIZED units can be dispatched.\"}");
                return;
            }

            if (!"AVAILABLE".equalsIgnoreCase(amb.getStatus())) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.getWriter().write("{\"success\":false,\"message\":\"Ambulance is currently " + amb.getStatus() + " and cannot be dispatched.\"}");
                return;
            }

            boolean assigned = emergencyDAO.assignAmbulance(requestId, hospital.getHospitalId(), ambulanceId);
            if (assigned) {
                response.getWriter().write("{\"success\":true,\"message\":\"Ambulance " + escapeJson(amb.getVehicleNumber()) + " successfully assigned.\",\"vehicleNumber\":\"" + escapeJson(amb.getVehicleNumber()) + "\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.getWriter().write("{\"success\":false,\"message\":\"Assignment failed. Ensure the request is active and the ambulance is available.\"}");
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Invalid ID format.\"}");
        }
    }

    private void handleStatusUpdate(HttpServletRequest request, HttpServletResponse response, Hospital hospital) throws IOException {
        String reqIdStr = request.getParameter("requestId");
        String status = request.getParameter("status");

        if (reqIdStr == null || status == null || reqIdStr.trim().isEmpty() || status.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Request ID and new status are required.\"}");
            return;
        }

        try {
            int requestId = Integer.parseInt(reqIdStr.trim());
            String newStatus = status.trim().toUpperCase();

            boolean updated = emergencyDAO.updateStatus(requestId, hospital.getHospitalId(), newStatus);
            if (updated) {
                response.getWriter().write("{\"success\":true,\"message\":\"Request status updated to " + newStatus + ".\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false,\"message\":\"Could not update status. Verify request permissions.\"}");
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Invalid Request ID format.\"}");
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}

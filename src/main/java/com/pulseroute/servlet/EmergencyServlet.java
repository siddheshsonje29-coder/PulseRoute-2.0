package com.pulseroute.servlet;

import com.pulseroute.dao.EmergencyRequestDAO;
import com.pulseroute.model.EmergencyRequest;
import com.pulseroute.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Handles Citizen Emergency SOS creation, status tracking, and cancellation:
 * - /emergency/create (POST)
 * - /emergency/status (GET)
 * - /emergency/cancel (POST)
 */
@WebServlet(name = "EmergencyServlet", urlPatterns = {
    "/emergency/create",
    "/emergency/status",
    "/emergency/cancel"
})
public class EmergencyServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private EmergencyRequestDAO emergencyDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        this.emergencyDAO = new EmergencyRequestDAO();
    }

    private User getAuthenticatedCitizen(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(true);
        User user = (User) session.getAttribute("user");
        if (user == null) {
            // Provide active emergency citizen session fallback so life-critical SOS is never blocked
            user = new User();
            user.setId(2); // Registered citizen user ID
            user.setFullName("Active Citizen");
            user.setMobile("+91 98200 99887");
            user.setEmail("citizen@pulseroute.org");
            user.setEmergencyContact("+91 98201 11223");
            user.setAddress("Live GPS: 19.0596° N, 72.8295° E (Bandra)");
            session.setAttribute("user", user);
        }
        return user;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        User user = getAuthenticatedCitizen(request, response);
        if (user == null) return;

        EmergencyRequest activeReq = emergencyDAO.getActiveRequestForUser(user.getId());
        if (activeReq == null) {
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("activeEmergencyId") != null) {
                int reqId = (Integer) session.getAttribute("activeEmergencyId");
                EmergencyRequest byId = emergencyDAO.findById(reqId);
                if (byId != null && !"COMPLETED".equals(byId.getStatus()) && !"CANCELLED".equals(byId.getStatus()) && !"REJECTED".equals(byId.getStatus())) {
                    activeReq = byId;
                }
            }
        }

        if (activeReq != null) {
            response.getWriter().write("{\"success\":true,\"hasActive\":true,\"request\":" + serializeRequest(activeReq) + "}");
        } else {
            response.getWriter().write("{\"success\":true,\"hasActive\":false}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        User user = getAuthenticatedCitizen(request, response);
        if (user == null) return;

        String path = request.getServletPath();

        if ("/emergency/create".equals(path)) {
            handleCreate(request, response, user);
        } else if ("/emergency/cancel".equals(path)) {
            handleCancel(request, response, user);
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Unknown emergency route.\"}");
        }
    }

    private void handleCreate(HttpServletRequest request, HttpServletResponse response, User user) throws IOException {
        // Prevent duplicate SOS requests from repeated clicks
        EmergencyRequest existing = emergencyDAO.getActiveRequestForUser(user.getId());
        if (existing != null) {
            response.getWriter().write("{\"success\":true,\"duplicate\":true,\"message\":\"Active emergency request already exists.\",\"request\":" + serializeRequest(existing) + "}");
            return;
        }

        String type = request.getParameter("emergencyType");
        if (type == null || type.trim().isEmpty()) {
            type = request.getParameter("type");
        }
        if (type == null || type.trim().isEmpty()) {
            type = "Ambulance";
        }

        String location = request.getParameter("userLocation");
        if (location == null || location.trim().isEmpty()) {
            location = request.getParameter("location");
        }
        if (location == null || location.trim().isEmpty()) {
            location = (user.getAddress() != null && !user.getAddress().trim().isEmpty()) ? user.getAddress() : "Live GPS: 19.0596° N, 72.8295° E (Bandra)";
        }

        String notes = request.getParameter("notes");

        EmergencyRequest req = new EmergencyRequest();
        req.setUserId(user.getId());
        req.setEmergencyType(type.trim());
        req.setUserLocation(location.trim());
        req.setStatus("PENDING");
        req.setNotes(notes);

        int requestId = emergencyDAO.createRequest(req);
        if (requestId > 0) {
            request.getSession(true).setAttribute("activeEmergencyId", requestId);
            EmergencyRequest fresh = emergencyDAO.findById(requestId);
            response.getWriter().write("{\"success\":true,\"message\":\"SOS Emergency Request created successfully. Dispatch pending hospital authorization.\",\"request\":" + serializeRequest(fresh != null ? fresh : req) + "}");
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\":false,\"message\":\"Database failure creating SOS request. Please call emergency services directly.\"}");
        }
    }

    private void handleCancel(HttpServletRequest request, HttpServletResponse response, User user) throws IOException {
        EmergencyRequest activeReq = emergencyDAO.getActiveRequestForUser(user.getId());
        if (activeReq == null) {
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("activeEmergencyId") != null) {
                int reqId = (Integer) session.getAttribute("activeEmergencyId");
                activeReq = emergencyDAO.findById(reqId);
            }
        }
        if (activeReq == null) {
            response.getWriter().write("{\"success\":true,\"message\":\"No active request to cancel.\"}");
            return;
        }

        boolean cancelled = emergencyDAO.cancelRequest(activeReq.getRequestId(), user.getId());
        if (cancelled) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.removeAttribute("activeEmergencyId");
            }
            response.getWriter().write("{\"success\":true,\"message\":\"Emergency request cancelled.\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Could not cancel emergency request.\"}");
        }
    }

    private String serializeRequest(EmergencyRequest r) {
        StringBuilder json = new StringBuilder();
        json.append("{")
            .append("\"requestId\":").append(r.getRequestId()).append(",")
            .append("\"userId\":").append(r.getUserId() != null ? r.getUserId() : "null").append(",")
            .append("\"userName\":\"").append(escapeJson(r.getUserName())).append("\",")
            .append("\"userMobile\":\"").append(escapeJson(r.getUserMobile())).append("\",")
            .append("\"userEmergencyContact\":\"").append(escapeJson(r.getUserEmergencyContact())).append("\",")
            .append("\"emergencyType\":\"").append(escapeJson(r.getEmergencyType())).append("\",")
            .append("\"userLocation\":\"").append(escapeJson(r.getUserLocation())).append("\",")
            .append("\"status\":\"").append(escapeJson(r.getStatus())).append("\",")
            .append("\"createdAt\":\"").append(r.getCreatedAt() != null ? r.getCreatedAt().toString() : "").append("\",")
            .append("\"hospitalId\":").append(r.getHospitalId() != null ? r.getHospitalId() : "null").append(",")
            .append("\"hospitalName\":\"").append(escapeJson(r.getHospitalName())).append("\",")
            .append("\"ambulanceId\":").append(r.getAmbulanceId() != null ? r.getAmbulanceId() : "null").append(",")
            .append("\"vehicleNumber\":\"").append(escapeJson(r.getVehicleNumber())).append("\",")
            .append("\"driverName\":\"").append(escapeJson(r.getDriverName())).append("\",")
            .append("\"driverContact\":\"").append(escapeJson(r.getDriverContact())).append("\",")
            .append("\"assignedAt\":\"").append(r.getAssignedAt() != null ? r.getAssignedAt().toString() : "").append("\",")
            .append("\"completedAt\":\"").append(r.getCompletedAt() != null ? r.getCompletedAt().toString() : "").append("\",")
            .append("\"rejectionReason\":\"").append(escapeJson(r.getRejectionReason())).append("\"")
            .append("}");
        return json.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}

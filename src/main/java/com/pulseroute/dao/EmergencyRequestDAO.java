package com.pulseroute.dao;

import com.pulseroute.model.EmergencyRequest;
import com.pulseroute.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Citizen Emergency SOS Requests and Hospital Dispatching.
 */
public class EmergencyRequestDAO {

    public int createRequest(EmergencyRequest req) {
        String sql = "INSERT INTO emergency_requests (user_id, emergency_type, user_location, status, notes) " +
                     "VALUES (?, ?, ?, 'PENDING', ?)";

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            if (req.getUserId() != null) {
                stmt.setInt(1, req.getUserId());
            } else {
                stmt.setNull(1, java.sql.Types.INTEGER);
            }
            stmt.setString(2, req.getEmergencyType() != null ? req.getEmergencyType() : "Ambulance");
            stmt.setString(3, req.getUserLocation() != null ? req.getUserLocation() : "Current Location");
            stmt.setString(4, req.getNotes());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        req.setRequestId(id);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[EmergencyRequestDAO] Error createRequest: " + e.getMessage());
        } finally {
            DBConnection.closeQuietly(null, stmt, conn);
        }
        return -1;
    }

    public EmergencyRequest getActiveRequestForUser(int userId) {
        String sql = "SELECT r.request_id, r.user_id, r.emergency_type, r.user_location, r.created_at, " +
                     "r.status, r.hospital_id, r.ambulance_id, r.assigned_at, r.completed_at, r.rejection_reason, r.notes, " +
                     "u.full_name AS user_name, u.mobile AS user_mobile, u.email AS user_email, u.emergency_contact AS user_emergency_contact, " +
                     "h.hospital_name, a.vehicle_number, a.driver_name, a.driver_contact " +
                     "FROM emergency_requests r " +
                     "LEFT JOIN users u ON r.user_id = u.id " +
                     "LEFT JOIN hospitals h ON r.hospital_id = h.hospital_id " +
                     "LEFT JOIN ambulances a ON r.ambulance_id = a.ambulance_id " +
                     "WHERE r.user_id = ? AND r.status NOT IN ('COMPLETED', 'CANCELLED', 'REJECTED') " +
                     "ORDER BY r.request_id DESC LIMIT 1";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToRequest(rs);
            }
        } catch (SQLException e) {
            System.err.println("[EmergencyRequestDAO] Error getActiveRequestForUser: " + e.getMessage());
        } finally {
            DBConnection.closeQuietly(rs, stmt, conn);
        }
        return null;
    }

    public EmergencyRequest findById(int requestId) {
        String sql = "SELECT r.request_id, r.user_id, r.emergency_type, r.user_location, r.created_at, " +
                     "r.status, r.hospital_id, r.ambulance_id, r.assigned_at, r.completed_at, r.rejection_reason, r.notes, " +
                     "u.full_name AS user_name, u.mobile AS user_mobile, u.email AS user_email, u.emergency_contact AS user_emergency_contact, " +
                     "h.hospital_name, a.vehicle_number, a.driver_name, a.driver_contact " +
                     "FROM emergency_requests r " +
                     "LEFT JOIN users u ON r.user_id = u.id " +
                     "LEFT JOIN hospitals h ON r.hospital_id = h.hospital_id " +
                     "LEFT JOIN ambulances a ON r.ambulance_id = a.ambulance_id " +
                     "WHERE r.request_id = ? LIMIT 1";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, requestId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToRequest(rs);
            }
        } catch (SQLException e) {
            System.err.println("[EmergencyRequestDAO] Error findById: " + e.getMessage());
        } finally {
            DBConnection.closeQuietly(rs, stmt, conn);
        }
        return null;
    }

    public List<EmergencyRequest> getRequestsForHospital(int hospitalId) {
        List<EmergencyRequest> list = new ArrayList<>();
        // Shows requests assigned/handled by this hospital, or unassigned requests pending review
        String sql = "SELECT r.request_id, r.user_id, r.emergency_type, r.user_location, r.created_at, " +
                     "r.status, r.hospital_id, r.ambulance_id, r.assigned_at, r.completed_at, r.rejection_reason, r.notes, " +
                     "u.full_name AS user_name, u.mobile AS user_mobile, u.email AS user_email, u.emergency_contact AS user_emergency_contact, " +
                     "h.hospital_name, a.vehicle_number, a.driver_name, a.driver_contact " +
                     "FROM emergency_requests r " +
                     "LEFT JOIN users u ON r.user_id = u.id " +
                     "LEFT JOIN hospitals h ON r.hospital_id = h.hospital_id " +
                     "LEFT JOIN ambulances a ON r.ambulance_id = a.ambulance_id " +
                     "WHERE r.hospital_id = ? OR (r.hospital_id IS NULL AND r.status IN ('PENDING', 'RECEIVED', 'UNDER_REVIEW')) " +
                     "ORDER BY CASE " +
                     "  WHEN r.status IN ('PENDING', 'RECEIVED', 'UNDER_REVIEW') THEN 1 " +
                     "  WHEN r.status IN ('APPROVED', 'AMBULANCE_ASSIGNED', 'DISPATCHED', 'ARRIVING') THEN 2 " +
                     "  ELSE 3 END, r.request_id DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, hospitalId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToRequest(rs));
            }
        } catch (SQLException e) {
            System.err.println("[EmergencyRequestDAO] Error getRequestsForHospital: " + e.getMessage());
        } finally {
            DBConnection.closeQuietly(rs, stmt, conn);
        }
        return list;
    }

    public boolean approveRequest(int requestId, int hospitalId) {
        String sql = "UPDATE emergency_requests SET status = 'APPROVED', hospital_id = ? " +
                     "WHERE request_id = ? AND status IN ('PENDING', 'RECEIVED', 'UNDER_REVIEW')";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, hospitalId);
            stmt.setInt(2, requestId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[EmergencyRequestDAO] Error approveRequest: " + e.getMessage());
        } finally {
            DBConnection.closeQuietly(null, stmt, conn);
        }
        return false;
    }

    /**
     * Transactionally assigns an authorized, available ambulance to an emergency request.
     * Enforces ALL business conditions:
     * 1. Ambulance belongs to the hospital
     * 2. Ambulance authorization_status = 'AUTHORIZED'
     * 3. Ambulance status = 'AVAILABLE'
     * 4. Ambulance is not already assigned to another active emergency
     */
    public boolean assignAmbulance(int requestId, int hospitalId, int ambulanceId) {
        Connection conn = null;
        PreparedStatement checkStmt = null;
        PreparedStatement updateReqStmt = null;
        PreparedStatement updateAmbStmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Verify ambulance eligibility
            String checkSql = "SELECT ambulance_id, hospital_id, status, authorization_status FROM ambulances " +
                              "WHERE ambulance_id = ? FOR UPDATE";
            checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, ambulanceId);
            rs = checkStmt.executeQuery();

            if (!rs.next()) {
                conn.rollback();
                return false; // Ambulance not found
            }

            int ownerHospId = rs.getInt("hospital_id");
            String ambStatus = rs.getString("status");
            String authStatus = rs.getString("authorization_status");

            if (ownerHospId != hospitalId || !"AUTHORIZED".equalsIgnoreCase(authStatus) || !"AVAILABLE".equalsIgnoreCase(ambStatus)) {
                conn.rollback();
                return false; // Unauthorized or unavailable
            }

            // 2. Verify ambulance is not already assigned in another active request
            String conflictSql = "SELECT COUNT(*) FROM emergency_requests WHERE ambulance_id = ? " +
                                 "AND status IN ('AMBULANCE_ASSIGNED', 'DISPATCHED', 'ARRIVING') AND request_id != ?";
            try (PreparedStatement confStmt = conn.prepareStatement(conflictSql)) {
                confStmt.setInt(1, ambulanceId);
                confStmt.setInt(2, requestId);
                try (ResultSet confRs = confStmt.executeQuery()) {
                    if (confRs.next() && confRs.getInt(1) > 0) {
                        conn.rollback();
                        return false; // Already on active trip
                    }
                }
            }

            // 3. Update request
            String reqSql = "UPDATE emergency_requests SET hospital_id = ?, ambulance_id = ?, " +
                            "status = 'AMBULANCE_ASSIGNED', assigned_at = CURRENT_TIMESTAMP " +
                            "WHERE request_id = ? AND status IN ('PENDING', 'RECEIVED', 'UNDER_REVIEW', 'APPROVED')";
            updateReqStmt = conn.prepareStatement(reqSql);
            updateReqStmt.setInt(1, hospitalId);
            updateReqStmt.setInt(2, ambulanceId);
            updateReqStmt.setInt(3, requestId);
            int reqRows = updateReqStmt.executeUpdate();

            if (reqRows <= 0) {
                conn.rollback();
                return false;
            }

            // 4. Update ambulance status to ASSIGNED
            String ambSql = "UPDATE ambulances SET status = 'ASSIGNED' WHERE ambulance_id = ?";
            updateAmbStmt = conn.prepareStatement(ambSql);
            updateAmbStmt.setInt(1, ambulanceId);
            updateAmbStmt.executeUpdate();

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("[EmergencyRequestDAO] Error assignAmbulance: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
        } finally {
            DBConnection.closeQuietly(rs, checkStmt, null);
            DBConnection.closeQuietly(null, updateReqStmt, null);
            DBConnection.closeQuietly(null, updateAmbStmt, conn);
        }
        return false;
    }

    /**
     * Updates emergency status (e.g. DISPATCHED, ARRIVING, ARRIVED, COMPLETED).
     */
    public boolean updateStatus(int requestId, int hospitalId, String newStatus) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Fetch current request details
            String findSql = "SELECT ambulance_id, status FROM emergency_requests WHERE request_id = ? AND hospital_id = ? FOR UPDATE";
            Integer ambId = null;
            try (PreparedStatement findStmt = conn.prepareStatement(findSql)) {
                findStmt.setInt(1, requestId);
                findStmt.setInt(2, hospitalId);
                try (ResultSet rs = findStmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    int id = rs.getInt("ambulance_id");
                    if (!rs.wasNull()) {
                        ambId = id;
                    }
                }
            }

            // Update emergency request
            String updateSql = "UPDATE emergency_requests SET status = ?, " +
                    ( "COMPLETED".equalsIgnoreCase(newStatus) ? "completed_at = CURRENT_TIMESTAMP " : "" ) +
                    "WHERE request_id = ? AND hospital_id = ?";
            if (!"COMPLETED".equalsIgnoreCase(newStatus)) {
                updateSql = "UPDATE emergency_requests SET status = ? WHERE request_id = ? AND hospital_id = ?";
            }

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, newStatus.toUpperCase());
                updateStmt.setInt(2, requestId);
                updateStmt.setInt(3, hospitalId);
                updateStmt.executeUpdate();
            }

            // If ambulance involved, update ambulance status accordingly
            if (ambId != null) {
                String ambStatus = null;
                if ("DISPATCHED".equalsIgnoreCase(newStatus)) {
                    ambStatus = "ON_TRIP";
                } else if ("COMPLETED".equalsIgnoreCase(newStatus)) {
                    ambStatus = "AVAILABLE";
                }

                if (ambStatus != null) {
                    try (PreparedStatement ambStmt = conn.prepareStatement("UPDATE ambulances SET status = ? WHERE ambulance_id = ?")) {
                        ambStmt.setString(1, ambStatus);
                        ambStmt.setInt(2, ambId);
                        ambStmt.executeUpdate();
                    }
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("[EmergencyRequestDAO] Error updateStatus: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
        return false;
    }

    public boolean rejectRequest(int requestId, int hospitalId, String reason) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            String fetchSql = "SELECT ambulance_id FROM emergency_requests WHERE request_id = ? FOR UPDATE";
            Integer ambId = null;
            try (PreparedStatement fs = conn.prepareStatement(fetchSql)) {
                fs.setInt(1, requestId);
                try (ResultSet rs = fs.executeQuery()) {
                    if (rs.next()) {
                        int id = rs.getInt("ambulance_id");
                        if (!rs.wasNull()) ambId = id;
                    }
                }
            }

            String sql = "UPDATE emergency_requests SET status = 'REJECTED', hospital_id = ?, rejection_reason = ? " +
                         "WHERE request_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, hospitalId);
                stmt.setString(2, reason != null ? reason : "Unable to accommodate at this time.");
                stmt.setInt(3, requestId);
                stmt.executeUpdate();
            }

            if (ambId != null) {
                try (PreparedStatement ambStmt = conn.prepareStatement("UPDATE ambulances SET status = 'AVAILABLE' WHERE ambulance_id = ?")) {
                    ambStmt.setInt(1, ambId);
                    ambStmt.executeUpdate();
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("[EmergencyRequestDAO] Error rejectRequest: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
        return false;
    }

    public boolean cancelRequest(int requestId, int userId) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            String fetchSql = "SELECT ambulance_id FROM emergency_requests WHERE request_id = ? AND user_id = ? FOR UPDATE";
            Integer ambId = null;
            try (PreparedStatement fs = conn.prepareStatement(fetchSql)) {
                fs.setInt(1, requestId);
                fs.setInt(2, userId);
                try (ResultSet rs = fs.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    int id = rs.getInt("ambulance_id");
                    if (!rs.wasNull()) ambId = id;
                }
            }

            String sql = "UPDATE emergency_requests SET status = 'CANCELLED' WHERE request_id = ? AND user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, requestId);
                stmt.setInt(2, userId);
                stmt.executeUpdate();
            }

            if (ambId != null) {
                try (PreparedStatement ambStmt = conn.prepareStatement("UPDATE ambulances SET status = 'AVAILABLE' WHERE ambulance_id = ?")) {
                    ambStmt.setInt(1, ambId);
                    ambStmt.executeUpdate();
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("[EmergencyRequestDAO] Error cancelRequest: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
        return false;
    }

    private EmergencyRequest mapResultSetToRequest(ResultSet rs) throws SQLException {
        EmergencyRequest r = new EmergencyRequest();
        r.setRequestId(rs.getInt("request_id"));
        int uid = rs.getInt("user_id");
        if (!rs.wasNull()) r.setUserId(uid);
        r.setEmergencyType(rs.getString("emergency_type"));
        r.setUserLocation(rs.getString("user_location"));
        r.setCreatedAt(rs.getTimestamp("created_at"));
        r.setStatus(rs.getString("status"));
        int hid = rs.getInt("hospital_id");
        if (!rs.wasNull()) r.setHospitalId(hid);
        int aid = rs.getInt("ambulance_id");
        if (!rs.wasNull()) r.setAmbulanceId(aid);
        r.setAssignedAt(rs.getTimestamp("assigned_at"));
        r.setCompletedAt(rs.getTimestamp("completed_at"));
        r.setRejectionReason(rs.getString("rejection_reason"));
        r.setNotes(rs.getString("notes"));

        try { r.setUserName(rs.getString("user_name")); } catch (Exception ignored) {}
        try { r.setUserMobile(rs.getString("user_mobile")); } catch (Exception ignored) {}
        try { r.setUserEmail(rs.getString("user_email")); } catch (Exception ignored) {}
        try { r.setUserEmergencyContact(rs.getString("user_emergency_contact")); } catch (Exception ignored) {}
        try { r.setHospitalName(rs.getString("hospital_name")); } catch (Exception ignored) {}
        try { r.setVehicleNumber(rs.getString("vehicle_number")); } catch (Exception ignored) {}
        try { r.setDriverName(rs.getString("driver_name")); } catch (Exception ignored) {}
        try { r.setDriverContact(rs.getString("driver_contact")); } catch (Exception ignored) {}

        return r;
    }
}

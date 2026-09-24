package com.pulseroute.dao;

import com.pulseroute.model.Hospital;
import com.pulseroute.util.BCrypt;
import com.pulseroute.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for Hospital operations and administration.
 */
public class HospitalDAO {

    /**
     * Verifies hospital admin credentials using BCrypt.
     */
    public Hospital verifyCredentials(String identifier, String plainPassword) {
        if (identifier == null || plainPassword == null || identifier.trim().isEmpty() || plainPassword.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT hospital_id, hospital_code, hospital_name, email, password_hash, location, contact, status, created_at " +
                     "FROM hospitals WHERE (email = ? OR hospital_code = ?) AND status = 'ACTIVE' LIMIT 1";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            String clean = identifier.trim();
            stmt.setString(1, clean);
            stmt.setString(2, clean);
            rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (storedHash != null && BCrypt.checkpw(plainPassword.trim(), storedHash)) {
                    return mapResultSetToHospital(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[HospitalDAO] Error verifying credentials: " + e.getMessage());
        } finally {
            DBConnection.closeQuietly(rs, stmt, conn);
        }
        return null;
    }

    public Hospital findById(int hospitalId) {
        String sql = "SELECT hospital_id, hospital_code, hospital_name, email, password_hash, location, contact, status, created_at " +
                     "FROM hospitals WHERE hospital_id = ? LIMIT 1";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, hospitalId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToHospital(rs);
            }
        } catch (SQLException e) {
            System.err.println("[HospitalDAO] Error findById: " + e.getMessage());
        } finally {
            DBConnection.closeQuietly(rs, stmt, conn);
        }
        return null;
    }

    public List<Hospital> getAllActiveHospitals() {
        List<Hospital> list = new ArrayList<>();
        String sql = "SELECT hospital_id, hospital_code, hospital_name, email, password_hash, location, contact, status, created_at " +
                     "FROM hospitals WHERE status = 'ACTIVE' ORDER BY hospital_name ASC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToHospital(rs));
            }
        } catch (SQLException e) {
            System.err.println("[HospitalDAO] Error getAllActiveHospitals: " + e.getMessage());
        } finally {
            DBConnection.closeQuietly(rs, stmt, conn);
        }
        return list;
    }

    /**
     * Retrieves key operational statistics for a specific hospital.
     */
    public Map<String, Integer> getHospitalStats(int hospitalId) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalAmbulances", 0);
        stats.put("availableAmbulances", 0);
        stats.put("assignedAmbulances", 0);
        stats.put("onTripAmbulances", 0);
        stats.put("offlineAmbulances", 0);
        stats.put("authorizedAmbulances", 0);
        stats.put("pendingRequests", 0);
        stats.put("activeDispatches", 0);

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();

            // Ambulance counts
            String ambSql = "SELECT " +
                    "COUNT(*) AS total, " +
                    "SUM(CASE WHEN status = 'AVAILABLE' AND authorization_status = 'AUTHORIZED' THEN 1 ELSE 0 END) AS available, " +
                    "SUM(CASE WHEN status = 'ASSIGNED' THEN 1 ELSE 0 END) AS assigned, " +
                    "SUM(CASE WHEN status = 'ON_TRIP' THEN 1 ELSE 0 END) AS on_trip, " +
                    "SUM(CASE WHEN status = 'OFFLINE' THEN 1 ELSE 0 END) AS offline, " +
                    "SUM(CASE WHEN authorization_status = 'AUTHORIZED' THEN 1 ELSE 0 END) AS authorized " +
                    "FROM ambulances WHERE hospital_id = ?";
            stmt = conn.prepareStatement(ambSql);
            stmt.setInt(1, hospitalId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                stats.put("totalAmbulances", rs.getInt("total"));
                stats.put("availableAmbulances", rs.getInt("available"));
                stats.put("assignedAmbulances", rs.getInt("assigned"));
                stats.put("onTripAmbulances", rs.getInt("on_trip"));
                stats.put("offlineAmbulances", rs.getInt("offline"));
                stats.put("authorizedAmbulances", rs.getInt("authorized"));
            }
            rs.close();
            stmt.close();

            // Emergency requests count
            String reqSql = "SELECT " +
                    "SUM(CASE WHEN status IN ('PENDING', 'RECEIVED', 'UNDER_REVIEW') THEN 1 ELSE 0 END) AS pending, " +
                    "SUM(CASE WHEN status IN ('APPROVED', 'AMBULANCE_ASSIGNED', 'DISPATCHED', 'ARRIVING') THEN 1 ELSE 0 END) AS active_disp " +
                    "FROM emergency_requests WHERE hospital_id = ? OR hospital_id IS NULL";
            stmt = conn.prepareStatement(reqSql);
            stmt.setInt(1, hospitalId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                stats.put("pendingRequests", rs.getInt("pending"));
                stats.put("activeDispatches", rs.getInt("active_disp"));
            }

        } catch (SQLException e) {
            System.err.println("[HospitalDAO] Error getHospitalStats: " + e.getMessage());
        } finally {
            DBConnection.closeQuietly(rs, stmt, conn);
        }
        return stats;
    }

    private Hospital mapResultSetToHospital(ResultSet rs) throws SQLException {
        Hospital h = new Hospital();
        h.setHospitalId(rs.getInt("hospital_id"));
        h.setHospitalCode(rs.getString("hospital_code"));
        h.setHospitalName(rs.getString("hospital_name"));
        h.setEmail(rs.getString("email"));
        h.setPasswordHash(rs.getString("password_hash"));
        h.setLocation(rs.getString("location"));
        h.setContact(rs.getString("contact"));
        h.setStatus(rs.getString("status"));
        h.setCreatedAt(rs.getTimestamp("created_at"));
        return h;
    }
}

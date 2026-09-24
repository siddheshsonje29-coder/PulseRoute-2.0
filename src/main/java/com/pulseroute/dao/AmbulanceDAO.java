package com.pulseroute.dao;

import com.pulseroute.model.Ambulance;
import com.pulseroute.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Ambulance fleet operations.
 */
public class AmbulanceDAO {

    public List<Ambulance> findByHospitalId(int hospitalId) {
        List<Ambulance> list = new ArrayList<>();
        String sql = "SELECT a.ambulance_id, a.hospital_id, a.vehicle_number, a.driver_name, a.driver_contact, " +
                     "a.status, a.authorization_status, a.current_location, a.created_at, h.hospital_name " +
                     "FROM ambulances a " +
                     "JOIN hospitals h ON a.hospital_id = h.hospital_id " +
                     "WHERE a.hospital_id = ? ORDER BY a.ambulance_id DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, hospitalId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToAmbulance(rs));
            }
        } catch (SQLException e) {
            System.err.println("[AmbulanceDAO] Error findByHospitalId: " + e.getMessage());
        } finally {
            DBConnection.closeQuietly(rs, stmt, conn);
        }
        return list;
    }

    /**
     * Finds ambulances that satisfy ALL conditions:
     * 1. Belongs to the specific hospital
     * 2. Authorized by the hospital (authorization_status = 'AUTHORIZED')
     * 3. Currently active & available (status = 'AVAILABLE')
     * 4. Not already assigned to another active emergency request
     */
    public List<Ambulance> findDispatchableAmbulances(int hospitalId) {
        List<Ambulance> list = new ArrayList<>();
        String sql = "SELECT a.ambulance_id, a.hospital_id, a.vehicle_number, a.driver_name, a.driver_contact, " +
                     "a.status, a.authorization_status, a.current_location, a.created_at, h.hospital_name " +
                     "FROM ambulances a " +
                     "JOIN hospitals h ON a.hospital_id = h.hospital_id " +
                     "WHERE a.hospital_id = ? " +
                     "AND a.authorization_status = 'AUTHORIZED' " +
                     "AND a.status = 'AVAILABLE' " +
                     "AND a.ambulance_id NOT IN (" +
                     "   SELECT ambulance_id FROM emergency_requests " +
                     "   WHERE ambulance_id IS NOT NULL AND status IN ('AMBULANCE_ASSIGNED', 'DISPATCHED', 'ARRIVING')" +
                     ") " +
                     "ORDER BY a.ambulance_id ASC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, hospitalId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToAmbulance(rs));
            }
        } catch (SQLException e) {
            System.err.println("[AmbulanceDAO] Error findDispatchableAmbulances: " + e.getMessage());
        } finally {
            DBConnection.closeQuietly(rs, stmt, conn);
        }
        return list;
    }

    public Ambulance findById(int ambulanceId) {
        String sql = "SELECT a.ambulance_id, a.hospital_id, a.vehicle_number, a.driver_name, a.driver_contact, " +
                     "a.status, a.authorization_status, a.current_location, a.created_at, h.hospital_name " +
                     "FROM ambulances a " +
                     "JOIN hospitals h ON a.hospital_id = h.hospital_id " +
                     "WHERE a.ambulance_id = ? LIMIT 1";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, ambulanceId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToAmbulance(rs);
            }
        } catch (SQLException e) {
            System.err.println("[AmbulanceDAO] Error findById: " + e.getMessage());
        } finally {
            DBConnection.closeQuietly(rs, stmt, conn);
        }
        return null;
    }

    public boolean addAmbulance(Ambulance amb) {
        String sql = "INSERT INTO ambulances (hospital_id, vehicle_number, driver_name, driver_contact, status, authorization_status, current_location) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, amb.getHospitalId());
            stmt.setString(2, amb.getVehicleNumber().trim());
            stmt.setString(3, amb.getDriverName().trim());
            stmt.setString(4, amb.getDriverContact().trim());
            stmt.setString(5, amb.getStatus() != null ? amb.getStatus() : "AVAILABLE");
            stmt.setString(6, amb.getAuthorizationStatus() != null ? amb.getAuthorizationStatus() : "AUTHORIZED");
            stmt.setString(7, amb.getCurrentLocation() != null ? amb.getCurrentLocation().trim() : "Hospital Bay");

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        amb.setAmbulanceId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[AmbulanceDAO] Error addAmbulance: " + e.getMessage());
        } finally {
            DBConnection.closeQuietly(null, stmt, conn);
        }
        return false;
    }

    public boolean updateStatus(int ambulanceId, int hospitalId, String status) {
        String sql = "UPDATE ambulances SET status = ? WHERE ambulance_id = ? AND hospital_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, status);
            stmt.setInt(2, ambulanceId);
            stmt.setInt(3, hospitalId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AmbulanceDAO] Error updateStatus: " + e.getMessage());
        } finally {
            DBConnection.closeQuietly(null, stmt, conn);
        }
        return false;
    }

    public boolean updateAuthorization(int ambulanceId, int hospitalId, String authStatus) {
        String sql = "UPDATE ambulances SET authorization_status = ? WHERE ambulance_id = ? AND hospital_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, authStatus);
            stmt.setInt(2, ambulanceId);
            stmt.setInt(3, hospitalId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AmbulanceDAO] Error updateAuthorization: " + e.getMessage());
        } finally {
            DBConnection.closeQuietly(null, stmt, conn);
        }
        return false;
    }

    public boolean updateAmbulance(Ambulance amb) {
        String sql = "UPDATE ambulances SET vehicle_number = ?, driver_name = ?, driver_contact = ?, current_location = ? " +
                     "WHERE ambulance_id = ? AND hospital_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, amb.getVehicleNumber().trim());
            stmt.setString(2, amb.getDriverName().trim());
            stmt.setString(3, amb.getDriverContact().trim());
            stmt.setString(4, amb.getCurrentLocation().trim());
            stmt.setInt(5, amb.getAmbulanceId());
            stmt.setInt(6, amb.getHospitalId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AmbulanceDAO] Error updateAmbulance: " + e.getMessage());
        } finally {
            DBConnection.closeQuietly(null, stmt, conn);
        }
        return false;
    }

    private Ambulance mapResultSetToAmbulance(ResultSet rs) throws SQLException {
        Ambulance a = new Ambulance();
        a.setAmbulanceId(rs.getInt("ambulance_id"));
        a.setHospitalId(rs.getInt("hospital_id"));
        a.setVehicleNumber(rs.getString("vehicle_number"));
        a.setDriverName(rs.getString("driver_name"));
        a.setDriverContact(rs.getString("driver_contact"));
        a.setStatus(rs.getString("status"));
        a.setAuthorizationStatus(rs.getString("authorization_status"));
        a.setCurrentLocation(rs.getString("current_location"));
        a.setCreatedAt(rs.getTimestamp("created_at"));
        try {
            a.setHospitalName(rs.getString("hospital_name"));
        } catch (Exception ignored) {}
        return a;
    }
}

package com.pulseroute.dao;

import com.pulseroute.model.User;
import com.pulseroute.util.BCrypt;
import com.pulseroute.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Data Access Object for Citizen User operations using JDBC PreparedStatements.
 */
public class UserDAO {

    /**
     * Finds a citizen user by either Citizen ID or Registered Email.
     */
    public User findByCitizenIdOrEmail(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT id, full_name, email, mobile, citizen_id, password, emergency_contact, address, created_at " +
                     "FROM users WHERE citizen_id = ? OR email = ? LIMIT 1";

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
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error in findByCitizenIdOrEmail: " + e.getMessage());
        } finally {
            DBConnection.closeQuietly(rs, stmt, conn);
        }
        return null;
    }

    /**
     * Finds a user specifically by email address.
     */
    public User findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) return null;
        String sql = "SELECT id, full_name, email, mobile, citizen_id, password, emergency_contact, address, created_at " +
                     "FROM users WHERE email = ? LIMIT 1";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email.trim().toLowerCase());
            rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error in findByEmail: " + e.getMessage());
        } finally {
            DBConnection.closeQuietly(rs, stmt, conn);
        }
        return null;
    }

    /**
     * Finds a user specifically by citizen ID.
     */
    public User findByCitizenId(String citizenId) {
        if (citizenId == null || citizenId.trim().isEmpty()) return null;
        String sql = "SELECT id, full_name, email, mobile, citizen_id, password, emergency_contact, address, created_at " +
                     "FROM users WHERE citizen_id = ? LIMIT 1";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, citizenId.trim());
            rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error in findByCitizenId: " + e.getMessage());
        } finally {
            DBConnection.closeQuietly(rs, stmt, conn);
        }
        return null;
    }

    /**
     * Registers a new citizen user into MySQL.
     * Passwords MUST already be BCrypt hashed before calling or will be hashed here.
     */
    public boolean save(User user) {
        if (user == null) return false;

        String sql = "INSERT INTO users (full_name, email, mobile, citizen_id, password, emergency_contact, address) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            stmt.setString(1, user.getFullName().trim());
            stmt.setString(2, user.getEmail().trim().toLowerCase());
            stmt.setString(3, user.getMobile().trim());
            stmt.setString(4, user.getCitizenId().trim());
            stmt.setString(5, user.getPassword()); // BCrypt hash
            stmt.setString(6, user.getEmergencyContact() != null ? user.getEmergencyContact().trim() : "");
            stmt.setString(7, user.getAddress() != null ? user.getAddress().trim() : "");

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error in save: " + e.getMessage());
        } finally {
            DBConnection.closeQuietly(rs, stmt, conn);
        }
        return false;
    }

    /**
     * Verifies citizen login credentials.
     * Matches raw password against stored BCrypt hash using BCrypt.checkpw().
     *
     * @param identifier citizen_id or email
     * @param rawPassword plain text password entered in login form
     * @return User object on successful match, null otherwise
     */
    public User verifyCredentials(String identifier, String rawPassword) {
        if (identifier == null || rawPassword == null) return null;
        User user = findByCitizenIdOrEmail(identifier);
        if (user == null) {
            return null;
        }

        // Validate BCrypt hash
        if (BCrypt.checkpw(rawPassword, user.getPassword())) {
            return user;
        }
        return null;
    }

    /**
     * Resets or updates a citizen's password.
     */
    public boolean updatePassword(String citizenId, String newHashedPassword) {
        if (citizenId == null || newHashedPassword == null) return false;
        String sql = "UPDATE users SET password = ? WHERE citizen_id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, newHashedPassword);
            stmt.setString(2, citizenId.trim());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error in updatePassword: " + e.getMessage());
        } finally {
            DBConnection.closeQuietly(stmt, conn);
        }
        return false;
    }

    /**
     * Validates identity and resets password for Forgot Password flow.
     */
    public boolean resetPasswordWithVerification(String citizenId, String email, String emergencyContact, String newRawPassword) {
        if (citizenId == null || email == null || newRawPassword == null) return false;

        String sql = "SELECT id FROM users WHERE citizen_id = ? AND email = ? AND emergency_contact = ? LIMIT 1";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, citizenId.trim());
            stmt.setString(2, email.trim().toLowerCase());
            stmt.setString(3, emergencyContact != null ? emergencyContact.trim() : "");
            rs = stmt.executeQuery();
            if (rs.next()) {
                String hashed = BCrypt.hashpw(newRawPassword, BCrypt.gensalt(12));
                return updatePassword(citizenId, hashed);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error in resetPasswordWithVerification: " + e.getMessage());
        } finally {
            DBConnection.closeQuietly(rs, stmt, conn);
        }
        return false;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setMobile(rs.getString("mobile"));
        user.setCitizenId(rs.getString("citizen_id"));
        user.setPassword(rs.getString("password"));
        user.setEmergencyContact(rs.getString("emergency_contact"));
        user.setAddress(rs.getString("address"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }
}

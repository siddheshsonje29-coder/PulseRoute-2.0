package com.pulseroute.listener;

import com.pulseroute.util.DBConnection;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Automatically initializes MySQL database tables and seed data on application boot
 * if the database is newly provisioned or tables are missing.
 */
@WebListener
public class DatabaseInitListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("[DatabaseInitListener] Verifying database schema...");

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            stmt = conn.createStatement();

            boolean needsInit = false;
            try {
                rs = stmt.executeQuery("SELECT 1 FROM users LIMIT 1");
            } catch (Exception e) {
                // Table 'users' does not exist yet
                needsInit = true;
            }

            if (needsInit) {
                System.out.println("[DatabaseInitListener] Tables not found. Initializing database from init.sql...");
                executeInitScript(conn);
                System.out.println("[DatabaseInitListener] Database schema and initial seeds initialized successfully!");
            } else {
                System.out.println("[DatabaseInitListener] Database schema verified. Tables already exist.");
            }

        } catch (Exception e) {
            System.err.println("[DatabaseInitListener] Error verifying/initializing database: " + e.getMessage());
        } finally {
            DBConnection.closeQuietly(rs, stmt, conn);
        }
    }

    private void executeInitScript(Connection conn) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("init.sql")) {
            if (in == null) {
                System.err.println("[DatabaseInitListener] init.sql not found in resources!");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder currentQuery = new StringBuilder();
                String line;

                try (Statement stmt = conn.createStatement()) {
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("/*")) {
                            continue;
                        }

                        currentQuery.append(line).append("\n");

                        if (trimmed.endsWith(";")) {
                            String sql = currentQuery.toString().trim();
                            // Remove trailing semicolon
                            if (sql.endsWith(";")) {
                                sql = sql.substring(0, sql.length() - 1).trim();
                            }
                            if (!sql.isEmpty()) {
                                try {
                                    stmt.execute(sql);
                                } catch (Exception ex) {
                                    System.err.println("[DatabaseInitListener] Error running statement: " + ex.getMessage());
                                }
                            }
                            currentQuery.setLength(0);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DatabaseInitListener] Failed to parse and execute init.sql: " + e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}

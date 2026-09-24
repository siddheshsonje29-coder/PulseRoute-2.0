package com.pulseroute.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Thread-safe Database Connection utility for PulseRoute MySQL backend.
 */
public class DBConnection {

    private static String dbUrl = "jdbc:mysql://localhost:3306/pulseroute?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
    private static String dbUser = "root";
    private static String dbPassword = "root";
    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    static {
        // 1. Try reading properties file if bundled
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                if (props.getProperty("db.url") != null) dbUrl = props.getProperty("db.url");
                if (props.getProperty("db.user") != null) dbUser = props.getProperty("db.user");
                if (props.getProperty("db.password") != null) dbPassword = props.getProperty("db.password");
            }
        } catch (Exception ignored) {
        }

        // 2. Override with system properties or environment variables if present
        String envUrl = System.getenv("PULSEROUTE_DB_URL");
        if (envUrl != null && !envUrl.trim().isEmpty()) dbUrl = envUrl.trim();
        else if (System.getProperty("db.url") != null) dbUrl = System.getProperty("db.url").trim();

        String envUser = System.getenv("PULSEROUTE_DB_USER");
        if (envUser != null && !envUser.trim().isEmpty()) dbUser = envUser.trim();
        else if (System.getProperty("db.user") != null) dbUser = System.getProperty("db.user").trim();

        String envPass = System.getenv("PULSEROUTE_DB_PASSWORD");
        if (envPass != null) dbPassword = envPass;
        else if (System.getProperty("db.password") != null) dbPassword = System.getProperty("db.password");

        // Load MySQL Driver
        try {
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            System.err.println("[PulseRoute DBConnection] Warning: MySQL Driver class not found: " + DRIVER_CLASS);
        }
    }

    /**
     * Obtains a fresh database connection.
     *
     * @return active java.sql.Connection
     * @throws SQLException on connection failure
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    /**
     * Programmatic credential configuration helper.
     */
    public static void configure(String url, String username, String password) {
        if (url != null && !url.trim().isEmpty()) dbUrl = url.trim();
        if (username != null) dbUser = username.trim();
        if (password != null) dbPassword = password;
    }

    public static String getDbUrl() {
        return dbUrl;
    }

    public static String getDbUser() {
        return dbUser;
    }

    /**
     * Closes AutoCloseable resources silently.
     */
    public static void closeQuietly(AutoCloseable... closeables) {
        if (closeables == null) return;
        for (AutoCloseable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}

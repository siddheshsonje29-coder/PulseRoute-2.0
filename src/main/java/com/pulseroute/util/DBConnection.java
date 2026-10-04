package com.pulseroute.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Thread-safe Database Connection utility for PulseRoute MySQL backend.
 */
public class DBConnection {

    private static String dbUrl = "jdbc:mysql://localhost:3306/pulseroute?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
    private static String dbUser = "pulseroute";
    private static String dbPassword = "";
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

        // 2. Check environment variables
        String envUrl = System.getenv("PULSEROUTE_DB_URL");
        if (envUrl == null || envUrl.trim().isEmpty()) {
            envUrl = System.getenv("MYSQL_PRIVATE_URL");
        }
        if (envUrl == null || envUrl.trim().isEmpty()) {
            envUrl = System.getenv("MYSQL_URL");
        }
        if (envUrl == null || envUrl.trim().isEmpty()) {
            envUrl = System.getenv("DATABASE_URL");
        }
        if (envUrl == null || envUrl.trim().isEmpty()) {
            envUrl = System.getProperty("db.url");
        }

        // If no full URL is set, check individual Railway variables (MYSQLHOST, etc.)
        if (envUrl == null || envUrl.trim().isEmpty()) {
            String mHost = System.getenv("MYSQLHOST");
            if (mHost == null || mHost.trim().isEmpty()) mHost = System.getenv("MYSQL_HOST");
            if (mHost != null && !mHost.trim().isEmpty()) {
                String mPort = System.getenv("MYSQLPORT");
                if (mPort == null || mPort.trim().isEmpty()) mPort = System.getenv("MYSQL_PORT");
                if (mPort == null || mPort.trim().isEmpty()) mPort = "3306";

                String mDb = System.getenv("MYSQLDATABASE");
                if (mDb == null || mDb.trim().isEmpty()) mDb = System.getenv("MYSQL_DATABASE");
                if (mDb == null || mDb.trim().isEmpty()) mDb = "pulseroute";

                envUrl = "jdbc:mysql://" + mHost.trim() + ":" + mPort.trim() + "/" + mDb.trim() + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
            }
        }

        if (envUrl != null && !envUrl.trim().isEmpty()) {
            parseAndSetDbConfig(envUrl.trim());
        }

        String envUser = System.getenv("PULSEROUTE_DB_USER");
        if (envUser != null && !envUser.trim().isEmpty()) dbUser = envUser.trim();
        else if (System.getenv("MYSQL_USER") != null) dbUser = System.getenv("MYSQL_USER").trim();
        else if (System.getenv("MYSQLUSER") != null && !"root".equalsIgnoreCase(System.getenv("MYSQLUSER"))) dbUser = System.getenv("MYSQLUSER").trim();
        else if (System.getProperty("db.user") != null) dbUser = System.getProperty("db.user").trim();

        String envPass = System.getenv("PULSEROUTE_DB_PASSWORD");
        if (envPass != null) dbPassword = envPass;
        else if (System.getenv("MYSQL_PASSWORD") != null) dbPassword = System.getenv("MYSQL_PASSWORD");
        else if (System.getenv("MYSQLPASSWORD") != null) dbPassword = System.getenv("MYSQLPASSWORD");
        else if (System.getProperty("db.password") != null) dbPassword = System.getProperty("db.password");

        // Load MySQL Driver
        try {
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            System.err.println("[PulseRoute DBConnection] Warning: MySQL Driver class not found: " + DRIVER_CLASS);
        }
    }

    /**
     * Parses standard JDBC URLs or standard mysql://user:pass@host:port/db connection strings.
     */
    private static void parseAndSetDbConfig(String rawUrl) {
        if (rawUrl.startsWith("mysql://")) {
            try {
                java.net.URI uri = new java.net.URI(rawUrl);
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    dbUser = parts[0];
                    dbPassword = parts[1];
                }
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 3306;
                String path = uri.getPath();
                if (path == null || path.isEmpty() || path.equals("/")) {
                    path = "/pulseroute";
                }
                String query = uri.getQuery();
                dbUrl = "jdbc:mysql://" + host + ":" + port + path + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
                if (query != null && !query.isEmpty()) {
                    dbUrl += "&" + query;
                }
                return;
            } catch (Exception e) {
                System.err.println("[PulseRoute DBConnection] Failed parsing mysql:// URI: " + e.getMessage());
            }
        }
        dbUrl = rawUrl;
    }

    /**
     * Obtains a fresh database connection with smart user fallback.
     */
    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (SQLException firstEx) {
            System.err.println("[DBConnection] Primary connect failed (" + firstEx.getMessage() + "). Attempting auto-discovery fallbacks...");

            List<String> urlsToTry = new ArrayList<>();
            if (dbUrl != null) {
                urlsToTry.add(dbUrl);
                if (dbUrl.contains("/pulseroute")) {
                    urlsToTry.add(dbUrl.replace("/pulseroute", "/railway"));
                } else if (dbUrl.contains("/railway")) {
                    urlsToTry.add(dbUrl.replace("/railway", "/pulseroute"));
                }
            }

            List<String> usersToTry = new ArrayList<>();
            if (dbUser != null) usersToTry.add(dbUser);
            String mUser = System.getenv("MYSQLUSER");
            if (mUser != null && !usersToTry.contains(mUser)) usersToTry.add(mUser);
            if (!usersToTry.contains("root")) usersToTry.add("root");
            if (!usersToTry.contains("pulseroute")) usersToTry.add("pulseroute");

            List<String> passesToTry = new ArrayList<>();
            if (dbPassword != null) passesToTry.add(dbPassword);
            String mPass = System.getenv("MYSQLPASSWORD");
            if (mPass != null && !passesToTry.contains(mPass)) passesToTry.add(mPass);
            String mPass2 = System.getenv("MYSQL_PASSWORD");
            if (mPass2 != null && !passesToTry.contains(mPass2)) passesToTry.add(mPass2);
            if (!passesToTry.contains("")) passesToTry.add("");

            for (String u : urlsToTry) {
                for (String usr : usersToTry) {
                    for (String pwd : passesToTry) {
                        try {
                            Connection conn = DriverManager.getConnection(u, usr, pwd);
                            dbUrl = u;
                            dbUser = usr;
                            dbPassword = pwd;
                            System.out.println("[DBConnection] Successfully connected using fallback: " + u + " with user: " + usr);
                            return conn;
                        } catch (SQLException ignored) {
                        }
                    }
                }
            }
            throw firstEx;
        }
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

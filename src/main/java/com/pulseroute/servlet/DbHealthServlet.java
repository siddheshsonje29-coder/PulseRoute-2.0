package com.pulseroute.servlet;

import com.pulseroute.listener.DatabaseInitListener;
import com.pulseroute.util.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Diagnostic and self-healing servlet to test database connectivity
 * and trigger schema initialization on demand.
 */
@WebServlet(name = "DbHealthServlet", urlPatterns = {"/db-health", "/api/db-health"})
public class DbHealthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String doInit = request.getParameter("init");

        String dbUrl = DBConnection.getDbUrl();
        String dbUser = DBConnection.getDbUser();

        // Mask password
        String sanitizedUrl = (dbUrl != null) ? dbUrl.replaceAll(":[^/@:]+@", ":***@") : "null";

        boolean connected = false;
        String errorMessage = null;
        List<String> tables = new ArrayList<>();
        int userCount = -1;
        String initResult = null;

        try (Connection conn = DBConnection.getConnection()) {
            connected = true;

            // Optional manual trigger to run init.sql
            if ("true".equalsIgnoreCase(doInit)) {
                DatabaseInitListener listener = new DatabaseInitListener();
                listener.executeInitScript(conn);
                initResult = "init.sql executed successfully!";
            }

            // Retrieve tables in current database
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }

            // Check users count
            if (tables.contains("users")) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                    if (rs.next()) {
                        userCount = rs.getInt(1);
                    }
                }
            } else if ("auto".equalsIgnoreCase(request.getParameter("auto")) || tables.isEmpty()) {
                DatabaseInitListener listener = new DatabaseInitListener();
                listener.executeInitScript(conn);
                initResult = "Auto-initialized tables because database was empty!";
                // Refresh tables
                tables.clear();
                try (ResultSet rs = meta.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
                    while (rs.next()) {
                        tables.add(rs.getString("TABLE_NAME"));
                    }
                }
                if (tables.contains("users")) {
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                        if (rs.next()) userCount = rs.getInt(1);
                    }
                }
            }

        } catch (Exception e) {
            errorMessage = e.getClass().getName() + ": " + e.getMessage();
        }

        // Render simple diagnostics HTML
        out.println("<!DOCTYPE html><html><head><title>PulseRoute DB Health</title>");
        out.println("<style>body{font-family:sans-serif;background:#0f172a;color:#f8fafc;padding:30px;max-width:800px;margin:auto;}");
        out.println(".card{background:#1e293b;border-radius:12px;padding:24px;border:1px solid #334155;margin-bottom:20px;}");
        out.println(".badge-ok{color:#4ade80;font-weight:bold;}.badge-err{color:#f87171;font-weight:bold;}");
        out.println("code{background:#090d16;padding:4px 8px;border-radius:6px;color:#38bdf8;}");
        out.println("a.btn{display:inline-block;background:#e11d48;color:white;padding:10px 18px;border-radius:8px;text-decoration:none;font-weight:bold;margin-top:10px;}");
        out.println("</style></head><body>");

        out.println("<h2>PulseRoute 2.0 &mdash; Database Health & Diagnostics</h2>");

        out.println("<div class='card'>");
        out.println("<p><strong>Status:</strong> " + (connected ? "<span class='badge-ok'>CONNECTED &#10004;</span>" : "<span class='badge-err'>FAILED &#10008;</span>") + "</p>");
        out.println("<p><strong>Database URL:</strong> <code>" + sanitizedUrl + "</code></p>");
        out.println("<p><strong>Database User:</strong> <code>" + dbUser + "</code></p>");

        if (!connected) {
            out.println("<p style='color:#f87171;'><strong>Error:</strong> " + errorMessage + "</p>");
            out.println("<p><em>Make sure Railway MySQL is running and variables like PULSEROUTE_DB_URL or MYSQLHOST are accessible.</em></p>");
        } else {
            out.println("<p><strong>Tables in Database (" + tables.size() + "):</strong> " + (tables.isEmpty() ? "<span style='color:#fbbf24'>No tables found yet</span>" : "<code>" + String.join(", ", tables) + "</code>") + "</p>");
            if (userCount >= 0) {
                out.println("<p><strong>Seeded Citizens in 'users' table:</strong> " + userCount + "</p>");
            }
            if (initResult != null) {
                out.println("<p style='color:#4ade80;'><strong>Action Result:</strong> " + initResult + "</p>");
            }
            out.println("<p><a class='btn' href='db-health?init=true'>&#9881; Re-Run Schema Initialization (init.sql)</a></p>");
            out.println("<p style='margin-top:16px;'><a style='color:#38bdf8;' href='login.html'>&larr; Return to Login Portal</a></p>");
        }

        out.println("</div></body></html>");
    }
}

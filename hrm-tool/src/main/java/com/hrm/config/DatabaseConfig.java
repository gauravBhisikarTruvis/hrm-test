package com.hrm.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConfig {
    private static final String URL = "jdbc:postgresql://192.168.1.10:5432/hrm_test";
    private static final String USER = "postgres";
    private static final String PASS = "PGTruvis@2025";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static void initializeDatabase() {
        String dropOldReports = "DROP TABLE IF EXISTS audit_reports;";
        String dropEmpAudit = "DROP TABLE IF EXISTS employee_audit_reports;";
        String dropSysAudit = "DROP TABLE IF EXISTS system_audit_reports;";
        String dropRules = "DROP TABLE IF EXISTS business_rules;";
        
        String rulesTable = "CREATE TABLE IF NOT EXISTS business_rules (" +
                            "rule_id INTEGER PRIMARY KEY," +
                            "rule_name VARCHAR(100) UNIQUE NOT NULL," +
                            "audit_category VARCHAR(100) NOT NULL," +
                            "rule_content TEXT NOT NULL," +
                            "is_active BOOLEAN DEFAULT TRUE," +
                            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                            ");";

        String empAuditTable = "CREATE TABLE employee_audit_reports (" +
                                "id SERIAL PRIMARY KEY," +
                                "employee_id VARCHAR(50) NOT NULL," +
                                "rule_ids INTEGER[] NOT NULL," +
                                "check_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                                ");";

        String sysAuditTable = "CREATE TABLE system_audit_reports (" +
                                "id SERIAL PRIMARY KEY," +
                                "config_id VARCHAR(100) NOT NULL," +
                                "rule_ids INTEGER[] NOT NULL," +
                                "check_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                                ");";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(dropOldReports);
            stmt.execute(dropEmpAudit);
            stmt.execute(dropSysAudit);
            stmt.execute(dropRules);
            stmt.execute(rulesTable);
            stmt.execute(empAuditTable);
            stmt.execute(sysAuditTable);
            System.out.println("Database initialized with Enterprise GoRules.");
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
        }
    }
}

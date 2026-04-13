package com.hrm.repository;

import com.hrm.config.DatabaseConfig;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class ViolationRepository {

    public void saveEmployeeAudit(String employeeId, List<Integer> ruleIds) {
        String sql = "INSERT INTO employee_audit_reports (employee_id, rule_ids) VALUES (?, ?)";
        saveToAuditTable(sql, employeeId, ruleIds);
    }

    public void saveSystemAudit(String configId, List<Integer> ruleIds) {
        String sql = "INSERT INTO system_audit_reports (config_id, rule_ids) VALUES (?, ?)";
        saveToAuditTable(sql, configId, ruleIds);
    }
    
    private void saveToAuditTable(String sql, String id, List<Integer> ruleIds) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            Array ruleIdsArray = conn.createArrayOf("integer", ruleIds.toArray());
            pstmt.setString(1, id);
            pstmt.setArray(2, ruleIdsArray);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error saving audit report for " + id + ": " + e.getMessage());
        }
    }

    public void clearOldReports() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM employee_audit_reports")) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM system_audit_reports")) {
                stmt.executeUpdate();
            }
            System.out.println("All audit tables cleared.");
        } catch (SQLException e) {
            System.err.println("Error clearing reports: " + e.getMessage());
        }
    }
}

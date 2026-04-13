package com.hrm.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.config.DatabaseConfig;
import com.hrm.model.Rule;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RuleRepository {
    private final ObjectMapper mapper = new ObjectMapper();

    public void saveRule(int id, String name, String content, String category) {
        String sql = "INSERT INTO business_rules (rule_id, rule_name, rule_content, audit_category) VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT (rule_name) DO UPDATE SET rule_content = EXCLUDED.rule_content, audit_category = EXCLUDED.audit_category";
        
        try {
            JsonNode root = mapper.readTree(content);
            if (root.has("content")) {
                content = root.get("content").toString();
            }
        } catch (Exception e) { }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            pstmt.setString(2, name);
            pstmt.setString(3, content);
            pstmt.setString(4, category);
            pstmt.executeUpdate();
            System.out.println("Rule saved with ID " + id + ": " + name);
            
        } catch (SQLException e) {
            System.err.println("Error saving rule " + name + ": " + e.getMessage());
        }
    }

    public List<Rule> getAllActiveRules() {
        List<Rule> rules = new ArrayList<>();
        String sql = "SELECT rule_id, rule_name, audit_category, rule_content FROM business_rules WHERE is_active = TRUE";
        
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                rules.add(new Rule(
                    rs.getInt("rule_id"),
                    rs.getString("rule_name"),
                    rs.getString("audit_category"),
                    rs.getString("rule_content")
                ));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching active rules: " + e.getMessage());
        }
        return rules;
    }

    public boolean isRuleTableEmpty() {
        String sql = "SELECT COUNT(*) FROM business_rules";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking rules table: " + e.getMessage());
        }
        return true;
    }
}

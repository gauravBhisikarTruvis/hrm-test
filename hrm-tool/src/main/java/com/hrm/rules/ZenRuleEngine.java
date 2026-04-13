package com.hrm.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.model.*;
import com.hrm.repository.RuleRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ZenRuleEngine {
    private final List<Rule> activeRules = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public ZenRuleEngine() {
        refreshRules();
    }

    public void refreshRules() {
        activeRules.clear();
        RuleRepository repo = new RuleRepository();
        activeRules.addAll(repo.getAllActiveRules());
        System.out.println("Engine: Loaded " + activeRules.size() + " Enterprise GoRules from DB.");
    }

    public List<Violation> executeAudit(DataBundle bundle) {
        if (activeRules.isEmpty()) {
            System.err.println("Engine: No active rules found in DB. Audit aborted.");
            return Collections.emptyList();
        }

        List<Violation> violations = new ArrayList<>();
        java.util.Set<String> resignedEmployeeIds = new java.util.HashSet<>();

        // Phase 1: Audit Employees & Discover Metadata (Chaining Source)
        if (bundle.employees() != null) {
            System.out.println("Engine Phase 1: Auditing " + bundle.employees().size() + " employees...");
            for (Employee emp : bundle.employees()) {
                for (Rule rule : activeRules) {
                    JsonNode table = findDecisionTable(rule.getContent());
                    if (table == null) continue;

                    // Standard Audit
                    if (rule.getName().equals("underage_check") && evaluateDecisionTable(emp.getAge(), table)) {
                        System.out.println("  [!] Underage violation: Emp " + emp.getEmployeeId() + " (Age: " + emp.getAge() + ")");
                        violations.add(new Violation("EMPLOYEE", emp.getEmployeeId(), rule.getId()));
                    }
                    if (rule.getName().equals("pre_employment_screening") && emp.getEmployeeType() != null && evaluateStringDecisionTable(emp.getEmployeeType(), table)) {
                        violations.add(new Violation("EMPLOYEE", emp.getEmployeeId(), rule.getId()));
                    }

                    // Metadata Discovery (CHAINING LOGIC)
                    if (rule.getName().equals("resigned_status_discovery") && evaluateStringDecisionTable(emp.getDateOfExit(), table)) {
                        resignedEmployeeIds.add(emp.getEmployeeId());
                    }
                }
            }
            System.out.println("Engine: Phase 1 complete. Found " + resignedEmployeeIds.size() + " resigned employees for chaining.");
        }

        // Phase 2: Audit System Data (Chained Correlative Audit)
        if (bundle.systemData() != null) {
            System.out.println("Engine Phase 2: Auditing System data...");
            processSystemData(bundle.systemData(), violations, resignedEmployeeIds);
        }

        return violations;
    }

    private void processSystemData(JsonNode systemData, List<Violation> violations, java.util.Set<String> resignedIds) {
        if (systemData.has("leaves")) {
            JsonNode leaveData = systemData.get("leaves").get("data");
            for (JsonNode entry : leaveData) {
                for (Rule rule : activeRules) {
                    if (rule.getName().equals("mass_payroll_error_prevention") && entry.has("Paid/Unpaid") && entry.get("Paid/Unpaid").asText().equalsIgnoreCase("Unpaid")) {
                        violations.add(new Violation("SYSTEM", entry.get("Employee Id").asText(), rule.getId()));
                    }
                }
            }
        }

        if (systemData.has("expenses")) {
            JsonNode expenseData = systemData.get("expenses").get("data");
            for (JsonNode entry : expenseData) {
                for (Rule rule : activeRules) {
                    if (rule.getName().equals("maker_checker_approval") && entry.has("acted_by")) {
                        String claimedBy = entry.get("claimed_by").asText();
                        String actedBy = entry.get("acted_by").asText();
                        if (claimedBy.contains(actedBy) || actedBy.equalsIgnoreCase("System")) {
                            violations.add(new Violation("SYSTEM", entry.get("transaction_id").asText(), rule.getId()));
                        }
                    }
                }
            }
        }

        if (systemData.has("access_logs")) {
            JsonNode logs = systemData.get("access_logs").get("data");
            for (JsonNode entry : logs) {
                String empId = entry.has("employee_id") ? entry.get("employee_id").asText() : null;
                
                for (Rule rule : activeRules) {
                    JsonNode table = findDecisionTable(rule.getContent());
                    if (table == null) continue;

                    // CHAINED RULE DEPENDENCY
                    if (rule.getName().equals("post_resignation_activity_violation")) {
                        if (resignedIds.contains(empId) && evaluateStringDecisionTable(entry.get("action").asText(), table)) {
                            System.out.println("  [!!] CHAIN HIT: Unauthorized Access by Resigned Employee " + empId);
                            violations.add(new Violation("EMPLOYEE", empId, rule.getId()));
                        }
                    }

                    if (rule.getName().equals("salary_download_resigned") && entry.has("action") && evaluateStringDecisionTable(entry.get("action").asText(), table)) {
                        violations.add(new Violation("EMPLOYEE", entry.get("employee_id").asText(), rule.getId()));
                    }
                    if (rule.getName().equals("pii_exposure_masking") && entry.has("user_role") && evaluateStringDecisionTable(entry.get("user_role").asText(), table)) {
                        violations.add(new Violation("EMPLOYEE", entry.get("employee_id").asText(), rule.getId()));
                    }
                }
            }
        }
    }

    private JsonNode findDecisionTable(String graphJson) {
        try {
            JsonNode root = mapper.readTree(graphJson);
            if (!root.has("nodes")) return null;
            for (JsonNode node : root.get("nodes")) {
                if (node.get("type").asText().equals("decisionTableNode")) {
                    return node.get("content");
                }
            }
        } catch (Exception e) {}
        return null;
    }

    private boolean evaluateDecisionTable(int actual, JsonNode tableContent) {
        try {
            JsonNode rules = tableContent.get("rules");
            if (rules == null || !rules.isArray()) return false;

            for (JsonNode r : rules) {
                String inputKey = null;
                java.util.Iterator<String> names = r.fieldNames();
                while (names.hasNext()) {
                    String name = names.next();
                    if (name.startsWith("in-")) {
                        inputKey = name;
                        break;
                    }
                }
                
                if (inputKey == null) continue;
                
                String cond = r.get(inputKey).asText();
                String outVal = r.get("out-1").asText(); // Use text to be safe
                boolean out = outVal.equalsIgnoreCase("true");
                
                if (cond.startsWith("<") && actual < Integer.parseInt(cond.replace("<", "").trim())) {
                    return out;
                }
                if (cond.startsWith(">=") && actual >= Integer.parseInt(cond.replace(">=", "").trim())) {
                    return out;
                }
            }
        } catch (Exception e) {
            System.err.println("Evaluation error: " + e.getMessage());
        }
        return false;
    }

    private boolean evaluateStringDecisionTable(String actual, JsonNode tableContent) {
        try {
            JsonNode rules = tableContent.get("rules");
            if (rules == null || !rules.isArray()) return false;

            for (JsonNode r : rules) {
                String inputKey = null;
                java.util.Iterator<String> names = r.fieldNames();
                while (names.hasNext()) {
                    String name = names.next();
                    if (name.startsWith("in-")) {
                        inputKey = name;
                        break;
                    }
                }
                
                if (inputKey == null) continue;
                
                String cond = r.get(inputKey).asText();
                String outVal = r.get("out-1").asText();
                boolean out = outVal.equalsIgnoreCase("true");
                
                if (cond.startsWith("contains ") && actual.contains(cond.replace("contains", "").trim())) {
                    return out;
                }
                if (cond.startsWith("not ") && !actual.equalsIgnoreCase(cond.replace("not", "").trim())) {
                    return out;
                }
                if (actual.equalsIgnoreCase(cond.trim())) {
                    return out;
                }
            }
        } catch (Exception e) {}
        return false;
    }
}

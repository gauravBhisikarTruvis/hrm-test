package com.hrm.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.model.Employee;
import com.hrm.model.EmployeeData;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;

public class DataFetcher {
    private final ObjectMapper mapper = new ObjectMapper();

    public List<Employee> fetchEmployees() {
        try {
            File file = new File("../mock_data/mock_employee.json");
            if (!file.exists()) file = new File("./mock_data/mock_employee.json");
            
            if (!file.exists()) {
                System.err.println("Employee mock data NOT FOUND at " + file.getAbsolutePath());
                return Collections.emptyList();
            }
            
            System.out.println("Loading Employee data: " + file.getAbsolutePath());
            try (FileInputStream fis = new FileInputStream(file)) {
                EmployeeData data = mapper.readValue(fis, EmployeeData.class);
                List<Employee> list = data.getEmployees() != null ? data.getEmployees() : Collections.emptyList();
                System.out.println("Successfully loaded " + list.size() + " employees.");
                return list;
            }
        } catch (Exception e) {
            System.err.println("Error fetching employees: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public JsonNode fetchSystemData() {
        try {
            File file = new File("../mock_data/mock_system.json");
            if (!file.exists()) file = new File("./mock_data/mock_system.json");
            
            if (!file.exists()) {
                System.err.println("System mock data NOT FOUND at " + file.getAbsolutePath());
                return null;
            }
            
            System.out.println("Loading System audit data: " + file.getAbsolutePath());
            try (FileInputStream fis = new FileInputStream(file)) {
                JsonNode node = mapper.readTree(fis);
                System.out.println("Successfully loaded System audit mock data.");
                return node;
            }
        } catch (Exception e) {
            System.err.println("Error fetching system data: " + e.getMessage());
            return null;
        }
    }
}

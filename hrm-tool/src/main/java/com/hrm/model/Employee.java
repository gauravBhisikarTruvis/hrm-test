package com.hrm.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Employee {

    @JsonProperty("employee_id")
    private String employeeId;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("date_of_birth")
    private String dateOfBirth;

    private int age;

    @JsonProperty("date_of_exit")
    private String dateOfExit;

    @JsonProperty("department_name")
    private String departmentName;

    @JsonProperty("grade")
    private String grade;

    @JsonProperty("employee_type")
    private String employeeType;

    // Getters and Setters
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getEmployeeType() { return employeeType; }
    public void setEmployeeType(String employeeType) { this.employeeType = employeeType; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { 
        this.dateOfBirth = dateOfBirth;
        calculateAge();
    }

    public int getAge() { 
        if (this.age == 0) calculateAge();
        return age; 
    }

    public String getDateOfExit() { return dateOfExit; }
    public void setDateOfExit(String dateOfExit) { this.dateOfExit = dateOfExit; }

    private void calculateAge() {
        if (dateOfBirth == null || dateOfBirth.isEmpty()) {
            this.age = 0;
            return;
        }
        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy", java.util.Locale.ENGLISH);
            java.time.LocalDate birthDate = java.time.LocalDate.parse(dateOfBirth, formatter);
            this.age = java.time.Period.between(birthDate, java.time.LocalDate.now()).getYears();
        } catch (Exception e) {
            this.age = 0;
        }
    }

    @Override
    public String toString() {
        return "Employee{" +
                "id='" + employeeId + '\'' +
                ", name='" + firstName + " " + lastName + '\'' +
                ", age=" + age +
                '}';
    }
}

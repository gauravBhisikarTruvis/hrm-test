package com.hrm.model;

/**
 * Represents a global system configuration (e.g., payroll calendars).
 */
public record SystemConfig(String configId, String configName, String value) { }

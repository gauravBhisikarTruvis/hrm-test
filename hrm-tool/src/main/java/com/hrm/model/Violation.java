package com.hrm.model;

/**
 * A generic audit violation record.
 * Entities are categorized as EMPLOYEE (for individuals and security actions) 
 * or SYSTEM (for global configurations).
 */
public record Violation(
    String entityType, 
    String entityId, 
    int ruleId
) { }

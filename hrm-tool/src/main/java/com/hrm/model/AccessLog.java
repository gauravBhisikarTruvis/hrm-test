package com.hrm.model;

/**
 * Represents a security or access event (e.g., bulk downloads).
 */
public record AccessLog(String logId, String userId, String action, String resource) { }

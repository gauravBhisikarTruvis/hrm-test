package com.hrm.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * A container for all data sources required for a full system audit.
 */
public record DataBundle(
    List<Employee> employees,
    JsonNode systemData
) { }

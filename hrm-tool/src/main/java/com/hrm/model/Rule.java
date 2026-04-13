package com.hrm.model;

public class Rule {
    private int id;
    private String name;
    private String auditCategory;
    private String content;

    public Rule(int id, String name, String auditCategory, String content) {
        this.id = id;
        this.name = name;
        this.auditCategory = auditCategory;
        this.content = content;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getAuditCategory() { return auditCategory; }
    public String getContent() { return content; }
}

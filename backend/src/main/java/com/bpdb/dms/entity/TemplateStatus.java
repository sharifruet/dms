package com.bpdb.dms.entity;

/**
 * Template status
 */
public enum TemplateStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    DRAFT("Draft"),
    ARCHIVED("Archived");
    
    private final String displayName;
    
    TemplateStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

package com.bpdb.dms.entity;

/**
 * Audit status enumeration
 */
public enum AuditStatus {
    SUCCESS("Success"),
    FAILURE("Failure"),
    WARNING("Warning");
    
    private final String displayName;
    
    AuditStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

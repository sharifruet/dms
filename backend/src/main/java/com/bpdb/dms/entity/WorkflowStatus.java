package com.bpdb.dms.entity;

/**
 * Workflow status
 */
public enum WorkflowStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    SUSPENDED("Suspended"),
    COMPLETED("Completed"),
    FAILED("Failed");
    
    private final String displayName;
    
    WorkflowStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

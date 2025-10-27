package com.bpdb.dms.entity;

/**
 * Workflow instance status
 */
public enum WorkflowInstanceStatus {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled"),
    FAILED("Failed"),
    SUSPENDED("Suspended");
    
    private final String displayName;
    
    WorkflowInstanceStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

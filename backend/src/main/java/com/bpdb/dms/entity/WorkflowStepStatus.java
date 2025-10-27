package com.bpdb.dms.entity;

/**
 * Workflow step status
 */
public enum WorkflowStepStatus {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    REJECTED("Rejected"),
    SKIPPED("Skipped"),
    FAILED("Failed");
    
    private final String displayName;
    
    WorkflowStepStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

package com.bpdb.dms.entity;

/**
 * Workflow step types
 */
public enum WorkflowStepType {
    APPROVAL("Approval"),
    REVIEW("Review"),
    NOTIFICATION("Notification"),
    CONDITIONAL("Conditional"),
    AUTOMATED("Automated"),
    MANUAL("Manual"),
    PARALLEL("Parallel"),
    SEQUENTIAL("Sequential");
    
    private final String displayName;
    
    WorkflowStepType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

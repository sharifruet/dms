package com.bpdb.dms.entity;

/**
 * Types of workflows
 */
public enum WorkflowType {
    DOCUMENT_APPROVAL("Document Approval"),
    DOCUMENT_REVIEW("Document Review"),
    DOCUMENT_PUBLISH("Document Publish"),
    DOCUMENT_ARCHIVE("Document Archive"),
    DOCUMENT_EXPIRY("Document Expiry"),
    USER_ONBOARDING("User Onboarding"),
    COMPLIANCE_CHECK("Compliance Check"),
    CUSTOM_WORKFLOW("Custom Workflow");
    
    private final String displayName;
    
    WorkflowType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

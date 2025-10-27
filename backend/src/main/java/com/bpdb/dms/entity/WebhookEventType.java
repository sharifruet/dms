package com.bpdb.dms.entity;

/**
 * Webhook event types
 */
public enum WebhookEventType {
    DOCUMENT_CREATED("Document Created"),
    DOCUMENT_UPDATED("Document Updated"),
    DOCUMENT_DELETED("Document Deleted"),
    DOCUMENT_APPROVED("Document Approved"),
    DOCUMENT_REJECTED("Document Rejected"),
    USER_CREATED("User Created"),
    USER_UPDATED("User Updated"),
    USER_DELETED("User Deleted"),
    WORKFLOW_STARTED("Workflow Started"),
    WORKFLOW_COMPLETED("Workflow Completed"),
    WORKFLOW_FAILED("Workflow Failed"),
    NOTIFICATION_SENT("Notification Sent"),
    EXPIRY_ALERT("Expiry Alert"),
    SYSTEM_ERROR("System Error"),
    CUSTOM_EVENT("Custom Event");
    
    private final String displayName;
    
    WebhookEventType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

package com.bpdb.dms.entity;

/**
 * Webhook status
 */
public enum WebhookStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    SUSPENDED("Suspended"),
    FAILED("Failed");
    
    private final String displayName;
    
    WebhookStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

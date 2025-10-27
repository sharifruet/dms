package com.bpdb.dms.entity;

/**
 * Notification status
 */
public enum NotificationStatus {
    PENDING("Pending"),
    SENT("Sent"),
    DELIVERED("Delivered"),
    READ("Read"),
    FAILED("Failed"),
    CANCELLED("Cancelled"),
    EXPIRED("Expired");
    
    private final String displayName;
    
    NotificationStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

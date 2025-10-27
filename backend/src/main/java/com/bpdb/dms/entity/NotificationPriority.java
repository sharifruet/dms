package com.bpdb.dms.entity;

/**
 * Notification priority levels
 */
public enum NotificationPriority {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical");
    
    private final String displayName;
    
    NotificationPriority(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

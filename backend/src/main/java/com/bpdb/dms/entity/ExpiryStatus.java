package com.bpdb.dms.entity;

public enum ExpiryStatus {
    ACTIVE("Active"),
    EXPIRED("Expired"),
    RENEWED("Renewed"),
    CANCELLED("Cancelled"),
    SUSPENDED("Suspended");
    
    private final String displayName;
    
    ExpiryStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

package com.bpdb.dms.entity;

/**
 * Health status
 */
public enum HealthStatus {
    HEALTHY("Healthy"),
    WARNING("Warning"),
    CRITICAL("Critical"),
    UNKNOWN("Unknown"),
    FAILED("Failed");
    
    private final String displayName;
    
    HealthStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

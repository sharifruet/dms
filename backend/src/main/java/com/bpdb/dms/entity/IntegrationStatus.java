package com.bpdb.dms.entity;

/**
 * Integration status
 */
public enum IntegrationStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    CONFIGURING("Configuring"),
    TESTING("Testing"),
    FAILED("Failed"),
    SUSPENDED("Suspended"),
    MAINTENANCE("Maintenance");
    
    private final String displayName;
    
    IntegrationStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

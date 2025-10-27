package com.bpdb.dms.entity;

public enum DashboardType {
    EXECUTIVE("Executive Dashboard"),
    DEPARTMENT("Department Dashboard"),
    USER("User Dashboard"),
    SYSTEM("System Dashboard"),
    COMPLIANCE("Compliance Dashboard"),
    CUSTOM("Custom Dashboard");
    
    private final String displayName;
    
    DashboardType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

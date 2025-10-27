package com.bpdb.dms.entity;

public enum ReportStatus {
    PENDING("Pending"),
    GENERATING("Generating"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    EXPIRED("Expired"),
    CANCELLED("Cancelled");
    
    private final String displayName;
    
    ReportStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

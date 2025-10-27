package com.bpdb.dms.entity;

public enum MetricType {
    DOCUMENT_COUNT("Document Count"),
    USER_ACTIVITY("User Activity"),
    STORAGE_USAGE("Storage Usage"),
    SYSTEM_PERFORMANCE("System Performance"),
    EXPIRY_METRICS("Expiry Metrics"),
    SEARCH_METRICS("Search Metrics"),
    UPLOAD_METRICS("Upload Metrics"),
    ACCESS_METRICS("Access Metrics"),
    COMPLIANCE_METRICS("Compliance Metrics"),
    CUSTOM_METRIC("Custom Metric");
    
    private final String displayName;
    
    MetricType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

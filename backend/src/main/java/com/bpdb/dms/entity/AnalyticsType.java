package com.bpdb.dms.entity;

/**
 * Analytics data types
 */
public enum AnalyticsType {
    USER_ACTIVITY("User Activity"),
    DOCUMENT_USAGE("Document Usage"),
    SYSTEM_PERFORMANCE("System Performance"),
    WORKFLOW_METRICS("Workflow Metrics"),
    SEARCH_ANALYTICS("Search Analytics"),
    STORAGE_METRICS("Storage Metrics"),
    SECURITY_EVENTS("Security Events"),
    INTEGRATION_METRICS("Integration Metrics"),
    BUSINESS_METRICS("Business Metrics"),
    PREDICTIVE_ANALYTICS("Predictive Analytics"),
    COMPLIANCE_METRICS("Compliance Metrics"),
    COST_ANALYTICS("Cost Analytics"),
    PRODUCTIVITY_METRICS("Productivity Metrics"),
    QUALITY_METRICS("Quality Metrics"),
    CUSTOM_METRIC("Custom Metric");
    
    private final String displayName;
    
    AnalyticsType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

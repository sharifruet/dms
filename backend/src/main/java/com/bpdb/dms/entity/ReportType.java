package com.bpdb.dms.entity;

/**
 * Types of reports that can be generated
 */
public enum ReportType {
    DOCUMENT_SUMMARY("Document Summary"),
    USER_ACTIVITY("User Activity"),
    EXPIRY_REPORT("Expiry Report"),
    DEPARTMENT_SUMMARY("Department Summary"),
    VENDOR_SUMMARY("Vendor Summary"),
    AUDIT_REPORT("Audit Report"),
    SYSTEM_PERFORMANCE("System Performance"),
    STORAGE_UTILIZATION("Storage Utilization"),
    COMPLIANCE_REPORT("Compliance Report"),
    CUSTOM_REPORT("Custom Report");
    
    private final String displayName;
    
    ReportType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

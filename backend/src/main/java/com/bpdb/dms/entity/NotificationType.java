package com.bpdb.dms.entity;

/**
 * Notification types for different system events
 */
public enum NotificationType {
    DOCUMENT_UPLOAD("Document Upload"),
    DOCUMENT_EXPIRY("Document Expiry"),
    CONTRACT_EXPIRY("Contract Expiry"),
    BG_EXPIRY("Bank Guarantee Expiry"),
    LC_EXPIRY("Letter of Credit Expiry"),
    PS_EXPIRY("Performance Security Expiry"),
    RENEWAL_REMINDER("Renewal Reminder"),
    COMPLIANCE_ALERT("Compliance Alert"),
    SYSTEM_ALERT("System Alert"),
    USER_INVITATION("User Invitation"),
    PASSWORD_RESET("Password Reset"),
    AUDIT_REPORT("Audit Report"),
    BACKUP_COMPLETE("Backup Complete"),
    SECURITY_ALERT("Security Alert"),
    MAINTENANCE_NOTICE("Maintenance Notice");
    
    private final String displayName;
    
    NotificationType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

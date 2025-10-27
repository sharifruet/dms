package com.bpdb.dms.entity;

/**
 * Integration types for enterprise systems
 */
public enum IntegrationType {
    ERP_SYSTEM("ERP System"),
    CRM_SYSTEM("CRM System"),
    HR_SYSTEM("HR System"),
    ACCOUNTING_SYSTEM("Accounting System"),
    EMAIL_SYSTEM("Email System"),
    CALENDAR_SYSTEM("Calendar System"),
    FILE_STORAGE("File Storage"),
    BACKUP_SYSTEM("Backup System"),
    MONITORING_SYSTEM("Monitoring System"),
    LDAP_AD("LDAP/Active Directory"),
    SSO_PROVIDER("SSO Provider"),
    API_GATEWAY("API Gateway"),
    MESSAGE_QUEUE("Message Queue"),
    DATABASE_SYSTEM("Database System"),
    CUSTOM_INTEGRATION("Custom Integration");
    
    private final String displayName;
    
    IntegrationType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

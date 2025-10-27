package com.bpdb.dms.entity;

/**
 * Template types
 */
public enum TemplateType {
    CONTRACT("Contract"),
    AGREEMENT("Agreement"),
    LETTER("Letter"),
    REPORT("Report"),
    FORM("Form"),
    CERTIFICATE("Certificate"),
    INVOICE("Invoice"),
    QUOTATION("Quotation"),
    PROPOSAL("Proposal"),
    PRESENTATION("Presentation"),
    MANUAL("Manual"),
    POLICY("Policy"),
    PROCEDURE("Procedure"),
    CHECKLIST("Checklist"),
    CUSTOM("Custom");
    
    private final String displayName;
    
    TemplateType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

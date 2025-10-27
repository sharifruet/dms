package com.bpdb.dms.entity;

/**
 * Types of document expiries that can be tracked
 */
public enum ExpiryType {
    CONTRACT("Contract"),
    BANK_GUARANTEE("Bank Guarantee"),
    LETTER_OF_CREDIT("Letter of Credit"),
    PERFORMANCE_SECURITY("Performance Security"),
    WARRANTY("Warranty"),
    INSURANCE("Insurance"),
    LICENSE("License"),
    PERMIT("Permit"),
    CERTIFICATE("Certificate"),
    OTHER("Other");
    
    private final String displayName;
    
    ExpiryType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

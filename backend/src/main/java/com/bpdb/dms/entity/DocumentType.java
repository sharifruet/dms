package com.bpdb.dms.entity;

/**
 * Document types supported by the DMS system
 */
public enum DocumentType {
    TENDER("Tender Document"),
    PURCHASE_ORDER("Purchase Order"),
    LETTER_OF_CREDIT("Letter of Credit"),
    BANK_GUARANTEE("Bank Guarantee"),
    CONTRACT("Contract"),
    CORRESPONDENCE("Correspondence"),
    STATIONERY_RECORD("Stationery Record"),
    OTHER("Other");
    
    private final String displayName;
    
    DocumentType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

package com.bpdb.dms.entity;

/**
 * Types of relationships between documents
 */
public enum DocumentRelationshipType {
    CONTRACT_TO_LC("Contract → Letter of Credit"),
    LC_TO_BG("Letter of Credit → Bank Guarantee"),
    BG_TO_PO("Bank Guarantee → Purchase Order"),
    PO_TO_CORRESPONDENCE("Purchase Order → Correspondence"),
    CONTRACT_TO_BG("Contract → Bank Guarantee"),
    CONTRACT_TO_PO("Contract → Purchase Order"),
    LC_TO_PO("Letter of Credit → Purchase Order"),
    BG_TO_CORRESPONDENCE("Bank Guarantee → Correspondence"),
    OTHER("Other Relationship");
    
    private final String displayName;
    
    DocumentRelationshipType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}


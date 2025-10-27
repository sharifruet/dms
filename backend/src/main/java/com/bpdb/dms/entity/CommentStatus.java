package com.bpdb.dms.entity;

/**
 * Comment status
 */
public enum CommentStatus {
    ACTIVE("Active"),
    RESOLVED("Resolved"),
    ARCHIVED("Archived"),
    DELETED("Deleted");
    
    private final String displayName;
    
    CommentStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

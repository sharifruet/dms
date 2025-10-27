package com.bpdb.dms.entity;

/**
 * Version types
 */
public enum VersionType {
    MAJOR("Major"),
    MINOR("Minor"),
    PATCH("Patch"),
    DRAFT("Draft"),
    FINAL("Final");
    
    private final String displayName;
    
    VersionType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

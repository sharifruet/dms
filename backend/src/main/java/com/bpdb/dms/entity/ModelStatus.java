package com.bpdb.dms.entity;

/**
 * Machine learning model status
 */
public enum ModelStatus {
    TRAINING("Training"),
    TRAINED("Trained"),
    DEPLOYED("Deployed"),
    FAILED("Failed"),
    RETRAINING("Retraining"),
    DEPRECATED("Deprecated"),
    ARCHIVED("Archived");
    
    private final String displayName;
    
    ModelStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

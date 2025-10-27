package com.bpdb.dms.entity;

/**
 * Comment types
 */
public enum CommentType {
    GENERAL("General"),
    REVIEW("Review"),
    APPROVAL("Approval"),
    REJECTION("Rejection"),
    QUESTION("Question"),
    SUGGESTION("Suggestion"),
    ANNOTATION("Annotation"),
    HIGHLIGHT("Highlight");
    
    private final String displayName;
    
    CommentType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

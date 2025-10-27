package com.bpdb.dms.entity;

public enum ReportFormat {
    PDF("PDF"),
    EXCEL("Excel"),
    WORD("Word"),
    CSV("CSV"),
    JSON("JSON"),
    HTML("HTML");
    
    private final String displayName;
    
    ReportFormat(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

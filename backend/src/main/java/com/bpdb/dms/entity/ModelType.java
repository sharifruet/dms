package com.bpdb.dms.entity;

/**
 * Machine learning model types
 */
public enum ModelType {
    DOCUMENT_CLASSIFICATION("Document Classification"),
    SENTIMENT_ANALYSIS("Sentiment Analysis"),
    TEXT_EXTRACTION("Text Extraction"),
    ANOMALY_DETECTION("Anomaly Detection"),
    PREDICTIVE_MAINTENANCE("Predictive Maintenance"),
    RECOMMENDATION_ENGINE("Recommendation Engine"),
    FRAUD_DETECTION("Fraud Detection"),
    RISK_ASSESSMENT("Risk Assessment"),
    PERFORMANCE_PREDICTION("Performance Prediction"),
    USER_BEHAVIOR_ANALYSIS("User Behavior Analysis"),
    CONTENT_MODERATION("Content Moderation"),
    LANGUAGE_TRANSLATION("Language Translation"),
    IMAGE_RECOGNITION("Image Recognition"),
    OPTICAL_CHARACTER_RECOGNITION("OCR"),
    CUSTOM_MODEL("Custom Model");
    
    private final String displayName;
    
    ModelType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

package com.bpdb.dms.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.util.Map;

/**
 * Elasticsearch document entity for search indexing
 */
@Document(indexName = "documents")
public class DocumentIndex {
    
    @Id
    private String id;
    
    @Field(type = FieldType.Long)
    private Long documentId;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String fileName;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String originalName;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String extractedText;
    
    @Field(type = FieldType.Keyword)
    private String documentType;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String tags;
    
    @Field(type = FieldType.Keyword)
    private String department;
    
    @Field(type = FieldType.Keyword)
    private String uploadedBy;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String uploadedByUsername;
    
    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private LocalDate createdAt;
    
    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private LocalDate updatedAt;
    
    @Field(type = FieldType.Object)
    private Map<String, String> metadata;
    
    @Field(type = FieldType.Double)
    private Double ocrConfidence;
    
    @Field(type = FieldType.Double)
    private Double classificationConfidence;
    
    @Field(type = FieldType.Keyword)
    private String mimeType;
    
    @Field(type = FieldType.Long)
    private Long fileSize;
    
    @Field(type = FieldType.Boolean)
    private Boolean isActive;
    
    // Constructors
    public DocumentIndex() {}
    
    public DocumentIndex(Long documentId, String fileName, String originalName, String extractedText) {
        this.documentId = documentId;
        this.fileName = fileName;
        this.originalName = originalName;
        this.extractedText = extractedText;
        this.id = documentId.toString();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    
    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }
    
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    
    public String getUploadedByUsername() { return uploadedByUsername; }
    public void setUploadedByUsername(String uploadedByUsername) { this.uploadedByUsername = uploadedByUsername; }
    
    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }
    
    public LocalDate getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDate updatedAt) { this.updatedAt = updatedAt; }
    
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    
    public Double getOcrConfidence() { return ocrConfidence; }
    public void setOcrConfidence(Double ocrConfidence) { this.ocrConfidence = ocrConfidence; }
    
    public Double getClassificationConfidence() { return classificationConfidence; }
    public void setClassificationConfidence(Double classificationConfidence) { this.classificationConfidence = classificationConfidence; }
    
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}

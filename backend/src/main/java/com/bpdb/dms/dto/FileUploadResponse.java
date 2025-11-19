package com.bpdb.dms.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for file upload operations
 */
public class FileUploadResponse {
    
    private Long documentId;
    private String fileName;
    private String originalName;
    private Long fileSize;
    private String mimeType;
    private String documentType;
    private String message;
    private boolean success;
    private boolean isDuplicate;
    private Long duplicateDocumentId;
    private String duplicateFileName;
    private String duplicateOriginalName;
    private Long duplicateFileSize;
    private String duplicateMimeType;
    private String duplicateDocumentType;
    private LocalDateTime duplicateCreatedAt;
    private String duplicateUploadedBy;
    
    // Constructors
    public FileUploadResponse() {}
    
    public FileUploadResponse(Long documentId, String fileName, String originalName,
                            Long fileSize, String mimeType, String documentType,
                            String message, boolean success) {
        this.documentId = documentId;
        this.fileName = fileName;
        this.originalName = originalName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.documentType = documentType;
        this.message = message;
        this.success = success;
        this.isDuplicate = false;
    }
    
    // Static factory methods
    public static FileUploadResponse success(Long documentId, String fileName, String originalName,
                                           Long fileSize, String mimeType, String documentType) {
        return new FileUploadResponse(documentId, fileName, originalName, fileSize, 
                                    mimeType, documentType, "File uploaded successfully", true);
    }
    
    public static FileUploadResponse error(String message) {
        FileUploadResponse response = new FileUploadResponse();
        response.setMessage(message);
        response.setSuccess(false);
        response.setIsDuplicate(false);
        return response;
    }
    
    public static FileUploadResponse duplicate(Long duplicateDocumentId, String duplicateFileName,
                                             String duplicateOriginalName, Long duplicateFileSize,
                                             String duplicateMimeType, String duplicateDocumentType,
                                             LocalDateTime duplicateCreatedAt, String duplicateUploadedBy,
                                             String message) {
        FileUploadResponse response = new FileUploadResponse();
        response.setIsDuplicate(true);
        response.setSuccess(false);
        response.setDuplicateDocumentId(duplicateDocumentId);
        response.setDuplicateFileName(duplicateFileName);
        response.setDuplicateOriginalName(duplicateOriginalName);
        response.setDuplicateFileSize(duplicateFileSize);
        response.setDuplicateMimeType(duplicateMimeType);
        response.setDuplicateDocumentType(duplicateDocumentType);
        response.setDuplicateCreatedAt(duplicateCreatedAt);
        response.setDuplicateUploadedBy(duplicateUploadedBy);
        response.setMessage(message);
        return response;
    }
    
    // Getters and Setters
    public Long getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getOriginalName() {
        return originalName;
    }
    
    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    public String getDocumentType() {
        return documentType;
    }
    
    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public boolean isDuplicate() {
        return isDuplicate;
    }
    
    public void setIsDuplicate(boolean isDuplicate) {
        this.isDuplicate = isDuplicate;
    }
    
    public Long getDuplicateDocumentId() {
        return duplicateDocumentId;
    }
    
    public void setDuplicateDocumentId(Long duplicateDocumentId) {
        this.duplicateDocumentId = duplicateDocumentId;
    }
    
    public String getDuplicateFileName() {
        return duplicateFileName;
    }
    
    public void setDuplicateFileName(String duplicateFileName) {
        this.duplicateFileName = duplicateFileName;
    }
    
    public String getDuplicateOriginalName() {
        return duplicateOriginalName;
    }
    
    public void setDuplicateOriginalName(String duplicateOriginalName) {
        this.duplicateOriginalName = duplicateOriginalName;
    }
    
    public Long getDuplicateFileSize() {
        return duplicateFileSize;
    }
    
    public void setDuplicateFileSize(Long duplicateFileSize) {
        this.duplicateFileSize = duplicateFileSize;
    }
    
    public String getDuplicateMimeType() {
        return duplicateMimeType;
    }
    
    public void setDuplicateMimeType(String duplicateMimeType) {
        this.duplicateMimeType = duplicateMimeType;
    }
    
    public String getDuplicateDocumentType() {
        return duplicateDocumentType;
    }
    
    public void setDuplicateDocumentType(String duplicateDocumentType) {
        this.duplicateDocumentType = duplicateDocumentType;
    }
    
    public LocalDateTime getDuplicateCreatedAt() {
        return duplicateCreatedAt;
    }
    
    public void setDuplicateCreatedAt(LocalDateTime duplicateCreatedAt) {
        this.duplicateCreatedAt = duplicateCreatedAt;
    }
    
    public String getDuplicateUploadedBy() {
        return duplicateUploadedBy;
    }
    
    public void setDuplicateUploadedBy(String duplicateUploadedBy) {
        this.duplicateUploadedBy = duplicateUploadedBy;
    }
}

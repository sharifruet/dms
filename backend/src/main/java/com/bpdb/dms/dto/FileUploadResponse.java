package com.bpdb.dms.dto;

import com.bpdb.dms.entity.DocumentType;

/**
 * Response DTO for file upload operations
 */
public class FileUploadResponse {
    
    private Long documentId;
    private String fileName;
    private String originalName;
    private Long fileSize;
    private String mimeType;
    private DocumentType documentType;
    private String message;
    private boolean success;
    
    // Constructors
    public FileUploadResponse() {}
    
    public FileUploadResponse(Long documentId, String fileName, String originalName, 
                            Long fileSize, String mimeType, DocumentType documentType, 
                            String message, boolean success) {
        this.documentId = documentId;
        this.fileName = fileName;
        this.originalName = originalName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.documentType = documentType;
        this.message = message;
        this.success = success;
    }
    
    // Static factory methods
    public static FileUploadResponse success(Long documentId, String fileName, String originalName, 
                                           Long fileSize, String mimeType, DocumentType documentType) {
        return new FileUploadResponse(documentId, fileName, originalName, fileSize, 
                                    mimeType, documentType, "File uploaded successfully", true);
    }
    
    public static FileUploadResponse error(String message) {
        FileUploadResponse response = new FileUploadResponse();
        response.setMessage(message);
        response.setSuccess(false);
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
    
    public DocumentType getDocumentType() {
        return documentType;
    }
    
    public void setDocumentType(DocumentType documentType) {
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
}

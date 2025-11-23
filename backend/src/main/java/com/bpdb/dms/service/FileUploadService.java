package com.bpdb.dms.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bpdb.dms.dto.FileUploadResponse;
import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.DocumentMetadata;
import com.bpdb.dms.entity.DocumentMetadata.MetadataSource;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.entity.WorkflowInstance;
import com.bpdb.dms.entity.WorkflowInstanceStatus;
import com.bpdb.dms.entity.WorkflowType;
import com.bpdb.dms.model.DocumentType;
import com.bpdb.dms.repository.DocumentMetadataRepository;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.FolderRepository;
import com.bpdb.dms.repository.WorkflowInstanceRepository;

/**
 * Service for handling file upload operations
 */
@Service
public class FileUploadService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);
    
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;
    
    @Value("${app.max.file.size:104857600}") // 100MB default
    private long maxFileSize;
    
    // Allowed file types
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "image/jpeg",
        "image/png",
        "image/tiff",
        "text/plain"
    );
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private FolderRepository folderRepository;
    
    @Autowired
    private OCRService ocrService;
    
    @Autowired
    private DocumentIndexingService documentIndexingService;
    
    @Autowired
    private AppDocumentService appDocumentService;

    @Autowired
    private AppExcelImportService appExcelImportService;

    @Autowired
    private DocumentMetadataService documentMetadataService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;
    
    @Autowired
    private DocumentVersioningService documentVersioningService;
    
    @Autowired
    private DocumentMetadataRepository documentMetadataRepository;

    /**
     * Upload a single file
     */
    public FileUploadResponse uploadFile(MultipartFile file, User user, String documentType, String description,
                                         Map<String, String> manualMetadata, Long folderId) {
        try {
            // Validate file
            String validationError = validateFile(file);
            if (validationError != null) {
                return FileUploadResponse.error(validationError);
            }

            // Validate and normalize document type
            DocumentType resolvedType = DocumentType.resolve(documentType).orElse(null);
            if (resolvedType == null) {
                String message = "Invalid document type. Allowed types are: " + DocumentType.allowedTypesList();
                logger.warn("Document type validation failed for value '{}': {}", documentType, message);
                return FileUploadResponse.error(message);
            }

            // Enforce workflow for follow-up documents (types 2–7)
            boolean requiresTenderWorkflow = requiresTenderWorkflow(resolvedType);
            Long providedWorkflowInstanceId = null;
            if (requiresTenderWorkflow) {
                if (manualMetadata == null || !manualMetadata.containsKey("tenderWorkflowInstanceId")) {
                    return FileUploadResponse.error("This document type must be uploaded via a Tender workflow. Please provide 'tenderWorkflowInstanceId'.");
                }
                try {
                    providedWorkflowInstanceId = Long.parseLong(manualMetadata.get("tenderWorkflowInstanceId"));
                } catch (NumberFormatException nfe) {
                    return FileUploadResponse.error("Invalid 'tenderWorkflowInstanceId'. It must be a numeric ID.");
                }
                WorkflowInstance instance = workflowInstanceRepository.findById(providedWorkflowInstanceId)
                    .orElse(null);
                if (instance == null) {
                    return FileUploadResponse.error("Workflow instance not found for ID: " + providedWorkflowInstanceId);
                }
                if (instance.getStatus() == WorkflowInstanceStatus.COMPLETED ||
                    instance.getStatus() == WorkflowInstanceStatus.CANCELLED ||
                    instance.getStatus() == WorkflowInstanceStatus.REJECTED) {
                    return FileUploadResponse.error("The referenced workflow instance is not active. Please start a new Tender workflow.");
                }
                // Ensure the workflow is tied to a Tender Notice
                if (instance.getDocument() == null || instance.getDocument().getDocumentType() == null ||
                    !DocumentType.TENDER_NOTICE.name().equals(instance.getDocument().getDocumentType())) {
                    return FileUploadResponse.error("Provided 'tenderWorkflowInstanceId' is not associated with a Tender Notice.");
                }
            }
            
            // Calculate file hash for duplicate detection
            String fileHash = calculateFileHash(file);
            
            // Check for duplicate files
            Optional<Document> existingDocument = documentRepository.findFirstByFileHashAndIsActiveTrue(fileHash);
            if (existingDocument.isPresent()) {
                // Return duplicate information - frontend will handle the decision
                Document duplicate = existingDocument.get();
                return FileUploadResponse.duplicate(
                    duplicate.getId(),
                    duplicate.getFileName(),
                    duplicate.getOriginalName(),
                    duplicate.getFileSize(),
                    duplicate.getMimeType(),
                    duplicate.getDocumentType(),
                    duplicate.getCreatedAt(),
                    duplicate.getUploadedBy() != null ? duplicate.getUploadedBy().getUsername() : "Unknown",
                    "A file with identical content already exists in the system."
                );
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = generateUniqueFilename(fileExtension);
            
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Save file to disk
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Create document record
            Document document = new Document();
            document.setFileName(uniqueFilename);
            document.setOriginalName(originalFilename);
            document.setFilePath(filePath.toString());
            document.setFileSize(file.getSize());
            document.setMimeType(file.getContentType());
            document.setDocumentType(resolvedType.name());
            document.setDescription(description);
            document.setUploadedBy(user);
            document.setDepartment(user.getDepartment());
            document.setIsActive(true);
            document.setFileHash(fileHash);
            
            // Set folder if provided
            if (folderId != null) {
                folderRepository.findById(folderId).ifPresent(document::setFolder);
            }
            
            // Save to database
            Document savedDocument = documentRepository.save(document);

            Map<String, String> combinedMetadata = new HashMap<>();
            if (manualMetadata != null && !manualMetadata.isEmpty()) {
                combinedMetadata.putAll(documentMetadataService.applyManualMetadata(savedDocument, manualMetadata));
            }

            // If Tender Notice, auto-create and start a workflow; store its ID in metadata
            if (resolvedType == DocumentType.TENDER_NOTICE) {
                String definition = "{\"steps\":[{\"order\":1,\"name\":\"Collect Tender Documents (2–7)\",\"type\":\"SEQUENTIAL\"},{\"order\":2,\"name\":\"Review & Finalize\",\"type\":\"APPROVAL\"}]}";
                var workflow = workflowService.createWorkflow(
                    "Tender Package - " + (originalFilename != null ? originalFilename : savedDocument.getId()),
                    "Workflow to collect documents 2–7 for the tender package",
                    WorkflowType.CUSTOM_WORKFLOW,
                    definition,
                    user
                );
                WorkflowInstance instance = workflowService.startWorkflow(workflow.getId(), savedDocument.getId(), user);
                String instanceIdStr = String.valueOf(instance.getId());
                combinedMetadata.put("tenderWorkflowInstanceId", instanceIdStr);
                documentMetadataService.applyManualMetadata(savedDocument, Map.of("tenderWorkflowInstanceId", instanceIdStr));
            }

            // For follow-ups, persist and index the workflow reference if provided
            if (requiresTenderWorkflow && providedWorkflowInstanceId != null) {
                String instanceIdStr = String.valueOf(providedWorkflowInstanceId);
                combinedMetadata.put("tenderWorkflowInstanceId", instanceIdStr);
                // ensure persisted in metadata entries
                documentMetadataService.applyManualMetadata(savedDocument, Map.of("tenderWorkflowInstanceId", instanceIdStr));
            }

            Map<String, String> additionalMetadata = new HashMap<>();
            if (isExcelFile(file.getContentType(), originalFilename)) {
                // For APP documents, import into app_headers and app_lines tables
                if (resolvedType == DocumentType.APP) {
                    try {
                        appExcelImportService.importApp(file, user);
                        additionalMetadata.put("appImportStatus", "success");
                        logger.info("APP data imported into app_headers and app_lines tables for document: {}", savedDocument.getId());
                    } catch (Exception e) {
                        logger.error("Failed to import APP data for document {}: {}", savedDocument.getId(), e.getMessage());
                        additionalMetadata.put("appImportStatus", "failed");
                        additionalMetadata.put("appImportError", e.getMessage());
                    }
                }
                // Also process for app_document_entries
                // Read from saved file path since MultipartFile stream can only be read once
                try {
                    Path savedFilePath = Paths.get(savedDocument.getFilePath());
                    if (Files.exists(savedFilePath)) {
                        // Create a simple MultipartFile implementation from saved file
                        MultipartFile savedFile = createMultipartFileFromPath(savedFilePath, savedDocument);
                        Map<String, String> appDocMetadata = appDocumentService.processAndStoreEntries(savedDocument, savedFile);
                        additionalMetadata.putAll(appDocMetadata);
                        logger.info("APP document entries processed for document: {} ({}), status: {}, entryCount: {}", 
                            savedDocument.getId(), savedDocument.getOriginalName(), 
                            appDocMetadata.get("appStatus"), appDocMetadata.get("appEntryCount"));
                        
                        // Log any errors
                        if ("failed".equals(appDocMetadata.get("appStatus")) || 
                            "unsupported_format".equals(appDocMetadata.get("appStatus"))) {
                            logger.error("APP document processing failed for {}: status={}, error={}, headers={}", 
                                savedDocument.getOriginalName(), 
                                appDocMetadata.get("appStatus"),
                                appDocMetadata.get("appError"),
                                appDocMetadata.get("appHeadersDetected"));
                        }
                    } else {
                        logger.warn("Saved file not found at path: {} for document {}", 
                            savedDocument.getFilePath(), savedDocument.getId());
                        additionalMetadata.put("appDocStatus", "file_not_found");
                    }
                } catch (Exception e) {
                    logger.error("Failed to process APP document entries for document {}: {}", 
                        savedDocument.getId(), e.getMessage(), e);
                    additionalMetadata.put("appDocStatus", "failed");
                    additionalMetadata.put("appDocError", e.getMessage());
                }
                combinedMetadata.putAll(additionalMetadata);
            }
            
            // Process OCR and indexing asynchronously
            processDocumentAsync(savedDocument, file, combinedMetadata);
            
            logger.info("File uploaded successfully: {} by user: {}", originalFilename, user.getUsername());
            
            return FileUploadResponse.success(
                savedDocument.getId(),
                savedDocument.getFileName(),
                savedDocument.getOriginalName(),
                savedDocument.getFileSize(),
                savedDocument.getMimeType(),
                savedDocument.getDocumentType()
            );
            
        } catch (IOException e) {
            logger.error("Error uploading file: {}", e.getMessage());
            return FileUploadResponse.error("Failed to upload file: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during file upload: {}", e.getMessage());
            return FileUploadResponse.error("Unexpected error occurred during upload");
        }
    }
    
    /**
     * Validate uploaded file
     */
    private String validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            return "File is empty";
        }
        
        if (file.getSize() > maxFileSize) {
            return "File size exceeds maximum allowed size of " + (maxFileSize / 1024 / 1024) + "MB";
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            return "File type not supported. Allowed types: PDF, DOC, DOCX, XLS, XLSX, JPEG, PNG, TIFF, TXT";
        }
        
        return null;
    }
    
    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
    
    /**
     * Generate unique filename
     */
    private String generateUniqueFilename(String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return timestamp + "_" + uuid + extension;
    }
    
    /**
     * Get file path for download
     */
    public Path getFilePath(Long documentId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        return Paths.get(document.getFilePath());
    }
    
    /**
     * Delete file from storage and database
     */
    public boolean deleteFile(Long documentId) {
        try {
            Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
            
            // Delete file from storage
            Path filePath = Paths.get(document.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
            
            // Mark as inactive in database (soft delete)
            document.setIsActive(false);
            documentRepository.save(document);
            
            logger.info("File deleted successfully: {}", document.getOriginalName());
            return true;
            
        } catch (Exception e) {
            logger.error("Error deleting file: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Process document with OCR and indexing asynchronously
     */
    @Async
    public void processDocumentAsync(Document document, MultipartFile file, Map<String, String> additionalMetadata) {
        final Long documentId = document.getId();
        
        try {
            logger.info("Starting async processing for document: {}", documentId);

            Document managedDocument = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found for OCR processing: " + documentId));
            
            // Ensure user is loaded
            if (managedDocument.getUploadedBy() != null) {
                managedDocument.getUploadedBy().getId();
            }
            
            // Perform OCR processing
            Map<String, String> combinedMetadata = new HashMap<>(documentMetadataService.getMetadataMap(managedDocument));
            if (additionalMetadata != null) {
                combinedMetadata.putAll(additionalMetadata);
            }
            
            if (!ocrService.isOcrAvailable()) {
                logger.warn("Skipping OCR processing for document {} because OCR service is unavailable", documentId);
                combinedMetadata.put("ocrStatus", "unavailable");
                documentIndexingService.indexDocument(
                    managedDocument,
                    "",
                    combinedMetadata,
                    0.0,
                    0.0
                );
                return;
            }
            
            OCRService.OCRResult ocrResult;
            try {
                ocrResult = ocrService.extractText(file);
                managedDocument.setExtractedText(ocrResult.getExtractedText());
                documentRepository.save(managedDocument);
            } catch (Throwable ocrError) {
                logger.error("OCR extraction threw an error for document {}: {}", documentId, ocrError.getMessage());
                combinedMetadata.put("ocrStatus", "failed");
                String errorMsg = ocrError.getMessage();
                if (errorMsg != null && (errorMsg.contains("TessAPI") || errorMsg.contains("Could not initialize"))) {
                    errorMsg = "Tesseract native library not available. Please install Tesseract on the system. " +
                              "On macOS: brew install tesseract. " +
                              "On Linux: apt-get install tesseract-ocr. " +
                              "Error: " + errorMsg;
                }
                combinedMetadata.put("error", errorMsg != null ? errorMsg : "unknown");
                documentIndexingService.indexDocument(
                    managedDocument,
                    "",
                    combinedMetadata,
                    0.0,
                    0.0
                );
                return;
            }
            
            
            try {
            	Map<String, String> extractedMetadata = ocrService.getContractData(managedDocument.getExtractedText());
				for (Entry<String, String> entry : extractedMetadata.entrySet()) {
					DocumentMetadata docMeta = new DocumentMetadata();
					docMeta.setDocument(managedDocument);
					docMeta.setKey(entry.getKey());
					docMeta.setValue(entry.getValue());
					docMeta.setSource(MetadataSource.AUTO_OCR);	
					documentMetadataRepository.save(docMeta);
				}
            } catch (Exception e) {
            	logger.error("Error extracting contract data from OCR text for document {}: {}", documentId, e.getMessage());
            	throw e;
            }
            
            if (ocrResult != null && ocrResult.isSuccess()) {
                // Update document with OCR results if needed
                if (ocrResult.getDocumentType() != null && managedDocument.getDocumentType() == null) {
                    try {
                        managedDocument.setDocumentType(ocrResult.getDocumentType());
                        documentRepository.save(managedDocument);
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid document type from OCR: {}", ocrResult.getDocumentType());
                    }
                }

                if (ocrResult.getMetadata() != null) {
                    combinedMetadata.putAll(ocrResult.getMetadata());
                }

                combinedMetadata.putAll(documentMetadataService.extractMetadataFromText(managedDocument, ocrResult.getExtractedText()));
                
                // Index document for search
                documentIndexingService.indexDocument(
                    managedDocument,
                    ocrResult.getExtractedText(),
                    combinedMetadata,
                    ocrResult.getConfidence(),
                    ocrResult.getClassificationConfidence()
                );
                
                logger.info("Async processing completed for document: {} - OCR confidence: {}", 
                           documentId, ocrResult.getConfidence());
                
            } else {
                String errorMsg = ocrResult != null ? ocrResult.getErrorMessage() : "Unknown error";
                logger.error("OCR processing failed for document: {} - Error: {}", 
                           documentId, errorMsg);
                
                // Still index the document without OCR text
                if (ocrResult != null && ocrResult.getMetadata() != null) {
                    combinedMetadata.putAll(ocrResult.getMetadata());
                }
                combinedMetadata.put("ocrStatus", "failed");
                if (errorMsg != null && !errorMsg.isEmpty()) {
                    combinedMetadata.put("ocrError", errorMsg);
                }
                documentIndexingService.indexDocument(
                    managedDocument,
                    "",
                    combinedMetadata,
                    0.0,
                    0.0
                );
            }
            
        } catch (Throwable e) {
            // Catch any remaining errors to prevent async task from crashing
            logger.error("Async processing failed for document: {} - Error type: {}, Message: {}", 
                        documentId, e.getClass().getName(), e.getMessage());
            
            // Still try to index the document even if OCR failed
            try {
                // Refetch document for indexing in case of error
                Document docForIndex = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
                if (docForIndex.getUploadedBy() != null) {
                    docForIndex.getUploadedBy().getId();
                }
                documentIndexingService.indexDocument(
                    docForIndex,
                    "",
                    new HashMap<>(),
                    0.0,
                    0.0
                );
            } catch (Exception indexError) {
                logger.error("Failed to index document {} after OCR failure: {}", 
                            documentId, indexError.getMessage());
            }
        }
    }
    
    /**
     * Re-process OCR for an existing document
     */
    @Async
    public void reprocessOCR(Long documentId) {
        try {
            logger.info("Starting OCR re-processing for document: {}", documentId);
            
            // Note: We fetch document here but processDocumentAsync will refetch it 
            // in its own async context to ensure user is loaded
            Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
            
            // Check if file exists
            Path filePath = Paths.get(document.getFilePath());
            if (!Files.exists(filePath)) {
                logger.error("File not found for document {}: {}", documentId, document.getFilePath());
                return;
            }
            
            // Create a MultipartFile wrapper from the existing file
            File file = filePath.toFile();
            byte[] fileBytes = Files.readAllBytes(filePath);
            
            MultipartFile multipartFile = new MultipartFile() {
                @Override
                public String getName() {
                    return "file";
                }

                @Override
                public String getOriginalFilename() {
                    return document.getOriginalName();
                }

                @Override
                public String getContentType() {
                    return document.getMimeType();
                }

                @Override
                public boolean isEmpty() {
                    return fileBytes.length == 0;
                }

                @Override
                public long getSize() {
                    return fileBytes.length;
                }

                @Override
                public byte[] getBytes() throws IOException {
                    return fileBytes;
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return new FileInputStream(file);
                }

                @Override
                public void transferTo(File dest) throws IOException, IllegalStateException {
                    Files.copy(filePath, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            };
            
            // Process with OCR
            processDocumentAsync(document, multipartFile, new HashMap<>());
            
            logger.info("OCR re-processing triggered for document: {}", documentId);
            
        } catch (Exception e) {
            logger.error("Error re-processing OCR for document {}: {}", documentId, e.getMessage(), e);
        }
    }
    
    /**
     * Re-process OCR for all documents (or documents without OCR text)
     */
    @Async
    public void reprocessAllDocumentsOCR() {
        try {
            logger.info("Starting OCR re-processing for all documents");
            
            List<Document> documents = documentRepository.findByIsActiveTrue(
                org.springframework.data.domain.PageRequest.of(0, 1000)
            ).getContent();
            
            int processed = 0;
            int failed = 0;
            
            for (Document document : documents) {
                try {
                    reprocessOCR(document.getId());
                    processed++;
                } catch (Exception e) {
                    logger.error("Failed to re-process OCR for document {}: {}", 
                               document.getId(), e.getMessage());
                    failed++;
                }
            }
            
            logger.info("OCR re-processing completed. Processed: {}, Failed: {}", processed, failed);
            
        } catch (Exception e) {
            logger.error("Error re-processing OCR for all documents: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Create a MultipartFile from a saved file path
     */
    private MultipartFile createMultipartFileFromPath(Path filePath, Document document) {
        return new MultipartFile() {
            @Override
            public String getName() {
                return "file";
            }

            @Override
            public String getOriginalFilename() {
                return document.getOriginalName();
            }

            @Override
            public String getContentType() {
                return document.getMimeType();
            }

            @Override
            public boolean isEmpty() {
                try {
                    return Files.size(filePath) == 0;
                } catch (IOException e) {
                    return true;
                }
            }

            @Override
            public long getSize() {
                try {
                    return Files.size(filePath);
                } catch (IOException e) {
                    return 0;
                }
            }

            @Override
            public byte[] getBytes() throws IOException {
                return Files.readAllBytes(filePath);
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return Files.newInputStream(filePath);
            }

            @Override
            public void transferTo(File dest) throws IOException, IllegalStateException {
                Files.copy(filePath, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        };
    }

    private boolean requiresTenderWorkflow(DocumentType type) {
        return type == DocumentType.TENDER_DOCUMENT
            || type == DocumentType.CONTRACT_AGREEMENT
            || type == DocumentType.BANK_GUARANTEE_BG
            || type == DocumentType.PERFORMANCE_SECURITY_PS
            || type == DocumentType.PERFORMANCE_GUARANTEE_PG;
    }

    private boolean isExcelFile(String contentType, String originalFilename) {
        if (contentType != null && contentType.contains("excel")) {
            return true;
        }
        if (originalFilename == null) {
            return false;
        }
        String lowerName = originalFilename.toLowerCase();
        return lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx");
    }
    
    /**
     * Handle duplicate file upload with user's choice
     * @param file The file being uploaded
     * @param user The user uploading
     * @param documentType Document type
     * @param description Description
     * @param manualMetadata Manual metadata
     * @param folderId Folder ID
     * @param duplicateDocumentId The ID of the duplicate document
     * @param action The action to take: "skip", "version", or "replace"
     * @return FileUploadResponse
     */
    public FileUploadResponse handleDuplicateUpload(MultipartFile file, User user, String documentType,
                                                    String description, Map<String, String> manualMetadata,
                                                    Long folderId, Long duplicateDocumentId, String action) {
        try {
            // Verify duplicate document exists
            if (!documentRepository.existsById(duplicateDocumentId)) {
                return FileUploadResponse.error("Duplicate document not found");
            }
            
            switch (action.toLowerCase()) {
                case "skip":
                    return FileUploadResponse.error("Upload skipped - duplicate file already exists");
                    
                case "version":
                    // Upload as a new version of the duplicate document
                    return uploadAsVersion(file, user, duplicateDocumentId, description, manualMetadata);
                    
                case "replace":
                    // Replace the duplicate document
                    return replaceDocument(file, user, duplicateDocumentId, documentType, description, manualMetadata, folderId);
                    
                default:
                    return FileUploadResponse.error("Invalid action. Must be 'skip', 'version', or 'replace'");
            }
        } catch (Exception e) {
            logger.error("Error handling duplicate upload: {}", e.getMessage());
            return FileUploadResponse.error("Failed to handle duplicate upload: " + e.getMessage());
        }
    }
    
    /**
     * Upload file as a new version of an existing document
     */
    private FileUploadResponse uploadAsVersion(MultipartFile file, User user, Long documentId,
                                              String description, Map<String, String> manualMetadata) {
        try {
            Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
            
            // Create a new version
            com.bpdb.dms.entity.VersionType versionType = com.bpdb.dms.entity.VersionType.PATCH;
            String changeDescription = description != null ? description : "New version uploaded";
            
            documentVersioningService.createDocumentVersion(
                documentId,
                file,
                changeDescription,
                versionType,
                user
            );
            
            logger.info("File uploaded as new version of document: {} by user: {}", 
                      document.getOriginalName(), user.getUsername());
            
            return FileUploadResponse.success(
                documentId,
                document.getFileName(),
                document.getOriginalName(),
                document.getFileSize(),
                document.getMimeType(),
                document.getDocumentType()
            );
        } catch (Exception e) {
            logger.error("Error uploading as version: {}", e.getMessage());
            return FileUploadResponse.error("Failed to upload as version: " + e.getMessage());
        }
    }
    
    /**
     * Replace an existing document with a new file
     */
    private FileUploadResponse replaceDocument(MultipartFile file, User user, Long documentId,
                                              String documentType, String description,
                                              Map<String, String> manualMetadata, Long folderId) {
        try {
            Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
            
            // Validate file
            String validationError = validateFile(file);
            if (validationError != null) {
                return FileUploadResponse.error(validationError);
            }
            
            // Validate and normalize document type
            DocumentType resolvedType = DocumentType.resolve(documentType).orElse(null);
            if (resolvedType == null) {
                String message = "Invalid document type. Allowed types are: " + DocumentType.allowedTypesList();
                return FileUploadResponse.error(message);
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = generateUniqueFilename(fileExtension);
            
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Save new file
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Calculate new file hash
            String fileHash = calculateFileHash(file);
            
            // Update document
            document.setFileName(uniqueFilename);
            if (originalFilename != null && !originalFilename.equals(document.getOriginalName())) {
                document.setOriginalName(originalFilename);
            }
            document.setFilePath(filePath.toString());
            document.setFileSize(file.getSize());
            document.setMimeType(file.getContentType());
            document.setDocumentType(resolvedType.name());
            if (description != null) {
                document.setDescription(description);
            }
            document.setFileHash(fileHash);
            
            // Set folder if provided
            if (folderId != null) {
                folderRepository.findById(folderId).ifPresent(document::setFolder);
            }
            
            Document savedDocument = documentRepository.save(document);
            
            // Apply metadata
            Map<String, String> combinedMetadata = new HashMap<>();
            if (manualMetadata != null && !manualMetadata.isEmpty()) {
                combinedMetadata.putAll(documentMetadataService.applyManualMetadata(savedDocument, manualMetadata));
            }
            
            // Process OCR and indexing asynchronously
            processDocumentAsync(savedDocument, file, combinedMetadata);
            
            logger.info("Document replaced successfully: {} by user: {}", 
                      savedDocument.getOriginalName(), user.getUsername());
            
            return FileUploadResponse.success(
                savedDocument.getId(),
                savedDocument.getFileName(),
                savedDocument.getOriginalName(),
                savedDocument.getFileSize(),
                savedDocument.getMimeType(),
                savedDocument.getDocumentType()
            );
        } catch (Exception e) {
            logger.error("Error replacing document: {}", e.getMessage());
            return FileUploadResponse.error("Failed to replace document: " + e.getMessage());
        }
    }
    
    /**
     * Calculate SHA-256 hash of file content for duplicate detection
     */
    private String calculateFileHash(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(file.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.error("Failed to calculate file hash: {}", e.getMessage());
            return "";
        }
    }
}

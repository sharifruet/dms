package com.bpdb.dms.service;

import com.bpdb.dms.entity.*;
import com.bpdb.dms.repository.DocumentVersionRepository;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing document versioning
 */
@Service
@Transactional
public class DocumentVersioningService {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentVersioningService.class);
    
    @Autowired
    private DocumentVersionRepository documentVersionRepository;
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AuditService auditService;
    
    /**
     * Create a new version of a document
     */
    public DocumentVersion createDocumentVersion(Long documentId, MultipartFile file, 
                                               String changeDescription, VersionType versionType, 
                                               User createdBy) {
        try {
            Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
            
            // Generate version number
            String versionNumber = generateVersionNumber(document, versionType);
            
            // Save file and get file path
            String filePath = saveVersionFile(document, file, versionNumber);
            
            // Calculate file hash
            String fileHash = calculateFileHash(file);
            
            // Create document version
            DocumentVersion version = new DocumentVersion(document, versionNumber, filePath, createdBy, changeDescription);
            version.setFileSize(file.getSize());
            version.setFileHash(fileHash);
            version.setMimeType(file.getContentType());
            version.setVersionType(versionType);
            
            // Mark previous versions as not current
            markPreviousVersionsAsNotCurrent(document);
            
            // Mark this version as current
            version.setIsCurrent(true);
            
            DocumentVersion savedVersion = documentVersionRepository.save(version);
            
            // Update document with new file path
            document.setFilePath(filePath);
            document.setFileSize(file.getSize());
            document.setMimeType(file.getContentType());
            documentRepository.save(document);
            
            auditService.logActivity(createdBy.getUsername(), "DOCUMENT_VERSION_CREATED", 
                "New version created for document: " + document.getOriginalName() + " (v" + versionNumber + ")", null);
            
            logger.info("Document version created: {} v{} by user: {}", 
                document.getOriginalName(), versionNumber, createdBy.getUsername());
            
            return savedVersion;
            
        } catch (Exception e) {
            logger.error("Failed to create document version: {}", e.getMessage());
            throw new RuntimeException("Failed to create document version", e);
        }
    }
    
    /**
     * Get all versions of a document
     */
    public List<DocumentVersion> getDocumentVersions(Long documentId) {
        try {
            Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
            
            return documentVersionRepository.findByDocumentOrderByCreatedAtDesc(document);
            
        } catch (Exception e) {
            logger.error("Failed to get document versions: {}", e.getMessage());
            throw new RuntimeException("Failed to get document versions", e);
        }
    }
    
    /**
     * Get a specific version of a document
     */
    public Optional<DocumentVersion> getDocumentVersion(Long documentId, String versionNumber) {
        try {
            Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
            
            return documentVersionRepository.findByDocumentAndVersionNumber(document, versionNumber);
            
        } catch (Exception e) {
            logger.error("Failed to get document version: {}", e.getMessage());
            throw new RuntimeException("Failed to get document version", e);
        }
    }
    
    /**
     * Restore a document to a specific version
     */
    public Document restoreDocumentToVersion(Long documentId, String versionNumber, User restoredBy) {
        try {
            Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
            
            DocumentVersion version = documentVersionRepository.findByDocumentAndVersionNumber(document, versionNumber)
                .orElseThrow(() -> new RuntimeException("Version not found"));
            
            // Create a new version with current content before restoring
            String currentVersionNumber = generateVersionNumber(document, VersionType.PATCH);
            String backupFilePath = saveVersionFile(document, null, currentVersionNumber);
            
            DocumentVersion backupVersion = new DocumentVersion(document, currentVersionNumber, 
                backupFilePath, restoredBy, "Backup before restore to v" + versionNumber);
            backupVersion.setFileSize(document.getFileSize());
            backupVersion.setFileHash(document.getFileHash());
            backupVersion.setMimeType(document.getMimeType());
            backupVersion.setVersionType(VersionType.PATCH);
            documentVersionRepository.save(backupVersion);
            
            // Restore document to selected version
            document.setFilePath(version.getFilePath());
            document.setFileSize(version.getFileSize());
            document.setMimeType(version.getMimeType());
            documentRepository.save(document);
            
            // Mark restored version as current
            markPreviousVersionsAsNotCurrent(document);
            version.setIsCurrent(true);
            documentVersionRepository.save(version);
            
            auditService.logActivity(restoredBy.getUsername(), "DOCUMENT_VERSION_RESTORED", 
                "Document restored to version: " + versionNumber, null);
            
            logger.info("Document restored to version: {} v{} by user: {}", 
                document.getOriginalName(), versionNumber, restoredBy.getUsername());
            
            return document;
            
        } catch (Exception e) {
            logger.error("Failed to restore document version: {}", e.getMessage());
            throw new RuntimeException("Failed to restore document version", e);
        }
    }
    
    /**
     * Archive old versions
     */
    public void archiveOldVersions(Long documentId, int keepVersions) {
        try {
            Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
            
            List<DocumentVersion> versions = documentVersionRepository.findByDocumentOrderByCreatedAtDesc(document);
            
            if (versions.size() > keepVersions) {
                List<DocumentVersion> versionsToArchive = versions.subList(keepVersions, versions.size());
                
                for (DocumentVersion version : versionsToArchive) {
                    version.setIsArchived(true);
                    version.setArchivedAt(LocalDateTime.now());
                    documentVersionRepository.save(version);
                }
                
                logger.info("Archived {} old versions for document: {}", 
                    versionsToArchive.size(), document.getOriginalName());
            }
            
        } catch (Exception e) {
            logger.error("Failed to archive old versions: {}", e.getMessage());
        }
    }
    
    /**
     * Get version statistics
     */
    public long getVersionCount(Long documentId) {
        try {
            Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
            
            return documentVersionRepository.countByDocument(document);
            
        } catch (Exception e) {
            logger.error("Failed to get version count: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Compare two versions
     */
    public Map<String, Object> compareVersions(Long documentId, String version1, String version2) {
        try {
            Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
            
            DocumentVersion v1 = documentVersionRepository.findByDocumentAndVersionNumber(document, version1)
                .orElseThrow(() -> new RuntimeException("Version 1 not found"));
            
            DocumentVersion v2 = documentVersionRepository.findByDocumentAndVersionNumber(document, version2)
                .orElseThrow(() -> new RuntimeException("Version 2 not found"));
            
            return Map.of(
                "version1", Map.of(
                    "versionNumber", v1.getVersionNumber(),
                    "createdAt", v1.getCreatedAt(),
                    "createdBy", v1.getCreatedBy().getUsername(),
                    "changeDescription", v1.getChangeDescription(),
                    "fileSize", v1.getFileSize()
                ),
                "version2", Map.of(
                    "versionNumber", v2.getVersionNumber(),
                    "createdAt", v2.getCreatedAt(),
                    "createdBy", v2.getCreatedBy().getUsername(),
                    "changeDescription", v2.getChangeDescription(),
                    "fileSize", v2.getFileSize()
                ),
                "differences", Map.of(
                    "fileSizeChanged", !v1.getFileSize().equals(v2.getFileSize()),
                    "contentChanged", !v1.getFileHash().equals(v2.getFileHash()),
                    "timeDifference", java.time.Duration.between(v1.getCreatedAt(), v2.getCreatedAt()).toDays()
                )
            );
            
        } catch (Exception e) {
            logger.error("Failed to compare versions: {}", e.getMessage());
            throw new RuntimeException("Failed to compare versions", e);
        }
    }
    
    /**
     * Generate version number
     */
    private String generateVersionNumber(Document document, VersionType versionType) {
        try {
            List<DocumentVersion> versions = documentVersionRepository.findByDocumentOrderByCreatedAtDesc(document);
            
            if (versions.isEmpty()) {
                return "1.0.0";
            }
            
            String latestVersion = versions.get(0).getVersionNumber();
            String[] parts = latestVersion.split("\\.");
            
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            
            switch (versionType) {
                case MAJOR:
                    major++;
                    minor = 0;
                    patch = 0;
                    break;
                case MINOR:
                    minor++;
                    patch = 0;
                    break;
                case PATCH:
                    patch++;
                    break;
                case DRAFT:
                    return latestVersion + "-draft";
                case FINAL:
                    return latestVersion + "-final";
            }
            
            return major + "." + minor + "." + patch;
            
        } catch (Exception e) {
            logger.error("Failed to generate version number: {}", e.getMessage());
            return "1.0.0";
        }
    }
    
    /**
     * Save version file
     */
    private String saveVersionFile(Document document, MultipartFile file, String versionNumber) {
        try {
            String uploadDir = System.getProperty("user.dir") + "/uploads/versions";
            Path uploadPath = Paths.get(uploadDir);
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            String fileName = document.getId() + "_v" + versionNumber + "_" + 
                (file != null ? file.getOriginalFilename() : document.getOriginalName());
            
            Path filePath = uploadPath.resolve(fileName);
            
            if (file != null) {
                Files.copy(file.getInputStream(), filePath);
            } else {
                // Copy existing file for backup
                Path existingPath = Paths.get(document.getFilePath());
                if (Files.exists(existingPath)) {
                    Files.copy(existingPath, filePath);
                }
            }
            
            return filePath.toString();
            
        } catch (IOException e) {
            logger.error("Failed to save version file: {}", e.getMessage());
            throw new RuntimeException("Failed to save version file", e);
        }
    }
    
    /**
     * Calculate file hash
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
    
    /**
     * Mark previous versions as not current
     */
    private void markPreviousVersionsAsNotCurrent(Document document) {
        try {
            List<DocumentVersion> versions = documentVersionRepository.findByDocumentOrderByCreatedAtDesc(document);
            
            for (DocumentVersion version : versions) {
                if (version.getIsCurrent()) {
                    version.setIsCurrent(false);
                    documentVersionRepository.save(version);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to mark previous versions as not current: {}", e.getMessage());
        }
    }
}

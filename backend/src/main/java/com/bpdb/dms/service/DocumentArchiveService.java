package com.bpdb.dms.service;

import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for document archive and restore operations
 */
@Service
@Transactional
public class DocumentArchiveService {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentArchiveService.class);
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private AuditService auditService;
    
    /**
     * Archive a document
     */
    public Document archiveDocument(Long documentId, User archivedBy) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
        
        if (document.getIsArchived() != null && document.getIsArchived()) {
            throw new RuntimeException("Document is already archived");
        }
        
        document.setIsArchived(true);
        document.setArchivedAt(LocalDateTime.now());
        Document saved = documentRepository.save(document);
        
        auditService.logActivity(
            archivedBy.getUsername(),
            "DOCUMENT_ARCHIVED",
            "Document archived: " + document.getOriginalName(),
            documentId
        );
        
        logger.info("Document archived: {} by user: {}", document.getOriginalName(), archivedBy.getUsername());
        return saved;
    }
    
    /**
     * Restore an archived document
     */
    public Document restoreArchivedDocument(Long documentId, User restoredBy) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
        
        if (document.getIsArchived() == null || !document.getIsArchived()) {
            throw new RuntimeException("Document is not archived");
        }
        
        document.setIsArchived(false);
        document.setArchivedAt(null);
        Document saved = documentRepository.save(document);
        
        auditService.logActivity(
            restoredBy.getUsername(),
            "DOCUMENT_RESTORED",
            "Document restored from archive: " + document.getOriginalName(),
            documentId
        );
        
        logger.info("Document restored from archive: {} by user: {}", document.getOriginalName(), restoredBy.getUsername());
        return saved;
    }
    
    /**
     * Soft delete a document (sets deletedAt)
     */
    public Document deleteDocument(Long documentId, User deletedBy) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
        
        if (document.getDeletedAt() != null) {
            throw new RuntimeException("Document is already deleted");
        }
        
        document.setIsActive(false);
        document.setDeletedAt(LocalDateTime.now());
        Document saved = documentRepository.save(document);
        
        auditService.logActivity(
            deletedBy.getUsername(),
            "DOCUMENT_DELETED",
            "Document deleted: " + document.getOriginalName(),
            documentId
        );
        
        logger.info("Document deleted: {} by user: {}", document.getOriginalName(), deletedBy.getUsername());
        return saved;
    }
    
    /**
     * Restore a deleted document
     */
    public Document restoreDeletedDocument(Long documentId, User restoredBy) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
        
        if (document.getDeletedAt() == null) {
            throw new RuntimeException("Document is not deleted");
        }
        
        document.setIsActive(true);
        document.setDeletedAt(null);
        Document saved = documentRepository.save(document);
        
        auditService.logActivity(
            restoredBy.getUsername(),
            "DOCUMENT_RESTORED_FROM_DELETE",
            "Document restored from deletion: " + document.getOriginalName(),
            documentId
        );
        
        logger.info("Document restored from deletion: {} by user: {}", document.getOriginalName(), restoredBy.getUsername());
        return saved;
    }
    
    /**
     * Get archived documents
     */
    public Page<Document> getArchivedDocuments(Pageable pageable) {
        return documentRepository.findArchivedDocuments(pageable);
    }
    
    /**
     * Get deleted documents
     */
    public Page<Document> getDeletedDocuments(Pageable pageable) {
        return documentRepository.findDeletedDocuments(pageable);
    }
    
    /**
     * Get active, non-archived documents
     */
    public Page<Document> getActiveNonArchivedDocuments(Pageable pageable) {
        return documentRepository.findActiveNonArchivedDocuments(pageable);
    }
    
    /**
     * Get archive statistics
     */
    public ArchiveStatistics getArchiveStatistics() {
        long archivedCount = documentRepository.countArchivedDocuments();
        long deletedCount = documentRepository.countDeletedDocuments();
        long activeCount = documentRepository.countByIsActiveTrue();
        
        return new ArchiveStatistics(archivedCount, deletedCount, activeCount);
    }
    
    /**
     * Archive multiple documents
     */
    public List<Document> archiveDocuments(List<Long> documentIds, User archivedBy) {
        return documentIds.stream()
            .map(id -> {
                try {
                    return archiveDocument(id, archivedBy);
                } catch (Exception e) {
                    logger.error("Failed to archive document {}: {}", id, e.getMessage());
                    return null;
                }
            })
            .filter(doc -> doc != null)
            .toList();
    }
    
    /**
     * Restore multiple archived documents
     */
    public List<Document> restoreArchivedDocuments(List<Long> documentIds, User restoredBy) {
        return documentIds.stream()
            .map(id -> {
                try {
                    return restoreArchivedDocument(id, restoredBy);
                } catch (Exception e) {
                    logger.error("Failed to restore archived document {}: {}", id, e.getMessage());
                    return null;
                }
            })
            .filter(doc -> doc != null)
            .toList();
    }
    
    /**
     * Restore multiple deleted documents
     */
    public List<Document> restoreDeletedDocuments(List<Long> documentIds, User restoredBy) {
        return documentIds.stream()
            .map(id -> {
                try {
                    return restoreDeletedDocument(id, restoredBy);
                } catch (Exception e) {
                    logger.error("Failed to restore deleted document {}: {}", id, e.getMessage());
                    return null;
                }
            })
            .filter(doc -> doc != null)
            .toList();
    }
    
    /**
     * Statistics class for archive information
     */
    public static class ArchiveStatistics {
        private final long archivedCount;
        private final long deletedCount;
        private final long activeCount;
        
        public ArchiveStatistics(long archivedCount, long deletedCount, long activeCount) {
            this.archivedCount = archivedCount;
            this.deletedCount = deletedCount;
            this.activeCount = activeCount;
        }
        
        public long getArchivedCount() {
            return archivedCount;
        }
        
        public long getDeletedCount() {
            return deletedCount;
        }
        
        public long getActiveCount() {
            return activeCount;
        }
    }
}


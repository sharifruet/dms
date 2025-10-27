package com.bpdb.dms.repository;

import com.bpdb.dms.entity.DocumentComment;
import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.entity.CommentType;
import com.bpdb.dms.entity.CommentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for DocumentComment entity
 */
@Repository
public interface DocumentCommentRepository extends JpaRepository<DocumentComment, Long> {
    
    /**
     * Find comments by document
     */
    Page<DocumentComment> findByDocumentOrderByCreatedAtDesc(Document document, Pageable pageable);
    
    /**
     * Find comments by creator
     */
    Page<DocumentComment> findByCreatedBy(User createdBy, Pageable pageable);
    
    /**
     * Find comments by type
     */
    Page<DocumentComment> findByCommentType(CommentType commentType, Pageable pageable);
    
    /**
     * Find comments by status
     */
    Page<DocumentComment> findByStatus(CommentStatus status, Pageable pageable);
    
    /**
     * Find resolved comments
     */
    Page<DocumentComment> findByIsResolvedTrue(Pageable pageable);
    
    /**
     * Find unresolved comments
     */
    Page<DocumentComment> findByIsResolvedFalse(Pageable pageable);
    
    /**
     * Find comments by page number
     */
    List<DocumentComment> findByDocumentAndPageNumber(Document document, Integer pageNumber);
    
    /**
     * Find root comments (no parent)
     */
    Page<DocumentComment> findByDocumentAndParentCommentIsNullOrderByCreatedAtDesc(Document document, Pageable pageable);
    
    /**
     * Find replies to a comment
     */
    List<DocumentComment> findByParentCommentOrderByCreatedAtAsc(DocumentComment parentComment);
    
    /**
     * Find comments created after date
     */
    List<DocumentComment> findByDocumentAndCreatedAtAfter(Document document, LocalDateTime date);
    
    /**
     * Count comments by document
     */
    long countByDocument(Document document);
    
    /**
     * Count unresolved comments by document
     */
    long countByDocumentAndIsResolvedFalse(Document document);
    
    /**
     * Count comments by creator
     */
    long countByCreatedBy(User createdBy);
    
    /**
     * Find comments by multiple criteria
     */
    @Query("SELECT dc FROM DocumentComment dc WHERE " +
           "(:document IS NULL OR dc.document = :document) AND " +
           "(:createdBy IS NULL OR dc.createdBy = :createdBy) AND " +
           "(:commentType IS NULL OR dc.commentType = :commentType) AND " +
           "(:status IS NULL OR dc.status = :status) AND " +
           "(:isResolved IS NULL OR dc.isResolved = :isResolved)")
    Page<DocumentComment> findByMultipleCriteria(@Param("document") Document document,
                                                @Param("createdBy") User createdBy,
                                                @Param("commentType") CommentType commentType,
                                                @Param("status") CommentStatus status,
                                                @Param("isResolved") Boolean isResolved,
                                                Pageable pageable);
}

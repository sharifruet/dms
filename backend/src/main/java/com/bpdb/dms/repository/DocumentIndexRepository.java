package com.bpdb.dms.repository;

import com.bpdb.dms.entity.DocumentIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Elasticsearch repository for document search
 */
@Repository
public interface DocumentIndexRepository extends ElasticsearchRepository<DocumentIndex, String> {
    
    /**
     * Search documents by text content
     */
    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"fileName^2\", \"originalName^2\", \"extractedText\", \"description\", \"tags\"]}}")
    Page<DocumentIndex> searchByText(String searchText, Pageable pageable);
    
    /**
     * Search documents by document type
     */
    Page<DocumentIndex> findByDocumentType(String documentType, Pageable pageable);
    
    /**
     * Search documents by department
     */
    Page<DocumentIndex> findByDepartment(String department, Pageable pageable);
    
    /**
     * Search documents by uploaded by user
     */
    Page<DocumentIndex> findByUploadedBy(String uploadedBy, Pageable pageable);
    
    /**
     * Search documents by tags
     */
    @Query("{\"wildcard\": {\"tags\": \"*?0*\"}}")
    List<DocumentIndex> findByTagsContaining(String tag);
    
    /**
     * Search documents by date range
     */
    Page<DocumentIndex> findByCreatedAtBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    /**
     * Suggestions by file name or original name prefix (simple wildcard)
     */
    @Query("{\"wildcard\": {\"fileName\": \"?0*\"}}")
    List<DocumentIndex> suggestByFileNamePrefix(String prefix);
    
    @Query("{\"wildcard\": {\"originalName\": \"?0*\"}}")
    List<DocumentIndex> suggestByOriginalNamePrefix(String prefix);
    
    /**
     * Search documents by OCR confidence
     */
    Page<DocumentIndex> findByOcrConfidenceGreaterThan(Double minConfidence, Pageable pageable);
    
    /**
     * Search active documents
     */
    Page<DocumentIndex> findByIsActiveTrue(Pageable pageable);
    
    /**
     * Complex search with multiple criteria
     */
    @Query("{\"bool\": {\"must\": [{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"fileName\", \"extractedText\", \"description\"]}}, {\"term\": {\"isActive\": true}}], \"filter\": [{\"term\": {\"documentType\": \"?1\"}}]}}")
    Page<DocumentIndex> searchByTextAndType(String searchText, String documentType, Pageable pageable);
    
    /**
     * Search with faceted filters
     */
    @Query("{\"bool\": {\"must\": [{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"fileName\", \"extractedText\", \"description\"]}}], \"filter\": [{\"terms\": {\"documentType\": ?1}}, {\"terms\": {\"department\": ?2}}]}}")
    Page<DocumentIndex> searchWithFilters(String searchText, List<String> documentTypes, List<String> departments, Pageable pageable);
    
    /**
     * Get document suggestions for autocomplete
     */
    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"fileName^3\", \"originalName^3\", \"description^2\"], \"type\": \"phrase_prefix\"}}")
    List<DocumentIndex> findSuggestions(String prefix, Pageable pageable);
    
    /**
     * Count documents by type
     */
    long countByDocumentType(String documentType);
    
    /**
     * Count documents by department
     */
    long countByDepartment(String department);
    
    /**
     * Count documents by user
     */
    long countByUploadedBy(String uploadedBy);
    
    /**
     * Count active documents
     */
    long countByIsActiveTrue();
}

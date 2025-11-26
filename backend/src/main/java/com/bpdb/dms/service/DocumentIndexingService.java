package com.bpdb.dms.service;

import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.DocumentIndex;
import com.bpdb.dms.repository.DocumentIndexRepository;
import com.bpdb.dms.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageImpl;

/**
 * Service for document indexing and search operations
 */
@Service
@Transactional
public class DocumentIndexingService {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentIndexingService.class);
    
    @Autowired
    private DocumentIndexRepository documentIndexRepository;
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private DocumentCategoryService documentCategoryService;
    
    /**
     * Index a document for search
     */
    public void indexDocument(Document document, String extractedText, Map<String, String> metadata, 
                            Double ocrConfidence, Double classificationConfidence) {
        try {
            DocumentIndex documentIndex = new DocumentIndex();
            documentIndex.setDocumentId(document.getId());
            documentIndex.setFileName(document.getFileName());
            documentIndex.setOriginalName(document.getOriginalName());
            documentIndex.setExtractedText(extractedText);
            documentIndex.setDocumentType(document.getDocumentType() != null ? document.getDocumentType() : "OTHER");
            documentIndex.setDescription(document.getDescription());
            documentIndex.setTags(document.getTags());
            documentIndex.setDepartment(document.getDepartment());
            documentIndex.setUploadedBy(document.getUploadedBy().getId().toString());
            documentIndex.setUploadedByUsername(document.getUploadedBy().getUsername());
            documentIndex.setCreatedAt(document.getCreatedAt() != null ? document.getCreatedAt().toLocalDate() : null);
            documentIndex.setUpdatedAt(document.getUpdatedAt() != null ? document.getUpdatedAt().toLocalDate() : null);
            documentIndex.setMetadata(metadata);
            documentIndex.setOcrConfidence(ocrConfidence);
            documentIndex.setClassificationConfidence(classificationConfidence);
            documentIndex.setMimeType(document.getMimeType());
            documentIndex.setFileSize(document.getFileSize());
            documentIndex.setIsActive(document.getIsActive());
            
            documentIndexRepository.save(documentIndex);
            // Invalidate Smart Folder caches on index writes
            clearDmcCache();
            
            logger.info("Document indexed successfully: {} (ID: {})", document.getOriginalName(), document.getId());
            
        } catch (Exception e) {
            logger.error("Failed to index document {}: {}", document.getId(), e.getMessage());
            throw new RuntimeException("Failed to index document", e);
        }
    }
    
    /**
     * Update document index
     */
    public void updateDocumentIndex(Document document) {
        try {
            DocumentIndex existingIndex = documentIndexRepository.findById(document.getId().toString()).orElse(null);
            
            if (existingIndex != null) {
                existingIndex.setFileName(document.getFileName());
                existingIndex.setOriginalName(document.getOriginalName());
                existingIndex.setDocumentType(document.getDocumentType() != null ? document.getDocumentType() : "OTHER");
                existingIndex.setDescription(document.getDescription());
                existingIndex.setTags(document.getTags());
                existingIndex.setDepartment(document.getDepartment());
                existingIndex.setUpdatedAt(document.getUpdatedAt() != null ? document.getUpdatedAt().toLocalDate() : null);
                existingIndex.setIsActive(document.getIsActive());
                
                documentIndexRepository.save(existingIndex);
                logger.info("Document index updated: {} (ID: {})", document.getOriginalName(), document.getId());
            } else {
                // Re-index if not found
                indexDocument(document, "", new HashMap<>(), 0.0, 0.0);
            }
            
        } catch (Exception e) {
            logger.error("Failed to update document index {}: {}", document.getId(), e.getMessage());
        }
    }
    
    /**
     * Remove document from index
     */
    @CacheEvict(cacheNames = "dmcEval", allEntries = true)
    public void removeDocumentFromIndex(Long documentId) {
        try {
            documentIndexRepository.deleteById(documentId.toString());
            logger.info("Document removed from index: {}", documentId);
            clearDmcCache();
        } catch (Exception e) {
            logger.error("Failed to remove document from index {}: {}", documentId, e.getMessage());
        }
    }
    
    /**
     * Search documents with advanced query (supports Boolean operators)
     */
    public SearchResult searchDocuments(String query, SearchFilters filters, Pageable pageable) {
        try {
            Page<DocumentIndex> results;
            
            if (query != null && !query.trim().isEmpty()) {
                // Parse query for Boolean operators
                BooleanQueryParser.ParsedQueryResult parsedQuery = BooleanQueryParser.parseQuery(query);
                
                if (parsedQuery.getType() == BooleanQueryParser.QueryType.BOOLEAN) {
                    // Use Boolean query with operators
                    results = searchWithBooleanQuery(parsedQuery, pageable);
                } else {
                    // Simple query - use existing method
                    results = documentIndexRepository.searchByText(query, pageable);
                }
            } else {
                results = documentIndexRepository.findAll(pageable);
            }
            
            // Apply filters manually for now
            List<DocumentIndex> filteredResults = results.getContent().stream()
                .filter(doc -> {
                    if (filters == null) return true;
                    
                    if (filters.getDocumentTypes() != null && !filters.getDocumentTypes().isEmpty()) {
                        if (!filters.getDocumentTypes().contains(doc.getDocumentType())) return false;
                    }
                    
                    if (filters.getDepartments() != null && !filters.getDepartments().isEmpty()) {
                        if (!filters.getDepartments().contains(doc.getDepartment())) return false;
                    }
                    
                    if (filters.getIsActive() != null) {
                        if (!filters.getIsActive().equals(doc.getIsActive())) return false;
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
            
            // Convert to search result
            SearchResult result = new SearchResult();
            result.setTotalHits(filteredResults.size());
            result.setMaxScore(1.0f);
            
            List<SearchResultItem> items = filteredResults.stream()
                .map(this::convertToSearchResultItem)
                .collect(Collectors.toList());
            
            result.setItems(items);
            result.setPageNumber(pageable.getPageNumber());
            result.setPageSize(pageable.getPageSize());
            result.setTotalPages((int) Math.ceil((double) filteredResults.size() / pageable.getPageSize()));
            
            return result;
            
        } catch (Exception e) {
            logger.error("Search failed: {}", e.getMessage());
            throw new RuntimeException("Search operation failed", e);
        }
    }
    
    /**
     * Search with Boolean query operators (AND, OR, NOT)
     * For now, we implement basic Boolean logic in-memory after fetching results
     * A production implementation would use Elasticsearch's bool query directly
     */
    private Page<DocumentIndex> searchWithBooleanQuery(BooleanQueryParser.ParsedQueryResult parsedQuery, Pageable pageable) {
        List<String> terms = parsedQuery.getTerms();
        List<String> operators = parsedQuery.getOperators();
        
        if (terms.isEmpty()) {
            return documentIndexRepository.findAll(pageable);
        }
        
        // For simplicity, implement Boolean logic by:
        // 1. Searching for each term separately
        // 2. Combining results based on operators
        
        // Get results for each term
        List<Page<DocumentIndex>> termResults = new ArrayList<>();
        for (String term : terms) {
            Page<DocumentIndex> termPage = documentIndexRepository.searchByText(term, Pageable.unpaged());
            termResults.add(termPage);
        }
        
        // Combine results based on operators
        Map<Long, DocumentIndex> combinedResultsMap = new LinkedHashMap<>();
        
        if (termResults.isEmpty()) {
            return documentIndexRepository.findAll(pageable);
        }
        
        // Start with first term's results
        for (DocumentIndex doc : termResults.get(0).getContent()) {
            if (doc.getDocumentId() != null) {
                combinedResultsMap.put(doc.getDocumentId(), doc);
            }
        }
        
        // Apply operators sequentially
        for (int i = 0; i < operators.size() && i + 1 < termResults.size(); i++) {
            String operator = operators.get(i);
            List<DocumentIndex> nextTermResults = termResults.get(i + 1).getContent();
            
            // Extract document IDs from next term results
            Set<Long> nextTermDocIds = nextTermResults.stream()
                .map(DocumentIndex::getDocumentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            
            if ("AND".equals(operator)) {
                // Keep only documents that are in both result sets
                combinedResultsMap.keySet().retainAll(nextTermDocIds);
            } else if ("OR".equals(operator)) {
                // Add all documents from next term (avoid duplicates)
                for (DocumentIndex doc : nextTermResults) {
                    if (doc.getDocumentId() != null && !combinedResultsMap.containsKey(doc.getDocumentId())) {
                        combinedResultsMap.put(doc.getDocumentId(), doc);
                    }
                }
            } else if ("NOT".equals(operator)) {
                // Remove documents that are in next term results
                combinedResultsMap.keySet().removeAll(nextTermDocIds);
            }
        }
        
        List<DocumentIndex> combinedResults = new ArrayList<>(combinedResultsMap.values());
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), combinedResults.size());
        List<DocumentIndex> paginatedResults = start < combinedResults.size() 
            ? combinedResults.subList(start, end) 
            : Collections.emptyList();
        
        return new PageImpl<>(paginatedResults, pageable, combinedResults.size());
    }
    
    /**
     * Get document suggestions for autocomplete
     */
    public List<String> getSuggestions(String prefix, int limit) {
        try {
            Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit);
            List<DocumentIndex> suggestions = documentIndexRepository.findSuggestions(prefix, pageable);
            
            return suggestions.stream()
                .map(DocumentIndex::getOriginalName)
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Failed to get suggestions: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Get search statistics
     */
    public Map<String, Object> getSearchStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            stats.put("totalDocuments", documentIndexRepository.count());
            stats.put("activeDocuments", documentIndexRepository.countByIsActiveTrue());
            
            // Count by document type
            Map<String, Long> typeCounts = new HashMap<>();
            List<String> categoryNames = documentCategoryService.getActiveCategoryNames();
            for (String category : categoryNames) {
                typeCounts.put(category, documentIndexRepository.countByDocumentType(category));
            }
            stats.put("documentTypeCounts", typeCounts);
            
        } catch (Exception e) {
            logger.error("Failed to get search statistics: {}", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Re-index all documents
     */
    public void reindexAllDocuments() {
        try {
            logger.info("Starting full reindex of all documents");
            
            // Clear existing index
            documentIndexRepository.deleteAll();
            
            // Re-index all active documents
            List<Document> documents = documentRepository.findByIsActiveTrue(org.springframework.data.domain.PageRequest.of(0, 1000)).getContent();
            
            for (Document document : documents) {
                try {
                    indexDocument(document, "", new HashMap<>(), 0.0, 0.0);
                } catch (Exception e) {
                    logger.error("Failed to reindex document {}: {}", document.getId(), e.getMessage());
                }
            }
            
            logger.info("Full reindex completed. Processed {} documents", documents.size());
            clearDmcCache();
            
        } catch (Exception e) {
            logger.error("Full reindex failed: {}", e.getMessage());
            throw new RuntimeException("Full reindex failed", e);
        }
    }

    @Autowired(required = false)
    private org.springframework.cache.CacheManager cacheManager;

    private void clearDmcCache() {
        if (cacheManager != null) {
            var cache = cacheManager.getCache("dmcEval");
            if (cache != null) {
                cache.clear();
            }
        }
    }
    
    /**
     * Convert DocumentIndex to SearchResultItem
     */
    private SearchResultItem convertToSearchResultItem(DocumentIndex document) {
        SearchResultItem item = new SearchResultItem();
        
        item.setDocumentId(document.getDocumentId());
        item.setFileName(document.getFileName());
        item.setOriginalName(document.getOriginalName());
        item.setDocumentType(document.getDocumentType());
        item.setDescription(document.getDescription());
        item.setDepartment(document.getDepartment());
        item.setUploadedBy(document.getUploadedByUsername());
        item.setCreatedAt(document.getCreatedAt() != null ? document.getCreatedAt().toString() : null);
        item.setOcrConfidence(document.getOcrConfidence());
        item.setClassificationConfidence(document.getClassificationConfidence());
        item.setScore(1.0f); // Default score for now
        
        return item;
    }
    
    /**
     * Search Filters class
     */
    public static class SearchFilters {
        private List<String> documentTypes;
        private List<String> departments;
        private List<String> uploadedBy;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Double minOcrConfidence;
        private Boolean isActive;
        
        // Getters and setters
        public List<String> getDocumentTypes() { return documentTypes; }
        public void setDocumentTypes(List<String> documentTypes) { this.documentTypes = documentTypes; }
        public List<String> getDepartments() { return departments; }
        public void setDepartments(List<String> departments) { this.departments = departments; }
        public List<String> getUploadedBy() { return uploadedBy; }
        public void setUploadedBy(List<String> uploadedBy) { this.uploadedBy = uploadedBy; }
        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
        public LocalDateTime getEndDate() { return endDate; }
        public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
        public Double getMinOcrConfidence() { return minOcrConfidence; }
        public void setMinOcrConfidence(Double minOcrConfidence) { this.minOcrConfidence = minOcrConfidence; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    }
    
    /**
     * Search Result class
     */
    public static class SearchResult {
        private long totalHits;
        private float maxScore;
        private List<SearchResultItem> items;
        private int pageNumber;
        private int pageSize;
        private int totalPages;
        
        // Getters and setters
        public long getTotalHits() { return totalHits; }
        public void setTotalHits(long totalHits) { this.totalHits = totalHits; }
        public float getMaxScore() { return maxScore; }
        public void setMaxScore(float maxScore) { this.maxScore = maxScore; }
        public List<SearchResultItem> getItems() { return items; }
        public void setItems(List<SearchResultItem> items) { this.items = items; }
        public int getPageNumber() { return pageNumber; }
        public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }
        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    }
    
    /**
     * Search Result Item class
     */
    public static class SearchResultItem {
        private Long documentId;
        private String fileName;
        private String originalName;
        private String documentType;
        private String description;
        private String department;
        private String uploadedBy;
        private String createdAt;
        private Double ocrConfidence;
        private Double classificationConfidence;
        private float score;
        private Map<String, List<String>> highlights;
        
        // Getters and setters
        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getOriginalName() { return originalName; }
        public void setOriginalName(String originalName) { this.originalName = originalName; }
        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public String getUploadedBy() { return uploadedBy; }
        public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public Double getOcrConfidence() { return ocrConfidence; }
        public void setOcrConfidence(Double ocrConfidence) { this.ocrConfidence = ocrConfidence; }
        public Double getClassificationConfidence() { return classificationConfidence; }
        public void setClassificationConfidence(Double classificationConfidence) { this.classificationConfidence = classificationConfidence; }
        public float getScore() { return score; }
        public void setScore(float score) { this.score = score; }
        public Map<String, List<String>> getHighlights() { return highlights; }
        public void setHighlights(Map<String, List<String>> highlights) { this.highlights = highlights; }
    }
}

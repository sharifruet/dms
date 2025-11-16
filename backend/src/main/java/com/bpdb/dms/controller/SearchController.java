package com.bpdb.dms.controller;

import com.bpdb.dms.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "*")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping
    public ResponseEntity<?> search(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) Set<String> documentTypes,
                                    @RequestParam(required = false) Set<String> departments,
                                    @RequestParam(required = false) Boolean isActive,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(searchService.search(q, documentTypes, departments, isActive, createdFrom, createdTo, page, size));
    }

    @GetMapping("/suggest")
    public ResponseEntity<List<String>> suggest(@RequestParam String prefix,
                                                @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(searchService.suggest(prefix, limit));
    }
}

package com.bpdb.dms.controller;

import com.bpdb.dms.service.DocumentIndexingService;
import com.bpdb.dms.service.DocumentIndexingService.SearchFilters;
import com.bpdb.dms.service.DocumentIndexingService.SearchResult;
import com.bpdb.dms.service.DocumentIndexingService.SearchResultItem;
import com.bpdb.dms.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controller for document search operations
 */
@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "*")
public class SearchController {
    
    @Autowired
    private DocumentIndexingService documentIndexingService;
    
    @Autowired
    private AuditService auditService;
    
    /**
     * Search documents with advanced query
     */
    @GetMapping("/documents")
    public ResponseEntity<SearchResult> searchDocuments(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) List<String> documentTypes,
            @RequestParam(required = false) List<String> departments,
            @RequestParam(required = false) List<String> uploadedBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Double minOcrConfidence,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        try {
            // Create search filters
            SearchFilters filters = new SearchFilters();
            filters.setDocumentTypes(documentTypes);
            filters.setDepartments(departments);
            filters.setUploadedBy(uploadedBy);
            filters.setStartDate(startDate);
            filters.setEndDate(endDate);
            filters.setMinOcrConfidence(minOcrConfidence);
            filters.setIsActive(isActive);
            
            // Create pageable
            Pageable pageable = PageRequest.of(page, size);
            
            // Perform search
            SearchResult result = documentIndexingService.searchDocuments(query, filters, pageable);
            
            // Log search activity
            auditService.logActivity(
                authentication.getName(),
                "SEARCH",
                "Document search performed",
                Map.of("query", query != null ? query : "", "filters", filters.toString())
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get document suggestions for autocomplete
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSuggestions(
            @RequestParam String prefix,
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        
        try {
            List<String> suggestions = documentIndexingService.getSuggestions(prefix, limit);
            
            // Log suggestion activity
            auditService.logActivity(
                authentication.getName(),
                "SEARCH_SUGGESTION",
                "Document suggestion requested",
                Map.of("prefix", prefix, "limit", limit)
            );
            
            return ResponseEntity.ok(suggestions);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get search statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getSearchStatistics(Authentication authentication) {
        try {
            Map<String, Object> statistics = documentIndexingService.getSearchStatistics();
            
            // Log statistics activity
            auditService.logActivity(
                authentication.getName(),
                "SEARCH_STATISTICS",
                "Search statistics requested",
                Map.of()
            );
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Re-index all documents (Admin only)
     */
    @PostMapping("/reindex")
    public ResponseEntity<Map<String, String>> reindexAllDocuments(Authentication authentication) {
        try {
            documentIndexingService.reindexAllDocuments();
            
            // Log reindex activity
            auditService.logActivity(
                authentication.getName(),
                "REINDEX",
                "Full document reindex performed",
                Map.of()
            );
            
            return ResponseEntity.ok(Map.of("message", "Reindex completed successfully"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Reindex failed: " + e.getMessage()));
        }
    }
    
    /**
     * Advanced search with complex queries
     */
    @PostMapping("/advanced")
    public ResponseEntity<SearchResult> advancedSearch(
            @RequestBody AdvancedSearchRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        try {
            // Create search filters from request
            SearchFilters filters = new SearchFilters();
            filters.setDocumentTypes(request.getDocumentTypes());
            filters.setDepartments(request.getDepartments());
            filters.setUploadedBy(request.getUploadedBy());
            filters.setStartDate(request.getStartDate());
            filters.setEndDate(request.getEndDate());
            filters.setMinOcrConfidence(request.getMinOcrConfidence());
            filters.setIsActive(request.getIsActive());
            
            // Create pageable
            Pageable pageable = PageRequest.of(page, size);
            
            // Perform search
            SearchResult result = documentIndexingService.searchDocuments(request.getQuery(), filters, pageable);
            
            // Log advanced search activity
            auditService.logActivity(
                authentication.getName(),
                "ADVANCED_SEARCH",
                "Advanced document search performed",
                Map.of("request", request.toString())
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Search by document content similarity
     */
    @GetMapping("/similar/{documentId}")
    public ResponseEntity<List<SearchResultItem>> findSimilarDocuments(
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        
        try {
            // This would require implementing similarity search
            // For now, return empty list
            List<SearchResultItem> similarDocuments = List.of();
            
            // Log similarity search activity
            auditService.logActivity(
                authentication.getName(),
                "SIMILARITY_SEARCH",
                "Similar documents search performed",
                Map.of("documentId", documentId, "limit", limit)
            );
            
            return ResponseEntity.ok(similarDocuments);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Advanced Search Request class
     */
    public static class AdvancedSearchRequest {
        private String query;
        private List<String> documentTypes;
        private List<String> departments;
        private List<String> uploadedBy;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Double minOcrConfidence;
        private Boolean isActive;
        private String sortBy;
        private String sortDirection;
        
        // Getters and setters
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
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
        public String getSortBy() { return sortBy; }
        public void setSortBy(String sortBy) { this.sortBy = sortBy; }
        public String getSortDirection() { return sortDirection; }
        public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }
        
        @Override
        public String toString() {
            return "AdvancedSearchRequest{" +
                    "query='" + query + '\'' +
                    ", documentTypes=" + documentTypes +
                    ", departments=" + departments +
                    ", uploadedBy=" + uploadedBy +
                    ", startDate=" + startDate +
                    ", endDate=" + endDate +
                    ", minOcrConfidence=" + minOcrConfidence +
                    ", isActive=" + isActive +
                    ", sortBy='" + sortBy + '\'' +
                    ", sortDirection='" + sortDirection + '\'' +
                    '}';
        }
    }
}

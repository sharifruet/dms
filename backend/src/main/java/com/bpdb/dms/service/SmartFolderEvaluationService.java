package com.bpdb.dms.service;

import com.bpdb.dms.entity.SmartFolderDefinition;
import com.bpdb.dms.entity.SmartFolderScope;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.entity.DocumentIndex;
import com.bpdb.dms.repository.DocumentIndexRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Evaluates Smart Folder (DMC) rule definitions into search results with permission scoping.
 */
@Service
public class SmartFolderEvaluationService {

    private static final Logger logger = LoggerFactory.getLogger(SmartFolderEvaluationService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private DocumentIndexRepository documentIndexRepository;

    /**
     * Evaluate a Smart Folder definition for the requesting user with permission scoping.
     */
    @Cacheable(cacheNames = "dmcEval", key = "T(java.util.Objects).hash(#folder.id, #folder.updatedAt, #requestingUser.id, #requestingUser.department, #pageable.pageNumber, #pageable.pageSize)")
    public Page<DocumentIndex> evaluate(SmartFolderDefinition folder, User requestingUser, Pageable pageable) {
        try {
            if (folder.getIsActive() == null || !folder.getIsActive()) {
                return Page.empty(pageable);
            }

            // Enforce scope: if PRIVATE and not owner, return empty
            if (folder.getScope() == SmartFolderScope.PRIVATE && (folder.getOwner() == null
                || !Objects.equals(folder.getOwner().getId(), requestingUser.getId()))) {
                return Page.empty(pageable);
            }

            Rule rule = parseRule(folder.getDefinition());

            // Initial ES fetch: text query if present, otherwise all (paged)
            Page<DocumentIndex> basePage;
            if (rule.query != null && !rule.query.isBlank()) {
                basePage = documentIndexRepository.searchByText(rule.query, pageable);
            } else {
                basePage = documentIndexRepository.findAll(pageable);
            }

            // Apply filters and permission scoping in-memory for now
            List<DocumentIndex> filtered = basePage.getContent().stream()
                .filter(doc -> {
                    // Permission scoping: non-admins see only their department
                    if (!isAdmin(requestingUser)) {
                        if (doc.getDepartment() != null && requestingUser.getDepartment() != null) {
                            if (!requestingUser.getDepartment().equals(doc.getDepartment())) {
                                return false;
                            }
                        }
                    }
                    // Rule: departments
                    if (!rule.departments.isEmpty()) {
                        if (doc.getDepartment() == null || !rule.departments.contains(doc.getDepartment())) {
                            return false;
                        }
                    }
                    // Rule: document types
                    if (!rule.documentTypes.isEmpty()) {
                        if (doc.getDocumentType() == null || !rule.documentTypes.contains(doc.getDocumentType())) {
                            return false;
                        }
                    }
                    // Rule: uploadedBy usernames
                    if (!rule.uploadedBys.isEmpty()) {
                        if (doc.getUploadedByUsername() == null || !rule.uploadedBys.contains(doc.getUploadedByUsername())) {
                            return false;
                        }
                    }
                    // Rule: createdAt date range
                    if (rule.createdFrom != null) {
                        if (doc.getCreatedAt() == null || doc.getCreatedAt().isBefore(rule.createdFrom)) {
                            return false;
                        }
                    }
                    if (rule.createdTo != null) {
                        if (doc.getCreatedAt() == null || doc.getCreatedAt().isAfter(rule.createdTo)) {
                            return false;
                        }
                    }
                    // Rule: isActive
                    if (rule.isActive != null) {
                        if (doc.getIsActive() == null || !rule.isActive.equals(doc.getIsActive())) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

            return new PageImpl<>(filtered, pageable, basePage.getTotalElements());
        } catch (Exception e) {
            logger.error("Failed to evaluate Smart Folder {}: {}", folder.getId(), e.getMessage());
            return Page.empty(pageable);
        }
    }

    private boolean isAdmin(User user) {
        try {
            if (user == null || user.getRole() == null || user.getRole().getName() == null) {
                return false;
            }
            return "ADMIN".equalsIgnoreCase(user.getRole().getName().name());
        } catch (Throwable t) {
            return false;
        }
    }

    private Rule parseRule(String json) {
        Rule rule = new Rule();
        if (json == null || json.isBlank()) {
            return rule;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            rule.query = text(root, "query");
            rule.documentTypes = asStringSet(root, "documentTypes");
            rule.departments = asStringSet(root, "departments");
            rule.uploadedBys = asStringSet(root, "uploadedBy");
            rule.tags = asStringSet(root, "tags");
            rule.isActive = bool(root, "isActive");
            rule.createdFrom = date(root, "createdFrom");
            rule.createdTo = date(root, "createdTo");
        } catch (Exception e) {
            // fallback to empty rule on parse failure
        }
        return rule;
    }

    private String text(JsonNode root, String field) {
        return root.has(field) && !root.get(field).isNull() ? root.get(field).asText() : null;
    }

    private Boolean bool(JsonNode root, String field) {
        return root.has(field) && !root.get(field).isNull() ? root.get(field).asBoolean() : null;
    }

    private Set<String> asStringSet(JsonNode root, String field) {
        if (!root.has(field) || root.get(field).isNull()) {
            return Collections.emptySet();
        }
        JsonNode node = root.get(field);
        if (node.isArray()) {
            Set<String> values = new HashSet<>();
            node.forEach(n -> values.add(n.asText()));
            return values;
        } else {
            return Collections.singleton(node.asText());
        }
    }

    private LocalDate date(JsonNode root, String field) {
        String v = text(root, field);
        if (v == null || v.isBlank()) return null;
        try {
            return LocalDate.parse(v);
        } catch (Exception e) {
            return null;
        }
    }

    private static class Rule {
        String query;
        Set<String> documentTypes = Collections.emptySet();
        Set<String> departments = Collections.emptySet();
        Set<String> uploadedBys = Collections.emptySet();
        Set<String> tags = Collections.emptySet();
        LocalDate createdFrom;
        LocalDate createdTo;
        Boolean isActive;
    }
}



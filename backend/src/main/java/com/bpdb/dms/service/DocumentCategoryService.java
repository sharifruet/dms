package com.bpdb.dms.service;

import com.bpdb.dms.entity.DocumentCategory;
import com.bpdb.dms.repository.DocumentCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DocumentCategoryService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentCategoryService.class);

    private final DocumentCategoryRepository documentCategoryRepository;

    public DocumentCategoryService(DocumentCategoryRepository documentCategoryRepository) {
        this.documentCategoryRepository = documentCategoryRepository;
    }

    public List<DocumentCategory> getActiveCategories() {
        return documentCategoryRepository.findAllByIsActiveTrueOrderByDisplayNameAsc();
    }

    public List<String> getActiveCategoryNames() {
        return getActiveCategories()
            .stream()
            .map(DocumentCategory::getName)
            .collect(Collectors.toList());
    }

    public Optional<DocumentCategory> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return documentCategoryRepository.findByNameIgnoreCase(name);
    }

    @Transactional
    public DocumentCategory ensureCategoryExists(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new IllegalArgumentException("Document category name must not be empty");
        }

        String normalized = rawName.trim().toUpperCase(Locale.ROOT);
        return documentCategoryRepository
            .findByNameIgnoreCase(normalized)
            .orElseGet(() -> {
                logger.info("Creating new document category dynamically: {}", normalized);
                DocumentCategory category = new DocumentCategory();
                category.setName(normalized);
                category.setDisplayName(toTitleCase(normalized));
                category.setDescription("Created automatically from document upload");
                return documentCategoryRepository.save(category);
            });
    }

    @Transactional
    public DocumentCategory createCategory(String name, String displayName, String description, Boolean isActive) {
        if (documentCategoryRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new IllegalArgumentException("Category already exists with name: " + name);
        }

        DocumentCategory category = new DocumentCategory();
        category.setName(name);
        category.setDisplayName(displayName != null ? displayName : toTitleCase(name));
        category.setDescription(description);
        category.setIsActive(isActive != null ? isActive : Boolean.TRUE);
        return documentCategoryRepository.save(category);
    }

    @Transactional
    public DocumentCategory updateCategory(Long id, Map<String, Object> updates) {
        DocumentCategory category = documentCategoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Document category not found for id " + id));

        if (updates.containsKey("name")) {
            category.setName((String) updates.get("name"));
        }
        if (updates.containsKey("displayName")) {
            category.setDisplayName((String) updates.get("displayName"));
        }
        if (updates.containsKey("description")) {
            category.setDescription((String) updates.get("description"));
        }
        if (updates.containsKey("isActive")) {
            category.setIsActive((Boolean) updates.get("isActive"));
        }

        return documentCategoryRepository.save(category);
    }

    private String toTitleCase(String value) {
        if (value == null) {
            return null;
        }
        String lower = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] words = lower.split("\\s+");
        return java.util.Arrays.stream(words)
            .filter(w -> !w.isBlank())
            .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
            .collect(Collectors.joining(" "));
    }
}


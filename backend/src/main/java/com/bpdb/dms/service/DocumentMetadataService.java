package com.bpdb.dms.service;

import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.DocumentMetadata;
import com.bpdb.dms.entity.DocumentMetadata.MetadataSource;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.service.DocumentTypeFieldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentMetadataService {

    @Autowired(required = false)
    private DocumentTypeFieldService documentTypeFieldService;

    private static final Map<String, List<String>> REQUIRED_FIELDS = Map.of(
        "CONTRACT", List.of("title", "contractNo", "date"),
        "TENDER", List.of("title", "expiryDate"),
        "BILL", List.of("date", "amount")
    );

    private static final Map<String, String> KEY_ALIASES = Map.ofEntries(
        Map.entry("contractnumber", "contractNo"),
        Map.entry("contract_no", "contractNo"),
        Map.entry("contractnum", "contractNo"),
        Map.entry("contractno", "contractNo"),
        Map.entry("contract_id", "contractNo"),
        Map.entry("expiry", "expiryDate"),
        Map.entry("expirydate", "expiryDate"),
        Map.entry("expirationdate", "expiryDate"),
        Map.entry("expiration", "expiryDate"),
        Map.entry("amountdue", "amount"),
        Map.entry("totalamount", "amount"),
        Map.entry("total", "amount"),
        Map.entry("billamount", "amount"),
        Map.entry("billdate", "date"),
        Map.entry("documentdate", "date")
    );

    private static final Pattern DATE_PATTERN = Pattern.compile(
        "(\\d{1,2}[\\-/]\\d{1,2}[\\-/]\\d{2,4}|\\d{4}[\\-/]\\d{1,2}[\\-/]\\d{1,2})"
    );

    private static final Pattern CONTRACT_NO_PATTERN = Pattern.compile(
        "(?i)contract\\s*(number|no\\.?|#)\\s*[:\\-]?\\s*([A-Z0-9\\-/]+)"
    );

    private static final Pattern EXPIRY_PATTERN = Pattern.compile(
        "(?i)(expiry|expiration|closing)\\s*(date)?\\s*[:\\-]?\\s*" + DATE_PATTERN.pattern()
    );

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
        "(?i)(amount|total|due)\\s*[:\\-]?\\s*([$€£]?\\s*[0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?)"
    );

    private static final DateTimeFormatter[] SUPPORTED_DATE_FORMATS = new DateTimeFormatter[] {
        DateTimeFormatter.ofPattern("d/M/uuuu"),
        DateTimeFormatter.ofPattern("d-M-uuuu"),
        DateTimeFormatter.ofPattern("uuuu-M-d"),
        DateTimeFormatter.ofPattern("uuuu/M/d"),
        DateTimeFormatter.ofPattern("d/M/uu"),
        DateTimeFormatter.ofPattern("d-M-uu"),
        DateTimeFormatter.ISO_LOCAL_DATE
    };

    private final DocumentRepository documentRepository;

    public DocumentMetadataService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public Map<String, String> applyManualMetadata(Document document, Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return getMetadataMap(document);
        }
        Map<String, String> normalized = normalizeMetadata(document.getDocumentType(), metadata);
        Map<String, String> existing = getMetadataMap(document);
        Map<String, String> combined = new HashMap<>(existing);
        combined.putAll(normalized);
        validateRequiredFields(document.getDocumentType(), combined);
        applyMetadata(document, normalized, MetadataSource.MANUAL);
        return getMetadataMap(document);
    }

    public Map<String, String> applyStructuredMetadata(Document document, Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return getMetadataMap(document);
        }
        Map<String, String> normalized = normalizeMetadata(document.getDocumentType(), metadata);
        applyMetadata(document, normalized, MetadataSource.AUTO_STRUCTURE);
        return getMetadataMap(document);
    }

    public Map<String, String> applyAutoMetadata(Document document, Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return getMetadataMap(document);
        }
        Map<String, String> normalized = normalizeMetadata(document.getDocumentType(), metadata);
        applyMetadata(document, normalized, MetadataSource.AUTO_OCR);
        return getMetadataMap(document);
    }

    public Map<String, String> extractMetadataFromText(Document document, String extractedText) {
        if (extractedText == null || extractedText.isBlank()) {
            return getMetadataMap(document);
        }
        String documentType = normalizeType(document.getDocumentType());
        Map<String, String> inferred = new LinkedHashMap<>();
        String fallbackTitle = deriveTitle(document);

        // First, try to use configured field mappings if available
        if (documentTypeFieldService != null) {
            Map<String, String> mappedFields = documentTypeFieldService.mapOcrDataToFields(documentType, extractedText);
            inferred.putAll(mappedFields);
        }

        // Fallback to legacy extraction patterns
        switch (documentType) {
            case "CONTRACT", "CONTRACT_AGREEMENT" -> {
                inferred.putIfAbsent("title", fallbackTitle);
                if (!inferred.containsKey("contractNumber") && !inferred.containsKey("contractNo")) {
                    extractFirstMatch(CONTRACT_NO_PATTERN, extractedText, 2)
                        .ifPresent(value -> inferred.put("contractNumber", value));
                }
                if (!inferred.containsKey("contractDate") && !inferred.containsKey("date")) {
                    extractDate(extractedText).ifPresent(date -> inferred.put("contractDate", date));
                }
            }
            case "TENDER", "TENDER_NOTICE", "TENDER_DOCUMENT" -> {
                inferred.putIfAbsent("title", fallbackTitle);
                if (!inferred.containsKey("expiryDate") && !inferred.containsKey("closingDate")) {
                    extractExpiryDate(extractedText).ifPresent(value -> inferred.put("closingDate", value));
                }
                if (!inferred.containsKey("tenderDate") && !inferred.containsKey("date")) {
                    extractDate(extractedText).ifPresent(date -> inferred.put("tenderDate", date));
                }
            }
            case "BILL" -> {
                // Bill-specific metadata extraction will be enhanced in Phase 3
                if (!inferred.containsKey("date")) {
                    extractDate(extractedText).ifPresent(date -> inferred.put("date", date));
                }
                if (!inferred.containsKey("amount")) {
                    extractAmount(extractedText).ifPresent(amount -> inferred.put("amount", amount));
                }
                inferred.putIfAbsent("title", fallbackTitle);
            }
            // Note: APP is no longer a document type - removed from metadata inference
            default -> inferred.putIfAbsent("title", fallbackTitle);
        }

        applyMetadata(document, normalizeMetadata(documentType, inferred), MetadataSource.AUTO_OCR);
        return getMetadataMap(document);
    }

    public Map<String, String> getMetadataMap(Document document) {
        Document managedDocument = documentRepository.findById(document.getId())
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + document.getId()));
        return managedDocument.getMetadataEntries()
            .stream()
            .collect(Collectors.toMap(DocumentMetadata::getKey, DocumentMetadata::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    private void applyMetadata(Document document, Map<String, String> metadata, MetadataSource source) {
        if (metadata.isEmpty()) {
            return;
        }

        Document managedDocument = documentRepository.findById(document.getId())
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + document.getId()));

        List<DocumentMetadata> entries = managedDocument.getMetadataEntries();
        if (entries == null) {
            entries = new ArrayList<>();
            managedDocument.setMetadataEntries(entries);
        }

        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null || value.isBlank()) {
                continue;
            }

            Optional<DocumentMetadata> manual = entries.stream()
                .filter(meta -> meta.getKey().equals(key) && meta.getSource() == MetadataSource.MANUAL)
                .findFirst();

            if (manual.isPresent() && source != MetadataSource.MANUAL) {
                continue;
            }

            DocumentMetadata target = entries.stream()
                .filter(meta -> meta.getKey().equals(key))
                .findFirst()
                .orElse(null);

            if (target == null) {
                target = new DocumentMetadata(managedDocument, key, value.trim(), source);
                entries.add(target);
            } else {
                if (target.getSource() == MetadataSource.MANUAL && source != MetadataSource.MANUAL) {
                    continue;
                }
                target.setValue(value.trim());
                target.setSource(source);
            }
        }

        managedDocument.setUpdatedAt(LocalDateTime.now());
        documentRepository.save(managedDocument);
    }

    private Map<String, String> normalizeMetadata(String documentType, Map<String, String> metadata) {
        Map<String, String> normalized = new LinkedHashMap<>();
        if (metadata == null) {
            return normalized;
        }
        metadata.forEach((rawKey, rawValue) -> {
            if (rawKey == null || rawValue == null) {
                return;
            }
            String key = canonicalKey(rawKey.trim());
            String value = rawValue.trim();
            if (value.isBlank()) {
                return;
            }
            if ("amount".equals(key)) {
                value = sanitizeAmount(value);
            }
            if ("date".equals(key) || "expiryDate".equals(key)) {
                value = standardizeDateString(value).orElse(value);
            }
            normalized.put(key, value);
        });
        if (!normalized.containsKey("title") && normalized.containsKey("name")) {
            normalized.put("title", normalized.get("name"));
        }
        return normalized;
    }

    private void validateRequiredFields(String documentType, Map<String, String> metadata) {
        if (documentType == null) {
            return;
        }
        List<String> required = REQUIRED_FIELDS.get(normalizeType(documentType));
        if (required == null || required.isEmpty()) {
            return;
        }
        List<String> missing = required.stream()
            .filter(field -> !metadata.containsKey(field))
            .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing required metadata fields for " + documentType + ": " + missing);
        }
    }

    private String canonicalKey(String rawKey) {
        String lower = rawKey.toLowerCase(Locale.ROOT).replaceAll("[\\s\\-]+", "");
        return KEY_ALIASES.getOrDefault(lower, switch (lower) {
            case "contractdate" -> "date";
            case "expirydate" -> "expiryDate";
            case "amount" -> "amount";
            case "title" -> "title";
            case "date" -> "date";
            default -> rawKey.trim();
        });
    }

    private Optional<String> extractFirstMatch(Pattern pattern, String text, int group) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return Optional.ofNullable(matcher.group(group)).map(String::trim);
        }
        return Optional.empty();
    }

    private Optional<String> extractDate(String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        while (matcher.find()) {
            String candidate = matcher.group(1);
            for (DateTimeFormatter formatter : SUPPORTED_DATE_FORMATS) {
                try {
                    LocalDate parsed = LocalDate.parse(candidate.replace('.', '/'), formatter);
                    return Optional.of(parsed.toString());
                } catch (DateTimeParseException ignored) {
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> extractAmount(String text) {
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        if (matcher.find()) {
            return Optional.ofNullable(matcher.group(2)).map(this::sanitizeAmount);
        }
        return Optional.empty();
    }

    private Optional<String> extractExpiryDate(String text) {
        Matcher matcher = EXPIRY_PATTERN.matcher(text);
        if (matcher.find()) {
            String value = matcher.group(matcher.groupCount());
            return Optional.ofNullable(value).map(String::trim);
        }
        return Optional.empty();
    }

    private Optional<String> standardizeDateString(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String cleaned = raw.trim().replace('.', '/');
        for (DateTimeFormatter formatter : SUPPORTED_DATE_FORMATS) {
            try {
                LocalDate parsed = LocalDate.parse(cleaned, formatter);
                return Optional.of(parsed.toString());
            } catch (DateTimeParseException ignored) {
            }
        }
        return Optional.empty();
    }

    private String sanitizeAmount(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("[^0-9.,-]", "");
        if (cleaned.chars().filter(ch -> ch == ',').count() > 1 && cleaned.contains(".")) {
            cleaned = cleaned.replace(",", "");
        }
        return cleaned.trim();
    }

    private String normalizeType(String documentType) {
        return documentType != null ? documentType.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String deriveTitle(Document document) {
        if (document.getDescription() != null && !document.getDescription().isBlank()) {
            return document.getDescription();
        }
        if (document.getOriginalName() != null && !document.getOriginalName().isBlank()) {
            return document.getOriginalName();
        }
        return document.getFileName();
    }
}


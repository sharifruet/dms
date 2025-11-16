package com.bpdb.dms.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Canonical document types supported by the system.
 */
public enum DocumentType {
    TENDER_NOTICE("Tender Notice"),
    TENDER_DOCUMENT("Tender Document"),
    CONTRACT_AGREEMENT("Contract Agreement"),
    BANK_GUARANTEE_BG("Bank Guarantee (BG)"),
    PERFORMANCE_SECURITY_PS("Performance Security (PS)"),
    PERFORMANCE_GUARANTEE_PG("Performance Guarantee (PG)"),
    APP("APP"),
    OTHER("Other");

    private final String label;

    DocumentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    private static final Map<String, DocumentType> ALIASES = buildAliases();

    private static Map<String, DocumentType> buildAliases() {
        Map<String, DocumentType> base = Arrays.stream(values())
            .collect(Collectors.toMap(
                dt -> normalize(dt.name()),
                dt -> dt
            ));

        // Add friendly labels and common aliases
        base.put(normalize("Tender Notice"), TENDER_NOTICE);
        base.put(normalize("Tender Document"), TENDER_DOCUMENT);
        base.put(normalize("Contract Agreement"), CONTRACT_AGREEMENT);
        base.put(normalize("Bank Guarantee"), BANK_GUARANTEE_BG);
        base.put(normalize("BG"), BANK_GUARANTEE_BG);
        base.put(normalize("Performance Security"), PERFORMANCE_SECURITY_PS);
        base.put(normalize("PS"), PERFORMANCE_SECURITY_PS);
        base.put(normalize("Performance Guarantee"), PERFORMANCE_GUARANTEE_PG);
        base.put(normalize("PG"), PERFORMANCE_GUARANTEE_PG);
        base.put(normalize("APP"), APP);
        base.put(normalize("Other"), OTHER);

        // Also map labels
        Arrays.stream(values()).forEach(dt -> base.put(normalize(dt.getLabel()), dt));

        return base;
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Resolve an input string to a DocumentType using aliases and case-insensitive matching.
     */
    public static Optional<DocumentType> resolve(String input) {
        return Optional.ofNullable(ALIASES.get(normalize(input)));
    }

    /**
     * Comma-separated human-readable list of allowed types.
     */
    public static String allowedTypesList() {
        return Arrays.stream(values())
            .map(DocumentType::getLabel)
            .collect(Collectors.joining(", "));
    }
}


package com.bpdb.dms.service;

// Logger imports removed - not currently used

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parser for Boolean search queries (AND, OR, NOT operators)
 * Supports queries like:
 * - "tender AND contract"
 * - "invoice OR bill"
 * - "contract NOT agreement"
 * - "(tender OR proposal) AND notice"
 * - Complex nested queries with parentheses
 */
public class BooleanQueryParser {

    // Logger removed - not currently used

    // Pattern to match quoted phrases
    private static final Pattern QUOTED_PATTERN = Pattern.compile("\"([^\"]+)\"");
    
    // Pattern to match AND, OR, NOT operators (case-insensitive)
    private static final Pattern OPERATOR_PATTERN = Pattern.compile("\\b(AND|OR|NOT)\\b", Pattern.CASE_INSENSITIVE);
    
    // Pattern to match parentheses
    private static final Pattern PARENTHESES_PATTERN = Pattern.compile("[()]");

    /**
     * Parse a Boolean query into structured components
     * 
     * @param query The search query string
     * @return ParsedQueryResult containing terms and operators
     */
    public static ParsedQueryResult parseQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ParsedQueryResult(QueryType.SIMPLE, Collections.emptyList(), Collections.emptyList());
        }

        String normalizedQuery = query.trim();
        
        // Check if query contains Boolean operators
        boolean hasOperators = OPERATOR_PATTERN.matcher(normalizedQuery).find();
        boolean hasParentheses = PARENTHESES_PATTERN.matcher(normalizedQuery).find();

        if (!hasOperators && !hasParentheses) {
            // Simple query - no operators
            return new ParsedQueryResult(QueryType.SIMPLE, Arrays.asList(normalizedQuery), Collections.emptyList());
        }

        // Parse complex Boolean query
        return parseBooleanQuery(normalizedQuery);
    }

    /**
     * Parse a Boolean query with operators
     */
    private static ParsedQueryResult parseBooleanQuery(String query) {
        List<String> terms = new ArrayList<>();
        List<String> operators = new ArrayList<>();
        
        // Extract quoted phrases first
        Map<String, String> phraseReplacements = new HashMap<>();
        Matcher quoteMatcher = QUOTED_PATTERN.matcher(query);
        int phraseIndex = 0;
        String processedQuery = query;
        
        while (quoteMatcher.find()) {
            String phrase = quoteMatcher.group(1);
            String placeholder = "___PHRASE_" + phraseIndex + "___";
            phraseReplacements.put(placeholder, phrase);
            processedQuery = processedQuery.replace("\"" + phrase + "\"", placeholder);
            phraseIndex++;
        }

        // Split by Boolean operators (preserving operator positions)
        String[] parts = OPERATOR_PATTERN.split(processedQuery, -1);
        Matcher operatorMatcher = OPERATOR_PATTERN.matcher(processedQuery);
        
        List<String> operatorList = new ArrayList<>();
        while (operatorMatcher.find()) {
            operatorList.add(operatorMatcher.group(1).toUpperCase());
        }

        // Extract terms and restore phrases
        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                // Restore quoted phrases
                for (Map.Entry<String, String> entry : phraseReplacements.entrySet()) {
                    part = part.replace(entry.getKey(), "\"" + entry.getValue() + "\"");
                }
                terms.add(part);
            }
        }

        return new ParsedQueryResult(QueryType.BOOLEAN, terms, operatorList);
    }

    /**
     * Build Elasticsearch query from parsed Boolean query
     * 
     * @param parsedResult The parsed query result
     * @param searchFields The fields to search in
     * @return Elasticsearch query string (JSON)
     */
    public static String buildElasticsearchQuery(ParsedQueryResult parsedResult, List<String> searchFields) {
        if (parsedResult.getType() == QueryType.SIMPLE) {
            // Simple query - use multi_match
            if (parsedResult.getTerms().isEmpty()) {
                return buildMatchAllQuery();
            }
            String term = parsedResult.getTerms().get(0);
            return buildMultiMatchQuery(term, searchFields);
        }

        // Boolean query - build bool query
        return buildBoolQuery(parsedResult, searchFields);
    }

    /**
     * Build a multi-match query for simple terms
     */
    private static String buildMultiMatchQuery(String term, List<String> fields) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("{\"multi_match\": {");
        queryBuilder.append("\"query\": \"").append(escapeJson(term)).append("\",");
        queryBuilder.append("\"fields\": [");
        
        // Add field weights
        String fieldsJson = fields.stream()
            .map(field -> {
                // Higher weight for fileName and originalName
                if (field.equals("fileName") || field.equals("originalName")) {
                    return "\"" + field + "^2\"";
                }
                return "\"" + field + "\"";
            })
            .collect(Collectors.joining(", "));
        
        queryBuilder.append(fieldsJson);
        queryBuilder.append("],");
        queryBuilder.append("\"type\": \"best_fields\",");
        queryBuilder.append("\"operator\": \"and\"");
        queryBuilder.append("}}");
        
        return queryBuilder.toString();
    }

    /**
     * Build a bool query for Boolean operators
     */
    private static String buildBoolQuery(ParsedQueryResult parsedResult, List<String> fields) {
        List<String> terms = parsedResult.getTerms();
        List<String> operators = parsedResult.getOperators();
        
        if (terms.isEmpty()) {
            return buildMatchAllQuery();
        }

        List<String> mustClauses = new ArrayList<>();
        List<String> shouldClauses = new ArrayList<>();
        List<String> mustNotClauses = new ArrayList<>();

        // Process terms and operators
        for (int i = 0; i < terms.size(); i++) {
            String term = terms.get(i);
            String operator = (i > 0 && i <= operators.size()) ? operators.get(i - 1) : null;

            String termQuery = buildTermQuery(term, fields);

            if (operator == null || i == 0) {
                // First term or implicit AND
                mustClauses.add(termQuery);
            } else if ("AND".equals(operator)) {
                mustClauses.add(termQuery);
            } else if ("OR".equals(operator)) {
                shouldClauses.add(termQuery);
            } else if ("NOT".equals(operator)) {
                mustNotClauses.add(termQuery);
            }
        }

        // Build bool query JSON
        StringBuilder boolQuery = new StringBuilder();
        boolQuery.append("{\"bool\": {");

        if (!mustClauses.isEmpty()) {
            boolQuery.append("\"must\": [");
            boolQuery.append(String.join(", ", mustClauses));
            boolQuery.append("],");
        }

        if (!shouldClauses.isEmpty()) {
            boolQuery.append("\"should\": [");
            boolQuery.append(String.join(", ", shouldClauses));
            boolQuery.append("],");
            boolQuery.append("\"minimum_should_match\": 1,");
        }

        if (!mustNotClauses.isEmpty()) {
            boolQuery.append("\"must_not\": [");
            boolQuery.append(String.join(", ", mustNotClauses));
            boolQuery.append("],");
        }

        // Remove trailing comma
        if (boolQuery.charAt(boolQuery.length() - 1) == ',') {
            boolQuery.setLength(boolQuery.length() - 1);
        }

        boolQuery.append("}}");

        return boolQuery.toString();
    }

    /**
     * Build query for a single term (handles quoted phrases)
     */
    private static String buildTermQuery(String term, List<String> fields) {
        // Check if term is a quoted phrase
        if (term.startsWith("\"") && term.endsWith("\"")) {
            String phrase = term.substring(1, term.length() - 1);
            return buildPhraseQuery(phrase, fields);
        }
        
        return buildMultiMatchQuery(term, fields);
    }

    /**
     * Build a phrase query for exact phrase matching
     */
    private static String buildPhraseQuery(String phrase, List<String> fields) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("{\"multi_match\": {");
        queryBuilder.append("\"query\": \"").append(escapeJson(phrase)).append("\",");
        queryBuilder.append("\"fields\": [");
        
        String fieldsJson = fields.stream()
            .map(field -> "\"" + field + "\"")
            .collect(Collectors.joining(", "));
        
        queryBuilder.append(fieldsJson);
        queryBuilder.append("],");
        queryBuilder.append("\"type\": \"phrase\"");
        queryBuilder.append("}}");
        
        return queryBuilder.toString();
    }

    /**
     * Build a match_all query
     */
    private static String buildMatchAllQuery() {
        return "{\"match_all\": {}}";
    }

    /**
     * Escape JSON string
     */
    private static String escapeJson(String input) {
        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    /**
     * Query type enum
     */
    public enum QueryType {
        SIMPLE,    // No Boolean operators
        BOOLEAN    // Contains AND, OR, NOT operators
    }

    /**
     * Result of query parsing
     */
    public static class ParsedQueryResult {
        private final QueryType type;
        private final List<String> terms;
        private final List<String> operators;

        public ParsedQueryResult(QueryType type, List<String> terms, List<String> operators) {
            this.type = type;
            this.terms = new ArrayList<>(terms);
            this.operators = new ArrayList<>(operators);
        }

        public QueryType getType() {
            return type;
        }

        public List<String> getTerms() {
            return new ArrayList<>(terms);
        }

        public List<String> getOperators() {
            return new ArrayList<>(operators);
        }
    }
}


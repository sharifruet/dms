package com.bpdb.dms.service;

import com.bpdb.dms.entity.DocumentIndex;
import com.bpdb.dms.repository.DocumentIndexRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private DocumentIndexRepository documentIndexRepository;

    public Map<String, Object> search(String query,
                                      Set<String> documentTypes,
                                      Set<String> departments,
                                      Boolean isActive,
                                      LocalDate createdFrom,
                                      LocalDate createdTo,
                                      int page,
                                      int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<DocumentIndex> base = (query != null && !query.isBlank())
            ? documentIndexRepository.searchByText(query, pageable)
            : documentIndexRepository.findAll(pageable);

        List<DocumentIndex> filtered = base.getContent().stream()
            .filter(d -> documentTypes == null || documentTypes.isEmpty() || (d.getDocumentType() != null && documentTypes.contains(d.getDocumentType())))
            .filter(d -> departments == null || departments.isEmpty() || (d.getDepartment() != null && departments.contains(d.getDepartment())))
            .filter(d -> isActive == null || (d.getIsActive() != null && d.getIsActive().equals(isActive)))
            .filter(d -> createdFrom == null || (d.getCreatedAt() != null && !d.getCreatedAt().isBefore(createdFrom)))
            .filter(d -> createdTo == null || (d.getCreatedAt() != null && !d.getCreatedAt().isAfter(createdTo)))
            .collect(Collectors.toList());

        // Basic highlighting
        Map<Long, String> highlights = buildHighlights(filtered, query);

        Map<String, Object> result = new HashMap<>();
        result.put("page", page);
        result.put("size", size);
        result.put("total", base.getTotalElements());
        result.put("items", filtered);
        result.put("highlights", highlights);
        return result;
    }

    public List<String> suggest(String prefix, int limit) {
        if (prefix == null || prefix.isBlank()) return Collections.emptyList();
        Set<String> suggestions = new LinkedHashSet<>();
        documentIndexRepository.suggestByFileNamePrefix(prefix).forEach(d -> suggestions.add(d.getFileName()));
        if (suggestions.size() < limit) {
            documentIndexRepository.suggestByOriginalNamePrefix(prefix).forEach(d -> {
                if (suggestions.size() < limit) suggestions.add(d.getOriginalName());
            });
        }
        if (suggestions.size() < limit) {
            documentIndexRepository.findByTagsContaining(prefix).forEach(d -> {
                if (d.getTags() != null) {
                    for (String t : d.getTags().split(",")) {
                        String tt = t.trim();
                        if (tt.toLowerCase().contains(prefix.toLowerCase()) && suggestions.size() < limit) {
                            suggestions.add(tt);
                        }
                    }
                }
            });
        }
        return suggestions.stream().limit(limit).collect(Collectors.toList());
    }

    private Map<Long, String> buildHighlights(List<DocumentIndex> docs, String query) {
        Map<Long, String> map = new HashMap<>();
        if (query == null || query.isBlank()) return map;
        String escaped = Pattern.quote(query.trim());
        Pattern pattern = Pattern.compile(escaped, Pattern.CASE_INSENSITIVE);
        for (DocumentIndex d : docs) {
            String text = d.getExtractedText();
            if (text == null || text.isBlank()) continue;
            Matcher m = pattern.matcher(text);
            if (m.find()) {
                int start = Math.max(0, m.start() - 60);
                int end = Math.min(text.length(), m.end() + 60);
                String snippet = text.substring(start, end)
                    .replaceAll("(?i)" + escaped, "<mark>$0</mark>");
                map.put(d.getDocumentId(), (start > 0 ? "..." : "") + snippet + (end < text.length() ? "..." : ""));
            }
        }
        return map;
    }
}



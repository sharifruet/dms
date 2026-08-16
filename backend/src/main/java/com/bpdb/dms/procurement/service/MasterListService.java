package com.bpdb.dms.procurement.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.bpdb.dms.procurement.entity.ProcurementMasterList;
import com.bpdb.dms.procurement.repository.ProcurementMasterListRepository;

/**
 * The permitted values for Procurement Type, Method and Nature (Q-8, REQ-2.4).
 *
 * <p>The requirement is specific about how a mismatch behaves: unmatched OCR values are
 * "flagged for manual selection rather than accepted silently". Not rejected — a tender
 * notice that says "Open Tender" instead of "OTM" is a real document and blocking it would
 * push people out of the system. So an unknown value is kept, reported, and the user picks
 * the right one from the list.
 *
 * <p>Matching is lenient about the things OCR gets wrong — case, spacing, punctuation, and
 * the label as well as the code — because "e-GP/ OTM" on the supplied APP should resolve
 * to OTM rather than be flagged as a stranger.
 */
@Service
public class MasterListService {

    private final ProcurementMasterListRepository repository;

    public MasterListService(ProcurementMasterListRepository repository) {
        this.repository = repository;
    }

    /** Every list, keyed by list name, for the Stage 2 form's dropdowns. */
    public Map<String, List<ProcurementMasterList>> allLists() {
        Map<String, List<ProcurementMasterList>> lists = new LinkedHashMap<>();
        for (ProcurementMasterList value : repository.findByIsActiveTrueOrderByListKeyAscDisplayOrderAsc()) {
            lists.computeIfAbsent(value.getListKey(), k -> new java.util.ArrayList<>()).add(value);
        }
        return lists;
    }

    public List<ProcurementMasterList> values(String listKey) {
        return repository.findByListKeyAndIsActiveTrueOrderByDisplayOrderAsc(listKey);
    }

    /**
     * Resolve a captured value to a permitted code.
     *
     * @return the matching code, or empty when nothing matches — which is a prompt for
     *         manual selection, not an error
     */
    public Optional<String> resolve(String listKey, String captured) {
        if (captured == null || captured.isBlank()) {
            return Optional.empty();
        }
        String needle = normalize(captured);
        List<ProcurementMasterList> permitted = values(listKey);

        for (ProcurementMasterList value : permitted) {
            if (needle.equals(normalize(value.getValueCode()))
                    || needle.equals(normalize(value.getValueLabel()))) {
                return Optional.of(value.getValueCode());
            }
        }
        // A tender notice writes "e-GP/ OTM"; the method is in there, surrounded by how it
        // was procured. Match on a whole token so "OTM" is found but "NCT" is not matched
        // inside an unrelated word.
        for (ProcurementMasterList value : permitted) {
            String code = normalize(value.getValueCode());
            if (!code.isEmpty() && containsToken(needle, code)) {
                return Optional.of(value.getValueCode());
            }
        }
        return Optional.empty();
    }

    /** Is this value one of the permitted ones? */
    public boolean isPermitted(String listKey, String captured) {
        return resolve(listKey, captured).isPresent();
    }

    /**
     * The message shown against an unmatched value. Names what was read and what the
     * options are, because "invalid value" tells the user nothing they can act on.
     */
    public String unmatchedMessage(String listKey, String captured) {
        String options = String.join(", ",
                values(listKey).stream().map(ProcurementMasterList::getValueCode).toList());
        return "'" + captured + "' is not one of the permitted values (" + options
                + ") - please choose the right one";
    }

    private static boolean containsToken(String haystack, String token) {
        for (String part : haystack.split("[^a-z0-9]+")) {
            if (part.equals(token)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}

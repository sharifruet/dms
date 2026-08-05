package com.bpdb.dms.procurement.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bpdb.dms.entity.ExpiryTracking;
import com.bpdb.dms.procurement.entity.BudgetEntry;
import com.bpdb.dms.procurement.service.BudgetService;
import com.bpdb.dms.procurement.service.ExtractionService;
import com.bpdb.dms.procurement.service.LinkageService;
import com.bpdb.dms.procurement.service.ProcurementExpiryService;

/**
 * Budget, expiries and the exceptions dashboard - the cross-cutting views that sit
 * beside the stage workspace.
 */
@RestController
@RequestMapping("/api/procurement")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProcurementSupportController {

    private final BudgetService budgetService;
    private final ProcurementExpiryService expiryService;
    private final LinkageService linkageService;
    private final ExtractionService extractionService;

    public ProcurementSupportController(BudgetService budgetService,
                                        ProcurementExpiryService expiryService,
                                        LinkageService linkageService,
                                        ExtractionService extractionService) {
        this.budgetService = budgetService;
        this.expiryService = expiryService;
        this.linkageService = linkageService;
        this.extractionService = extractionService;
    }

    // ------------------------------------------------------------------ budget

    @GetMapping("/packages/{packageId}/budget")
    public ResponseEntity<Map<String, Object>> budget(@PathVariable Long packageId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", budgetService.summary(packageId));
        result.put("entries", budgetService.entries(packageId));
        result.put("consumption", budgetService.consumption(packageId));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/packages/{packageId}/budget")
    public ResponseEntity<?> addBudgetEntry(@PathVariable Long packageId,
                                            @RequestBody BudgetEntryRequest request) {
        try {
            BudgetEntry entry = budgetService.addEntry(packageId, request.entryType,
                    request.amount, request.currency, request.effectiveDate,
                    request.reason, CurrentUser.id());
            return ResponseEntity.ok(entry);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ----------------------------------------------------------------- expiries

    @GetMapping("/expiries")
    public ResponseEntity<List<ExpiryTracking>> expiries(
            @RequestParam(defaultValue = "90") int withinDays) {
        return ResponseEntity.ok(expiryService.expiringWithin(withinDays));
    }

    @GetMapping("/packages/{packageId}/expiries")
    public ResponseEntity<List<ExpiryTracking>> packageExpiries(@PathVariable Long packageId) {
        return ResponseEntity.ok(expiryService.forPackage(packageId));
    }

    /** Extend or amend an instrument: supersede rather than overwrite (REQ-E5). */
    @PostMapping("/expiries/{id}/supersede")
    public ResponseEntity<?> supersede(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            LocalDate newDate = LocalDate.parse(body.get("expiryDate"));
            return ResponseEntity.ok(expiryService.supersede(id, newDate, body.get("reason")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // --------------------------------------------------------------- exceptions

    /**
     * Work that fell through the cracks: documents nobody linked, edges that no longer
     * resolve, and OCR runs that failed (REQ-L10, REQ-P11).
     */
    @GetMapping("/exceptions")
    public ResponseEntity<Map<String, Object>> exceptions() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orphanedDocuments", linkageService.orphanedDocuments());
        result.put("brokenLinks", linkageService.brokenLinks());
        result.put("failedOcrJobs", extractionService.failedJobs());
        return ResponseEntity.ok(result);
    }

    public static class BudgetEntryRequest {
        public String entryType;
        public BigDecimal amount;
        public String currency;
        public LocalDate effectiveDate;
        public String reason;
    }
}

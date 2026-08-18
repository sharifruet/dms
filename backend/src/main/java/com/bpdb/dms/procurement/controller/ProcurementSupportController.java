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
import com.bpdb.dms.procurement.service.LegacyMigrationService;
import com.bpdb.dms.procurement.service.LinkageService;
import com.bpdb.dms.procurement.service.MasterListService;
import com.bpdb.dms.procurement.service.PackageAccessService;
import com.bpdb.dms.procurement.service.ProcurementExpiryAlertService;
import com.bpdb.dms.procurement.service.ProcurementExpiryService;
import com.bpdb.dms.procurement.service.ProcurementPackageService;
import com.bpdb.dms.procurement.service.RetentionService;

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
    private final MasterListService masterListService;
    private final ProcurementExpiryAlertService alertService;
    private final ProcurementPackageService packageService;
    private final RetentionService retentionService;
    private final LegacyMigrationService migrationService;
    private final PackageAccessService accessService;

    public ProcurementSupportController(BudgetService budgetService,
                                        ProcurementExpiryService expiryService,
                                        LinkageService linkageService,
                                        ExtractionService extractionService,
                                        MasterListService masterListService,
                                        ProcurementExpiryAlertService alertService,
                                        ProcurementPackageService packageService,
                                        RetentionService retentionService,
                                        LegacyMigrationService migrationService,
                                        PackageAccessService accessService) {
        this.budgetService = budgetService;
        this.expiryService = expiryService;
        this.linkageService = linkageService;
        this.extractionService = extractionService;
        this.masterListService = masterListService;
        this.alertService = alertService;
        this.packageService = packageService;
        this.retentionService = retentionService;
        this.migrationService = migrationService;
        this.accessService = accessService;
    }

    // ------------------------------------------------------------------ budget

    @GetMapping("/packages/{packageId}/budget")
    public ResponseEntity<Map<String, Object>> budget(@PathVariable Long packageId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", budgetService.summary(packageId));
        result.put("entries", budgetService.entries(packageId));
        result.put("consumption", budgetService.consumption(packageId));
        // The annual departmental budget this package draws down (Q-13, REQ-B0)
        budgetService.departmentBudgetFor(packageId).ifPresent(db -> {
            result.put("departmentBudget", db);
            result.put("departmentPosition",
                    budgetService.departmentPosition(db.getFiscalYear(), db.getDepartment()));
        });
        return ResponseEntity.ok(result);
    }

    /** The department's annual budget position - allocated, committed, remaining (REQ-B0). */
    @GetMapping("/department-budgets")
    public ResponseEntity<BudgetService.DepartmentBudgetPosition> departmentBudget(
            @RequestParam Integer fiscalYear, @RequestParam String department) {
        // The department comes from the caller's identity when scoping is on, not from
        // the query string - otherwise anyone reads any department by asking (REQ-P20)
        return ResponseEntity.ok(budgetService.departmentPosition(
                fiscalYear, accessService.scopeDepartment(department)));
    }

    /**
     * Set a department's annual budget. Approval is a permission check on the caller, not
     * a routed workflow (Q-13) - anyone holding BUDGET_APPROVE may do this.
     */
    @PostMapping("/department-budgets")
    public ResponseEntity<?> saveDepartmentBudget(@RequestBody DepartmentBudgetRequest request) {
        try {
            return ResponseEntity.ok(budgetService.saveDepartmentBudget(
                    request.fiscalYear, request.department, request.allocatedAmount,
                    request.currency, request.notes, CurrentUser.id()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
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

    // -------------------------------------------------------------- master lists

    /**
     * The permitted values for Procurement Type, Method and Nature (Q-8, REQ-2.4), so the
     * Stage 2 form can offer them rather than leaving the user to guess the spelling.
     */
    @GetMapping("/master-lists")
    public ResponseEntity<Map<String, ?>> masterLists() {
        return ResponseEntity.ok(masterListService.allLists());
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

    /**
     * The expiry dashboard as a file (REQ-E7).
     *
     * <p>CSV, which every spreadsheet opens and which survives being mailed around a
     * ministry. A PDF would look better and be harder to work with; the people who ask for
     * this export are the ones who then want to sort and filter it.
     */
    @GetMapping("/expiries/export")
    public ResponseEntity<byte[]> exportExpiries(
            @RequestParam(defaultValue = "365") int withinDays) {
        List<ExpiryTracking> rows = expiryService.expiringWithin(withinDays);

        StringBuilder csv = new StringBuilder();
        csv.append("Package,Instrument,Expiry Date,Days Remaining,Status,Department,Notes\n");
        LocalDate today = LocalDate.now();
        for (ExpiryTracking row : rows) {
            LocalDate expiry = row.getExpiryDate() == null ? null : row.getExpiryDate().toLocalDate();
            long days = expiry == null ? 0 : java.time.temporal.ChronoUnit.DAYS.between(today, expiry);
            csv.append(csvCell(packageNumberOf(row))).append(',')
               .append(csvCell(row.getEntityType())).append(',')
               .append(csvCell(expiry == null ? "" : expiry.toString())).append(',')
               .append(expiry == null ? "" : String.valueOf(days)).append(',')
               .append(csvCell(row.getStatus() == null ? "" : row.getStatus().name())).append(',')
               .append(csvCell(row.getDepartment())).append(',')
               .append(csvCell(row.getNotes())).append('\n');
        }

        byte[] body = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition",
                        "attachment; filename=\"procurement-expiries-" + today + ".csv\"")
                .body(body);
    }

    /**
     * Run the expiry warning pass now (REQ-E2/E3).
     *
     * <p>The scheduler is opt-in — see SchedulingConfig for why — so this is how the
     * warnings are exercised in an environment where it is off, and how they are tested.
     */
    @PostMapping("/expiries/run-warnings")
    public ResponseEntity<Map<String, Object>> runExpiryWarnings() {
        int sent = alertService.run(LocalDate.now());
        return ResponseEntity.ok(Map.of("notificationsSent", sent));
    }

    private String packageNumberOf(ExpiryTracking row) {
        return row.getPackageId() == null ? "" : packageService.findById(row.getPackageId())
                .map(p -> p.getPackageNumber()).orElse("");
    }

    /** Quote anything containing a comma, quote or newline, per RFC 4180. */
    private static String csvCell(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
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

    // ---------------------------------------------------------------- migration

    /**
     * Phase 7 migration. Dry run by default — run it, read the report, then run it again
     * with {@code dryRun=false}. The steps are independent and safe to repeat.
     *
     * @param step one of {@code metadata}, {@code app-lines}, {@code relink}
     */
    @PostMapping("/migration/{step}")
    public ResponseEntity<?> migrate(@PathVariable String step,
                                     @RequestParam(defaultValue = "true") boolean dryRun,
                                     @RequestParam(defaultValue = "BPDB") String department) {
        Long userId = CurrentUser.id();
        try {
            return ResponseEntity.ok(switch (step) {
                case "metadata" -> migrationService.migrateDocumentMetadata(dryRun, userId);
                case "app-lines" -> migrationService.backfillPackagesFromAppLines(
                        dryRun, department, userId);
                case "relink" -> migrationService.relinkOrphanDocuments(dryRun, userId);
                default -> throw new IllegalArgumentException(
                        "Unknown migration step '" + step + "'. Expected metadata, app-lines or relink.");
            });
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ---------------------------------------------------------------- retention

    /**
     * Retention purge (REQ-P17, Q-18).
     *
     * <p>Defaults to a dry run: you get the counts and have to come back with
     * {@code dryRun=false} to remove anything. Never scheduled — the client's one-year
     * answer is shorter than the warranty on many of these contracts, so nothing here
     * happens on a clock.
     */
    @PostMapping("/retention/purge")
    public ResponseEntity<?> purge(@RequestParam(defaultValue = "true") boolean dryRun) {
        try {
            return ResponseEntity.ok(retentionService.purge(dryRun, CurrentUser.id()));
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

    public static class DepartmentBudgetRequest {
        public Integer fiscalYear;
        public String department;
        public BigDecimal allocatedAmount;
        public String currency;
        public String notes;
    }
}

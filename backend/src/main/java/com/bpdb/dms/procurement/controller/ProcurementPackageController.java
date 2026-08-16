package com.bpdb.dms.procurement.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bpdb.dms.procurement.entity.ProcurementPackage;
import com.bpdb.dms.procurement.service.AppPackageImportService;
import com.bpdb.dms.procurement.service.ProcurementPackageService;
import com.bpdb.dms.procurement.service.StageEngine;

/**
 * Packages: the entry point to the lifecycle.
 */
@RestController
@RequestMapping("/api/procurement/packages")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProcurementPackageController {

    private final ProcurementPackageService packageService;
    private final AppPackageImportService appImportService;

    public ProcurementPackageController(ProcurementPackageService packageService,
                                        AppPackageImportService appImportService) {
        this.packageService = packageService;
        this.appImportService = appImportService;
    }

    /**
     * Stage 1: create packages in bulk from an APP workbook (REQ-1.1).
     *
     * <p>Run it with {@code dryRun=true} first on a file nobody has imported before - the
     * report is identical but nothing is written, so a workbook with surprises in it can
     * be inspected before it lands.
     */
    @PostMapping(value = "/import-app", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importApp(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file supplied"));
        }
        try (InputStream in = file.getInputStream()) {
            AppPackageImportService.ImportReport report =
                    appImportService.importWorkbook(in, department, CurrentUser.id(), dryRun);
            return ResponseEntity.ok(report);
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Could not read the workbook: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<Page<ProcurementPackage>> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Short stage,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(packageService.search(query, stage, status, department, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcurementPackage> get(@PathVariable Long id) {
        return packageService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-number/{packageNumber}")
    public ResponseEntity<ProcurementPackage> getByNumber(@PathVariable String packageNumber) {
        return packageService.findByNumber(packageNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ProcurementPackage pkg) {
        try {
            return ResponseEntity.ok(packageService.create(pkg, CurrentUser.id()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Create one or more packages from an imported APP line; lots optional (Q-1). */
    @PostMapping("/from-app-line/{appLineId}")
    public ResponseEntity<?> createFromAppLine(@PathVariable Long appLineId,
                                               @RequestBody(required = false) List<String> lotNumbers) {
        try {
            return ResponseEntity.ok(
                    packageService.createFromAppLine(appLineId, lotNumbers, CurrentUser.id()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Stage rail data: status and blocking reasons for all 16 stages. */
    @GetMapping("/{id}/progress")
    public ResponseEntity<List<StageEngine.StageReadiness>> progress(@PathVariable Long id) {
        return ResponseEntity.ok(packageService.progress(id));
    }

    /** The full linked graph, APP down to closure (REQ-L8). */
    @GetMapping("/{id}/graph")
    public ResponseEntity<Map<String, Object>> graph(@PathVariable Long id) {
        return ResponseEntity.ok(packageService.graph(id));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(packageService.dashboard());
    }
}

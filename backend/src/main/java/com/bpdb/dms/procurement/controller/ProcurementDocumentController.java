package com.bpdb.dms.procurement.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bpdb.dms.procurement.entity.DocumentLink;
import com.bpdb.dms.procurement.entity.OcrPage;
import com.bpdb.dms.procurement.service.ExtractionService;
import com.bpdb.dms.procurement.service.LinkageService;
import com.bpdb.dms.procurement.service.ProcurementUploadService;

/**
 * Uploading lifecycle documents and reading back what OCR made of them.
 */
@RestController
@RequestMapping("/api/procurement/documents")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProcurementDocumentController {

    private final ProcurementUploadService uploadService;
    private final ExtractionService extractionService;
    private final LinkageService linkageService;

    public ProcurementDocumentController(ProcurementUploadService uploadService,
                                         ExtractionService extractionService,
                                         LinkageService linkageService) {
        this.uploadService = uploadService;
        this.extractionService = extractionService;
        this.linkageService = linkageService;
    }

    /**
     * Upload against a stage. docRole must be one of the roles the stage expects, so
     * the required-document checklist can tick itself off.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam Long packageId,
                                    @RequestParam short stageCode,
                                    @RequestParam String docRole) {
        try {
            return ResponseEntity.ok(
                    uploadService.upload(file, packageId, stageCode, docRole, CurrentUser.id()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    /** OCR text for a document, with its pages - the source pane of the verify screen. */
    @GetMapping("/{documentId}/ocr")
    public ResponseEntity<?> ocr(@PathVariable Long documentId) {
        return extractionService.currentResult(documentId)
                .<ResponseEntity<?>>map(result -> {
                    List<OcrPage> pages = extractionService.pages(result.getId());
                    return ResponseEntity.ok(Map.of("result", result, "pages", pages));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-package/{packageId}")
    public ResponseEntity<List<DocumentLink>> byPackage(@PathVariable Long packageId,
                                                        @RequestParam(required = false) Short stage) {
        return ResponseEntity.ok(stage == null
                ? linkageService.documentsFor(packageId)
                : linkageService.documentsForStage(packageId, stage));
    }
}

package com.bpdb.dms.procurement.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bpdb.dms.procurement.entity.ExtractedField;
import com.bpdb.dms.procurement.entity.ExtractedFieldHistory;
import com.bpdb.dms.procurement.repository.ExtractedFieldRepository;
import com.bpdb.dms.procurement.service.CaptureService;

/**
 * The verify screen's API: confirm, correct or reject what OCR read, and see the
 * history of every change (requirements section 3.5).
 */
@RestController
@RequestMapping("/api/procurement/fields")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CaptureController {

    private final CaptureService captureService;
    private final ExtractedFieldRepository fieldRepository;

    public CaptureController(CaptureService captureService, ExtractedFieldRepository fieldRepository) {
        this.captureService = captureService;
        this.fieldRepository = fieldRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExtractedField> get(@PathVariable Long id) {
        return fieldRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-package/{packageId}")
    public ResponseEntity<List<ExtractedField>> byPackage(@PathVariable Long packageId,
                                                          @RequestParam(required = false) Short stage) {
        return ResponseEntity.ok(stage == null
                ? fieldRepository.findByPackageId(packageId)
                : fieldRepository.findByPackageIdAndStageCode(packageId, stage));
    }

    @GetMapping("/by-document/{documentId}")
    public ResponseEntity<List<ExtractedField>> byDocument(@PathVariable Long documentId) {
        return ResponseEntity.ok(fieldRepository.findByDocumentId(documentId));
    }

    /** Accept the OCR value as read. */
    @PutMapping("/{id}/verify")
    public ResponseEntity<?> verify(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(captureService.verify(id, CurrentUser.id()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Correct the value. The raw OCR text is preserved (REQ-P5). */
    @PutMapping("/{id}/override")
    public ResponseEntity<?> override(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(captureService.override(
                    id, body.get("value"), CurrentUser.id(), body.get("reason")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(captureService.reject(id, CurrentUser.id(), body.get("reason")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/bulk-verify")
    public ResponseEntity<Map<String, Object>> bulkVerify(@RequestBody Map<String, Object> body) {
        Long packageId = Long.valueOf(String.valueOf(body.get("packageId")));
        Short stageCode = Short.valueOf(String.valueOf(body.get("stageCode")));
        int verified = captureService.verifyAll(packageId, stageCode, CurrentUser.id());
        return ResponseEntity.ok(Map.of("verified", verified));
    }

    /** Every change to this value, newest first (REQ-P5). */
    @GetMapping("/{id}/history")
    public ResponseEntity<List<ExtractedFieldHistory>> history(@PathVariable Long id) {
        return ResponseEntity.ok(captureService.history(id));
    }
}

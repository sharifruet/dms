package com.bpdb.dms.procurement.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bpdb.dms.entity.DocumentTypeField;
import com.bpdb.dms.procurement.entity.BerBidder;
import com.bpdb.dms.procurement.entity.Delivery;
import com.bpdb.dms.procurement.entity.ExtractedField;
import com.bpdb.dms.procurement.entity.InspectionEvent;
import com.bpdb.dms.procurement.entity.Invoice;
import com.bpdb.dms.procurement.entity.PackageStage;
import com.bpdb.dms.procurement.entity.Payment;
import com.bpdb.dms.procurement.entity.Tender;
import com.bpdb.dms.procurement.repository.ExtractedFieldRepository;
import com.bpdb.dms.procurement.service.LinkageService;
import com.bpdb.dms.procurement.service.ProcurementRecordService;
import com.bpdb.dms.procurement.service.StageDataService;
import com.bpdb.dms.procurement.service.StageDefinitionService;
import com.bpdb.dms.procurement.service.StageEngine;
import com.bpdb.dms.procurement.service.TenderService;
import com.bpdb.dms.procurement.service.ValidationService;

/**
 * Stage workspace: what a stage needs, what has been captured, and moving it on.
 */
@RestController
@RequestMapping("/api/procurement/packages/{packageId}/stages")
@CrossOrigin(origins = "*", maxAge = 3600)
public class StageController {

    private final StageEngine stageEngine;
    private final StageDefinitionService definitions;
    private final StageDataService stageDataService;
    private final ProcurementRecordService recordService;
    private final LinkageService linkageService;
    private final ValidationService validationService;
    private final TenderService tenderService;
    private final ExtractedFieldRepository fieldRepository;

    public StageController(StageEngine stageEngine,
                           StageDefinitionService definitions,
                           StageDataService stageDataService,
                           ProcurementRecordService recordService,
                           LinkageService linkageService,
                           ValidationService validationService,
                           TenderService tenderService,
                           ExtractedFieldRepository fieldRepository) {
        this.stageEngine = stageEngine;
        this.definitions = definitions;
        this.stageDataService = stageDataService;
        this.recordService = recordService;
        this.linkageService = linkageService;
        this.validationService = validationService;
        this.tenderService = tenderService;
        this.fieldRepository = fieldRepository;
    }

    @GetMapping
    public ResponseEntity<List<PackageStage>> stages(@PathVariable Long packageId) {
        return ResponseEntity.ok(stageEngine.stagesOf(packageId));
    }

    /**
     * Everything the stage panel renders: required documents and which are present,
     * the field catalogue with captured values, warnings, and the gate status.
     */
    @GetMapping("/{stageCode}")
    public ResponseEntity<Map<String, Object>> stageDetail(@PathVariable Long packageId,
                                                           @PathVariable short stageCode) {
        if (!definitions.isValidStage(stageCode)) {
            return ResponseEntity.badRequest().build();
        }
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("stageCode", stageCode);
        detail.put("stageName", definitions.stageName(stageCode));
        detail.put("stage", stageEngine.stage(packageId, stageCode));
        detail.put("readiness", stageEngine.readiness(packageId, stageCode));
        detail.put("requiredDocuments", definitions.requiredDocuments(stageCode));
        detail.put("documents", linkageService.documentsForStage(packageId, stageCode));

        List<DocumentTypeField> catalogue = definitions.catalogueFields(stageCode);
        detail.put("catalogue", catalogue);
        List<ExtractedField> captured = fieldRepository.findByPackageIdAndStageCode(packageId, stageCode);
        detail.put("fields", captured);
        detail.put("warnings", validationService.warningsForStage(packageId, stageCode));

        // repeating rows, where the stage has them
        switch (stageCode) {
            case 4 -> detail.put("bidders", recordService.bidders(packageId));
            case 11 -> detail.put("inspections", recordService.inspections(packageId));
            case 12 -> {
                detail.put("deliveries", recordService.deliveries(packageId));
                detail.put("cumulativeDelivered", recordService.cumulativeDelivered(packageId));
            }
            case 13 -> {
                detail.put("invoices", recordService.invoices(packageId));
                detail.put("deliveries", recordService.deliveries(packageId));
            }
            case 14 -> {
                detail.put("payments", recordService.payments(packageId));
                detail.put("invoices", recordService.invoices(packageId));
            }
            default -> { /* single-row stage */ }
        }
        return ResponseEntity.ok(detail);
    }

    /** Save the stage form. Values are keyed by catalogue field key. */
    @PutMapping("/{stageCode}/fields")
    public ResponseEntity<?> saveFields(@PathVariable Long packageId,
                                        @PathVariable short stageCode,
                                        @RequestBody Map<String, String> values) {
        try {
            return ResponseEntity.ok(
                    stageDataService.saveStageValues(packageId, stageCode, values, CurrentUser.id()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{stageCode}/complete")
    public ResponseEntity<?> complete(@PathVariable Long packageId,
                                      @PathVariable short stageCode,
                                      @RequestBody(required = false) Map<String, String> body) {
        String override = body == null ? null : body.get("overrideReason");
        try {
            return ResponseEntity.ok(
                    stageEngine.complete(packageId, stageCode, CurrentUser.id(), override));
        } catch (StageEngine.StageNotReadyException e) {
            // 409: the request was well-formed, the stage simply is not ready yet
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "Stage is not ready",
                    "readiness", e.getReadiness()));
        }
    }

    @PostMapping("/{stageCode}/not-applicable")
    public ResponseEntity<?> notApplicable(@PathVariable Long packageId,
                                           @PathVariable short stageCode,
                                           @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(stageEngine.markNotApplicable(
                    packageId, stageCode, CurrentUser.id(), body.get("reason")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{stageCode}/rework")
    public ResponseEntity<?> rework(@PathVariable Long packageId,
                                    @PathVariable short stageCode,
                                    @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(stageEngine.rework(
                    packageId, stageCode, CurrentUser.id(), body.get("reason")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ------------------------------------------------------------- re-tendering

    /**
     * Declare the current tender failed and open a fresh attempt under the same package
     * (Q-2, REQ-L14). The failed attempt keeps its documents, bidders and BER.
     */
    @PostMapping("/2/re-tender")
    public ResponseEntity<?> reTender(@PathVariable Long packageId,
                                      @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(
                    tenderService.reTender(packageId, body.get("reason"), CurrentUser.id()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Every tender attempt for the package, newest first - the re-tender history. */
    @GetMapping("/2/attempts")
    public ResponseEntity<List<Tender>> tenderAttempts(@PathVariable Long packageId) {
        return ResponseEntity.ok(tenderService.history(packageId));
    }

    // ----------------------------------------------------------- repeating rows

    @PutMapping("/4/bidders")
    public ResponseEntity<?> saveBidders(@PathVariable Long packageId,
                                         @RequestBody List<BerBidder> bidders) {
        return ResponseEntity.ok(recordService.saveBidders(packageId, bidders));
    }

    @PostMapping("/11/inspections")
    public ResponseEntity<?> saveInspection(@PathVariable Long packageId,
                                            @RequestBody InspectionEvent event) {
        try {
            return ResponseEntity.ok(recordService.saveInspection(packageId, event));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/12/deliveries")
    public ResponseEntity<?> saveDelivery(@PathVariable Long packageId,
                                          @RequestBody Delivery delivery) {
        try {
            return ResponseEntity.ok(recordService.saveDelivery(packageId, delivery));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Declare a delivery final, closing the delivery set (REQ-12.5).
     *
     * <p>Its own route rather than a flag on the delivery form, because it is an approval:
     * it decides the goods are all in and unblocks the stage. Q-11 left who decides
     * unstated, so it follows the Checker role of Q-17, enforced in SecurityConfig.
     */
    @PostMapping("/12/deliveries/{deliveryId}/final")
    public ResponseEntity<?> declareDeliveryFinal(@PathVariable Long packageId,
                                                  @PathVariable Long deliveryId) {
        try {
            return ResponseEntity.ok(
                    recordService.declareFinal(packageId, deliveryId, CurrentUser.id()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/12/deliveries/{deliveryId}/reopen")
    public ResponseEntity<?> reopenDeliveries(@PathVariable Long packageId,
                                              @PathVariable Long deliveryId,
                                              @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(recordService.reopenDeliveries(
                    packageId, deliveryId, CurrentUser.id(), body.get("reason")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/13/invoices")
    public ResponseEntity<?> saveInvoice(@PathVariable Long packageId,
                                         @RequestBody InvoiceRequest request) {
        try {
            return ResponseEntity.ok(
                    recordService.saveInvoice(packageId, request.invoice, request.deliveryIds));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/14/payments")
    public ResponseEntity<?> savePayment(@PathVariable Long packageId,
                                         @RequestBody PaymentRequest request) {
        try {
            return ResponseEntity.ok(
                    recordService.savePayment(packageId, request.payment, request.invoiceIds));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** An invoice plus the deliveries it bills (REQ-L7). */
    public static class InvoiceRequest {
        public Invoice invoice;
        public List<Long> deliveryIds;
    }

    /** A payment plus the invoices it settles (REQ-L7). */
    public static class PaymentRequest {
        public Payment payment;
        public List<Long> invoiceIds;
    }
}

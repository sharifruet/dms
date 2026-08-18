package com.bpdb.dms.controller;

import com.bpdb.dms.dto.CreateBillRequest;
import com.bpdb.dms.entity.BillHeader;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.BillHeaderRepository;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.BillService;
import com.bpdb.dms.service.FinanceReportService;
import com.bpdb.dms.service.FinanceDashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/finance")
@CrossOrigin(origins = "*")
public class FinanceController {

    private static final Logger logger = LoggerFactory.getLogger(FinanceController.class);

    @Autowired
    private BillService billService;


    @Autowired
    private FinanceReportService financeReportService;

    @Autowired
    private FinanceDashboardService financeDashboardService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BillHeaderRepository billHeaderRepository;

    @GetMapping(path = "/bills")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<?> getBills(@RequestParam(required = false) Integer fiscalYear,
                                      @RequestParam(required = false) String vendor,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "100") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<BillHeader> billsPage;
            
            if (fiscalYear != null) {
                List<BillHeader> allBills = billHeaderRepository.findAll();
                List<BillHeader> filtered = allBills.stream()
                    .filter(b -> b.getFiscalYear() != null && b.getFiscalYear().equals(fiscalYear))
                    .filter(b -> vendor == null || (b.getVendor() != null && b.getVendor().contains(vendor)))
                    .collect(java.util.stream.Collectors.toList());
                
                // Manual pagination for filtered results
                int start = page * size;
                int end = Math.min(start + size, filtered.size());
                List<BillHeader> paginatedBills = start < filtered.size() ? filtered.subList(start, end) : new java.util.ArrayList<>();
                
                billsPage = new PageImpl<>(
                    paginatedBills,
                    pageable,
                    filtered.size()
                );
            } else {
                // For all bills, filter manually
                List<BillHeader> allBills = billHeaderRepository.findAll();
                List<BillHeader> filtered = allBills.stream()
                    .filter(b -> vendor == null || (b.getVendor() != null && b.getVendor().contains(vendor)))
                    .collect(java.util.stream.Collectors.toList());
                
                int start = page * size;
                int end = Math.min(start + size, filtered.size());
                List<BillHeader> paginatedBills = start < filtered.size() ? filtered.subList(start, end) : new java.util.ArrayList<>();
                
                billsPage = new PageImpl<>(
                    paginatedBills,
                    pageable,
                    filtered.size()
                );
            }
            
            // Clear lines from response to avoid circular reference and reduce payload size
            // Lines will be loaded when viewing individual bill details
            billsPage.getContent().forEach(bill -> bill.setLines(new java.util.ArrayList<>()));
            
            return ResponseEntity.ok(billsPage);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(path = "/bills/{id}")
    public ResponseEntity<?> getBill(@PathVariable Long id) {
        return billHeaderRepository.findById(id)
            .map(bill -> {
                // Eagerly load lines to ensure they're serialized
                bill.getLines().size(); // Trigger lazy loading
                return ResponseEntity.ok(bill);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // APP import moved to the procurement Stage 1 path
    // (POST /api/procurement/packages/import-app), which creates packages from the workbook
    // instead of a standalone finance-shaped AppHeader.

    @PostMapping(path = "/bills")
    public ResponseEntity<?> createBill(@AuthenticationPrincipal UserDetails principal,
                                        @RequestBody CreateBillRequest request) {
        User user = principal == null ? null : userRepository.findByUsernameWithRole(principal.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        Long id = billService.createBill(request, user);
        return ResponseEntity.ok(id);
    }

    @GetMapping(path = "/reports/app-vs-bills")
    public ResponseEntity<?> appVsBills(@RequestParam Integer year,
                                        @RequestParam(required = false) String department,
                                        @RequestParam(required = false) String projectIdentifier) {
        if (year == null) return ResponseEntity.badRequest().body("year is required");
        return ResponseEntity.ok(financeReportService.appVsBillsByYear(year, department, projectIdentifier));
    }

    @GetMapping(path = "/dashboard/summary")
    public ResponseEntity<?> dashboardSummary(@RequestParam Integer year,
                                              @RequestParam(required = false) String department) {
        if (year == null) return ResponseEntity.badRequest().body("year is required");
        return ResponseEntity.ok(financeDashboardService.summary(year, department));
    }

    @GetMapping(path = "/dashboard/series")
    public ResponseEntity<?> dashboardSeries(@RequestParam Integer year,
                                             @RequestParam(required = false) String department) {
        if (year == null) return ResponseEntity.badRequest().body("year is required");
        return ResponseEntity.ok(financeDashboardService.series(year, department));
    }

    // /dashboard/budget-summary, /dashboard/budget-by-app and the /app-entries CRUD are gone:
    // the first two derived "billed" through the retired workflow->folder->bills hop (Q-16), and
    // APP entry creation is now package creation in procurement Stage 1. Bills, the app-vs-bills
    // report and the year/department dashboard below are untouched - finance stays independent (Q-6).

    // Bill OCR endpoints (Phase 3 - OCR-based bill upload)
    @PostMapping(path = "/bills/extract-ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<?> extractBillOCR(@AuthenticationPrincipal UserDetails principal,
                                            @RequestPart("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "File is required"));
            }

            // Validate file type (image or PDF only)
            String contentType = file.getContentType();
            if (contentType == null || 
                (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false, 
                    "error", "Only image files (JPEG, PNG, TIFF) or PDF files are allowed"
                ));
            }

            com.bpdb.dms.dto.BillOCRResult ocrResult = billService.extractBillFromFile(file);
            return ResponseEntity.ok(Map.of("success", true, "ocrResult", ocrResult));
        } catch (Exception e) {
            logger.error("Failed to extract bill OCR", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

}



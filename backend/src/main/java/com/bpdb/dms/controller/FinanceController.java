package com.bpdb.dms.controller;

import com.bpdb.dms.dto.CreateBillRequest;
import com.bpdb.dms.entity.AppHeader;
import com.bpdb.dms.entity.BillHeader;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.BillHeaderRepository;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.AppExcelImportService;
import com.bpdb.dms.service.AppEntryService;
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
    private AppExcelImportService appExcelImportService;

    @Autowired
    private AppEntryService appEntryService;

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

    @PostMapping(path = "/app/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importApp(@AuthenticationPrincipal UserDetails principal,
                                       @RequestPart("file") MultipartFile file) {
        User user = principal == null ? null : userRepository.findByUsernameWithRole(principal.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        if (file == null || file.isEmpty()) return ResponseEntity.badRequest().body("File is required");
        AppHeader header = appExcelImportService.importApp(file, user);
        return ResponseEntity.ok(header.getId());
    }

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

    @GetMapping(path = "/dashboard/budget-summary")
    public ResponseEntity<?> getBudgetSummary() {
        return ResponseEntity.ok(financeDashboardService.getBudgetSummary());
    }

    /**
     * Per-APP budget vs billed summary for dashboard.
     */
    @GetMapping(path = "/dashboard/budget-by-app")
    public ResponseEntity<?> getBudgetByApp() {
        return ResponseEntity.ok(financeDashboardService.getBudgetByApp());
    }

    // APP Entry endpoints (Phase 3 - Manual Entry)
    @PostMapping(path = "/app-entries", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<?> createAppEntry(@AuthenticationPrincipal UserDetails principal,
                                            @RequestPart("fiscalYear") String fiscalYearStr,
                                            @RequestPart("allocationType") String allocationType,
                                            @RequestPart("budgetReleaseDate") String budgetReleaseDateStr,
                                            @RequestPart("allocationAmount") String allocationAmountStr,
                                            @RequestPart("releaseInstallmentNo") String releaseInstallmentNoStr,
                                            @RequestPart(value = "referenceMemoNumber", required = false) String referenceMemoNumber,
                                            @RequestPart(value = "department", required = false) String department,
                                            @RequestPart(value = "attachment", required = false) MultipartFile attachment) {
        try {
            User user = userRepository.findByUsernameWithRole(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

            AppEntryService.CreateAppEntryRequest request = new AppEntryService.CreateAppEntryRequest();
            request.setFiscalYear(Integer.parseInt(fiscalYearStr));
            request.setAllocationType(allocationType);
            request.setBudgetReleaseDate(java.time.LocalDate.parse(budgetReleaseDateStr));
            request.setAllocationAmount(new java.math.BigDecimal(allocationAmountStr));
            request.setReleaseInstallmentNo(Integer.parseInt(releaseInstallmentNoStr));
            request.setReferenceMemoNumber(referenceMemoNumber);
            request.setDepartment(department);
            request.setAttachment(attachment);

            AppHeader header = appEntryService.createAppEntry(request, user);
            return ResponseEntity.ok(Map.of("success", true, "id", header.getId(), "message", "APP entry created successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping(path = "/app-entries")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<?> getAppEntries(@RequestParam(required = false) Integer fiscalYear) {
        try {
            List<AppHeader> entries;
            if (fiscalYear != null) {
                entries = appEntryService.getAppEntriesByFiscalYear(fiscalYear);
            } else {
                entries = appEntryService.getAllAppEntries();
            }
            return ResponseEntity.ok(Map.of("success", true, "entries", entries));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping(path = "/app-entries/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<?> getAppEntry(@PathVariable Long id) {
        return appEntryService.getAppEntryById(id)
            .map(entry -> ResponseEntity.ok(Map.of("success", true, "entry", entry)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(path = "/app-entries/next-installment")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<?> getNextInstallmentNo(@RequestParam Integer fiscalYear) {
        try {
            Integer nextInstallment = appEntryService.getNextInstallmentNo(fiscalYear);
            return ResponseEntity.ok(Map.of("success", true, "nextInstallmentNo", nextInstallment));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping(path = "/app-entries/fiscal-years")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<?> getFiscalYears() {
        try {
            List<Integer> fiscalYears = appEntryService.getDistinctFiscalYears();
            return ResponseEntity.ok(Map.of("success", true, "fiscalYears", fiscalYears));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping(path = "/app-entries/check-duplicate")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<?> checkDuplicate(@RequestParam Integer fiscalYear,
                                           @RequestParam Integer installmentNo) {
        try {
            boolean isDuplicate = appEntryService.isDuplicate(fiscalYear, installmentNo);
            return ResponseEntity.ok(Map.of("success", true, "isDuplicate", isDuplicate));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

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



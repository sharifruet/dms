package com.bpdb.dms.controller;

import com.bpdb.dms.dto.CreateBillRequest;
import com.bpdb.dms.entity.AppHeader;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.AppExcelImportService;
import com.bpdb.dms.service.BillService;
import com.bpdb.dms.service.FinanceReportService;
import com.bpdb.dms.service.FinanceDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/finance")
@CrossOrigin(origins = "*")
public class FinanceController {

    @Autowired
    private AppExcelImportService appExcelImportService;

    @Autowired
    private BillService billService;

    @Autowired
    private FinanceReportService financeReportService;

    @Autowired
    private FinanceDashboardService financeDashboardService;

    @Autowired
    private UserRepository userRepository;

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
}



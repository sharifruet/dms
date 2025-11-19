package com.bpdb.dms.service;

import com.bpdb.dms.entity.AppHeader;
import com.bpdb.dms.entity.AppLine;
import com.bpdb.dms.entity.BillHeader;
import com.bpdb.dms.entity.BillLine;
import com.bpdb.dms.repository.AppHeaderRepository;
import com.bpdb.dms.repository.AppLineRepository;
import com.bpdb.dms.repository.BillHeaderRepository;
import com.bpdb.dms.repository.BillLineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FinanceReportService {

    @Autowired
    private AppHeaderRepository appHeaderRepository;
    @Autowired
    private AppLineRepository appLineRepository;
    @Autowired
    private BillHeaderRepository billHeaderRepository;
    @Autowired
    private BillLineRepository billLineRepository;

    public List<Map<String, Object>> appVsBillsByYear(Integer fiscalYear, String department, String projectIdentifier) {
        AppHeader header = appHeaderRepository.findByFiscalYear(fiscalYear).orElse(null);
        List<AppLine> appLines = header == null ? List.of() : header.getLines();

        // Filter APP lines by department/project if provided
        appLines = appLines.stream().filter(l ->
            (department == null || department.isBlank() || department.equals(l.getDepartment())) &&
            (projectIdentifier == null || projectIdentifier.isBlank() || projectIdentifier.equals(l.getProjectIdentifier()))
        ).collect(Collectors.toList());

        // Fetch all bill lines for the year
        List<BillLine> billLines = billLineRepository.findAll().stream()
            .filter(bl -> bl.getHeader() != null && bl.getHeader().getFiscalYear() != null
                && bl.getHeader().getFiscalYear().equals(fiscalYear))
            .collect(Collectors.toList());

        // Aggregate bills per APP line or projectIdentifier
        Map<Long, BigDecimal> appLineActuals = new HashMap<>();
        Map<String, BigDecimal> projectActuals = new HashMap<>();
        for (BillLine bl : billLines) {
            BigDecimal total = safe(bl.getAmount()).add(safe(bl.getTaxAmount()));
            if (bl.getAppLine() != null) {
                appLineActuals.merge(bl.getAppLine().getId(), total, BigDecimal::add);
            } else if (bl.getProjectIdentifier() != null) {
                projectActuals.merge(bl.getProjectIdentifier(), total, BigDecimal::add);
            }
        }

        // Build rows
        return appLines.stream().map(al -> {
            BigDecimal budget = safe(al.getBudgetAmount());
            BigDecimal actual = appLineActuals.getOrDefault(al.getId(),
                projectActuals.getOrDefault(al.getProjectIdentifier(), BigDecimal.ZERO));
            BigDecimal remaining = budget.subtract(actual);
            BigDecimal utilization = budget.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                actual.multiply(BigDecimal.valueOf(100)).divide(budget, 2, java.math.RoundingMode.HALF_UP);
            Map<String, Object> row = new HashMap<>();
            row.put("fiscalYear", fiscalYear);
            row.put("projectIdentifier", al.getProjectIdentifier());
            row.put("projectName", al.getProjectName());
            row.put("department", al.getDepartment());
            row.put("costCenter", al.getCostCenter());
            row.put("category", al.getCategory());
            row.put("budget", budget);
            row.put("actual", actual);
            row.put("remaining", remaining);
            row.put("utilizationPct", utilization);
            row.put("appLineId", al.getId());
            return row;
        }).collect(Collectors.toList());
    }

    private BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}



package com.bpdb.dms.service;

import com.bpdb.dms.dto.AppBudgetSummaryDto;
import com.bpdb.dms.entity.AppHeader;
import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.Folder;
import com.bpdb.dms.entity.Workflow;
import com.bpdb.dms.repository.AppHeaderRepository;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.WorkflowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FinanceDashboardService {

    @Autowired
    private FinanceReportService financeReportService;

    @Autowired
    private AppHeaderRepository appHeaderRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentMetadataService documentMetadataService;

    public Map<String, Object> summary(Integer year, String department) {
        List<Map<String, Object>> rows = financeReportService.appVsBillsByYear(year, department, null);
        BigDecimal budget = BigDecimal.ZERO;
        BigDecimal actual = BigDecimal.ZERO;
        for (Map<String, Object> r : rows) {
            budget = budget.add((BigDecimal) r.getOrDefault("budget", BigDecimal.ZERO));
            actual = actual.add((BigDecimal) r.getOrDefault("actual", BigDecimal.ZERO));
        }
        BigDecimal remaining = budget.subtract(actual);
        BigDecimal utilization = budget.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
            : actual.multiply(BigDecimal.valueOf(100)).divide(budget, 2, java.math.RoundingMode.HALF_UP);
        Map<String, Object> result = new HashMap<>();
        result.put("year", year);
        result.put("department", department);
        result.put("budget", budget);
        result.put("actual", actual);
        result.put("remaining", remaining);
        result.put("utilizationPct", utilization);
        return result;
    }

    public Map<String, Object> series(Integer year, String department) {
        // For now, derive series by category totals to display stacked columns
        List<Map<String, Object>> rows = financeReportService.appVsBillsByYear(year, department, null);
        Map<String, BigDecimal> budgetByCategory = new HashMap<>();
        Map<String, BigDecimal> actualByCategory = new HashMap<>();
        for (Map<String, Object> r : rows) {
            String category = (String) r.getOrDefault("category", "Uncategorized");
            BigDecimal budget = (BigDecimal) r.getOrDefault("budget", BigDecimal.ZERO);
            BigDecimal actual = (BigDecimal) r.getOrDefault("actual", BigDecimal.ZERO);
            budgetByCategory.merge(category, budget, BigDecimal::add);
            actualByCategory.merge(category, actual, BigDecimal::add);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("year", year);
        result.put("department", department);
        result.put("budgetByCategory", budgetByCategory);
        result.put("actualByCategory", actualByCategory);
        return result;
    }

    /**
     * Legacy aggregate summary across all APP entries.
     * Kept for compatibility but now delegates to per-APP summaries.
     */
    public Map<String, Object> getBudgetSummary() {
        java.util.List<AppBudgetSummaryDto> perApp = getBudgetByApp();

        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalBilled = BigDecimal.ZERO;
        for (AppBudgetSummaryDto dto : perApp) {
            if (dto.getAllocationAmount() != null) {
                totalBudget = totalBudget.add(dto.getAllocationAmount());
            }
            if (dto.getTotalBilled() != null) {
                totalBilled = totalBilled.add(dto.getTotalBilled());
            }
        }

        BigDecimal remaining = totalBudget.subtract(totalBilled);
        BigDecimal utilizationPct = totalBudget.compareTo(BigDecimal.ZERO) == 0
            ? BigDecimal.ZERO
            : totalBilled.multiply(BigDecimal.valueOf(100))
                .divide(totalBudget, 2, java.math.RoundingMode.HALF_UP);

        Map<String, Object> result = new HashMap<>();
        result.put("totalBudget", totalBudget);
        result.put("totalBilled", totalBilled);
        result.put("remaining", remaining);
        result.put("utilizationPct", utilizationPct);
        return result;
    }

    /**
     * Get per-APP budget vs billed summary.
     * Budget = allocation_amount from AppHeader.
     * Billed = sum of bill amounts from BILL documents in workflows linked to that APP.
     */
    public java.util.List<AppBudgetSummaryDto> getBudgetByApp() {
        // Load all workflows that are linked to an APP entry and group by APP ID
        java.util.List<Workflow> workflowsWithApp = workflowRepository.findWithAppEntry();
        Map<Long, java.util.List<Workflow>> workflowsByAppId = new HashMap<>();
        for (Workflow workflow : workflowsWithApp) {
            if (workflow.getAppEntry() == null) {
                continue;
            }
            Long appId = workflow.getAppEntry().getId();
            workflowsByAppId.computeIfAbsent(appId, k -> new java.util.ArrayList<>()).add(workflow);
        }

        java.util.List<AppHeader> appHeaders = appHeaderRepository.findAll();
        java.util.List<AppBudgetSummaryDto> result = new java.util.ArrayList<>();

        for (AppHeader appHeader : appHeaders) {
            Long appId = appHeader.getId();

            BigDecimal allocation = appHeader.getAllocationAmount() != null
                ? appHeader.getAllocationAmount()
                : BigDecimal.ZERO;

            BigDecimal totalBilled = BigDecimal.ZERO;

            java.util.List<Workflow> workflows = workflowsByAppId.get(appId);
            if (workflows != null && !workflows.isEmpty()) {
                for (Workflow workflow : workflows) {
                    Folder folder = workflow.getFolder();
                    if (folder == null) {
                        continue;
                    }

                    java.util.List<Document> bills = documentRepository.findBillDocumentsByFolder(folder);
                    for (Document billDoc : bills) {
                        BigDecimal billAmount = extractBillAmountFromMetadata(billDoc);
                        totalBilled = totalBilled.add(billAmount);
                    }
                }
            }

            BigDecimal remaining = allocation.subtract(totalBilled);
            BigDecimal utilizationPct = allocation.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalBilled.multiply(BigDecimal.valueOf(100))
                    .divide(allocation, 2, java.math.RoundingMode.HALF_UP);

            AppBudgetSummaryDto dto = new AppBudgetSummaryDto();
            dto.setAppId(appId);
            dto.setFiscalYear(appHeader.getFiscalYear());
            dto.setReleaseInstallmentNo(appHeader.getReleaseInstallmentNo());
            dto.setAllocationType(appHeader.getAllocationType());
            dto.setAllocationAmount(allocation);
            dto.setTotalBilled(totalBilled);
            dto.setRemaining(remaining);
            dto.setUtilizationPct(utilizationPct);

            result.add(dto);
        }

        return result;
    }

    /**
     * Extract bill amount from document metadata.
     * Prefer netAmount, then totalAmount, otherwise 0.
     */
    private BigDecimal extractBillAmountFromMetadata(Document document) {
        try {
            Map<String, String> metadata = documentMetadataService.getMetadataMap(document);
            String netAmountStr = metadata.get("netAmount");
            String totalAmountStr = metadata.get("totalAmount");

            if (netAmountStr != null && !netAmountStr.isBlank()) {
                return new BigDecimal(netAmountStr.trim());
            }
            if (totalAmountStr != null && !totalAmountStr.isBlank()) {
                return new BigDecimal(totalAmountStr.trim());
            }
        } catch (Exception ignored) {
            // Fallback to zero if anything goes wrong
        }
        return BigDecimal.ZERO;
    }
}



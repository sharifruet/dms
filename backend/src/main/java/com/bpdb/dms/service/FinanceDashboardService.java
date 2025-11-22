package com.bpdb.dms.service;

import com.bpdb.dms.repository.AppLineRepository;
import com.bpdb.dms.repository.BillLineRepository;
import com.bpdb.dms.service.FinanceReportService;
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
    private AppLineRepository appLineRepository;

    @Autowired
    private BillLineRepository billLineRepository;

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
     * Get budget and billed amounts summary
     * Budget = sum of estimated_cost_lakh from app_lines * 100000
     * Billed = sum of amounts from bill_lines
     */
    public Map<String, Object> getBudgetSummary() {
        // Calculate total budget: sum of estimated_cost_lakh * 100000
        BigDecimal totalBudget = appLineRepository.findAll().stream()
            .map(line -> {
                BigDecimal costLakh = line.getEstimatedCostLakh();
                if (costLakh == null) {
                    return BigDecimal.ZERO;
                }
                return costLakh.multiply(BigDecimal.valueOf(100000));
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate total billed: sum of amounts from all bill lines
        BigDecimal totalBilled = billLineRepository.findAll().stream()
            .map(line -> {
                BigDecimal amount = line.getAmount() == null ? BigDecimal.ZERO : line.getAmount();
                BigDecimal taxAmount = line.getTaxAmount() == null ? BigDecimal.ZERO : line.getTaxAmount();
                return amount.add(taxAmount);
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);

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
}



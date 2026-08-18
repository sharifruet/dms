package com.bpdb.dms.service;

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

    // The per-APP budget-vs-billed aggregation that used to live here is gone. It derived
    // "billed" by walking workflow -> folder -> bill documents, and that linkage retired with
    // the generic workflow engine (Q-16). Procurement budget tracking is BudgetService's job
    // (REQ-B6); the year/department report below is unaffected and still serves finance (Q-6).

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

}

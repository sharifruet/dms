package com.bpdb.dms.service;

import com.bpdb.dms.entity.AppHeader;
import com.bpdb.dms.entity.AppLine;
import com.bpdb.dms.entity.BillHeader;
import com.bpdb.dms.entity.BillLine;
import com.bpdb.dms.repository.AppHeaderRepository;
import com.bpdb.dms.repository.AppLineRepository;
import com.bpdb.dms.repository.BillHeaderRepository;
import com.bpdb.dms.repository.BillLineRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class FinanceReportServiceTest {

    @Test
    void appVsBillsByYear_calculatesRemainingAndUtilization() {
        AppHeaderRepository appHeaderRepository = Mockito.mock(AppHeaderRepository.class);
        AppLineRepository appLineRepository = Mockito.mock(AppLineRepository.class);
        BillHeaderRepository billHeaderRepository = Mockito.mock(BillHeaderRepository.class);
        BillLineRepository billLineRepository = Mockito.mock(BillLineRepository.class);

        FinanceReportService svc = new FinanceReportService();
        inject(svc, "appHeaderRepository", appHeaderRepository);
        inject(svc, "appLineRepository", appLineRepository);
        inject(svc, "billHeaderRepository", billHeaderRepository);
        inject(svc, "billLineRepository", billLineRepository);

        AppHeader header = new AppHeader();
        header.setFiscalYear(2025);
        AppLine line = new AppLine();
        line.setId(10L);
        line.setProjectIdentifier("P1");
        line.setProjectName("Proj");
        line.setDepartment("Dept");
        line.setCostCenter("CC");
        line.setCategory("Cat");
        line.setBudgetAmount(new BigDecimal("1000.00"));
        header.setLines(List.of(line));
        Mockito.when(appHeaderRepository.findByFiscalYear(2025)).thenReturn(Optional.of(header));

        BillLine bl = new BillLine();
        BillHeader bh = new BillHeader();
        bh.setFiscalYear(2025);
        bl.setHeader(bh);
        bl.setAppLine(line);
        bl.setAmount(new BigDecimal("250.00"));
        bl.setTaxAmount(new BigDecimal("50.00"));
        Mockito.when(billLineRepository.findAll()).thenReturn(List.of(bl));

        List<Map<String, Object>> rows = svc.appVsBillsByYear(2025, null, null);
        Assertions.assertEquals(1, rows.size());
        Map<String, Object> r = rows.get(0);
        Assertions.assertEquals(new BigDecimal("1000.00"), r.get("budget"));
        Assertions.assertEquals(new BigDecimal("300.00"), r.get("actual"));
        Assertions.assertEquals(new BigDecimal("700.00"), r.get("remaining"));
        Assertions.assertEquals(new BigDecimal("30.00"), r.get("utilizationPct"));
    }

    private static void inject(Object target, String field, Object value) {
        try {
            var f = FinanceReportService.class.getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}



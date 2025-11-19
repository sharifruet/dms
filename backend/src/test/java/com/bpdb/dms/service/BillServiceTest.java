package com.bpdb.dms.service;

import com.bpdb.dms.dto.BillLineRequest;
import com.bpdb.dms.dto.CreateBillRequest;
import com.bpdb.dms.entity.AppHeader;
import com.bpdb.dms.entity.AppLine;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.AppLineRepository;
import com.bpdb.dms.repository.BillHeaderRepository;
import com.bpdb.dms.repository.BillLineRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

class BillServiceTest {

    @Test
    void createBill_throwsOnYearMismatch() {
        BillHeaderRepository billHeaderRepository = Mockito.mock(BillHeaderRepository.class);
        BillLineRepository billLineRepository = Mockito.mock(BillLineRepository.class);
        AppLineRepository appLineRepository = Mockito.mock(AppLineRepository.class);
        BillService billService = new BillService();
        inject(billService, "billHeaderRepository", billHeaderRepository);
        inject(billService, "billLineRepository", billLineRepository);
        inject(billService, "appLineRepository", appLineRepository);

        AppHeader appHeader = new AppHeader();
        appHeader.setFiscalYear(2024);
        AppLine appLine = new AppLine();
        appLine.setHeader(appHeader);
        Mockito.when(appLineRepository.findById(1L)).thenReturn(Optional.of(appLine));

        CreateBillRequest req = new CreateBillRequest();
        req.fiscalYear = 2025;
        req.invoiceDate = LocalDate.now();
        BillLineRequest line = new BillLineRequest();
        line.appLineId = 1L;
        req.lines = List.of(line);

        User user = new User();
        user.setDepartment("Dept");
        Assertions.assertThrows(IllegalArgumentException.class, () -> billService.createBill(req, user));
    }

    private static void inject(Object target, String field, Object value) {
        try {
            var f = BillService.class.getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}



package com.bpdb.dms.service;

import com.bpdb.dms.dto.BillLineRequest;
import com.bpdb.dms.dto.CreateBillRequest;
import com.bpdb.dms.entity.*;
import com.bpdb.dms.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class BillService {

    private static final Logger logger = LoggerFactory.getLogger(BillService.class);

    @Autowired
    private BillHeaderRepository billHeaderRepository;
    @Autowired
    private BillLineRepository billLineRepository;
    @Autowired
    private AppLineRepository appLineRepository;

    @Transactional
    public Long createBill(CreateBillRequest request, User user) {
        validateRequest(request);

        BillHeader header = new BillHeader();
        header.setFiscalYear(request.fiscalYear);
        header.setVendor(request.vendor);
        header.setInvoiceNumber(request.invoiceNumber);
        header.setInvoiceDate(request.invoiceDate);
        header.setCreatedBy(user);
        header = billHeaderRepository.save(header);

        List<BillLine> toSave = new ArrayList<>();
        if (request.lines != null) {
            for (BillLineRequest lr : request.lines) {
                BillLine line = new BillLine();
                line.setHeader(header);
                line.setProjectIdentifier(lr.projectIdentifier);
                line.setDepartment(lr.department != null ? lr.department : user.getDepartment());
                line.setCostCenter(lr.costCenter);
                line.setCategory(lr.category);
                line.setAmount(lr.amount);
                line.setTaxAmount(lr.taxAmount);

                if (lr.appLineId != null) {
                    AppLine appLine = appLineRepository.findById(lr.appLineId)
                        .orElseThrow(() -> new IllegalArgumentException("APP line not found: " + lr.appLineId));
                    // Validate year consistency
                    if (appLine.getHeader() == null || appLine.getHeader().getFiscalYear() == null
                        || !appLine.getHeader().getFiscalYear().equals(request.fiscalYear)) {
                        throw new IllegalArgumentException("Bill fiscal year must match APP line header fiscal year");
                    }
                    line.setAppLine(appLine);
                    // default project identifiers from APP line if missing
                    if (line.getProjectIdentifier() == null) line.setProjectIdentifier(appLine.getProjectIdentifier());
                    if (line.getDepartment() == null) line.setDepartment(appLine.getDepartment());
                    if (line.getCostCenter() == null) line.setCostCenter(appLine.getCostCenter());
                    if (line.getCategory() == null) line.setCategory(appLine.getCategory());
                }
                toSave.add(line);
            }
        }
        billLineRepository.saveAll(toSave);
        logger.info("Created bill id={} lines={}", header.getId(), toSave.size());
        return header.getId();
    }

    private void validateRequest(CreateBillRequest request) {
        if (request == null) throw new IllegalArgumentException("Request is required");
        if (request.fiscalYear == null) throw new IllegalArgumentException("fiscalYear is required");
        if (request.invoiceDate == null) throw new IllegalArgumentException("invoiceDate is required");
        if (request.lines == null || request.lines.isEmpty()) throw new IllegalArgumentException("At least one line is required");
    }
}



package com.bpdb.dms.dto;

import java.time.LocalDate;
import java.util.List;

public class CreateBillRequest {
    public Integer fiscalYear;
    public String vendor;
    public String invoiceNumber;
    public LocalDate invoiceDate;
    public List<BillLineRequest> lines;
}



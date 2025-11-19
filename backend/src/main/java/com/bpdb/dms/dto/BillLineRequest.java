package com.bpdb.dms.dto;

import java.math.BigDecimal;

public class BillLineRequest {
    public Long appLineId; // optional
    public String projectIdentifier;
    public String department;
    public String costCenter;
    public String category;
    public BigDecimal amount;
    public BigDecimal taxAmount;
}



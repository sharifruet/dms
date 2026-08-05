package com.bpdb.dms.procurement.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bpdb.dms.procurement.entity.BudgetConsumption;
import com.bpdb.dms.procurement.entity.BudgetEntry;
import com.bpdb.dms.procurement.entity.Invoice;
import com.bpdb.dms.procurement.repository.BudgetConsumptionRepository;
import com.bpdb.dms.procurement.repository.BudgetEntryRepository;
import com.bpdb.dms.procurement.repository.ContractPackageRepository;

/**
 * Budget module (requirements section 5).
 *
 * Allocation, release, revision and additional budget are entered by hand. Consumption
 * is never typed - it is posted automatically from verified invoice amounts (REQ-B5),
 * and Remaining is always derived, never stored as an editable figure (REQ-P15).
 */
@Service
public class BudgetService {

    private static final Logger log = LoggerFactory.getLogger(BudgetService.class);

    public static final String ALLOCATION = "ALLOCATION";
    public static final String RELEASE = "RELEASE";
    public static final String REVISION = "REVISION";
    public static final String ADDITIONAL = "ADDITIONAL";

    /** Warn when remaining budget drops below this share of the total (REQ-B8). */
    private static final BigDecimal LOW_BUDGET_RATIO = new BigDecimal("0.10");

    private final BudgetEntryRepository entryRepository;
    private final BudgetConsumptionRepository consumptionRepository;
    private final ContractPackageRepository contractPackageRepository;

    public BudgetService(BudgetEntryRepository entryRepository,
                         BudgetConsumptionRepository consumptionRepository,
                         ContractPackageRepository contractPackageRepository) {
        this.entryRepository = entryRepository;
        this.consumptionRepository = consumptionRepository;
        this.contractPackageRepository = contractPackageRepository;
    }

    @Transactional
    public BudgetEntry addEntry(Long packageId, String entryType, BigDecimal amount,
                                String currency, java.time.LocalDate effectiveDate,
                                String reason, Long userId) {
        if (REVISION.equals(entryType) && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("A revision must record a reason (REQ-B3)");
        }
        BudgetEntry entry = new BudgetEntry();
        entry.setPackageId(packageId);
        entry.setEntryType(entryType);
        entry.setAmount(amount);
        entry.setCurrency(currency == null ? "BDT" : currency);
        entry.setEffectiveDate(effectiveDate);
        entry.setReason(reason);
        entry.setCreatedBy(userId);
        BudgetEntry saved = entryRepository.save(entry);

        if (RELEASE.equals(entryType)) {
            BudgetSummary summary = summary(packageId);
            if (summary.totalRelease.compareTo(summary.totalAvailable) > 0) {
                log.warn("Package {} cumulative release {} exceeds available budget {} (REQ-B2)",
                        packageId, summary.totalRelease, summary.totalAvailable);
            }
        }
        return saved;
    }

    /**
     * Post consumption for a verified invoice. Idempotent per (invoice, package) so a
     * re-verified invoice does not double-count.
     *
     * A contract may span several packages (client answer Q-1), so the invoice amount is
     * apportioned across them by the allocation percentages on contract_package, falling
     * back to an even split when none are set.
     */
    @Transactional
    public List<BudgetConsumption> postConsumption(Invoice invoice) {
        var links = contractPackageRepository.findByContractId(invoice.getContractId());
        if (links.isEmpty()) {
            return List.of();
        }
        BigDecimal total = invoice.getInvoiceAmount() == null ? BigDecimal.ZERO : invoice.getInvoiceAmount();
        BigDecimal explicitPct = links.stream()
                .map(l -> l.getAllocationPct() == null ? BigDecimal.ZERO : l.getAllocationPct())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean useExplicit = explicitPct.compareTo(BigDecimal.ZERO) > 0;

        List<BudgetConsumption> posted = new java.util.ArrayList<>();
        for (var link : links) {
            BigDecimal share;
            if (useExplicit) {
                BigDecimal pct = link.getAllocationPct() == null ? BigDecimal.ZERO : link.getAllocationPct();
                share = total.multiply(pct).divide(explicitPct, 2, java.math.RoundingMode.HALF_UP);
            } else {
                share = total.divide(BigDecimal.valueOf(links.size()), 2, java.math.RoundingMode.HALF_UP);
            }

            BudgetConsumption existing = consumptionRepository.findByInvoiceId(invoice.getId()).stream()
                    .filter(c -> c.getPackageId().equals(link.getPackageId()))
                    .findFirst().orElse(null);
            BudgetConsumption consumption = existing == null ? new BudgetConsumption() : existing;
            consumption.setPackageId(link.getPackageId());
            consumption.setInvoiceId(invoice.getId());
            consumption.setConsumedAmount(share);
            consumption.setCurrency(invoice.getCurrency() == null ? "BDT" : invoice.getCurrency());
            consumption.setPostedAt(LocalDateTime.now());
            posted.add(consumptionRepository.save(consumption));
        }
        return posted;
    }

    public BudgetSummary summary(Long packageId) {
        BudgetSummary s = new BudgetSummary();
        s.packageId = packageId;
        for (BudgetEntry e : entryRepository.findByPackageId(packageId)) {
            BigDecimal amount = e.getAmount() == null ? BigDecimal.ZERO : e.getAmount();
            switch (e.getEntryType()) {
                case ALLOCATION -> s.totalAllocation = s.totalAllocation.add(amount);
                case RELEASE -> s.totalRelease = s.totalRelease.add(amount);
                case REVISION -> s.totalRevision = s.totalRevision.add(amount);
                case ADDITIONAL -> s.totalAdditional = s.totalAdditional.add(amount);
                default -> { /* unknown type, ignore */ }
            }
        }
        s.totalConsumption = consumptionRepository.findByPackageId(packageId).stream()
                .map(c -> c.getConsumedAmount() == null ? BigDecimal.ZERO : c.getConsumedAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Allocation +/- revisions + additional, less what invoices have consumed (REQ-B6)
        s.totalAvailable = s.totalAllocation.add(s.totalRevision).add(s.totalAdditional);
        s.remaining = s.totalAvailable.subtract(s.totalConsumption);
        s.lowBudget = s.totalAvailable.signum() > 0
                && s.remaining.compareTo(s.totalAvailable.multiply(LOW_BUDGET_RATIO)) < 0;
        s.overspent = s.remaining.signum() < 0;
        return s;
    }

    public List<BudgetEntry> entries(Long packageId) {
        return entryRepository.findByPackageId(packageId);
    }

    public List<BudgetConsumption> consumption(Long packageId) {
        return consumptionRepository.findByPackageId(packageId);
    }

    /** Derived budget position. Remaining is computed here and nowhere else. */
    public static class BudgetSummary {
        public Long packageId;
        public BigDecimal totalAllocation = BigDecimal.ZERO;
        public BigDecimal totalRelease = BigDecimal.ZERO;
        public BigDecimal totalRevision = BigDecimal.ZERO;
        public BigDecimal totalAdditional = BigDecimal.ZERO;
        public BigDecimal totalConsumption = BigDecimal.ZERO;
        public BigDecimal totalAvailable = BigDecimal.ZERO;
        public BigDecimal remaining = BigDecimal.ZERO;
        public boolean lowBudget;
        public boolean overspent;
    }
}

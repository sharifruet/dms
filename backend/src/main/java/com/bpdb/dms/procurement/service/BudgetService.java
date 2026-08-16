package com.bpdb.dms.procurement.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bpdb.dms.procurement.entity.BudgetConsumption;
import com.bpdb.dms.procurement.entity.BudgetEntry;
import com.bpdb.dms.procurement.entity.DepartmentBudget;
import com.bpdb.dms.procurement.entity.Invoice;
import com.bpdb.dms.procurement.repository.BudgetConsumptionRepository;
import com.bpdb.dms.procurement.repository.BudgetEntryRepository;
import com.bpdb.dms.procurement.repository.ContractPackageRepository;
import com.bpdb.dms.procurement.repository.DepartmentBudgetRepository;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;

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
    private final DepartmentBudgetRepository departmentBudgetRepository;
    private final ProcurementPackageRepository packageRepository;
    private final ProcurementAuditService auditService;

    public BudgetService(BudgetEntryRepository entryRepository,
                         BudgetConsumptionRepository consumptionRepository,
                         ContractPackageRepository contractPackageRepository,
                         DepartmentBudgetRepository departmentBudgetRepository,
                         ProcurementPackageRepository packageRepository,
                         ProcurementAuditService auditService) {
        this.entryRepository = entryRepository;
        this.consumptionRepository = consumptionRepository;
        this.contractPackageRepository = contractPackageRepository;
        this.departmentBudgetRepository = departmentBudgetRepository;
        this.packageRepository = packageRepository;
        this.auditService = auditService;
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
        // An allocation draws down the department's annual budget for the package's
        // fiscal year (Q-13). Packages with no departmental budget on file are allowed
        // through - the drawdown is reported, not gated, until the client loads them.
        if (ALLOCATION.equals(entryType)) {
            departmentBudgetFor(packageId).ifPresent(db -> entry.setDepartmentBudgetId(db.getId()));
        }
        entry.setAmount(amount);
        entry.setCurrency(currency == null ? "BDT" : currency);
        entry.setEffectiveDate(effectiveDate);
        entry.setReason(reason);
        entry.setCreatedBy(userId);
        BudgetEntry saved = entryRepository.save(entry);
        // Money moving is exactly what an audit asks about (REQ-X6)
        auditService.budgetEntryAdded(userId, packageId, entryType, amount, reason);

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

    // ------------------------------------------ Q-13: annual departmental budget

    /**
     * The departmental budget a package draws from - matched on its fiscal year and
     * department. Empty when the client has not loaded that year's figure yet.
     */
    public Optional<DepartmentBudget> departmentBudgetFor(Long packageId) {
        return packageRepository.findById(packageId)
                .filter(p -> p.getFiscalYear() != null && p.getDepartment() != null)
                .flatMap(p -> departmentBudgetRepository
                        .findByFiscalYearAndDepartment(p.getFiscalYear(), p.getDepartment()));
    }

    @Transactional
    public DepartmentBudget saveDepartmentBudget(Integer fiscalYear, String department,
                                                 BigDecimal amount, String currency,
                                                 String notes, Long approverId) {
        DepartmentBudget budget = departmentBudgetRepository
                .findByFiscalYearAndDepartment(fiscalYear, department)
                .orElseGet(DepartmentBudget::new);
        BigDecimal previousAmount = budget.getAllocatedAmount();
        budget.setFiscalYear(fiscalYear);
        budget.setDepartment(department);
        budget.setAllocatedAmount(amount);
        budget.setCurrency(currency == null ? "BDT" : currency);
        budget.setNotes(notes);
        // Approval is a permission check on the caller, not a routed workflow (Q-13)
        budget.setApprovedBy(approverId);
        budget.setApprovedAt(LocalDateTime.now());
        DepartmentBudget savedBudget = departmentBudgetRepository.save(budget);
        auditService.departmentBudgetSet(approverId, savedBudget.getId(), department, fiscalYear,
                previousAmount, amount);
        return savedBudget;
    }

    /**
     * How much of a departmental annual budget its packages have committed (REQ-B0).
     *
     * Sums the ALLOCATION lines drawn against it. Over-commitment is reported rather than
     * refused - the department's figure is a planning control, not a payment gate, and the
     * hard money ceilings live at the contract (REQ-13.3).
     */
    public DepartmentBudgetPosition departmentPosition(Integer fiscalYear, String department) {
        DepartmentBudgetPosition position = new DepartmentBudgetPosition();
        position.fiscalYear = fiscalYear;
        position.department = department;

        DepartmentBudget budget = departmentBudgetRepository
                .findByFiscalYearAndDepartment(fiscalYear, department).orElse(null);
        if (budget == null) {
            return position;
        }
        position.departmentBudgetId = budget.getId();
        position.allocated = budget.getAllocatedAmount() == null
                ? BigDecimal.ZERO : budget.getAllocatedAmount();
        position.currency = budget.getCurrency();
        position.committed = entryRepository.findByDepartmentBudgetId(budget.getId()).stream()
                .filter(e -> ALLOCATION.equals(e.getEntryType()))
                .map(e -> e.getAmount() == null ? BigDecimal.ZERO : e.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        position.remaining = position.allocated.subtract(position.committed);
        position.overCommitted = position.remaining.signum() < 0;
        if (position.overCommitted) {
            log.warn("Department {} FY{} has committed {} against an annual budget of {} (REQ-B0)",
                    department, fiscalYear, position.committed, position.allocated);
        }
        return position;
    }

    public List<BudgetEntry> entries(Long packageId) {
        return entryRepository.findByPackageId(packageId);
    }

    public List<BudgetConsumption> consumption(Long packageId) {
        return consumptionRepository.findByPackageId(packageId);
    }

    /** How much of a department's annual budget its packages have drawn down (REQ-B0). */
    public static class DepartmentBudgetPosition {
        public Integer fiscalYear;
        public String department;
        public Long departmentBudgetId;
        public String currency = "BDT";
        public BigDecimal allocated = BigDecimal.ZERO;
        public BigDecimal committed = BigDecimal.ZERO;
        public BigDecimal remaining = BigDecimal.ZERO;
        public boolean overCommitted;
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

package com.bpdb.dms.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.bpdb.dms.entity.ExpiryStatus;
import com.bpdb.dms.entity.ExpiryTracking;
import com.bpdb.dms.procurement.entity.BudgetConsumption;
import com.bpdb.dms.procurement.entity.BudgetEntry;
import com.bpdb.dms.procurement.entity.DepartmentBudget;
import com.bpdb.dms.procurement.entity.OcrJob;
import com.bpdb.dms.procurement.entity.PackageStage;
import com.bpdb.dms.procurement.entity.ProcurementPackage;
import com.bpdb.dms.procurement.entity.Tender;
import com.bpdb.dms.procurement.repository.BudgetConsumptionRepository;
import com.bpdb.dms.procurement.repository.BudgetEntryRepository;
import com.bpdb.dms.procurement.repository.DepartmentBudgetRepository;
import com.bpdb.dms.procurement.repository.OcrJobRepository;
import com.bpdb.dms.procurement.repository.PackageStageRepository;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;
import com.bpdb.dms.procurement.repository.TenderRepository;
import com.bpdb.dms.procurement.service.BudgetService;
import com.bpdb.dms.procurement.service.DeadlineAlertService;
import com.bpdb.dms.procurement.service.ExecutiveDashboardService;
import com.bpdb.dms.procurement.service.ProcurementExpiryService;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.ExpiryTrackingRepository;

/**
 * What the executive tiles actually count.
 *
 * <p>Every figure on that page is a definition — "a contract" is a package past the
 * signing gate, not a row in the contract table; "expiring in 15 days" nests inside
 * "expiring in 30". Those definitions are invisible in the rendered page and easy to
 * change by accident while editing something nearby, so they are pinned here.
 *
 * <p>No database: the point is the arithmetic over lifecycle records, and the records are
 * cheaper to state directly than to seed.
 */
class ExecutiveDashboardServiceTest {

    private static final int FY = 2025;

    @Mock private ProcurementPackageRepository packageRepository;
    @Mock private PackageStageRepository stageRepository;
    @Mock private TenderRepository tenderRepository;
    @Mock private BudgetEntryRepository budgetEntryRepository;
    @Mock private BudgetConsumptionRepository consumptionRepository;
    @Mock private DepartmentBudgetRepository departmentBudgetRepository;
    @Mock private ExpiryTrackingRepository expiryRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private OcrJobRepository ocrJobRepository;
    @Mock private DeadlineAlertService deadlineAlertService;

    private ExecutiveDashboardService service;
    private AutoCloseable mocks;

    private final List<ProcurementPackage> packages = new ArrayList<>();
    private final List<PackageStage> stages = new ArrayList<>();
    private final List<Tender> tenders = new ArrayList<>();
    private final List<ExpiryTracking> expiries = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        // Elasticsearch is absent here, which is also how the stack runs without it
        service = new ExecutiveDashboardService(packageRepository, stageRepository, tenderRepository,
                budgetEntryRepository, consumptionRepository, departmentBudgetRepository,
                expiryRepository, documentRepository, ocrJobRepository, deadlineAlertService, null);

        when(packageRepository.findAll()).thenReturn(packages);
        when(stageRepository.findAll()).thenReturn(stages);
        when(tenderRepository.findAll()).thenReturn(tenders);
        when(expiryRepository.findAll()).thenReturn(expiries);
        when(budgetEntryRepository.findAll()).thenReturn(List.of());
        when(consumptionRepository.findAll()).thenReturn(List.of());
        when(departmentBudgetRepository.findAll()).thenReturn(List.of());
        when(ocrJobRepository.findAll()).thenReturn(List.of());
        when(deadlineAlertService.openDeadlines(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        when(documentRepository.countByIsActiveTrue()).thenReturn(0L);
        when(documentRepository.countByCreatedAtAfter(org.mockito.ArgumentMatchers.any()))
                .thenReturn(0L);
        when(documentRepository.countArchivedDocuments()).thenReturn(0L);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    // ------------------------------------------------------------------ helpers

    private ProcurementPackage pkg(long id, String status, Integer fiscalYear, short... satisfied) {
        ProcurementPackage p = new ProcurementPackage();
        p.setId(id);
        p.setPackageNumber("PKG-" + id);
        p.setStatus(status);
        p.setFiscalYear(fiscalYear);
        packages.add(p);
        for (short code : satisfied) {
            PackageStage s = new PackageStage();
            s.setPackageId(id);
            s.setStageCode(code);
            s.setStatus(PackageStage.COMPLETED);
            stages.add(s);
        }
        return p;
    }

    private ExpiryTracking expiry(String entityType, ExpiryStatus status, Integer daysFromNow) {
        ExpiryTracking t = new ExpiryTracking();
        t.setEntityType(entityType);
        t.setStatus(status);
        if (daysFromNow != null) {
            t.setExpiryDate(LocalDate.now().plusDays(daysFromNow).atStartOfDay());
        }
        expiries.add(t);
        return t;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> section(Map<String, Object> snapshot, String key) {
        return (Map<String, Object>) snapshot.get(key);
    }

    @SuppressWarnings("unchecked")
    private long bucket(Map<String, Object> snapshot, String section, String label) {
        for (Map<String, Object> entry : (List<Map<String, Object>>) snapshot.get(section)) {
            if (label.equals(entry.get("label"))) {
                return ((Number) entry.get("value")).longValue();
            }
        }
        return -1;
    }

    // -------------------------------------------------------------------- tests

    @Test
    void aContractIsCountedFromTheSigningGateNotFromAContractRow() {
        // StageDataService creates a DRAFT contract row as soon as anyone types into a
        // Stage 8 form, so counting rows would report unsigned contracts as signed
        pkg(1, "ACTIVE", FY, (short) 8);            // signed, running
        pkg(2, "ACTIVE", FY, (short) 6);            // awarded, nothing signed yet
        pkg(3, "CLOSED", FY, (short) 8, (short) 16); // signed and closed

        Map<String, Object> kpis = section(service.snapshot(FY), "kpis");
        assertEquals(2L, kpis.get("totalContracts"));
        assertEquals(1L, kpis.get("runningContracts"));
        assertEquals(1L, kpis.get("completedContracts"));
    }

    @Test
    void aNotApplicableStageStillCountsAsPassed() {
        // A non-LC contract must not be reported as never having reached delivery (REQ-9.5)
        ProcurementPackage p = pkg(1, "ACTIVE", FY);
        PackageStage signing = new PackageStage();
        signing.setPackageId(p.getId());
        signing.setStageCode((short) 8);
        signing.setStatus(PackageStage.NOT_STARTED);
        signing.setIsApplicable(Boolean.FALSE);
        stages.add(signing);

        assertEquals(1L, section(service.snapshot(FY), "kpis").get("totalContracts"));
    }

    @Test
    void aTenderIsLiveOnceAdvertisedAndUntilEvaluationCloses() {
        pkg(1, "ACTIVE", FY, (short) 2);             // advertised
        pkg(2, "ACTIVE", FY, (short) 2, (short) 3);  // opened, still under evaluation
        pkg(3, "ACTIVE", FY, (short) 2, (short) 3, (short) 4); // evaluated: no longer live
        pkg(4, "ACTIVE", FY);                        // never advertised

        assertEquals(2L, section(service.snapshot(FY), "kpis").get("liveTenders"));
    }

    @Test
    void tenderBucketsAreExclusiveSoTheDonutReadsAsAWhole() {
        pkg(1, "ACTIVE", FY, (short) 2);                          // Live Tender
        pkg(2, "ACTIVE", FY, (short) 2, (short) 3);               // Under Evaluation
        pkg(3, "ACTIVE", FY, (short) 2, (short) 3, (short) 6);    // Awarded
        pkg(4, "CANCELLED", FY, (short) 2);                       // Cancelled, despite being advertised

        Map<String, Object> snapshot = service.snapshot(FY);
        assertEquals(1L, bucket(snapshot, "tenderStatistics", "Live Tender"));
        assertEquals(1L, bucket(snapshot, "tenderStatistics", "Under Evaluation"));
        assertEquals(1L, bucket(snapshot, "tenderStatistics", "Awarded"));
        assertEquals(1L, bucket(snapshot, "tenderStatistics", "Cancelled"));
    }

    @Test
    void aRetenderedPackageBackAtTheStartIsReportedAsRetendered() {
        // A failed attempt is superseded rather than deleted (Q-2), so a second attempt
        // number is the record that this package went back to market
        pkg(1, "ACTIVE", FY);
        Tender second = new Tender();
        second.setPackageId(1L);
        second.setAttemptNo(2);
        tenders.add(second);

        assertEquals(1L, bucket(service.snapshot(FY), "tenderStatistics", "Retendered"));
    }

    @Test
    void expiryWindowsNestRatherThanSum() {
        expiry(ProcurementExpiryService.PERFORMANCE_SECURITY, ExpiryStatus.ACTIVE, 3);
        expiry(ProcurementExpiryService.PERFORMANCE_SECURITY, ExpiryStatus.ACTIVE, 12);
        expiry(ProcurementExpiryService.PERFORMANCE_SECURITY, ExpiryStatus.ACTIVE, 25);
        expiry(ProcurementExpiryService.PERFORMANCE_SECURITY, ExpiryStatus.ACTIVE, 200);

        Map<String, Object> ps = section(service.snapshot(null), "performanceSecurity");
        assertEquals(4L, ps.get("active"));
        assertEquals(3L, ps.get("expiringIn30Days"));
        assertEquals(2L, ps.get("expiringIn15Days"));
        assertEquals(1L, ps.get("expiringIn7Days"));
    }

    @Test
    void aGuaranteeStillMarkedActivePastItsDateIsReportedAsExpired() {
        // The warning pass has not caught up with it; reporting it as active would hide
        // exactly the row somebody needs to see
        expiry(ProcurementExpiryService.PERFORMANCE_SECURITY, ExpiryStatus.ACTIVE, -5);
        expiry(ProcurementExpiryService.PERFORMANCE_SECURITY, ExpiryStatus.RENEWED, -5);

        Map<String, Object> ps = section(service.snapshot(null), "performanceSecurity");
        assertEquals(0L, ps.get("active"));
        assertEquals(1L, ps.get("expired"));
        assertEquals(1L, ps.get("renewed"));
    }

    @Test
    void remainingBudgetIsWhatWasReleasedLessWhatWasSpent() {
        pkg(1, "ACTIVE", FY);

        DepartmentBudget annual = new DepartmentBudget();
        annual.setFiscalYear(FY);
        annual.setAllocatedAmount(new BigDecimal("52500000000")); // 5,250 crore
        when(departmentBudgetRepository.findAll()).thenReturn(List.of(annual));

        BudgetEntry release = new BudgetEntry();
        release.setPackageId(1L);
        release.setEntryType(BudgetService.RELEASE);
        release.setAmount(new BigDecimal("48200000000")); // 4,820 crore
        when(budgetEntryRepository.findAll()).thenReturn(List.of(release));

        BudgetConsumption spent = new BudgetConsumption();
        spent.setPackageId(1L);
        spent.setConsumedAmount(new BigDecimal("39400000000")); // 3,940 crore
        when(consumptionRepository.findAll()).thenReturn(List.of(spent));

        Map<String, Object> budget = section(service.snapshot(FY), "budget");
        assertEquals(new BigDecimal("5250.00"), budget.get("approvedCrore"));
        assertEquals(new BigDecimal("4820.00"), budget.get("releasedCrore"));
        assertEquals(new BigDecimal("3940.00"), budget.get("expenditureCrore"));
        assertEquals(new BigDecimal("880.00"), budget.get("remainingCrore"));
        assertEquals(new BigDecimal("81.74"), budget.get("utilizationPct"));
    }

    @Test
    void anAllocationEntryDoesNotCountAsReleasedMoney() {
        pkg(1, "ACTIVE", FY);
        BudgetEntry allocation = new BudgetEntry();
        allocation.setPackageId(1L);
        allocation.setEntryType(BudgetService.ALLOCATION);
        allocation.setAmount(new BigDecimal("10000000"));
        when(budgetEntryRepository.findAll()).thenReturn(List.of(allocation));

        assertEquals(BigDecimal.ZERO, section(service.snapshot(FY), "budget").get("released"));
    }

    @Test
    void theFiscalYearFilterScopesPackagesButNotDocuments() {
        pkg(1, "ACTIVE", FY, (short) 8);
        pkg(2, "ACTIVE", FY - 1, (short) 8);
        when(documentRepository.countByIsActiveTrue()).thenReturn(4321L);

        Map<String, Object> snapshot = service.snapshot(FY);
        assertEquals(1L, section(snapshot, "kpis").get("totalContracts"));
        // A document is filed against a package, not a fiscal year
        assertEquals(4321L, section(snapshot, "documents").get("total"));
        assertEquals(List.of(FY, FY - 1), snapshot.get("fiscalYears"));
    }

    @Test
    void theYearOnYearTrendIsNullWhenThereIsNoPriorYearToCompare() {
        // "No comparison" and "no change" are different answers and the tile must not
        // claim the second one
        pkg(1, "CLOSED", FY, (short) 16);

        Map<String, Object> progress = section(service.snapshot(FY), "appProgress");
        assertEquals(new BigDecimal("100.0"), progress.get("completionPct"));
        assertNull(progress.get("completionTrendPct"));
    }

    @Test
    void theYearOnYearTrendComparesCompletionAgainstThePreviousYear() {
        pkg(1, "CLOSED", FY, (short) 16);
        pkg(2, "ACTIVE", FY, (short) 8);
        pkg(3, "ACTIVE", FY - 1, (short) 8); // last year: nothing closed

        Map<String, Object> progress = section(service.snapshot(FY), "appProgress");
        assertEquals(new BigDecimal("50.0"), progress.get("completionPct"));
        assertEquals(new BigDecimal("50.0"), progress.get("completionTrendPct"));
    }

    @Test
    void aPackageCarryingAnOverdueDeadlineIsDelayed() {
        pkg(1, "ACTIVE", FY, (short) 6);
        pkg(2, "ACTIVE", FY, (short) 6);
        when(deadlineAlertService.openDeadlines(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(new DeadlineAlertService.PackageDeadline(
                        1L, "PKG-1", DeadlineAlertService.PG_SUBMISSION, "Performance security",
                        LocalDate.now().minusDays(4), -4, true)));

        assertEquals(1L, section(service.snapshot(FY), "appProgress").get("delayed"));
    }

    @Test
    void reRunningOcrOnOneDocumentDoesNotCountItTwice() {
        // A re-run is a second attempt at the same document (REQ-P10)
        when(ocrJobRepository.findAll()).thenReturn(List.of(
                ocrJob(100L, "SUCCESS"), ocrJob(100L, "SUCCESS"), ocrJob(101L, "SUCCESS"),
                ocrJob(102L, "RUNNING"), ocrJob(103L, "FAILED")));

        Map<String, Object> docs = section(service.snapshot(null), "documents");
        assertEquals(2L, docs.get("ocrProcessed"));
        assertEquals(1L, docs.get("ocrPending"));
        assertEquals(1L, docs.get("ocrFailed"));
    }

    @Test
    void anUnreachableSearchIndexReportsNothingRatherThanFailingThePage() {
        assertNull(section(service.snapshot(null), "documents").get("indexed"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void clearAlertRowsStayInTheListSoTheCheckIsVisible() {
        // An absent row reads as "not computed", which is a different statement from
        // "checked and found clear"
        List<Map<String, Object>> alerts =
                (List<Map<String, Object>>) service.snapshot(null).get("alerts");
        assertEquals(6, alerts.size());
        assertTrue(alerts.stream().allMatch(a -> ((Number) a.get("count")).longValue() == 0L));
    }

    private OcrJob ocrJob(Long documentId, String status) {
        OcrJob job = new OcrJob();
        job.setDocumentId(documentId);
        job.setStatus(status);
        job.setStartedAt(LocalDateTime.now());
        return job;
    }
}

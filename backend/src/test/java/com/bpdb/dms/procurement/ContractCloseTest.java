package com.bpdb.dms.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.bpdb.dms.procurement.entity.PackageStage;
import com.bpdb.dms.procurement.entity.ProcurementPackage;
import com.bpdb.dms.procurement.repository.ContractClosureRepository;
import com.bpdb.dms.procurement.repository.DocumentLinkRepository;
import com.bpdb.dms.procurement.repository.ExtractedFieldRepository;
import com.bpdb.dms.procurement.repository.PackageStageRepository;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;
import com.bpdb.dms.procurement.repository.TenderRepository;
import com.bpdb.dms.procurement.service.BudgetService;
import com.bpdb.dms.procurement.service.ConditionalRequirementService;
import com.bpdb.dms.procurement.service.ProcurementAuditService;
import com.bpdb.dms.procurement.service.ProcurementExpiryService;
import com.bpdb.dms.procurement.service.StageDefinitionService;
import com.bpdb.dms.procurement.service.StageEngine;
import com.bpdb.dms.procurement.service.ValidationService;

/**
 * What happens when the last stage completes.
 *
 * The trackers matter here: an LC, a performance guarantee and a warranty all outlive the
 * stage that captured them, so unless closure releases them a closed package keeps warning
 * about instruments nobody holds any more (REQ-E6, REQ-16.2). The wiring is easy to lose -
 * it was written and left uncalled once already - so it is pinned rather than assumed.
 */
class ContractCloseTest {

    private static final Long PACKAGE_ID = 42L;
    private static final Long USER_ID = 7L;
    private static final short LAST_STAGE = 16;

    @Mock private ProcurementPackageRepository packageRepository;
    @Mock private PackageStageRepository stageRepository;
    @Mock private DocumentLinkRepository documentLinkRepository;
    @Mock private ExtractedFieldRepository fieldRepository;
    @Mock private TenderRepository tenderRepository;
    @Mock private StageDefinitionService definitions;
    @Mock private ValidationService validationService;
    @Mock private ProcurementAuditService auditService;
    @Mock private ProcurementExpiryService expiryService;
    @Mock private ConditionalRequirementService conditionalRequirements;
    @Mock private BudgetService budgetService;
    @Mock private ContractClosureRepository closureRepository;

    private StageEngine engine;
    private ProcurementPackage pkg;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        engine = new StageEngine(packageRepository, stageRepository, documentLinkRepository,
                fieldRepository, tenderRepository, definitions, validationService,
                auditService, expiryService, conditionalRequirements, budgetService,
                closureRepository);

        pkg = new ProcurementPackage();
        pkg.setId(PACKAGE_ID);
        pkg.setStatus("ACTIVE");
        pkg.setCurrentStage(LAST_STAGE);

        // A clean gate: nothing outstanding at any stage, so completion is about what
        // closure does rather than about whether it is allowed
        when(packageRepository.findById(PACKAGE_ID)).thenReturn(Optional.of(pkg));
        when(definitions.stageName(anyShort())).thenReturn("Stage");
        when(definitions.blockingDocuments(anyShort())).thenReturn(List.of());
        when(definitions.mandatoryFields(anyShort())).thenReturn(List.of());
        when(documentLinkRepository.findByPackageIdAndStageCode(anyLong(), any()))
                .thenReturn(List.of());
        when(fieldRepository.findUnconfirmedMandatory(anyLong(), any())).thenReturn(List.of());
        when(fieldRepository.findByPackageIdAndStageCode(anyLong(), any())).thenReturn(List.of());
        when(validationService.validateStage(anyLong(), anyShort())).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void completingTheLastStageClosesThePackageAndItsTrackers() {
        stageIs(LAST_STAGE, PackageStage.IN_PROGRESS);
        stageIs((short) (LAST_STAGE - 1), PackageStage.COMPLETED);
        when(expiryService.closeAllForPackage(PACKAGE_ID)).thenReturn(3);
        when(budgetService.reconcileAtClosure(PACKAGE_ID)).thenReturn(reconciliation());

        engine.complete(PACKAGE_ID, LAST_STAGE, USER_ID, null);

        assertEquals("CLOSED", pkg.getStatus());
        verify(expiryService).closeAllForPackage(PACKAGE_ID);
        // The money is reconciled and reported at closure, not left to be worked out later
        verify(budgetService).reconcileAtClosure(PACKAGE_ID);
    }

    @Test
    void completingAnEarlierStageLeavesTheTrackersAlone() {
        // The LC captured at stage 9 must keep warning while the contract is still running
        short stage = 12;
        stageIs(stage, PackageStage.IN_PROGRESS);
        stageIs((short) (stage - 1), PackageStage.COMPLETED);
        stageIs((short) (stage + 1), PackageStage.NOT_STARTED);

        engine.complete(PACKAGE_ID, stage, USER_ID, null);

        assertEquals("ACTIVE", pkg.getStatus());
        verify(expiryService, never()).closeAllForPackage(anyLong());
    }

    /** Released more than was consumed - the ordinary case at closure (REQ-16.3). */
    private BudgetService.ClosureReconciliation reconciliation() {
        BudgetService.ClosureReconciliation r = new BudgetService.ClosureReconciliation();
        r.packageId = PACKAGE_ID;
        r.released = new java.math.BigDecimal("1000.00");
        r.consumed = new java.math.BigDecimal("900.00");
        r.residual = new java.math.BigDecimal("100.00");
        return r;
    }

    private void stageIs(short code, String status) {
        PackageStage stage = new PackageStage();
        stage.setPackageId(PACKAGE_ID);
        stage.setStageCode(code);
        stage.setStatus(status);
        when(stageRepository.findByPackageIdAndStageCode(PACKAGE_ID, code))
                .thenReturn(Optional.of(stage));
    }
}

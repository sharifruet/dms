package com.bpdb.dms.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.bpdb.dms.procurement.entity.PackageStage;
import com.bpdb.dms.procurement.service.StageEngine;

/**
 * The stage gate rules, exercised without a database.
 *
 * These cover the invariants that make the lifecycle trustworthy: a stage is only
 * satisfied when it is genuinely done, a Not Applicable stage still lets the next one
 * open, and a stage that is not ready refuses to complete while saying why.
 */
class StageEngineTest {

    @Test
    void completedStageIsSatisfied() {
        PackageStage stage = new PackageStage();
        stage.setStatus(PackageStage.COMPLETED);
        assertTrue(stage.isCompleted());
        assertTrue(stage.isSatisfied());
    }

    @Test
    void inProgressStageIsNotSatisfied() {
        PackageStage stage = new PackageStage();
        stage.setStatus(PackageStage.IN_PROGRESS);
        assertFalse(stage.isSatisfied());
    }

    @Test
    void notApplicableStageSatisfiesTheGateWithoutBeingCompleted() {
        // A non-LC contract must not be blocked at stage 9 forever (REQ-9.5)
        PackageStage stage = new PackageStage();
        stage.setStatus(PackageStage.NOT_STARTED);
        stage.setIsApplicable(Boolean.FALSE);
        assertFalse(stage.isCompleted());
        assertTrue(stage.isSatisfied());
    }

    @Test
    void readinessListsEveryOutstandingReason() {
        StageEngine.StageReadiness readiness = new StageEngine.StageReadiness();
        readiness.stageCode = 3;
        readiness.blockers.add("Stage 2 (Tender Advertisement) is not complete");
        readiness.missingDocuments.add("Bid Opening Minutes");
        readiness.unconfirmedFields.add("Number of Bidders");
        readiness.validationErrors.add("Opening Date is after Closing Date (REQ-2.2)");

        List<String> reasons = readiness.allReasons();
        assertEquals(4, reasons.size());
        assertTrue(reasons.contains("Missing document: Bid Opening Minutes"));
        assertTrue(reasons.contains("Unverified field: Number of Bidders"));
    }

    @Test
    void notReadyExceptionCarriesTheReasonsToTheCaller() {
        StageEngine.StageReadiness readiness = new StageEngine.StageReadiness();
        readiness.stageCode = 8;
        readiness.missingDocuments.add("Signed Contract Agreement");

        StageEngine.StageNotReadyException e = assertThrows(
                StageEngine.StageNotReadyException.class,
                () -> {
                    throw new StageEngine.StageNotReadyException(readiness);
                });
        assertTrue(e.getMessage().contains("Signed Contract Agreement"));
        assertEquals(readiness, e.getReadiness());
    }
}

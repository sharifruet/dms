package com.bpdb.dms.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bpdb.dms.procurement.entity.Contract;
import com.bpdb.dms.procurement.entity.DeadlineAlertState;
import com.bpdb.dms.procurement.service.BudgetService;
import com.bpdb.dms.procurement.service.ExtractionService;
import com.bpdb.dms.procurement.service.ProcurementUploadService;
import com.bpdb.dms.procurement.service.ValidationService;
import com.bpdb.dms.service.OCRService;

/**
 * The stage rules added for the requirements that had no implementation: the item
 * baseline, the delivery window, supplier reconciliation, closure arithmetic, source
 * regions and the deadline machinery.
 *
 * <p>Exercised without a database, because each is a decision made on values rather than
 * a query — which is also why each was easy to leave out and hard to notice missing.
 */
class CrossStageRulesTest {

    // ------------------------------------------------- REQ-13.5: supplier vs award

    @Test
    void theSameCompanyWrittenTwoWaysIsTheSameCompany() {
        // An invoice from "ABC Engineering Limited" against an award to "ABC Engineering
        // Ltd." is not a discrepancy, and asking a person to confirm it every month is how
        // a reconciliation check gets ignored
        assertTrue(ValidationService.namesMatch("ABC Engineering Ltd.", "ABC Engineering Limited"));
        assertTrue(ValidationService.namesMatch("abc engineering", "ABC  Engineering"));
        assertTrue(ValidationService.namesMatch("The ABC Company", "ABC"));
    }

    @Test
    void adifferentCompanyIsStillFlagged() {
        assertFalse(ValidationService.namesMatch("ABC Engineering Ltd", "XYZ Engineering Ltd"));
        assertFalse(ValidationService.namesMatch("ABC Engineering", "ABC Electronics"));
    }

    // --------------------------------------------- REQ-16.3: closure reconciliation

    @Test
    void closureReportsWhatWasReleasedButNeverConsumed() {
        BudgetService.ClosureReconciliation r = new BudgetService.ClosureReconciliation();
        r.released = new BigDecimal("1000.00");
        r.consumed = new BigDecimal("750.00");
        r.residual = r.released.subtract(r.consumed);

        assertEquals(new BigDecimal("250.00"), r.residual);
        assertTrue(r.summary().contains("250.00"));
        assertTrue(r.summary().contains("surrendered"),
                "an unconsumed release is money to give back: " + r.summary());
    }

    @Test
    void closureNamesTheShortfallWhenConsumptionRanAhead() {
        // Legitimate - invoices are checked against the contract value, not the release
        // paperwork - but somebody has to see it
        BudgetService.ClosureReconciliation r = new BudgetService.ClosureReconciliation();
        r.released = new BigDecimal("500.00");
        r.consumed = new BigDecimal("800.00");
        r.residual = r.released.subtract(r.consumed);
        r.consumedBeyondRelease = r.residual.signum() < 0;

        assertTrue(r.consumedBeyondRelease);
        assertTrue(r.summary().contains("exceeds release"), r.summary());
    }

    // ------------------------------------------------------- REQ-P6: source regions

    @Test
    void aSingleWordValueIsLocatedOnItsPage() {
        List<OCRService.WordBox> words = List.of(
                word("Package", 1, 10, 10),
                word("GRL-24", 1, 100, 10),
                word("Contract", 2, 10, 10));

        OCRService.WordBox found = ExtractionService.locate("GRL-24", words);
        assertNotNull(found);
        assertEquals(1, found.getPageNo());
        assertEquals("100,10,50,20", found.asBbox());
    }

    @Test
    void aMultiWordValueHighlightsTheWholeThing() {
        // "12 March 2025" should highlight the date, not the "12"
        List<OCRService.WordBox> words = List.of(
                word("Dated", 1, 10, 40),
                word("12", 1, 70, 40),
                word("March", 1, 100, 40),
                word("2025", 1, 160, 40));

        OCRService.WordBox found = ExtractionService.locate("12 March 2025", words);
        assertNotNull(found);
        assertEquals("70,40,140,20", found.asBbox(),
                "the box should span the first word to the last");
    }

    @Test
    void punctuationAndCaseDoNotStopAMatch() {
        List<OCRService.WordBox> words = List.of(word("GRL-24,", 1, 10, 10));
        assertNotNull(ExtractionService.locate("grl-24", words));
    }

    @Test
    void aValueThatCannotBeFoundHasNoBoxRatherThanTheWrongOne() {
        // A highlight in the wrong place invites somebody to confirm the wrong region
        List<OCRService.WordBox> words = List.of(word("Package", 1, 10, 10));
        assertNull(ExtractionService.locate("GRL-24", words));
        assertNull(ExtractionService.locate("anything", List.of()));
        assertNull(ExtractionService.locate(null, words));
    }

    @Test
    void wordsSplitAcrossPagesAreNotJoinedIntoOneBox() {
        List<OCRService.WordBox> words = List.of(
                word("12", 1, 70, 40),
                word("March", 2, 10, 10));
        assertNull(ExtractionService.locate("12 March", words));
    }

    // --------------------------------------------------- REQ-P12: unusable scans

    @Test
    void aScanThatYieldedNothingUsableIsFlaggedForManualEntry() {
        assertFalse(ProcurementUploadService.usableText(null));
        assertFalse(ProcurementUploadService.usableText("   "));
        assertFalse(ProcurementUploadService.usableText("|| .- ' ^^"),
                "punctuation noise from a bad scan is not text");
        assertTrue(ProcurementUploadService.usableText(
                "Tender Notice for package GRL-24 issued on 12 March"));
    }

    // ------------------------------------------------------- REQ-X8: deadline state

    @Test
    void aDeadlineThatMovedStartsItsWarningsAgain() {
        DeadlineAlertState state = new DeadlineAlertState();
        state.setDeadlineDate(LocalDate.of(2026, 3, 1));
        assertTrue(state.dateChanged(LocalDate.of(2026, 4, 1)),
                "an extension is a new deadline, not the old one already announced");
        assertFalse(state.dateChanged(LocalDate.of(2026, 3, 1)));
        assertFalse(state.dateChanged(null));
    }

    @Test
    void sentMarkersSurviveARoundTrip() {
        DeadlineAlertState state = new DeadlineAlertState();
        state.setSentMarkers(new java.util.LinkedHashSet<>(List.of("30", "14", "overdue")));
        assertEquals(List.of("30", "14", "overdue"), List.copyOf(state.sentMarkers()));
    }

    @Test
    void anEmptyStateHasSentNothing() {
        assertTrue(new DeadlineAlertState().sentMarkers().isEmpty());
    }

    // ------------------------------------------- REQ-12.3: the contractual window

    @Test
    void theDeliveryWindowIsTheContractDatePlusItsPeriod() {
        ValidationService validation = validationWithoutRepositories();
        Contract contract = new Contract();
        contract.setContractDate(LocalDate.of(2026, 1, 1));
        contract.setDeliveryPeriodDays(90);

        // No price schedule repository available here, so this exercises the contract
        // fallback - the path taken whenever no schedule has been filed
        assertEquals(LocalDate.of(2026, 4, 1), validation.contractualDeliveryDeadline(contract));
    }

    @Test
    void withoutAPeriodTheCompletionDateIsTheNextBestStatementOfWhenItIsDue() {
        ValidationService validation = validationWithoutRepositories();
        Contract contract = new Contract();
        contract.setContractDate(LocalDate.of(2026, 1, 1));
        contract.setCompletionDate(LocalDate.of(2026, 6, 30));
        assertEquals(LocalDate.of(2026, 6, 30), validation.contractualDeliveryDeadline(contract));
    }

    @Test
    void anUnsignedContractHasNoWindowToMiss() {
        ValidationService validation = validationWithoutRepositories();
        assertNull(validation.contractualDeliveryDeadline(new Contract()));
        assertNull(validation.contractualDeliveryDeadline(null));
    }

    /**
     * The deadline calculation reaches the price schedule repository only when a contract
     * has a date, so a contract without one never touches it.
     */
    private ValidationService validationWithoutRepositories() {
        return new ValidationService(null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                new StubPriceScheduleRepository(), null, null, null);
    }

    private static OCRService.WordBox word(String text, int page, int x, int y) {
        return new OCRService.WordBox(text, page, x, y, 50, 20, 0.9);
    }

    /** Answers "no price schedule filed", which is the state at Stage 8. */
    private static class StubPriceScheduleRepository
            implements com.bpdb.dms.procurement.repository.PriceScheduleRepository {

        @Override
        public java.util.Optional<com.bpdb.dms.procurement.entity.PriceSchedule>
                findByContractId(Long contractId) {
            return java.util.Optional.empty();
        }

        // Everything else on JpaRepository is unused by the rule under test
        @Override public void flush() { }
        @Override public <S extends com.bpdb.dms.procurement.entity.PriceSchedule> S saveAndFlush(S e) {
            throw new UnsupportedOperationException(); }
        @Override public <S extends com.bpdb.dms.procurement.entity.PriceSchedule> List<S>
                saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(
                Iterable<com.bpdb.dms.procurement.entity.PriceSchedule> entities) { }
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { }
        @Override public void deleteAllInBatch() { }
        @Override public com.bpdb.dms.procurement.entity.PriceSchedule getOne(Long id) {
            throw new UnsupportedOperationException(); }
        @Override public com.bpdb.dms.procurement.entity.PriceSchedule getById(Long id) {
            throw new UnsupportedOperationException(); }
        @Override public com.bpdb.dms.procurement.entity.PriceSchedule getReferenceById(Long id) {
            throw new UnsupportedOperationException(); }
        @Override public <S extends com.bpdb.dms.procurement.entity.PriceSchedule> java.util.Optional<S>
                findOne(org.springframework.data.domain.Example<S> example) {
            return java.util.Optional.empty(); }
        @Override public <S extends com.bpdb.dms.procurement.entity.PriceSchedule> List<S>
                findAll(org.springframework.data.domain.Example<S> example) { return List.of(); }
        @Override public <S extends com.bpdb.dms.procurement.entity.PriceSchedule> List<S>
                findAll(org.springframework.data.domain.Example<S> example,
                        org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public <S extends com.bpdb.dms.procurement.entity.PriceSchedule>
                org.springframework.data.domain.Page<S> findAll(
                        org.springframework.data.domain.Example<S> example,
                        org.springframework.data.domain.Pageable pageable) {
            return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends com.bpdb.dms.procurement.entity.PriceSchedule> long
                count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends com.bpdb.dms.procurement.entity.PriceSchedule> boolean
                exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends com.bpdb.dms.procurement.entity.PriceSchedule, R> R findBy(
                org.springframework.data.domain.Example<S> example,
                java.util.function.Function<org.springframework.data.repository.query
                        .FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
            throw new UnsupportedOperationException(); }
        @Override public <S extends com.bpdb.dms.procurement.entity.PriceSchedule> S save(S entity) {
            throw new UnsupportedOperationException(); }
        @Override public <S extends com.bpdb.dms.procurement.entity.PriceSchedule> List<S>
                saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public java.util.Optional<com.bpdb.dms.procurement.entity.PriceSchedule>
                findById(Long id) { return java.util.Optional.empty(); }
        @Override public boolean existsById(Long id) { return false; }
        @Override public List<com.bpdb.dms.procurement.entity.PriceSchedule> findAll() {
            return List.of(); }
        @Override public List<com.bpdb.dms.procurement.entity.PriceSchedule> findAllById(
                Iterable<Long> ids) { return List.of(); }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long id) { }
        @Override public void delete(com.bpdb.dms.procurement.entity.PriceSchedule entity) { }
        @Override public void deleteAllById(Iterable<? extends Long> ids) { }
        @Override public void deleteAll(
                Iterable<? extends com.bpdb.dms.procurement.entity.PriceSchedule> entities) { }
        @Override public void deleteAll() { }
        @Override public List<com.bpdb.dms.procurement.entity.PriceSchedule> findAll(
                org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public org.springframework.data.domain.Page<
                com.bpdb.dms.procurement.entity.PriceSchedule> findAll(
                        org.springframework.data.domain.Pageable pageable) {
            return org.springframework.data.domain.Page.empty(); }
    }
}

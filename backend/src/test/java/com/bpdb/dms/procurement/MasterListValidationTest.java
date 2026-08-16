package com.bpdb.dms.procurement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.bpdb.dms.procurement.entity.ProcurementMasterList;
import com.bpdb.dms.procurement.repository.ProcurementMasterListRepository;
import com.bpdb.dms.procurement.service.MasterListService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The permitted values for Procurement Type, Method and Nature (Q-8, REQ-2.4).
 *
 * <p>The behaviour that matters is what happens to a value that does not match: it is
 * flagged for manual selection, never rejected. A tender notice worded differently is a
 * real document, and refusing it would push people out of the system.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MasterListValidationTest {

    @Autowired
    private MasterListService masterListService;

    @Autowired
    private ProcurementMasterListRepository repository;

    private void seed(String listKey, String code, String label, int order) {
        ProcurementMasterList value = new ProcurementMasterList();
        value.setListKey(listKey);
        value.setValueCode(code);
        value.setValueLabel(label);
        value.setDisplayOrder(order);
        value.setIsActive(Boolean.TRUE);
        repository.save(value);
    }

    private void seedQ8Lists() {
        seed(ProcurementMasterList.PROCUREMENT_TYPE, "NCT", "National Competitive Tender", 1);
        seed(ProcurementMasterList.PROCUREMENT_TYPE, "ICT", "International Competitive Tender", 2);
        seed(ProcurementMasterList.PROCUREMENT_METHOD, "OTM", "Open Tendering Method", 1);
        seed(ProcurementMasterList.PROCUREMENT_METHOD, "LTM", "Limited Tendering Method", 2);
        seed(ProcurementMasterList.PROCUREMENT_NATURE, "GOODS", "Goods", 3);
    }

    @Test
    void thePermittedCodesAreAccepted() {
        seedQ8Lists();
        assertTrue(masterListService.isPermitted(ProcurementMasterList.PROCUREMENT_TYPE, "ICT"));
        assertTrue(masterListService.isPermitted(ProcurementMasterList.PROCUREMENT_METHOD, "OTM"));
        assertTrue(masterListService.isPermitted(ProcurementMasterList.PROCUREMENT_NATURE, "GOODS"));
    }

    @Test
    void theFullLabelIsAcceptedToo() {
        // A document that spells it out rather than abbreviating is still correct
        seedQ8Lists();
        assertEquals("ICT", masterListService.resolve(
                ProcurementMasterList.PROCUREMENT_TYPE, "International Competitive Tender").orElseThrow());
    }

    @Test
    void casingAndSpacingAreNotADisagreement() {
        seedQ8Lists();
        assertEquals("OTM", masterListService.resolve(
                ProcurementMasterList.PROCUREMENT_METHOD, "  otm ").orElseThrow());
    }

    @Test
    void theMethodIsFoundInsideHowItWasProcured() {
        // The supplied BPDB workbook writes "e-GP/ OTM" - the method is in there, wrapped
        // in the platform it was run on
        seedQ8Lists();
        assertEquals("OTM", masterListService.resolve(
                ProcurementMasterList.PROCUREMENT_METHOD, "e-GP/ OTM").orElseThrow());
    }

    @Test
    void anUnknownValueIsFlaggedRatherThanMatched() {
        seedQ8Lists();
        assertFalse(masterListService.isPermitted(
                ProcurementMasterList.PROCUREMENT_METHOD, "Sealed envelope"));
    }

    @Test
    void theMessageNamesWhatWasReadAndWhatIsAllowed() {
        // "Invalid value" tells the user nothing they can act on
        seedQ8Lists();
        String message = masterListService.unmatchedMessage(
                ProcurementMasterList.PROCUREMENT_METHOD, "Sealed envelope");

        assertTrue(message.contains("Sealed envelope"), message);
        assertTrue(message.contains("OTM"), message);
        assertTrue(message.contains("LTM"), message);
    }

    @Test
    void aCodeIsNotMatchedInsideAnUnrelatedWord() {
        // "NCT" must not be found inside "sanctioned"
        seedQ8Lists();
        assertFalse(masterListService.isPermitted(
                ProcurementMasterList.PROCUREMENT_TYPE, "sanctioned procurement"));
    }
}

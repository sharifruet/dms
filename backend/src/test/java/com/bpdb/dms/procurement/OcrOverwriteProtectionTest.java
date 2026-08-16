package com.bpdb.dms.procurement;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.bpdb.dms.procurement.entity.ExtractedField;
import com.bpdb.dms.procurement.service.CaptureService;
import com.bpdb.dms.procurement.service.CaptureService.CaptureRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a later OCR pass may and may not do to a value that already exists (E-3).
 *
 * These exist because the first end-to-end run destroyed real data: four confirmed Stage 1
 * values were nulled by an extraction that read nothing at all. The verify screen is where
 * OCR is supposed to become trustworthy data — a re-read that can silently undo a review
 * inverts that.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OcrOverwriteProtectionTest {

    @Autowired
    private CaptureService captureService;

    private long entitySeq;

    @BeforeEach
    void setUp() {
        entitySeq = System.nanoTime();
    }

    private CaptureRequest request(String rawValue) {
        CaptureRequest req = new CaptureRequest();
        req.entityType = "PACKAGE";
        req.entityId = entitySeq;
        req.packageId = 1L;
        req.stageCode = (short) 1;
        req.fieldKey = "package_number";
        req.fieldLabel = "Package Number";
        req.dataType = "TEXT";
        req.rawValue = rawValue;
        return req;
    }

    // ------------------------------------- an empty read must not erase a value

    @Test
    void ocrFindingNothingDoesNotEraseAnExistingValue() {
        captureService.captureManual(request("GRL-18"), 1L);

        ExtractedField after = captureService.captureFromOcr(request(null));

        assertEquals("GRL-18", after.displayValue(),
                "an OCR pass that read nothing must not wipe the value that is there");
        assertTrue(after.isConfirmed(), "nor may it undo the confirmation");
        assertNotNull(after.getValidationMessage(),
                "but it should say that the re-read found nothing");
    }

    @Test
    void ocrFindingNothingStillRecordsAMissingFieldWhenThereIsNoValueYet() {
        // REQ-P8: "missing" is a recorded fact, not an absent row. The protection above
        // must not stop a genuinely empty field being recorded as not found.
        ExtractedField field = captureService.captureFromOcr(request(null));

        assertEquals(ExtractedField.NOT_FOUND, field.getValidationState());
        assertEquals(ExtractedField.OCR_SUGGESTED, field.getStatus());
    }

    // --------------------------------- a machine reading must not overrule a person

    @Test
    void ocrDisagreeingWithAConfirmedValueKeepsTheConfirmedValueAndFlagsIt() {
        captureService.captureManual(request("GRL-18"), 1L);

        ExtractedField after = captureService.captureFromOcr(request("GRL-l8"));

        assertEquals("GRL-18", after.displayValue(), "the confirmed value stands");
        assertTrue(after.isConfirmed(), "and stays confirmed");
        assertEquals(ExtractedField.CONFLICT, after.getValidationState());
        assertTrue(after.getValidationMessage().contains("GRL-l8"),
                "the competing reading must be visible, not discarded: "
                        + after.getValidationMessage());
        assertTrue(after.getValidationMessage().contains("GRL-18"),
                "and so must the confirmed one");
    }

    @Test
    void ocrAgreeingWithAConfirmedValueRaisesNoConflict() {
        captureService.captureManual(request("GRL-18"), 1L);

        ExtractedField after = captureService.captureFromOcr(request("  grl-18 "));

        assertEquals("GRL-18", after.displayValue());
        assertTrue(after.isConfirmed());
        assertEquals(ExtractedField.VALID, after.getValidationState(),
                "incidental spacing and casing are not a disagreement");
    }

    @Test
    void ocrMayStillReplaceAnUnconfirmedSuggestion() {
        // The protection is for confirmed values only. A suggestion nobody has looked at
        // is still just a suggestion, and a better read should win.
        captureService.captureFromOcr(request("GRL-l8"));

        ExtractedField after = captureService.captureFromOcr(request("GRL-18"));

        assertEquals("GRL-18", after.displayValue());
        assertEquals(ExtractedField.OCR_SUGGESTED, after.getStatus());
    }

    // ------------------------------------------------------------- provenance

    @Test
    void correctingAnOcrReadingIsRecordedAsAnOverrideNotAPlainVerification() {
        captureService.captureFromOcr(request("GRL-l8"));

        ExtractedField corrected = captureService.captureManual(request("GRL-18"), 1L);

        assertEquals(ExtractedField.MANUAL_OVERRIDE, corrected.getStatus(),
                "a person disagreeing with OCR is an override, and the audit trail should say so");
        assertEquals("GRL-l8", corrected.getRawValue(),
                "the raw reading is never rewritten (REQ-P5)");
    }

    @Test
    void aTypedValueOnAFreshFieldIsSimplyVerified() {
        ExtractedField typed = captureService.captureManual(request("GRL-18"), 1L);

        assertEquals(ExtractedField.VERIFIED, typed.getStatus(),
                "there was no OCR reading to override");
    }

    @Test
    void numericValuesAreProtectedTheSameWay() {
        CaptureRequest price = request("60");
        price.fieldKey = "price_lac_bdt";
        price.dataType = "CURRENCY";
        captureService.captureManual(price, 1L);

        CaptureRequest emptyRead = request(null);
        emptyRead.fieldKey = "price_lac_bdt";
        emptyRead.dataType = "CURRENCY";
        ExtractedField after = captureService.captureFromOcr(emptyRead);

        assertEquals(0, new BigDecimal("60").compareTo(after.getNumericValue()),
                "the parsed amount must survive an empty re-read");
    }
}

package com.bpdb.dms.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.bpdb.dms.procurement.entity.ExtractedField;

/**
 * The provenance invariants that make captured data auditable.
 *
 * The one that matters most: a correction never erases what the document said.
 */
class CaptureProvenanceTest {

    @Test
    void onlyConfirmedValuesCountTowardsStageCompletion() {
        ExtractedField suggested = new ExtractedField();
        suggested.setStatus(ExtractedField.OCR_SUGGESTED);
        assertFalse(suggested.isConfirmed());

        ExtractedField verified = new ExtractedField();
        verified.setStatus(ExtractedField.VERIFIED);
        assertTrue(verified.isConfirmed());

        ExtractedField overridden = new ExtractedField();
        overridden.setStatus(ExtractedField.MANUAL_OVERRIDE);
        assertTrue(overridden.isConfirmed());

        ExtractedField rejected = new ExtractedField();
        rejected.setStatus(ExtractedField.REJECTED);
        assertFalse(rejected.isConfirmed());
    }

    @Test
    void correctingAValueLeavesTheRawReadingIntact() {
        ExtractedField field = new ExtractedField();
        field.setDataType("CURRENCY");
        field.setRawValue("4,32O lac");      // OCR misread the zero as a letter
        field.setTextValue("4,32O lac");

        // user corrects it
        field.setNumericValue(new BigDecimal("4320"));
        field.setTextValue("4320");
        field.setStatus(ExtractedField.MANUAL_OVERRIDE);

        assertEquals("4,32O lac", field.getRawValue());
        assertEquals(new BigDecimal("4320"), field.getNumericValue());
        assertTrue(field.isConfirmed());
    }

    @Test
    void aFieldWithNoValueIsRecordedRatherThanOmitted() {
        // "OCR found nothing" is a fact worth storing (REQ-P8)
        ExtractedField field = new ExtractedField();
        field.setValidationState(ExtractedField.NOT_FOUND);
        assertFalse(field.hasValue());
        assertNull(field.displayValue());
        assertEquals(ExtractedField.NOT_FOUND, field.getValidationState());
    }

    @Test
    void displayValueReadsWhicheverTypedColumnHoldsTheValue() {
        ExtractedField date = new ExtractedField();
        date.setDataType("DATE");
        date.setDateValue(LocalDate.of(2026, 8, 20));
        assertEquals("2026-08-20", date.displayValue());

        ExtractedField number = new ExtractedField();
        number.setDataType("NUMBER");
        number.setNumericValue(new BigDecimal("5"));
        assertEquals("5", number.displayValue());

        ExtractedField bool = new ExtractedField();
        bool.setDataType("BOOL");
        bool.setBoolValue(Boolean.TRUE);
        assertEquals("true", bool.displayValue());
    }
}

package com.bpdb.dms.procurement;

import org.junit.jupiter.api.Test;

import com.bpdb.dms.procurement.service.StageDefinitionService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Repeating catalogue entities are stored on their own tables. Gate 3 must
 * recognise them so it does not demand an extracted_field row that saveBidders
 * (and the other row APIs) never write (REQ-4.1).
 */
class StageDefinitionServiceTest {

    private final StageDefinitionService definitions =
            new StageDefinitionService(null, null);

    @Test
    void bidderRowsAreRepeating() {
        assertTrue(definitions.isRepeatingEntity("BER_BIDDER"));
    }

    @Test
    void evaluationFieldsAreNotRepeating() {
        assertFalse(definitions.isRepeatingEntity("EVALUATION"));
        assertFalse(definitions.isRepeatingEntity(null));
        assertFalse(definitions.isRepeatingEntity(""));
    }
}

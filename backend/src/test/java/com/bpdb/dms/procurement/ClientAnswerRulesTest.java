package com.bpdb.dms.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.bpdb.dms.procurement.entity.Contract;
import com.bpdb.dms.procurement.entity.Tender;
import com.bpdb.dms.procurement.service.ValidationService;

/**
 * The rules that came out of the 2026-08-13 client answers, exercised without a database.
 *
 * These are the ones where getting it wrong is expensive and silent: money ceilings that
 * must refuse rather than warn, a currency rule with no FX fallback, and the derived LC
 * applicability that decides whether a whole stage is skipped.
 */
class ClientAnswerRulesTest {

    // Every collaborator is null: these rules are pure arithmetic on a contract and a
    // figure, and none of them reaches a repository
    private final ValidationService validation = new ValidationService(
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

    // ------------------------------------------------- Q-12: no over-billing

    @Test
    void billingUpToTheContractValueIsAllowed() {
        Contract contract = contractWorth("1000.00");
        assertFalse(validation.overBilled(contract, new BigDecimal("1000.00")));
        assertFalse(validation.overBilled(contract, new BigDecimal("999.99")));
    }

    @Test
    void billingAPennyOverTheContractValueIsRefused() {
        // Q-12: over-billing is not permitted - no tolerance band, no override role
        Contract contract = contractWorth("1000.00");
        assertTrue(validation.overBilled(contract, new BigDecimal("1000.01")));
    }

    @Test
    void overBillingMessageNamesTheExcessAndTheWayOut() {
        Contract contract = contractWorth("1000.00");
        String message = validation.overBillingMessage(contract, new BigDecimal("1250.00"));
        assertTrue(message.contains("250.00"), "should name the excess: " + message);
        assertTrue(message.contains("Stage 8"), "should point at the contract revision route");
    }

    @Test
    void aContractWithNoValueYetCannotBeOverBilled() {
        // Nothing to measure against - this is a missing-field problem, not a money one
        Contract contract = new Contract();
        assertFalse(validation.overBilled(contract, new BigDecimal("5000.00")));
    }

    // --------------------------------------------- Q-14: one package, one currency

    @Test
    void matchingCurrencyPasses() {
        Contract contract = contractWorth("1000.00");
        contract.setCurrency("BDT");
        assertFalse(validation.mismatchedCurrency(contract, "BDT"));
        assertFalse(validation.mismatchedCurrency(contract, "bdt"));
    }

    @Test
    void differingCurrencyIsAnErrorRatherThanAnFxConversion() {
        Contract contract = contractWorth("1000.00");
        contract.setCurrency("BDT");
        assertTrue(validation.mismatchedCurrency(contract, "USD"));
    }

    @Test
    void currencyIsNotCheckedUntilBothSidesAreKnown() {
        Contract contract = contractWorth("1000.00");
        assertFalse(validation.mismatchedCurrency(contract, "USD"), "contract currency not set yet");
        contract.setCurrency("BDT");
        assertFalse(validation.mismatchedCurrency(contract, null), "nothing supplied to compare");
    }

    // ------------------------------------------- Q-5: LC applicability from ICT

    @Test
    void ictTenderExpectsALetterOfCredit() {
        Tender tender = new Tender();
        tender.setProcurementType("ICT");
        assertTrue(tender.isInternational());
    }

    @Test
    void nationalTenderDoesNotExpectALetterOfCredit() {
        Tender tender = new Tender();
        tender.setProcurementType("NCT");
        assertFalse(tender.isInternational());
    }

    @Test
    void procurementTypeIsMatchedLeniently() {
        // OCR rarely returns a clean token - casing and stray spaces should not decide
        // whether a whole stage is skipped
        Tender tender = new Tender();
        tender.setProcurementType(" ict ");
        assertTrue(tender.isInternational());

        tender.setProcurementType(null);
        assertFalse(tender.isInternational());
    }

    // --------------------------------------------------- Q-2: tender attempts

    @Test
    void aNewTenderStartsAsCurrentAttemptOne() {
        Tender tender = new Tender();
        assertEquals(1, tender.getAttemptNo());
        assertTrue(tender.getIsCurrent());
    }

    private Contract contractWorth(String value) {
        Contract contract = new Contract();
        contract.setContractValue(new BigDecimal(value));
        return contract;
    }
}

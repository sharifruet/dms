package com.bpdb.dms.procurement.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.bpdb.dms.procurement.entity.BerBidder;
import com.bpdb.dms.procurement.entity.Contract;
import com.bpdb.dms.procurement.entity.Delivery;
import com.bpdb.dms.procurement.entity.Evaluation;
import com.bpdb.dms.procurement.entity.Invoice;
import com.bpdb.dms.procurement.entity.LetterOfCredit;
import com.bpdb.dms.procurement.entity.Noa;
import com.bpdb.dms.procurement.entity.Payment;
import com.bpdb.dms.procurement.entity.ProcurementMasterList;
import com.bpdb.dms.procurement.entity.PerformanceSecurity;
import com.bpdb.dms.procurement.entity.Tender;
import com.bpdb.dms.procurement.entity.TenderOpening;
import com.bpdb.dms.procurement.repository.BerBidderRepository;
import com.bpdb.dms.procurement.repository.ContractApprovalRepository;
import com.bpdb.dms.procurement.repository.ContractPackageRepository;
import com.bpdb.dms.procurement.repository.ContractRepository;
import com.bpdb.dms.procurement.repository.DeliveryRepository;
import com.bpdb.dms.procurement.repository.EvaluationRepository;
import com.bpdb.dms.procurement.repository.InvoiceRepository;
import com.bpdb.dms.procurement.repository.LetterOfCreditRepository;
import com.bpdb.dms.procurement.repository.NoaRepository;
import com.bpdb.dms.procurement.repository.ExtractedFieldRepository;
import com.bpdb.dms.procurement.repository.PaymentRepository;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;
import com.bpdb.dms.procurement.repository.PerformanceSecurityRepository;
import com.bpdb.dms.procurement.repository.TenderOpeningRepository;
import com.bpdb.dms.procurement.repository.TenderRepository;

/**
 * Cross-stage consistency rules - the checks that only make sense once you can see
 * more than one stage at a time.
 *
 * Rules return messages rather than throwing, because the caller decides whether a
 * given problem blocks completion or is merely flagged for review.
 */
@Service
public class ValidationService {

    private final TenderRepository tenderRepository;
    private final TenderOpeningRepository openingRepository;
    private final EvaluationRepository evaluationRepository;
    private final BerBidderRepository bidderRepository;
    private final ContractApprovalRepository approvalRepository;
    private final NoaRepository noaRepository;
    private final PerformanceSecurityRepository psRepository;
    private final ContractRepository contractRepository;
    private final ContractPackageRepository contractPackageRepository;
    private final LetterOfCreditRepository lcRepository;
    private final DeliveryRepository deliveryRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final MasterListService masterListService;
    private final ProcurementPackageRepository packageRepository;
    private final ExtractedFieldRepository fieldRepository;

    public ValidationService(TenderRepository tenderRepository,
                             TenderOpeningRepository openingRepository,
                             EvaluationRepository evaluationRepository,
                             BerBidderRepository bidderRepository,
                             ContractApprovalRepository approvalRepository,
                             NoaRepository noaRepository,
                             PerformanceSecurityRepository psRepository,
                             ContractRepository contractRepository,
                             ContractPackageRepository contractPackageRepository,
                             LetterOfCreditRepository lcRepository,
                             DeliveryRepository deliveryRepository,
                             InvoiceRepository invoiceRepository,
                             PaymentRepository paymentRepository,
                             MasterListService masterListService,
                             ProcurementPackageRepository packageRepository,
                             ExtractedFieldRepository fieldRepository) {
        this.tenderRepository = tenderRepository;
        this.openingRepository = openingRepository;
        this.evaluationRepository = evaluationRepository;
        this.bidderRepository = bidderRepository;
        this.approvalRepository = approvalRepository;
        this.noaRepository = noaRepository;
        this.psRepository = psRepository;
        this.contractRepository = contractRepository;
        this.contractPackageRepository = contractPackageRepository;
        this.lcRepository = lcRepository;
        this.deliveryRepository = deliveryRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.masterListService = masterListService;
        this.packageRepository = packageRepository;
        this.fieldRepository = fieldRepository;
    }

    /** Hard errors for a stage - these block completion. */
    public List<String> validateStage(Long packageId, short stageCode) {
        List<String> errors = new ArrayList<>();
        switch (stageCode) {
            case 2 -> validateTender(packageId, errors);
            case 5 -> validateApproval(packageId, errors);
            case 8 -> validateContract(packageId, errors);
            case 12 -> validateDeliveries(packageId, errors);
            case 13 -> validateInvoices(packageId, errors);
            case 14 -> validatePayments(packageId, errors);
            default -> { /* no hard rules at this stage */ }
        }
        return errors;
    }

    /** Soft findings - surfaced for review but never blocking. */
    public List<String> warningsForStage(Long packageId, short stageCode) {
        List<String> warnings = new ArrayList<>();
        switch (stageCode) {
            case 1 -> warnCapturedPackageNumberDiffers(packageId, warnings);
            case 2 -> warnUnmatchedMasterListValues(packageId, warnings);
            case 4 -> warnBidderCount(packageId, warnings);
            case 7 -> warnPerformanceSecurity(packageId, warnings);
            case 9 -> warnLcAgainstContract(packageId, warnings);
            case 15 -> warnWarrantyAgainstContract(packageId, warnings);
            default -> { /* nothing to warn about */ }
        }
        return warnings;
    }

    // -------------------------------------------------------------- hard rules

    private void validateTender(Long packageId, List<String> errors) {
        tenderRepository.findByPackageIdAndIsCurrentTrue(packageId).ifPresent(t -> {
            if (t.getOpeningDate() != null && t.getClosingDate() != null
                    && t.getOpeningDate().isAfter(t.getClosingDate())) {
                errors.add("Opening Date is after Closing Date (REQ-2.2)");
            }
            if (t.getTenderValidityDate() != null && t.getClosingDate() != null
                    && t.getTenderValidityDate().isBefore(t.getClosingDate())) {
                errors.add("Tender Validity expires before the Closing Date (REQ-2.2)");
            }
        });
    }

    private void validateApproval(Long packageId, List<String> errors) {
        approvalRepository.findByPackageId(packageId).ifPresent(a -> {
            if (a.getAwardedBidderId() == null) {
                errors.add("No awarded bidder selected at Tender Evaluation (REQ-4.4)");
            }
        });
    }

    private void validateContract(Long packageId, List<String> errors) {
        Optional<Contract> contract = primaryContract(packageId);
        if (contract.isEmpty()) {
            return;
        }
        Contract c = contract.get();
        approvalRepository.findByPackageId(packageId).ifPresent(a -> {
            if (c.getContractDate() != null && a.getApprovalDate() != null
                    && c.getContractDate().isBefore(a.getApprovalDate())) {
                errors.add("Contract Date precedes the Approval Date (REQ-8.7)");
            }
        });
        if (c.getContractDate() != null && c.getCompletionDate() != null
                && c.getCompletionDate().isBefore(c.getContractDate())) {
            errors.add("Completion Date precedes the Contract Date");
        }
    }

    private void validateDeliveries(Long packageId, List<String> errors) {
        Optional<Contract> contract = primaryContract(packageId);
        if (contract.isEmpty()) {
            return;
        }
        List<Delivery> deliveries = deliveryRepository.findByContractId(contract.get().getId());
        if (deliveries.isEmpty()) {
            errors.add("No delivery recorded");
            return;
        }
        boolean anyFinal = deliveries.stream().anyMatch(d -> Boolean.TRUE.equals(d.getIsFinal()));
        if (!anyFinal) {
            errors.add("No delivery has been declared final - cumulative quantity is not reconciled (REQ-12.2)");
        }
    }

    private void validateInvoices(Long packageId, List<String> errors) {
        Optional<Contract> contract = primaryContract(packageId);
        if (contract.isEmpty()) {
            return;
        }
        Contract c = contract.get();
        List<Invoice> invoices = invoiceRepository.findByContractId(c.getId());
        BigDecimal billed = invoices.stream()
                .map(i -> i.getInvoiceAmount() == null ? BigDecimal.ZERO : i.getInvoiceAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (overBilled(c, billed)) {
            errors.add(overBillingMessage(c, billed));
        }
        for (Invoice i : invoices) {
            if (mismatchedCurrency(c, i.getCurrency())) {
                errors.add("Invoice " + i.getInvoiceNumber() + " is in " + i.getCurrency()
                        + " but the contract is in " + c.getCurrency()
                        + " - a package is single-currency (REQ-B7)");
            }
        }
    }

    private void validatePayments(Long packageId, List<String> errors) {
        Optional<Contract> contract = primaryContract(packageId);
        if (contract.isEmpty()) {
            return;
        }
        Contract c = contract.get();
        BigDecimal paid = paymentRepository.findByContractId(c.getId()).stream()
                .map(p -> p.getPaymentAmount() == null ? BigDecimal.ZERO : p.getPaymentAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal billed = invoiceRepository.findByContractId(c.getId()).stream()
                .map(i -> i.getInvoiceAmount() == null ? BigDecimal.ZERO : i.getInvoiceAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (paid.compareTo(billed) > 0) {
            // Hard block, consistent with over-billing: a payment cannot settle more than
            // the invoice it is attached to (Q-12, REQ-14.2)
            errors.add("Cumulative payments " + paid.toPlainString()
                    + " exceed the invoiced total " + billed.toPlainString()
                    + " - over-payment is not permitted (REQ-14.2)");
        }
        for (Payment p : paymentRepository.findByContractId(c.getId())) {
            if (mismatchedCurrency(c, p.getCurrency())) {
                errors.add("Payment " + p.getVoucherNumber() + " is in " + p.getCurrency()
                        + " but the contract is in " + c.getCurrency()
                        + " - a package is single-currency (REQ-B7)");
            }
            if (p.getPaymentDate() == null) {
                continue;
            }
            for (Invoice i : invoiceRepository.findByContractId(c.getId())) {
                if (i.getInvoiceDate() != null && p.getPaymentDate().isBefore(i.getInvoiceDate())
                        && invoicesLinkedTo(p, i)) {
                    errors.add("Payment " + p.getVoucherNumber()
                            + " is dated before invoice " + i.getInvoiceNumber() + " (REQ-14.4)");
                }
            }
        }
    }

    // ------------------------------------------------------------ soft rules

    /**
     * The Package Number read from the APP against the one the package carries.
     *
     * <p>The package keeps its own number — it is the key every stage, document, budget
     * line and expiry tracker hangs off, and renaming it silently would cut all of them
     * adrift. But a document that says something different is worth knowing about: either
     * the reading is wrong or the document belongs to another package.
     */
    private void warnCapturedPackageNumberDiffers(Long packageId, List<String> warnings) {
        packageRepository.findById(packageId).ifPresent(pkg -> {
            if (pkg.getPackageNumber() == null) {
                return;
            }
            fieldRepository.findByPackageIdAndStageCode(packageId, (short) 1).stream()
                    .filter(f -> "package_number".equals(f.getFieldKey()))
                    .filter(f -> f.displayValue() != null)
                    .filter(f -> !pkg.getPackageNumber().trim()
                            .equalsIgnoreCase(f.displayValue().trim()))
                    .findFirst()
                    .ifPresent(f -> warnings.add(
                            "The document reads Package Number '" + f.displayValue()
                            + "' but this package is '" + pkg.getPackageNumber()
                            + "'. The package keeps its number - check the document belongs here."));
        });
    }

    /**
     * Procurement Type, Method and Nature against the permitted lists (Q-8, REQ-2.4).
     *
     * <p>A warning rather than an error, exactly as the requirement words it: unmatched
     * values are "flagged for manual selection rather than accepted silently". Blocking
     * the stage because a tender notice worded something differently would push people
     * out of the system, which costs more than the inconsistency does.
     */
    private void warnUnmatchedMasterListValues(Long packageId, List<String> warnings) {
        tenderRepository.findByPackageIdAndIsCurrentTrue(packageId).ifPresent(tender -> {
            checkAgainstList(ProcurementMasterList.PROCUREMENT_TYPE, "Procurement Type",
                    tender.getProcurementType(), warnings);
            checkAgainstList(ProcurementMasterList.PROCUREMENT_METHOD, "Procurement Method",
                    tender.getProcurementMethod(), warnings);
            checkAgainstList(ProcurementMasterList.PROCUREMENT_NATURE, "Procurement Nature",
                    tender.getProcurementNature(), warnings);
        });
    }

    private void checkAgainstList(String listKey, String label, String captured,
                                  List<String> warnings) {
        if (captured == null || captured.isBlank()) {
            return; // absence is the field gate's business, not this one
        }
        if (!masterListService.isPermitted(listKey, captured)) {
            warnings.add(label + ": " + masterListService.unmatchedMessage(listKey, captured));
        }
    }

    private void warnBidderCount(Long packageId, List<String> warnings) {
        Optional<Tender> tender = tenderRepository.findByPackageIdAndIsCurrentTrue(packageId);
        if (tender.isEmpty()) {
            return;
        }
        Optional<TenderOpening> opening = openingRepository.findByTenderId(tender.get().getId());
        if (opening.isEmpty() || opening.get().getNumberOfBidders() == null) {
            return;
        }
        Optional<Evaluation> evaluation = evaluationRepository.findByOpeningId(opening.get().getId());
        if (evaluation.isEmpty()) {
            return;
        }
        long rows = bidderRepository.countByEvaluationId(evaluation.get().getId());
        int declared = opening.get().getNumberOfBidders();
        if (rows != declared) {
            // Flagged, not blocked - the BER may list only evaluated bidders (REQ-4.3)
            warnings.add("BER lists " + rows + " bidders but " + declared
                    + " were recorded at Tender Opening");
        }
    }

    private void warnPerformanceSecurity(Long packageId, List<String> warnings) {
        approvalRepository.findByPackageId(packageId).ifPresent(a ->
            noaRepository.findByApprovalId(a.getId()).ifPresent(noa -> {
                List<PerformanceSecurity> securities = psRepository.findByNoaId(noa.getId());
                BigDecimal submitted = securities.stream()
                        .map(s -> s.getAmount() == null ? BigDecimal.ZERO : s.getAmount())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (noa.getPsAmount() != null && submitted.compareTo(noa.getPsAmount()) < 0) {
                    warnings.add("Performance security submitted (" + submitted.toPlainString()
                            + ") is short of the amount required by the NOA ("
                            + noa.getPsAmount().toPlainString() + ") (REQ-7.1)");
                }
            }));
    }

    private void warnLcAgainstContract(Long packageId, List<String> warnings) {
        primaryContract(packageId).ifPresent(c -> {
            for (LetterOfCredit lc : lcRepository.findByContractId(c.getId())) {
                if (lc.getLcAmount() != null && c.getContractValue() != null
                        && lc.getLcAmount().compareTo(c.getContractValue()) != 0) {
                    warnings.add("LC amount " + lc.getLcAmount().toPlainString()
                            + " differs from the contract value "
                            + c.getContractValue().toPlainString() + " (REQ-9.4)");
                }
            }
        });
    }

    private void warnWarrantyAgainstContract(Long packageId, List<String> warnings) {
        primaryContract(packageId).ifPresent(c -> {
            if (c.getWarrantyPeriodMonths() == null) {
                warnings.add("Contract does not record a warranty period to reconcile against (REQ-15.1)");
            }
        });
    }

    // ------------------------------------------------- ceilings and currency

    /**
     * Would this billing total breach the contract value?
     *
     * Over-billing is not permitted (client answer Q-12): there is no tolerance band and
     * no override role. Where a genuine increase is needed - price variation, taxes, a
     * scope change - the contract value is revised at Stage 8 with its own document trail
     * and the invoice is then accepted against the revised figure.
     */
    public boolean overBilled(Contract contract, BigDecimal cumulativeBilled) {
        return contract.getContractValue() != null
                && cumulativeBilled.compareTo(contract.getContractValue()) > 0;
    }

    /** Names the excess, so the user is told the size of the problem rather than just "no". */
    public String overBillingMessage(Contract contract, BigDecimal cumulativeBilled) {
        BigDecimal value = contract.getContractValue();
        return "Cumulative invoiced amount " + cumulativeBilled.toPlainString()
                + " exceeds the contract value " + value.toPlainString()
                + " by " + cumulativeBilled.subtract(value).toPlainString()
                + ". Over-billing is not permitted (REQ-13.3) - revise the contract value at"
                + " Stage 8 first if the increase is genuine.";
    }

    /**
     * One package, one currency (client answer Q-14). The contract fixes it at Stage 8 and
     * every LC, invoice and payment below must agree. Nothing is converted - a differing
     * currency is an error, not an FX problem.
     */
    public boolean mismatchedCurrency(Contract contract, String currency) {
        return contract.getCurrency() != null && currency != null
                && !contract.getCurrency().equalsIgnoreCase(currency);
    }

    // -------------------------------------------------------------- helpers

    /**
     * The contract for a package.
     *
     * A contract covers exactly one package (client answer Q-1 - the "one contract across
     * several APP packages" option was not selected). The link table remains because
     * lot-wise tendering is where that requirement tends to appear later; this resolves
     * through it and prefers the row marked primary.
     */
    public Optional<Contract> primaryContract(Long packageId) {
        return contractPackageRepository.findByPackageId(packageId).stream()
                .sorted((a, b) -> Boolean.compare(
                        Boolean.TRUE.equals(b.getIsPrimary()), Boolean.TRUE.equals(a.getIsPrimary())))
                .findFirst()
                .flatMap(cp -> contractRepository.findById(cp.getContractId()));
    }

    private boolean invoicesLinkedTo(Payment payment, Invoice invoice) {
        return payment.getContractId() != null
                && payment.getContractId().equals(invoice.getContractId());
    }

    /** Deviation of a bid against the OCE, as a percentage (REQ-4.2). */
    public BigDecimal deviationAgainstOce(BerBidder bidder, Evaluation evaluation) {
        if (bidder.getBiddingPrice() == null || evaluation.getOceValue() == null
                || evaluation.getOceValue().signum() == 0) {
            return null;
        }
        return bidder.getBiddingPrice()
                .subtract(evaluation.getOceValue())
                .multiply(BigDecimal.valueOf(100))
                .divide(evaluation.getOceValue(), 4, java.math.RoundingMode.HALF_UP);
    }
}

package com.bpdb.dms.procurement.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.bpdb.dms.procurement.entity.BerBidder;
import com.bpdb.dms.procurement.entity.Contract;
import com.bpdb.dms.procurement.entity.Delivery;
import com.bpdb.dms.procurement.entity.DeliveryLine;
import com.bpdb.dms.procurement.entity.Evaluation;
import com.bpdb.dms.procurement.entity.InspectionEvent;
import com.bpdb.dms.procurement.entity.PriceSchedule;
import com.bpdb.dms.procurement.entity.PriceScheduleLine;
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
import com.bpdb.dms.procurement.repository.DeliveryLineRepository;
import com.bpdb.dms.procurement.repository.DeliveryRepository;
import com.bpdb.dms.procurement.repository.EvaluationRepository;
import com.bpdb.dms.procurement.repository.InspectionEventRepository;
import com.bpdb.dms.procurement.repository.PriceScheduleLineRepository;
import com.bpdb.dms.procurement.repository.PriceScheduleRepository;
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

    private static final Logger log = LoggerFactory.getLogger(ValidationService.class);

    /**
     * Largest absolute value {@code ber_bidder.deviation_pct} can hold after
     * changeset 049 ({@code NUMERIC(18,4)}).
     */
    static final BigDecimal DEVIATION_PCT_ABS_MAX = new BigDecimal("99999999999999.9999");

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
    private final PriceScheduleRepository priceScheduleRepository;
    private final PriceScheduleLineRepository priceScheduleLineRepository;
    private final DeliveryLineRepository deliveryLineRepository;
    private final InspectionEventRepository inspectionRepository;

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
                             ExtractedFieldRepository fieldRepository,
                             PriceScheduleRepository priceScheduleRepository,
                             PriceScheduleLineRepository priceScheduleLineRepository,
                             DeliveryLineRepository deliveryLineRepository,
                             InspectionEventRepository inspectionRepository) {
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
        this.priceScheduleRepository = priceScheduleRepository;
        this.priceScheduleLineRepository = priceScheduleLineRepository;
        this.deliveryLineRepository = deliveryLineRepository;
        this.inspectionRepository = inspectionRepository;
    }

    /** Hard errors for a stage - these block completion. */
    public List<String> validateStage(Long packageId, short stageCode) {
        List<String> errors = new ArrayList<>();
        switch (stageCode) {
            case 2 -> validateTender(packageId, errors);
            case 4 -> validateBidders(packageId, errors);
            case 5 -> validateApproval(packageId, errors);
            case 8 -> validateContract(packageId, errors);
            case 11 -> validateInspections(packageId, errors);
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
            case 11 -> warnSatOutstanding(packageId, warnings);
            case 12 -> {
                warnLateDelivery(packageId, warnings);
                warnDeliveryAgainstBaseline(packageId, warnings);
            }
            case 13 -> {
                warnSupplierDiffersFromAward(packageId, warnings);
                warnInvoiceAgainstBaseline(packageId, warnings);
            }
            case 15 -> warnWarrantyAgainstContract(packageId, warnings);
            default -> { /* nothing to warn about */ }
        }
        return warnings;
    }

    // -------------------------------------------------------------- hard rules

    /**
     * BER bidder rows are the Stage 4 field store (REQ-4.1). Gate 3 no longer looks
     * for extracted_field copies of Bidder Name / Price / Deviation / Responsive.
     */
    private void validateBidders(Long packageId, List<String> errors) {
        List<BerBidder> bidders = biddersOf(packageId);
        if (bidders.isEmpty()) {
            errors.add("No bidders recorded from the BER (REQ-4.1)");
            return;
        }
        if (bidders.stream().anyMatch(b -> b.getBidderName() == null || b.getBidderName().isBlank())) {
            errors.add("A BER bidder row is missing a name (REQ-4.1)");
        }
        if (bidders.stream().anyMatch(b -> b.getBiddingPrice() == null)) {
            errors.add("A BER bidder row is missing a bidding price (REQ-4.1)");
        }
        if (bidders.stream().noneMatch(b -> Boolean.TRUE.equals(b.getIsAwarded()))) {
            errors.add("No awarded bidder selected (REQ-4.4)");
        }
    }

    private List<BerBidder> biddersOf(Long packageId) {
        Optional<Tender> tender = tenderRepository.findByPackageIdAndIsCurrentTrue(packageId);
        if (tender.isEmpty()) {
            return List.of();
        }
        Optional<TenderOpening> opening = openingRepository.findByTenderId(tender.get().getId());
        if (opening.isEmpty()) {
            return List.of();
        }
        Optional<Evaluation> evaluation = evaluationRepository.findByOpeningId(opening.get().getId());
        if (evaluation.isEmpty()) {
            return List.of();
        }
        return bidderRepository.findByEvaluationIdOrderByBidRankAsc(evaluation.get().getId());
    }

    private void validateInspections(Long packageId, List<String> errors) {
        Optional<Contract> contract = primaryContract(packageId);
        if (contract.isEmpty()) {
            errors.add("No contract recorded");
            return;
        }
        if (inspectionRepository.findByContractId(contract.get().getId()).isEmpty()) {
            errors.add("No inspection recorded");
        }
    }

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
     * A declared SAT that has not been carried out (REQ-11.3).
     *
     * <p>The blocking half of this rule lives in the stage gate, which makes the SAT
     * Report a required document once somebody declares it applies. This is the softer
     * half: the report may be filed while the test itself is still marked outstanding,
     * and that mismatch is worth saying out loud rather than inferring from a checkbox.
     */
    private void warnSatOutstanding(Long packageId, List<String> warnings) {
        List<InspectionEvent> events = inspectionsFor(packageId);
        boolean declared = events.stream().anyMatch(e -> Boolean.TRUE.equals(e.getSatApplicable()));
        boolean done = events.stream()
                .filter(e -> Boolean.TRUE.equals(e.getSatApplicable()))
                .anyMatch(e -> Boolean.TRUE.equals(e.getSatDone()));
        if (declared && !done) {
            warnings.add("A Site Acceptance Test has been declared applicable but is not "
                    + "recorded as done. The SAT Report is required before this stage can "
                    + "complete (REQ-11.3).");
        }
    }

    /**
     * Delivery against the contractual window (REQ-12.3).
     *
     * <p>The window is the contract date plus the delivery period agreed at Stage 8, or
     * the period on the price schedule where the schedule states its own. Late delivery is
     * flagged, never blocked: it is a fact about a delivery that already happened, and
     * refusing to record it would only mean the lateness goes unrecorded too.
     */
    private void warnLateDelivery(Long packageId, List<String> warnings) {
        Optional<Contract> found = primaryContract(packageId);
        if (found.isEmpty()) {
            return;
        }
        Contract contract = found.get();
        LocalDate due = contractualDeliveryDeadline(contract);
        if (due == null) {
            warnings.add("No Delivery Period is recorded on the contract, so delivery dates "
                    + "cannot be checked against the contractual window (REQ-12.3).");
            return;
        }
        for (Delivery d : deliveryRepository.findByContractId(contract.getId())) {
            if (d.getDeliveryDate() != null && d.getDeliveryDate().isAfter(due)) {
                long daysLate = ChronoUnit.DAYS.between(due, d.getDeliveryDate());
                warnings.add("Delivery " + reference(d) + " on " + d.getDeliveryDate()
                        + " is " + daysLate + (daysLate == 1 ? " day" : " days")
                        + " past the contractual window, which ended " + due
                        + " (REQ-12.3).");
            }
        }
    }

    /**
     * The contractual delivery deadline: contract date + delivery period in days.
     *
     * <p>The price schedule's own period wins where it has one, because the schedule is
     * the later and more specific document (REQ-10.3); the contract's figure is the
     * fallback.
     */
    public LocalDate contractualDeliveryDeadline(Contract contract) {
        if (contract == null || contract.getContractDate() == null) {
            return null;
        }
        Integer days = priceScheduleRepository.findByContractId(contract.getId())
                .map(PriceSchedule::getDeliveryPeriodDays)
                .orElse(null);
        if (days == null) {
            days = contract.getDeliveryPeriodDays();
        }
        if (days == null) {
            // A completion date is a weaker but still real statement of when it is due
            return contract.getCompletionDate();
        }
        return contract.getContractDate().plusDays(days);
    }

    /**
     * Delivered quantities against the price schedule (REQ-10.3).
     *
     * <p>The schedule is the item baseline, so over-delivering a line is as much a
     * discrepancy as over-billing — it is just one that shows up in a warehouse rather
     * than on an invoice.
     */
    private void warnDeliveryAgainstBaseline(Long packageId, List<String> warnings) {
        Optional<Contract> found = primaryContract(packageId);
        if (found.isEmpty()) {
            return;
        }
        List<PriceScheduleLine> baseline = baselineLines(found.get().getId());
        if (baseline.isEmpty()) {
            return; // no schedule filed - REQ-10.3 has nothing to measure against yet
        }
        Map<Long, BigDecimal> deliveredPerLine = new HashMap<>();
        for (Delivery d : deliveryRepository.findByContractId(found.get().getId())) {
            for (DeliveryLine line : deliveryLineRepository.findByDeliveryId(d.getId())) {
                if (line.getPriceScheduleLineId() == null) {
                    continue;
                }
                deliveredPerLine.merge(line.getPriceScheduleLineId(),
                        line.getQuantity() == null ? BigDecimal.ZERO : line.getQuantity(),
                        BigDecimal::add);
            }
        }
        for (PriceScheduleLine line : baseline) {
            BigDecimal delivered = deliveredPerLine.get(line.getId());
            if (delivered == null || line.getQuantity() == null) {
                continue;
            }
            if (delivered.compareTo(line.getQuantity()) > 0) {
                warnings.add("Item '" + itemLabel(line) + "': delivered "
                        + delivered.toPlainString() + " against a scheduled quantity of "
                        + line.getQuantity().toPlainString() + " (REQ-10.3).");
            }
        }
    }

    /**
     * Invoiced amount against what the price schedule says the goods are worth (REQ-10.3).
     *
     * <p>The contract ceiling (REQ-13.3) already refuses an invoice that would over-bill
     * the contract as a whole. This is the finer check: an invoice can sit under the
     * contract value and still be billing more for an item than the schedule prices it at.
     */
    private void warnInvoiceAgainstBaseline(Long packageId, List<String> warnings) {
        Optional<Contract> found = primaryContract(packageId);
        if (found.isEmpty()) {
            return;
        }
        List<PriceScheduleLine> baseline = baselineLines(found.get().getId());
        if (baseline.isEmpty()) {
            return;
        }
        BigDecimal scheduleValue = baseline.stream()
                .map(l -> l.getLineAmount() == null ? BigDecimal.ZERO : l.getLineAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (scheduleValue.signum() <= 0) {
            return;
        }
        BigDecimal billed = invoiceRepository.findByContractId(found.get().getId()).stream()
                .map(i -> i.getInvoiceAmount() == null ? BigDecimal.ZERO : i.getInvoiceAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (billed.compareTo(scheduleValue) > 0) {
            warnings.add("Invoiced " + billed.toPlainString()
                    + " against a price schedule totalling " + scheduleValue.toPlainString()
                    + " - the billed total has passed the item baseline (REQ-10.3).");
        }
    }

    /**
     * Supplier on the invoice against the bidder the BER awarded (REQ-13.5).
     *
     * <p>Names rarely match to the character — "Ltd." against "Limited", a branch name,
     * a trading name — so this compares leniently and reports rather than blocks. What it
     * is really looking for is the case where an invoice arrives from a company nobody
     * recognises, which is worth a human look and never worth an automatic rejection.
     */
    private void warnSupplierDiffersFromAward(Long packageId, List<String> warnings) {
        Optional<Contract> found = primaryContract(packageId);
        if (found.isEmpty()) {
            return;
        }
        Contract contract = found.get();
        String awarded = awardedBidderName(packageId);
        String reference = awarded != null ? awarded : contract.getSupplierName();
        if (reference == null || reference.isBlank()) {
            return;
        }
        String source = awarded != null ? "the awarded bidder" : "the contract";
        for (Invoice invoice : invoiceRepository.findByContractId(contract.getId())) {
            String supplier = invoice.getSupplierName();
            if (supplier == null || supplier.isBlank()) {
                warnings.add("Invoice " + invoice.getInvoiceNumber()
                        + " names no supplier to reconcile against " + source
                        + " '" + reference + "' (REQ-13.5).");
                continue;
            }
            if (!namesMatch(supplier, reference)) {
                warnings.add("Invoice " + invoice.getInvoiceNumber() + " is from '"
                        + supplier + "' but " + source + " is '" + reference
                        + "' - check this invoice belongs to this contract (REQ-13.5).");
            }
        }
    }

    /**
     * The bidder the BER awarded.
     *
     * <p>Preferring the id Contract Approval carries (L-08) over scanning for the awarded
     * flag: the approval is the decision, the flag is how it was recorded.
     */
    private String awardedBidderName(Long packageId) {
        String fromApproval = approvalRepository.findByPackageId(packageId)
                .map(a -> a.getAwardedBidderId())
                .flatMap(id -> id == null ? Optional.empty() : bidderRepository.findById(id))
                .map(BerBidder::getBidderName)
                .filter(n -> n != null && !n.isBlank())
                .orElse(null);
        if (fromApproval != null) {
            return fromApproval;
        }
        return evaluationFor(packageId)
                .map(e -> bidderRepository.findByEvaluationIdOrderByBidRankAsc(e.getId()))
                .orElseGet(List::of).stream()
                .filter(b -> Boolean.TRUE.equals(b.getIsAwarded()))
                .map(BerBidder::getBidderName)
                .filter(n -> n != null && !n.isBlank())
                .findFirst().orElse(null);
    }

    /** Evaluation reached the way the graph links it: tender to opening to evaluation. */
    private Optional<Evaluation> evaluationFor(Long packageId) {
        return tenderRepository.findByPackageIdAndIsCurrentTrue(packageId)
                .flatMap(t -> openingRepository.findByTenderId(t.getId()))
                .flatMap(o -> evaluationRepository.findByOpeningId(o.getId()));
    }

    /**
     * Company names compared the way a person would: case, punctuation and the usual
     * suffixes ignored, so "ABC Engineering Ltd." and "ABC Engineering Limited" are the
     * same company and nobody is asked to confirm it.
     */
    public static boolean namesMatch(String a, String b) {
        return normaliseCompany(a).equals(normaliseCompany(b));
    }

    private static String normaliseCompany(String name) {
        String n = name.toLowerCase().replaceAll("[^a-z0-9 ]", " ");
        n = n.replaceAll("\\b(limited|ltd|private|pvt|company|co|corporation|corp|"
                + "incorporated|inc|and|the)\\b", " ");
        return n.replaceAll("\\s+", " ").trim();
    }

    private List<PriceScheduleLine> baselineLines(Long contractId) {
        return priceScheduleRepository.findByContractId(contractId)
                .map(s -> priceScheduleLineRepository.findByScheduleIdOrderByLineNoAsc(s.getId()))
                .orElseGet(List::of);
    }

    private List<InspectionEvent> inspectionsFor(Long packageId) {
        return primaryContract(packageId)
                .map(c -> inspectionRepository.findByContractId(c.getId()))
                .orElseGet(List::of);
    }

    private static String itemLabel(PriceScheduleLine line) {
        if (line.getItemDescription() != null && !line.getItemDescription().isBlank()) {
            return line.getItemDescription();
        }
        return line.getItemCode() == null ? "line " + line.getLineNo() : line.getItemCode();
    }

    private static String reference(Delivery d) {
        return d.getDeliveryReferenceNumber() == null
                ? "#" + d.getId() : d.getDeliveryReferenceNumber();
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
        BigDecimal pct = bidder.getBiddingPrice()
                .subtract(evaluation.getOceValue())
                .multiply(BigDecimal.valueOf(100))
                .divide(evaluation.getOceValue(), 4, java.math.RoundingMode.HALF_UP);
        if (pct.abs().compareTo(DEVIATION_PCT_ABS_MAX) > 0) {
            // OCE and bidding price are almost certainly in different units; storing
            // the figure would overflow the column the way NUMERIC(9,4) used to.
            log.warn("Computed deviation {}% for '{}' exceeds what deviation_pct can hold "
                            + "(bidding price {}, OCE {}) — left blank",
                    pct, bidder.getBidderName(), bidder.getBiddingPrice(),
                    evaluation.getOceValue());
            return null;
        }
        return pct;
    }
}

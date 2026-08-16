package com.bpdb.dms.procurement.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bpdb.dms.procurement.entity.BerBidder;
import com.bpdb.dms.procurement.entity.Contract;
import com.bpdb.dms.procurement.entity.ContractApproval;
import com.bpdb.dms.procurement.entity.Delivery;
import com.bpdb.dms.procurement.entity.Evaluation;
import com.bpdb.dms.procurement.entity.InspectionEvent;
import com.bpdb.dms.procurement.entity.Invoice;
import com.bpdb.dms.procurement.entity.InvoiceDeliveryLink;
import com.bpdb.dms.procurement.entity.Payment;
import com.bpdb.dms.procurement.entity.PaymentInvoiceLink;
import com.bpdb.dms.procurement.repository.BerBidderRepository;
import com.bpdb.dms.procurement.repository.ContractApprovalRepository;
import com.bpdb.dms.procurement.repository.DeliveryRepository;
import com.bpdb.dms.procurement.repository.InspectionEventRepository;
import com.bpdb.dms.procurement.repository.InvoiceDeliveryLinkRepository;
import com.bpdb.dms.procurement.repository.InvoiceRepository;
import com.bpdb.dms.procurement.repository.PaymentInvoiceLinkRepository;
import com.bpdb.dms.procurement.repository.PaymentRepository;

/**
 * The records a stage can hold many of: BER bidder rows, inspections, deliveries,
 * invoices and payments.
 *
 * These are not stage forms - they are lists a user builds up - so they get explicit
 * create/update methods rather than going through the catalogue-driven form save.
 */
@Service
public class ProcurementRecordService {

    private static final Logger log = LoggerFactory.getLogger(ProcurementRecordService.class);

    private final BerBidderRepository bidderRepository;
    private final ContractApprovalRepository approvalRepository;
    private final InspectionEventRepository inspectionRepository;
    private final DeliveryRepository deliveryRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceDeliveryLinkRepository invoiceDeliveryLinkRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentInvoiceLinkRepository paymentInvoiceLinkRepository;
    private final StageDataService stageDataService;
    private final ValidationService validationService;
    private final BudgetService budgetService;
    private final ProcurementAuditService auditService;

    public ProcurementRecordService(BerBidderRepository bidderRepository,
                                    ContractApprovalRepository approvalRepository,
                                    InspectionEventRepository inspectionRepository,
                                    DeliveryRepository deliveryRepository,
                                    InvoiceRepository invoiceRepository,
                                    InvoiceDeliveryLinkRepository invoiceDeliveryLinkRepository,
                                    PaymentRepository paymentRepository,
                                    PaymentInvoiceLinkRepository paymentInvoiceLinkRepository,
                                    StageDataService stageDataService,
                                    ValidationService validationService,
                                    BudgetService budgetService,
                                    ProcurementAuditService auditService) {
        this.bidderRepository = bidderRepository;
        this.approvalRepository = approvalRepository;
        this.inspectionRepository = inspectionRepository;
        this.deliveryRepository = deliveryRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceDeliveryLinkRepository = invoiceDeliveryLinkRepository;
        this.paymentRepository = paymentRepository;
        this.paymentInvoiceLinkRepository = paymentInvoiceLinkRepository;
        this.stageDataService = stageDataService;
        this.validationService = validationService;
        this.budgetService = budgetService;
        this.auditService = auditService;
    }

    // ------------------------------------------------------- Stage 4: bidders

    /**
     * Replace the BER bidder table for a package. The BER is the single source of
     * bidder data (REQ-4.1), so a re-read of the BER replaces the rows wholesale.
     *
     * Deviation against the OCE is computed where the BER did not state it.
     */
    @Transactional
    public List<BerBidder> saveBidders(Long packageId, List<BerBidder> bidders) {
        Evaluation evaluation = (Evaluation) stageDataService.ensureEntity("EVALUATION", packageId);
        List<BerBidder> existing = bidderRepository.findByEvaluationIdOrderByBidRankAsc(evaluation.getId());
        bidderRepository.deleteAll(existing);

        List<BerBidder> saved = new ArrayList<>();
        int rank = 1;
        for (BerBidder b : bidders) {
            b.setId(null);
            b.setEvaluationId(evaluation.getId());
            if (b.getBidRank() == null) {
                b.setBidRank(rank);
            }
            if (b.getDeviationPct() == null) {
                b.setDeviationPct(validationService.deviationAgainstOce(b, evaluation));
            }
            saved.add(bidderRepository.save(b));
            rank++;
        }

        // Carry the awarded bidder through to Contract Approval (L-08)
        saved.stream().filter(b -> Boolean.TRUE.equals(b.getIsAwarded())).findFirst()
                .ifPresent(awarded -> {
                    ContractApproval approval =
                            (ContractApproval) stageDataService.ensureEntity("CONTRACT_APPROVAL", packageId);
                    approval.setAwardedBidderId(awarded.getId());
                    approvalRepository.save(approval);
                });
        return saved;
    }

    public List<BerBidder> bidders(Long packageId) {
        Evaluation evaluation = (Evaluation) stageDataService.ensureEntity("EVALUATION", packageId);
        return evaluation == null
                ? List.of()
                : bidderRepository.findByEvaluationIdOrderByBidRankAsc(evaluation.getId());
    }

    // --------------------------------------------------- Stage 11: inspections

    @Transactional
    public InspectionEvent saveInspection(Long packageId, InspectionEvent event) {
        Contract contract = requireContract(packageId);
        event.setContractId(contract.getId());
        if (event.getInspectionType() != null
                && !List.of("PDI", "PLI").contains(event.getInspectionType().toUpperCase())) {
            throw new IllegalArgumentException("Inspection Type must be PDI or PLI (REQ-11.1)");
        }
        return inspectionRepository.save(event);
    }

    public List<InspectionEvent> inspections(Long packageId) {
        return stageDataService.findContract(packageId)
                .map(c -> inspectionRepository.findByContractId(c.getId()))
                .orElseGet(List::of);
    }

    // ----------------------------------------------------- Stage 12: deliveries

    @Transactional
    public Delivery saveDelivery(Long packageId, Delivery delivery) {
        Contract contract = requireContract(packageId);
        delivery.setContractId(contract.getId());
        if (delivery.getDeliveryReferenceNumber() != null) {
            Optional<Delivery> clash = deliveryRepository.findByContractIdAndDeliveryReferenceNumber(
                    contract.getId(), delivery.getDeliveryReferenceNumber());
            if (clash.isPresent() && !clash.get().getId().equals(delivery.getId())) {
                throw new IllegalArgumentException("Delivery Reference Number "
                        + delivery.getDeliveryReferenceNumber()
                        + " is already used on this contract (REQ-12.4)");
            }
        }

        // Finality is not an ordinary field. Declaring a delivery final closes the delivery
        // set and unblocks the stage, so it is a Checker's decision made through
        // declareFinal (REQ-12.5) - not something that can ride in on a form payload.
        if (delivery.getId() == null) {
            delivery.setIsFinal(Boolean.FALSE);
        } else {
            deliveryRepository.findById(delivery.getId())
                    .ifPresent(existing -> delivery.setIsFinal(existing.getIsFinal()));
        }
        return deliveryRepository.save(delivery);
    }

    /**
     * Declare a delivery final, closing the delivery set for the contract (REQ-12.5).
     *
     * <p>Q-11 did not say who declares a delivery final, so it follows the maker/checker
     * split of Q-17: the Checker who approves the stage is the one who decides the goods
     * are all in. The route is permission-gated; this records the decision.
     */
    @Transactional
    public Delivery declareFinal(Long packageId, Long deliveryId, Long userId) {
        Contract contract = requireContract(packageId);
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery not found: " + deliveryId));
        if (!contract.getId().equals(delivery.getContractId())) {
            throw new IllegalArgumentException(
                    "Delivery " + deliveryId + " does not belong to this package");
        }

        delivery.setIsFinal(Boolean.TRUE);
        Delivery saved = deliveryRepository.save(delivery);
        auditService.deliveryDeclaredFinal(userId, saved.getId(),
                saved.getDeliveryReferenceNumber());
        return saved;
    }

    /** Reopen a delivery set that was closed too early. Also a Checker's decision. */
    @Transactional
    public Delivery reopenDeliveries(Long packageId, Long deliveryId, Long userId, String reason) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery not found: " + deliveryId));
        delivery.setIsFinal(Boolean.FALSE);
        Delivery saved = deliveryRepository.save(delivery);
        auditService.recordChange(userId, "PROCUREMENT_DELIVERY_REOPENED",
                ProcurementAuditService.DELIVERY, saved.getId(),
                "Delivery " + saved.getDeliveryReferenceNumber(), "final", "not final", reason);
        return saved;
    }

    public List<Delivery> deliveries(Long packageId) {
        return stageDataService.findContract(packageId)
                .map(c -> deliveryRepository.findByContractId(c.getId()))
                .orElseGet(List::of);
    }

    /** Cumulative delivered quantity against the contract (REQ-12.2). */
    public BigDecimal cumulativeDelivered(Long packageId) {
        return deliveries(packageId).stream()
                .map(d -> d.getDeliveredQuantity() == null ? BigDecimal.ZERO : d.getDeliveredQuantity())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // -------------------------------------------------------- Stage 13: invoices

    /**
     * Save an invoice and link it to the deliveries it bills. The link carries the
     * apportioned amount so progressive billing reconciles (REQ-L7).
     */
    @Transactional
    public Invoice saveInvoice(Long packageId, Invoice invoice, List<Long> deliveryIds) {
        Contract contract = requireContract(packageId);
        invoice.setContractId(contract.getId());

        Optional<Invoice> clash = invoiceRepository.findByContractIdAndSupplierNameAndInvoiceNumber(
                contract.getId(), invoice.getSupplierName(), invoice.getInvoiceNumber());
        if (clash.isPresent() && !clash.get().getId().equals(invoice.getId())) {
            throw new IllegalArgumentException("Invoice " + invoice.getInvoiceNumber()
                    + " already exists for this supplier (REQ-13.2)");
        }

        // Over-billing is refused at the door, not recorded and flagged (Q-12, REQ-13.3).
        // Checked against what the contract would total WITH this invoice applied.
        if (validationService.mismatchedCurrency(contract, invoice.getCurrency())) {
            throw new IllegalArgumentException("Invoice currency " + invoice.getCurrency()
                    + " differs from the contract currency " + contract.getCurrency()
                    + " - a package is single-currency (REQ-B7)");
        }
        BigDecimal wouldTotal = cumulativeBilledExcluding(contract.getId(), invoice.getId())
                .add(invoice.getInvoiceAmount() == null ? BigDecimal.ZERO : invoice.getInvoiceAmount());
        if (validationService.overBilled(contract, wouldTotal)) {
            String message = validationService.overBillingMessage(contract, wouldTotal);
            // An attempt to bill beyond the contract value is worth recording whether or
            // not it succeeded - a refused action is still an action (REQ-X6)
            auditService.refused(null, ProcurementAuditService.INVOICE, contract.getId(),
                    "Invoice " + invoice.getInvoiceNumber() + " rejected as over-billing", message);
            throw new IllegalArgumentException(message);
        }

        Invoice saved = invoiceRepository.save(invoice);

        if (deliveryIds != null) {
            invoiceDeliveryLinkRepository.deleteAll(
                    invoiceDeliveryLinkRepository.findByInvoiceId(saved.getId()));
            BigDecimal each = deliveryIds.isEmpty() || saved.getInvoiceAmount() == null
                    ? null
                    : saved.getInvoiceAmount().divide(
                            BigDecimal.valueOf(deliveryIds.size()), 2, java.math.RoundingMode.HALF_UP);
            for (Long deliveryId : deliveryIds) {
                InvoiceDeliveryLink link = new InvoiceDeliveryLink();
                link.setInvoiceId(saved.getId());
                link.setDeliveryId(deliveryId);
                link.setApportionedAmount(each);
                invoiceDeliveryLinkRepository.save(link);
            }
        }

        auditService.invoiceRecorded(null, saved.getId(), saved.getInvoiceNumber(),
                saved.getInvoiceAmount());

        // Consumption is posted from the invoice, never typed by hand (REQ-B5)
        budgetService.postConsumption(saved);
        log.info("Invoice {} saved for package {} - budget consumption posted",
                saved.getInvoiceNumber(), packageId);
        return saved;
    }

    public List<Invoice> invoices(Long packageId) {
        return stageDataService.findContract(packageId)
                .map(c -> invoiceRepository.findByContractId(c.getId()))
                .orElseGet(List::of);
    }

    /**
     * What the contract has been billed so far, ignoring one invoice - so an edit to an
     * existing invoice is measured against its siblings rather than against itself.
     */
    private BigDecimal cumulativeBilledExcluding(Long contractId, Long invoiceId) {
        return invoiceRepository.findByContractId(contractId).stream()
                .filter(i -> invoiceId == null || !invoiceId.equals(i.getId()))
                .map(i -> i.getInvoiceAmount() == null ? BigDecimal.ZERO : i.getInvoiceAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // -------------------------------------------------------- Stage 14: payments

    @Transactional
    public Payment savePayment(Long packageId, Payment payment, List<Long> invoiceIds) {
        Contract contract = requireContract(packageId);
        payment.setContractId(contract.getId());

        if (validationService.mismatchedCurrency(contract, payment.getCurrency())) {
            throw new IllegalArgumentException("Payment currency " + payment.getCurrency()
                    + " differs from the contract currency " + contract.getCurrency()
                    + " - a package is single-currency (REQ-B7)");
        }
        // A payment cannot settle more than the invoices it is attached to (Q-12, REQ-14.2)
        BigDecimal wouldPay = cumulativePaidExcluding(contract.getId(), payment.getId())
                .add(payment.getPaymentAmount() == null ? BigDecimal.ZERO : payment.getPaymentAmount());
        BigDecimal billed = cumulativeBilledExcluding(contract.getId(), null);
        if (wouldPay.compareTo(billed) > 0) {
            String message = "Cumulative payments " + wouldPay.toPlainString()
                    + " would exceed the invoiced total " + billed.toPlainString()
                    + " - over-payment is not permitted (REQ-14.2)";
            auditService.refused(null, ProcurementAuditService.PAYMENT, contract.getId(),
                    "Payment " + payment.getVoucherNumber() + " rejected as over-payment", message);
            throw new IllegalArgumentException(message);
        }

        Payment saved = paymentRepository.save(payment);
        auditService.paymentRecorded(null, saved.getId(), saved.getVoucherNumber(),
                saved.getPaymentAmount());

        if (invoiceIds != null) {
            paymentInvoiceLinkRepository.deleteAll(
                    paymentInvoiceLinkRepository.findByPaymentId(saved.getId()));
            BigDecimal each = invoiceIds.isEmpty() || saved.getPaymentAmount() == null
                    ? null
                    : saved.getPaymentAmount().divide(
                            BigDecimal.valueOf(invoiceIds.size()), 2, java.math.RoundingMode.HALF_UP);
            for (Long invoiceId : invoiceIds) {
                PaymentInvoiceLink link = new PaymentInvoiceLink();
                link.setPaymentId(saved.getId());
                link.setInvoiceId(invoiceId);
                link.setApportionedAmount(each);
                paymentInvoiceLinkRepository.save(link);
            }
        }
        return saved;
    }

    public List<Payment> payments(Long packageId) {
        return stageDataService.findContract(packageId)
                .map(c -> paymentRepository.findByContractId(c.getId()))
                .orElseGet(List::of);
    }

    /** What the contract has been paid so far, ignoring one payment (see the invoice twin). */
    private BigDecimal cumulativePaidExcluding(Long contractId, Long paymentId) {
        return paymentRepository.findByContractId(contractId).stream()
                .filter(p -> paymentId == null || !paymentId.equals(p.getId()))
                .map(p -> p.getPaymentAmount() == null ? BigDecimal.ZERO : p.getPaymentAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Outstanding balance per invoice, so part payments are visible (REQ-14.3). */
    public BigDecimal outstanding(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
        BigDecimal paid = paymentInvoiceLinkRepository.findByInvoiceId(invoiceId).stream()
                .map(l -> l.getApportionedAmount() == null ? BigDecimal.ZERO : l.getApportionedAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal amount = invoice.getInvoiceAmount() == null ? BigDecimal.ZERO : invoice.getInvoiceAmount();
        return amount.subtract(paid);
    }

    private Contract requireContract(Long packageId) {
        return stageDataService.findContract(packageId).orElseThrow(() ->
                new IllegalStateException(
                        "This package has no contract yet - complete Contract Signing (stage 8) first"));
    }
}

package com.bpdb.dms.procurement.service;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bpdb.dms.entity.DocumentTypeField;
import com.bpdb.dms.procurement.entity.BaseProcurementEntity;
import com.bpdb.dms.procurement.entity.Contract;
import com.bpdb.dms.procurement.entity.ContractApproval;
import com.bpdb.dms.procurement.entity.ContractClosure;
import com.bpdb.dms.procurement.entity.ContractPackage;
import com.bpdb.dms.procurement.entity.Evaluation;
import com.bpdb.dms.procurement.entity.ExtractedField;
import com.bpdb.dms.procurement.entity.LetterOfCredit;
import com.bpdb.dms.procurement.entity.Noa;
import com.bpdb.dms.procurement.entity.PerformanceSecurity;
import com.bpdb.dms.procurement.entity.ProcurementPackage;
import com.bpdb.dms.procurement.entity.ProductionSchedule;
import com.bpdb.dms.procurement.entity.Tender;
import com.bpdb.dms.procurement.entity.TenderOpening;
import com.bpdb.dms.procurement.entity.Warranty;
import com.bpdb.dms.procurement.repository.ContractApprovalRepository;
import com.bpdb.dms.procurement.repository.ContractClosureRepository;
import com.bpdb.dms.procurement.repository.ContractPackageRepository;
import com.bpdb.dms.procurement.repository.ContractRepository;
import com.bpdb.dms.procurement.repository.EvaluationRepository;
import com.bpdb.dms.procurement.repository.LetterOfCreditRepository;
import com.bpdb.dms.procurement.repository.NoaRepository;
import com.bpdb.dms.procurement.repository.PerformanceSecurityRepository;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;
import com.bpdb.dms.procurement.repository.ProductionScheduleRepository;
import com.bpdb.dms.procurement.repository.TenderOpeningRepository;
import com.bpdb.dms.procurement.repository.TenderRepository;
import com.bpdb.dms.procurement.repository.WarrantyRepository;

/**
 * Writes a stage's values into both places they have to live: the typed graph column
 * and the provenance record, in one transaction (REQ-P4).
 *
 * Which entity and column a field maps to comes from the catalogue, so this class has
 * no per-field knowledge - only the handful of rules for creating a stage's row if it
 * does not exist yet.
 */
@Service
public class StageDataService {

    private static final Logger log = LoggerFactory.getLogger(StageDataService.class);

    private final CaptureService captureService;
    private final StageDefinitionService definitions;
    private final ProcurementPackageRepository packageRepository;
    private final TenderRepository tenderRepository;
    private final TenderOpeningRepository openingRepository;
    private final EvaluationRepository evaluationRepository;
    private final ContractApprovalRepository approvalRepository;
    private final NoaRepository noaRepository;
    private final PerformanceSecurityRepository psRepository;
    private final ContractRepository contractRepository;
    private final ContractPackageRepository contractPackageRepository;
    private final LetterOfCreditRepository lcRepository;
    private final ProductionScheduleRepository productionScheduleRepository;
    private final WarrantyRepository warrantyRepository;
    private final ContractClosureRepository closureRepository;

    public StageDataService(CaptureService captureService,
                            StageDefinitionService definitions,
                            ProcurementPackageRepository packageRepository,
                            TenderRepository tenderRepository,
                            TenderOpeningRepository openingRepository,
                            EvaluationRepository evaluationRepository,
                            ContractApprovalRepository approvalRepository,
                            NoaRepository noaRepository,
                            PerformanceSecurityRepository psRepository,
                            ContractRepository contractRepository,
                            ContractPackageRepository contractPackageRepository,
                            LetterOfCreditRepository lcRepository,
                            ProductionScheduleRepository productionScheduleRepository,
                            WarrantyRepository warrantyRepository,
                            ContractClosureRepository closureRepository) {
        this.captureService = captureService;
        this.definitions = definitions;
        this.packageRepository = packageRepository;
        this.tenderRepository = tenderRepository;
        this.openingRepository = openingRepository;
        this.evaluationRepository = evaluationRepository;
        this.approvalRepository = approvalRepository;
        this.noaRepository = noaRepository;
        this.psRepository = psRepository;
        this.contractRepository = contractRepository;
        this.contractPackageRepository = contractPackageRepository;
        this.lcRepository = lcRepository;
        this.productionScheduleRepository = productionScheduleRepository;
        this.warrantyRepository = warrantyRepository;
        this.closureRepository = closureRepository;
    }

    /**
     * Save a stage form: values keyed by catalogue field key.
     * Manual values are confirmed on save; OCR values arrive through captureFromOcr.
     */
    @Transactional
    public List<ExtractedField> saveStageValues(Long packageId, short stageCode,
                                                Map<String, String> values, Long userId) {
        List<DocumentTypeField> catalogue = definitions.catalogueFields(stageCode);
        Map<String, Object> entityCache = new HashMap<>();
        List<ExtractedField> saved = new java.util.ArrayList<>();

        for (DocumentTypeField def : catalogue) {
            if (!values.containsKey(def.getFieldKey())) {
                continue;
            }
            String value = values.get(def.getFieldKey());
            Object entity = entityCache.computeIfAbsent(def.getEntityType(),
                    type -> ensureEntity(type, packageId));
            if (entity == null) {
                log.warn("No entity resolver for {} - value for {} recorded without a typed column",
                        def.getEntityType(), def.getFieldKey());
            } else if (rewritesTheCorrelationKey(def, entity, value)) {
                // Package Number is the key every stage, document, budget line and expiry
                // tracker hangs off (REQ-L2). Capturing a different one must not silently
                // rename the package out from under all of them - a mismatch is something
                // to resolve, the way REQ-2.1 treats it at Stage 2, not something to apply.
                log.warn("Captured Package Number '{}' differs from package {}'s own number "
                        + "'{}' - the package keeps its number and the reading is recorded",
                        value, packageId, ((ProcurementPackage) entity).getPackageNumber());
            } else {
                applyToEntity(entity, def, value);
            }

            CaptureService.CaptureRequest req = new CaptureService.CaptureRequest();
            req.entityType = def.getEntityType();
            req.entityId = entity instanceof BaseProcurementEntity be ? be.getId() : null;
            req.packageId = packageId;
            req.stageCode = stageCode;
            req.fieldKey = def.getFieldKey();
            req.fieldLabel = def.getFieldLabel();
            req.dataType = def.getFieldType();
            req.rawValue = value;
            req.mandatory = def.getIsMandatory();
            saved.add(captureService.captureManual(req, userId));
        }

        // persist the typed rows after all values applied
        entityCache.values().stream().filter(java.util.Objects::nonNull).forEach(this::persist);
        return saved;
    }

    /**
     * Create the row a stage writes into if it does not exist yet. Rows are created
     * lazily - a stage the user has not reached has no empty record sitting in the way.
     */
    @Transactional
    public Object ensureEntity(String entityType, Long packageId) {
        if (entityType == null) {
            return null;
        }
        switch (entityType) {
            case "PACKAGE":
                return packageRepository.findById(packageId).orElse(null);

            case "TENDER": {
                // The current attempt. A failed attempt is superseded, never written into
                // again, so the form always lands on the live tender (Q-2, REQ-L15).
                return tenderRepository.findByPackageIdAndIsCurrentTrue(packageId).orElseGet(() -> {
                    Tender t = new Tender();
                    t.setPackageId(packageId);
                    t.setAttemptNo(1);
                    t.setIsCurrent(Boolean.TRUE);
                    return tenderRepository.save(t);
                });
            }
            case "TENDER_OPENING": {
                Tender tender = (Tender) ensureEntity("TENDER", packageId);
                return openingRepository.findByTenderId(tender.getId()).orElseGet(() -> {
                    TenderOpening o = new TenderOpening();
                    o.setTenderId(tender.getId());
                    return openingRepository.save(o);
                });
            }
            case "EVALUATION": {
                TenderOpening opening = (TenderOpening) ensureEntity("TENDER_OPENING", packageId);
                return evaluationRepository.findByOpeningId(opening.getId()).orElseGet(() -> {
                    Evaluation e = new Evaluation();
                    e.setOpeningId(opening.getId());
                    return evaluationRepository.save(e);
                });
            }
            case "CONTRACT_APPROVAL": {
                return approvalRepository.findByPackageId(packageId).orElseGet(() -> {
                    ContractApproval a = new ContractApproval();
                    a.setPackageId(packageId);
                    return approvalRepository.save(a);
                });
            }
            case "NOA": {
                ContractApproval approval = (ContractApproval) ensureEntity("CONTRACT_APPROVAL", packageId);
                return noaRepository.findByApprovalId(approval.getId()).orElseGet(() -> {
                    Noa n = new Noa();
                    n.setApprovalId(approval.getId());
                    return noaRepository.save(n);
                });
            }
            case "PERFORMANCE_SECURITY": {
                Noa noa = (Noa) ensureEntity("NOA", packageId);
                List<PerformanceSecurity> existing = psRepository.findByNoaId(noa.getId());
                if (!existing.isEmpty()) {
                    return existing.get(0);
                }
                PerformanceSecurity ps = new PerformanceSecurity();
                ps.setNoaId(noa.getId());
                return psRepository.save(ps);
            }
            case "CONTRACT": {
                Optional<Contract> existing = findContract(packageId);
                if (existing.isPresent()) {
                    return existing.get();
                }
                Noa noa = (Noa) ensureEntity("NOA", packageId);
                Contract c = new Contract();
                c.setNoaId(noa.getId());
                // placeholder until the user supplies the real number in the same save
                c.setContractNumber("DRAFT-" + packageId + "-" + System.nanoTime());
                Contract savedContract = contractRepository.save(c);
                linkContractToPackage(savedContract.getId(), packageId);
                return savedContract;
            }
            case "LETTER_OF_CREDIT": {
                Contract contract = (Contract) ensureEntity("CONTRACT", packageId);
                List<LetterOfCredit> lcs = lcRepository.findByContractId(contract.getId());
                if (!lcs.isEmpty()) {
                    return lcs.get(0);
                }
                LetterOfCredit lc = new LetterOfCredit();
                lc.setContractId(contract.getId());
                lc.setIsCurrent(Boolean.TRUE);
                return lcRepository.save(lc);
            }
            case "PRODUCTION_SCHEDULE": {
                Contract contract = (Contract) ensureEntity("CONTRACT", packageId);
                List<ProductionSchedule> schedules =
                        productionScheduleRepository.findByContractId(contract.getId());
                if (!schedules.isEmpty()) {
                    return schedules.get(0);
                }
                ProductionSchedule s = new ProductionSchedule();
                s.setContractId(contract.getId());
                return productionScheduleRepository.save(s);
            }
            case "WARRANTY": {
                Contract contract = (Contract) ensureEntity("CONTRACT", packageId);
                return warrantyRepository.findByContractId(contract.getId()).orElseGet(() -> {
                    Warranty w = new Warranty();
                    w.setContractId(contract.getId());
                    return warrantyRepository.save(w);
                });
            }
            case "CONTRACT_CLOSURE": {
                Contract contract = (Contract) ensureEntity("CONTRACT", packageId);
                return closureRepository.findByContractId(contract.getId()).orElseGet(() -> {
                    ContractClosure cc = new ContractClosure();
                    cc.setContractId(contract.getId());
                    return closureRepository.save(cc);
                });
            }
            default:
                // BER_BIDDER, DELIVERY, INVOICE, PAYMENT and INSPECTION_EVENT are repeating
                // rows - they are created explicitly, not by form save.
                return null;
        }
    }

    /**
     * Attach a contract to its package.
     *
     * A contract covers exactly one package (client answer Q-1 - the option for one
     * contract spanning several APP packages was not selected). The link table stays,
     * because lot-wise tendering is where multi-package contracts tend to appear later
     * and re-adding it would be the same work twice; the rule is enforced here instead
     * of by a database constraint, so relaxing it is a one-line change.
     */
    @Transactional
    public ContractPackage linkContractToPackage(Long contractId, Long packageId) {
        List<ContractPackage> existing = contractPackageRepository.findByContractId(contractId);
        for (ContractPackage link : existing) {
            if (link.getPackageId().equals(packageId)) {
                return link;
            }
        }
        if (!existing.isEmpty()) {
            throw new IllegalStateException("Contract " + contractId + " is already linked to package "
                    + existing.get(0).getPackageId()
                    + " - a contract covers exactly one package (Q-1)");
        }
        ContractPackage cp = new ContractPackage();
        cp.setContractId(contractId);
        cp.setPackageId(packageId);
        cp.setIsPrimary(Boolean.TRUE);
        return contractPackageRepository.save(cp);
    }

    public Optional<Contract> findContract(Long packageId) {
        return contractPackageRepository.findByPackageId(packageId).stream()
                .sorted((a, b) -> Boolean.compare(
                        Boolean.TRUE.equals(b.getIsPrimary()), Boolean.TRUE.equals(a.getIsPrimary())))
                .findFirst()
                .flatMap(cp -> contractRepository.findById(cp.getContractId()));
    }

    /**
     * Would applying this value rename the package?
     *
     * <p>True only when the field is the package number, the package already has one, and
     * the captured value differs. A package that has not been given a number yet — created
     * from a form before Stage 1 was captured — still takes the value.
     */
    private boolean rewritesTheCorrelationKey(DocumentTypeField def, Object entity, String value) {
        if (!(entity instanceof ProcurementPackage pkg)) {
            return false;
        }
        if (!"packageNumber".equals(def.getEntityColumn())) {
            return false;
        }
        String current = pkg.getPackageNumber();
        return current != null && !current.isBlank()
                && value != null && !value.isBlank()
                && !current.trim().equalsIgnoreCase(value.trim());
    }

    /** Set the catalogue-named property, coercing the text to the property's type. */
    private void applyToEntity(Object entity, DocumentTypeField def, String value) {
        String property = def.getEntityColumn();
        if (property == null || property.isBlank()) {
            return;
        }
        String setter = "set" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
        for (Method m : entity.getClass().getMethods()) {
            if (!m.getName().equals(setter) || m.getParameterCount() != 1) {
                continue;
            }
            try {
                m.invoke(entity, coerce(m.getParameterTypes()[0], value));
                return;
            } catch (Exception e) {
                log.warn("Could not set {}.{} from \"{}\": {}",
                        entity.getClass().getSimpleName(), property, value, e.getMessage());
                return;
            }
        }
        log.warn("No setter {} on {}", setter, entity.getClass().getSimpleName());
    }

    private Object coerce(Class<?> target, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        if (target == String.class) {
            return v;
        }
        if (target == BigDecimal.class) {
            return new BigDecimal(v.replace(",", ""));
        }
        if (target == Integer.class || target == int.class) {
            return Integer.valueOf(v.replace(",", ""));
        }
        if (target == Short.class || target == short.class) {
            return Short.valueOf(v);
        }
        if (target == Long.class || target == long.class) {
            return Long.valueOf(v.replace(",", ""));
        }
        if (target == Boolean.class || target == boolean.class) {
            String lower = v.toLowerCase();
            return lower.startsWith("y") || lower.startsWith("t") || lower.equals("1");
        }
        if (target == LocalDate.class) {
            return LocalDate.parse(v);
        }
        throw new IllegalArgumentException("Unsupported field type " + target.getSimpleName());
    }

    private void persist(Object entity) {
        if (entity instanceof ProcurementPackage p) {
            packageRepository.save(p);
        } else if (entity instanceof Tender t) {
            tenderRepository.save(t);
        } else if (entity instanceof TenderOpening o) {
            openingRepository.save(o);
        } else if (entity instanceof Evaluation e) {
            evaluationRepository.save(e);
        } else if (entity instanceof ContractApproval a) {
            approvalRepository.save(a);
        } else if (entity instanceof Noa n) {
            noaRepository.save(n);
        } else if (entity instanceof PerformanceSecurity ps) {
            psRepository.save(ps);
        } else if (entity instanceof Contract c) {
            contractRepository.save(c);
        } else if (entity instanceof LetterOfCredit lc) {
            lcRepository.save(lc);
        } else if (entity instanceof ProductionSchedule s) {
            productionScheduleRepository.save(s);
        } else if (entity instanceof Warranty w) {
            warrantyRepository.save(w);
        } else if (entity instanceof ContractClosure cc) {
            closureRepository.save(cc);
        }
    }
}

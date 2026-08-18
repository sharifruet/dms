package com.bpdb.dms.procurement.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.bpdb.dms.procurement.entity.InspectionEvent;
import com.bpdb.dms.procurement.entity.StageDocumentRequirement;
import com.bpdb.dms.procurement.repository.InspectionEventRepository;

/**
 * Which conditional documents a particular package has switched on.
 *
 * <p>Most required documents are the same for every package, and the catalogue says so.
 * A few are required only when something about this package makes them so — the SAT
 * Report at Stage 11 is the case the requirements name (REQ-11.3): optional in general,
 * mandatory once a user declares it applies.
 *
 * <p>The catalogue marks those rows {@code is_conditional}, which until now meant "never
 * blocks". That is right until the condition is met and wrong afterwards, which is the
 * whole point of the flag.
 */
@Service
public class ConditionalRequirementService {

    /** Site Acceptance Test report, Stage 11. */
    public static final String SAT_REPORT = "SAT_REPORT";

    private final InspectionEventRepository inspectionRepository;
    private final StageDataService stageDataService;

    public ConditionalRequirementService(InspectionEventRepository inspectionRepository,
                                         StageDataService stageDataService) {
        this.inspectionRepository = inspectionRepository;
        this.stageDataService = stageDataService;
    }

    /**
     * The conditional document roles that are live for this package at this stage, and so
     * must be treated as mandatory.
     */
    public Set<String> activeConditionalRoles(Long packageId, short stageCode) {
        Set<String> active = new HashSet<>();
        if (stageCode == 11 && satApplicable(packageId)) {
            active.add(SAT_REPORT);
        }
        return active;
    }

    /**
     * Has anyone declared that this contract needs a Site Acceptance Test?
     *
     * <p>The declaration lives on the inspection event because that is where the user
     * makes it. One event declaring it applicable is enough — SAT applies to the contract,
     * not to a single inspection.
     */
    public boolean satApplicable(Long packageId) {
        return inspections(packageId).stream()
                .anyMatch(e -> Boolean.TRUE.equals(e.getSatApplicable()));
    }

    /** Whether the declared SAT has actually been carried out, for the warning text. */
    public boolean satDone(Long packageId) {
        return inspections(packageId).stream()
                .filter(e -> Boolean.TRUE.equals(e.getSatApplicable()))
                .anyMatch(e -> Boolean.TRUE.equals(e.getSatDone()));
    }

    /**
     * Fold the active conditional requirements into the blocking set, so the caller has a
     * single list to check documents against.
     */
    public List<StageDocumentRequirement> blockingWithConditionals(
            List<StageDocumentRequirement> allRequirements, Set<String> activeRoles) {
        return allRequirements.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsMandatory())
                        || activeRoles.contains(r.getDocRole()))
                .filter(r -> !Boolean.TRUE.equals(r.getIsConditional())
                        || activeRoles.contains(r.getDocRole()))
                .toList();
    }

    private List<InspectionEvent> inspections(Long packageId) {
        return stageDataService.findContract(packageId)
                .map(c -> inspectionRepository.findByContractId(c.getId()))
                .orElseGet(List::of);
    }
}

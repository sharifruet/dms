package com.bpdb.dms.procurement.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.bpdb.dms.entity.DocumentTypeField;
import com.bpdb.dms.procurement.entity.StageDocumentRequirement;
import com.bpdb.dms.procurement.repository.StageDocumentRequirementRepository;
import com.bpdb.dms.repository.DocumentTypeFieldRepository;

/**
 * Answers "what does stage N need?" entirely from configuration.
 *
 * Required documents come from stage_document_requirement and the field catalogue
 * from document_type_fields. Adding a field or a document to a stage is a data
 * change, never a code change (REQ-P13, REQ-P14).
 */
@Service
public class StageDefinitionService {

    /** The lifecycle is 16 stages, run in order (WF-01). */
    public static final short FIRST_STAGE = 1;
    public static final short LAST_STAGE = 16;

    private static final String[] STAGE_NAMES = {
        "", "APP Approved", "Tender Advertisement", "Tender Opening", "Tender Evaluation",
        "Contract Approval", "Notification of Award (NOA)", "Performance Security",
        "Contract Signing", "Letter of Credit (LC)", "Manufacturing / Supply", "Inspection",
        "Delivery", "Bill Submission", "Payment", "Warranty Period", "Contract Close"
    };

    private final StageDocumentRequirementRepository requirementRepository;
    private final DocumentTypeFieldRepository fieldRepository;

    public StageDefinitionService(StageDocumentRequirementRepository requirementRepository,
                                  DocumentTypeFieldRepository fieldRepository) {
        this.requirementRepository = requirementRepository;
        this.fieldRepository = fieldRepository;
    }

    public String stageName(short stageCode) {
        if (stageCode < FIRST_STAGE || stageCode > LAST_STAGE) {
            return "Unknown stage " + stageCode;
        }
        return STAGE_NAMES[stageCode];
    }

    public boolean isValidStage(short stageCode) {
        return stageCode >= FIRST_STAGE && stageCode <= LAST_STAGE;
    }

    public List<StageDocumentRequirement> requiredDocuments(short stageCode) {
        return requirementRepository.findByStageCodeAndIsActiveTrueOrderByDisplayOrderAsc(stageCode);
    }

    /** Documents that block completion: mandatory and not conditional (WF-09). */
    public List<StageDocumentRequirement> blockingDocuments(short stageCode) {
        return requiredDocuments(stageCode).stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsMandatory()))
                .filter(r -> !Boolean.TRUE.equals(r.getIsConditional()))
                .collect(Collectors.toList());
    }

    /** The field catalogue for a stage, across all its document types. */
    public List<DocumentTypeField> catalogueFields(short stageCode) {
        List<DocumentTypeField> all = fieldRepository.findAll();
        List<DocumentTypeField> forStage = new ArrayList<>();
        for (DocumentTypeField f : all) {
            if (f.getStageCode() != null && f.getStageCode() == stageCode
                    && Boolean.TRUE.equals(f.getIsActive())) {
                forStage.add(f);
            }
        }
        forStage.sort((a, b) -> {
            int ao = a.getDisplayOrder() == null ? 0 : a.getDisplayOrder();
            int bo = b.getDisplayOrder() == null ? 0 : b.getDisplayOrder();
            return Integer.compare(ao, bo);
        });
        return forStage;
    }

    public List<DocumentTypeField> mandatoryFields(short stageCode) {
        return catalogueFields(stageCode).stream()
                .filter(f -> Boolean.TRUE.equals(f.getIsMandatory()))
                .collect(Collectors.toList());
    }

    /** Field definitions to apply when a document of this type is uploaded. */
    public List<DocumentTypeField> fieldsForDocumentType(String documentType) {
        return fieldRepository.findAll().stream()
                .filter(f -> documentType != null && documentType.equalsIgnoreCase(f.getDocumentType()))
                .filter(f -> Boolean.TRUE.equals(f.getIsActive()))
                // Pre-revamp rows (018/030) share document_type but have no entity_type;
                // extracted_field cannot store them (REQ-P13 catalogue is stage-aware).
                .filter(f -> f.getEntityType() != null && !f.getEntityType().isBlank())
                .collect(Collectors.toList());
    }
}

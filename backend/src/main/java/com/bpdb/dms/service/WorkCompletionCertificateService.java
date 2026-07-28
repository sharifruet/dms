package com.bpdb.dms.service;

import com.bpdb.dms.dto.WorkCompletionCertificateDto;
import com.bpdb.dms.repository.WorkCompletionCertificateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for Work Completion Certificate type-specific records.
 */
@Service
@Transactional
public class WorkCompletionCertificateService {

    private final WorkCompletionCertificateRepository workCompletionCertificateRepository;

    public WorkCompletionCertificateService(WorkCompletionCertificateRepository workCompletionCertificateRepository) {
        this.workCompletionCertificateRepository = workCompletionCertificateRepository;
    }

    public List<WorkCompletionCertificateDto> findAll() {
        return workCompletionCertificateRepository.findAll().stream()
            .map(WorkCompletionCertificateDto::fromEntity)
            .collect(Collectors.toList());
    }

    public Optional<WorkCompletionCertificateDto> findById(Long id) {
        return workCompletionCertificateRepository.findById(id)
            .map(WorkCompletionCertificateDto::fromEntity);
    }

    public Optional<WorkCompletionCertificateDto> findByDocumentId(Long documentId) {
        return workCompletionCertificateRepository.findByDocumentId(documentId)
            .map(WorkCompletionCertificateDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public long count() {
        return workCompletionCertificateRepository.count();
    }

    public WorkCompletionCertificateDto create(WorkCompletionCertificateDto dto) {
        throw new UnsupportedOperationException("WorkCompletionCertificateService.create is not implemented yet");
    }

    public WorkCompletionCertificateDto update(Long id, WorkCompletionCertificateDto dto) {
        throw new UnsupportedOperationException("WorkCompletionCertificateService.update is not implemented yet");
    }

    public void delete(Long id) {
        throw new UnsupportedOperationException("WorkCompletionCertificateService.delete is not implemented yet");
    }

    public void deleteByDocumentId(Long documentId) {
        workCompletionCertificateRepository.deleteByDocumentId(documentId);
    }
}

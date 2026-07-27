package com.bpdb.dms.service;

import com.bpdb.dms.dto.PerformanceSecurityDto;
import com.bpdb.dms.repository.PerformanceSecurityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for Performance Security type-specific records.
 * Implementation to be extended as needed.
 */
@Service
@Transactional
public class PerformanceSecurityService {

    private final PerformanceSecurityRepository performanceSecurityRepository;

    public PerformanceSecurityService(PerformanceSecurityRepository performanceSecurityRepository) {
        this.performanceSecurityRepository = performanceSecurityRepository;
    }

    public List<PerformanceSecurityDto> findAll() {
        return performanceSecurityRepository.findAll().stream()
            .map(PerformanceSecurityDto::fromEntity)
            .collect(Collectors.toList());
    }

    public Optional<PerformanceSecurityDto> findById(Long id) {
        return performanceSecurityRepository.findById(id).map(PerformanceSecurityDto::fromEntity);
    }

    public Optional<PerformanceSecurityDto> findByDocumentId(Long documentId) {
        return performanceSecurityRepository.findByDocumentId(documentId).map(PerformanceSecurityDto::fromEntity);
    }

    public PerformanceSecurityDto create(PerformanceSecurityDto dto) {
        throw new UnsupportedOperationException("PerformanceSecurityService.create is not implemented yet");
    }

    public PerformanceSecurityDto update(Long id, PerformanceSecurityDto dto) {
        throw new UnsupportedOperationException("PerformanceSecurityService.update is not implemented yet");
    }

    public void delete(Long id) {
        throw new UnsupportedOperationException("PerformanceSecurityService.delete is not implemented yet");
    }

    public void deleteByDocumentId(Long documentId) {
        performanceSecurityRepository.deleteByDocumentId(documentId);
    }
}

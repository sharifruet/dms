package com.bpdb.dms.service;

import com.bpdb.dms.dto.PerformanceGuaranteeDto;
import com.bpdb.dms.repository.PerformanceGuaranteeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for Performance Guarantee type-specific records.
 * Implementation to be extended as needed.
 */
@Service
@Transactional
public class PerformanceGuaranteeService {

    private final PerformanceGuaranteeRepository performanceGuaranteeRepository;

    public PerformanceGuaranteeService(PerformanceGuaranteeRepository performanceGuaranteeRepository) {
        this.performanceGuaranteeRepository = performanceGuaranteeRepository;
    }

    public List<PerformanceGuaranteeDto> findAll() {
        return performanceGuaranteeRepository.findAll().stream()
            .map(PerformanceGuaranteeDto::fromEntity)
            .collect(Collectors.toList());
    }

    public Optional<PerformanceGuaranteeDto> findById(Long id) {
        return performanceGuaranteeRepository.findById(id).map(PerformanceGuaranteeDto::fromEntity);
    }

    public Optional<PerformanceGuaranteeDto> findByDocumentId(Long documentId) {
        return performanceGuaranteeRepository.findByDocumentId(documentId).map(PerformanceGuaranteeDto::fromEntity);
    }

    public PerformanceGuaranteeDto create(PerformanceGuaranteeDto dto) {
        throw new UnsupportedOperationException("PerformanceGuaranteeService.create is not implemented yet");
    }

    public PerformanceGuaranteeDto update(Long id, PerformanceGuaranteeDto dto) {
        throw new UnsupportedOperationException("PerformanceGuaranteeService.update is not implemented yet");
    }

    public void delete(Long id) {
        throw new UnsupportedOperationException("PerformanceGuaranteeService.delete is not implemented yet");
    }

    public void deleteByDocumentId(Long documentId) {
        performanceGuaranteeRepository.deleteByDocumentId(documentId);
    }
}

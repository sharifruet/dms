package com.bpdb.dms.service;

import com.bpdb.dms.dto.BankGuaranteeDto;
import com.bpdb.dms.repository.BankGuaranteeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for Bank Guarantee type-specific records.
 * Implementation to be extended as needed.
 */
@Service
@Transactional
public class BankGuaranteeService {

    private final BankGuaranteeRepository bankGuaranteeRepository;

    public BankGuaranteeService(BankGuaranteeRepository bankGuaranteeRepository) {
        this.bankGuaranteeRepository = bankGuaranteeRepository;
    }

    public List<BankGuaranteeDto> findAll() {
        return bankGuaranteeRepository.findAll().stream()
            .map(BankGuaranteeDto::fromEntity)
            .collect(Collectors.toList());
    }

    public Optional<BankGuaranteeDto> findById(Long id) {
        return bankGuaranteeRepository.findById(id).map(BankGuaranteeDto::fromEntity);
    }

    public Optional<BankGuaranteeDto> findByDocumentId(Long documentId) {
        return bankGuaranteeRepository.findByDocumentId(documentId).map(BankGuaranteeDto::fromEntity);
    }

    public BankGuaranteeDto create(BankGuaranteeDto dto) {
        throw new UnsupportedOperationException("BankGuaranteeService.create is not implemented yet");
    }

    public BankGuaranteeDto update(Long id, BankGuaranteeDto dto) {
        throw new UnsupportedOperationException("BankGuaranteeService.update is not implemented yet");
    }

    public void delete(Long id) {
        throw new UnsupportedOperationException("BankGuaranteeService.delete is not implemented yet");
    }

    public void deleteByDocumentId(Long documentId) {
        bankGuaranteeRepository.deleteByDocumentId(documentId);
    }
}

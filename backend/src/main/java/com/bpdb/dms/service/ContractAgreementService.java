package com.bpdb.dms.service;

import com.bpdb.dms.dto.ContractAgreementDto;
import com.bpdb.dms.repository.ContractAgreementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for Contract Agreement type-specific records.
 * Implementation to be extended as needed.
 */
@Service
@Transactional
public class ContractAgreementService {

    private final ContractAgreementRepository contractAgreementRepository;

    public ContractAgreementService(ContractAgreementRepository contractAgreementRepository) {
        this.contractAgreementRepository = contractAgreementRepository;
    }

    public List<ContractAgreementDto> findAll() {
        return contractAgreementRepository.findAll().stream()
            .map(ContractAgreementDto::fromEntity)
            .collect(Collectors.toList());
    }

    public Optional<ContractAgreementDto> findById(Long id) {
        return contractAgreementRepository.findById(id).map(ContractAgreementDto::fromEntity);
    }

    public Optional<ContractAgreementDto> findByDocumentId(Long documentId) {
        return contractAgreementRepository.findByDocumentId(documentId).map(ContractAgreementDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public long count() {
        return contractAgreementRepository.count();
    }

    @Transactional(readOnly = true)
    public long countRunningContracts() {
        return contractAgreementRepository.countRunningContracts(LocalDate.now());
    }

    public ContractAgreementDto create(ContractAgreementDto dto) {
        throw new UnsupportedOperationException("ContractAgreementService.create is not implemented yet");
    }

    public ContractAgreementDto update(Long id, ContractAgreementDto dto) {
        throw new UnsupportedOperationException("ContractAgreementService.update is not implemented yet");
    }

    public void delete(Long id) {
        throw new UnsupportedOperationException("ContractAgreementService.delete is not implemented yet");
    }

    public void deleteByDocumentId(Long documentId) {
        contractAgreementRepository.deleteByDocumentId(documentId);
    }
}

package com.bpdb.dms.service;

import com.bpdb.dms.dto.BillDocumentDto;
import com.bpdb.dms.repository.BillDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for Bill document type-specific records.
 * Separate from {@link BillService} finance billing tables.
 * Implementation to be extended as needed.
 */
@Service
@Transactional
public class BillDocumentService {

    private final BillDocumentRepository billDocumentRepository;

    public BillDocumentService(BillDocumentRepository billDocumentRepository) {
        this.billDocumentRepository = billDocumentRepository;
    }

    public List<BillDocumentDto> findAll() {
        return billDocumentRepository.findAll().stream()
            .map(BillDocumentDto::fromEntity)
            .collect(Collectors.toList());
    }

    public Optional<BillDocumentDto> findById(Long id) {
        return billDocumentRepository.findById(id).map(BillDocumentDto::fromEntity);
    }

    public Optional<BillDocumentDto> findByDocumentId(Long documentId) {
        return billDocumentRepository.findByDocumentId(documentId).map(BillDocumentDto::fromEntity);
    }

    public BillDocumentDto create(BillDocumentDto dto) {
        throw new UnsupportedOperationException("BillDocumentService.create is not implemented yet");
    }

    public BillDocumentDto update(Long id, BillDocumentDto dto) {
        throw new UnsupportedOperationException("BillDocumentService.update is not implemented yet");
    }

    public void delete(Long id) {
        throw new UnsupportedOperationException("BillDocumentService.delete is not implemented yet");
    }

    public void deleteByDocumentId(Long documentId) {
        billDocumentRepository.deleteByDocumentId(documentId);
    }
}

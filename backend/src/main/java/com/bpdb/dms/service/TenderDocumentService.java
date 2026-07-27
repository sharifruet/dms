package com.bpdb.dms.service;

import com.bpdb.dms.dto.TenderDocumentDto;
import com.bpdb.dms.repository.TenderDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for Tender Document type-specific records.
 * Implementation to be extended as needed.
 */
@Service
@Transactional
public class TenderDocumentService {

    private final TenderDocumentRepository tenderDocumentRepository;

    public TenderDocumentService(TenderDocumentRepository tenderDocumentRepository) {
        this.tenderDocumentRepository = tenderDocumentRepository;
    }

    public List<TenderDocumentDto> findAll() {
        return tenderDocumentRepository.findAll().stream()
            .map(TenderDocumentDto::fromEntity)
            .collect(Collectors.toList());
    }

    public Optional<TenderDocumentDto> findById(Long id) {
        return tenderDocumentRepository.findById(id).map(TenderDocumentDto::fromEntity);
    }

    public Optional<TenderDocumentDto> findByDocumentId(Long documentId) {
        return tenderDocumentRepository.findByDocumentId(documentId).map(TenderDocumentDto::fromEntity);
    }

    public TenderDocumentDto create(TenderDocumentDto dto) {
        throw new UnsupportedOperationException("TenderDocumentService.create is not implemented yet");
    }

    public TenderDocumentDto update(Long id, TenderDocumentDto dto) {
        throw new UnsupportedOperationException("TenderDocumentService.update is not implemented yet");
    }

    public void delete(Long id) {
        throw new UnsupportedOperationException("TenderDocumentService.delete is not implemented yet");
    }

    public void deleteByDocumentId(Long documentId) {
        tenderDocumentRepository.deleteByDocumentId(documentId);
    }
}

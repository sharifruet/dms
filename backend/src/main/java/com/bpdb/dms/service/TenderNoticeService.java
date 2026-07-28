package com.bpdb.dms.service;

import com.bpdb.dms.dto.TenderNoticeDto;
import com.bpdb.dms.repository.TenderNoticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for Tender Notice type-specific records.
 * Implementation to be extended as needed.
 */
@Service
@Transactional
public class TenderNoticeService {

    private final TenderNoticeRepository tenderNoticeRepository;

    public TenderNoticeService(TenderNoticeRepository tenderNoticeRepository) {
        this.tenderNoticeRepository = tenderNoticeRepository;
    }

    public List<TenderNoticeDto> findAll() {
        return tenderNoticeRepository.findAll().stream()
            .map(TenderNoticeDto::fromEntity)
            .collect(Collectors.toList());
    }

    public Optional<TenderNoticeDto> findById(Long id) {
        return tenderNoticeRepository.findById(id).map(TenderNoticeDto::fromEntity);
    }

    public Optional<TenderNoticeDto> findByDocumentId(Long documentId) {
        return tenderNoticeRepository.findByDocumentId(documentId).map(TenderNoticeDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public long countLiveTenders() {
        return tenderNoticeRepository.countLiveTenders(LocalDate.now());
    }

    public TenderNoticeDto create(TenderNoticeDto dto) {
        throw new UnsupportedOperationException("TenderNoticeService.create is not implemented yet");
    }

    public TenderNoticeDto update(Long id, TenderNoticeDto dto) {
        throw new UnsupportedOperationException("TenderNoticeService.update is not implemented yet");
    }

    public void delete(Long id) {
        throw new UnsupportedOperationException("TenderNoticeService.delete is not implemented yet");
    }

    public void deleteByDocumentId(Long documentId) {
        tenderNoticeRepository.deleteByDocumentId(documentId);
    }
}

package com.bpdb.dms.service;

import com.bpdb.dms.dto.CorrespondenceDto;
import com.bpdb.dms.repository.CorrespondenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for Correspondence type-specific records.
 * Implementation to be extended as needed.
 */
@Service
@Transactional
public class CorrespondenceService {

    private final CorrespondenceRepository correspondenceRepository;

    public CorrespondenceService(CorrespondenceRepository correspondenceRepository) {
        this.correspondenceRepository = correspondenceRepository;
    }

    public List<CorrespondenceDto> findAll() {
        return correspondenceRepository.findAll().stream()
            .map(CorrespondenceDto::fromEntity)
            .collect(Collectors.toList());
    }

    public Optional<CorrespondenceDto> findById(Long id) {
        return correspondenceRepository.findById(id).map(CorrespondenceDto::fromEntity);
    }

    public Optional<CorrespondenceDto> findByDocumentId(Long documentId) {
        return correspondenceRepository.findByDocumentId(documentId).map(CorrespondenceDto::fromEntity);
    }

    public CorrespondenceDto create(CorrespondenceDto dto) {
        throw new UnsupportedOperationException("CorrespondenceService.create is not implemented yet");
    }

    public CorrespondenceDto update(Long id, CorrespondenceDto dto) {
        throw new UnsupportedOperationException("CorrespondenceService.update is not implemented yet");
    }

    public void delete(Long id) {
        throw new UnsupportedOperationException("CorrespondenceService.delete is not implemented yet");
    }

    public void deleteByDocumentId(Long documentId) {
        correspondenceRepository.deleteByDocumentId(documentId);
    }
}

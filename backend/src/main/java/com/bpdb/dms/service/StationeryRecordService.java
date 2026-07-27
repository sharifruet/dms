package com.bpdb.dms.service;

import com.bpdb.dms.dto.StationeryRecordDto;
import com.bpdb.dms.repository.StationeryRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for Stationery Record type-specific records.
 * Implementation to be extended as needed.
 */
@Service
@Transactional
public class StationeryRecordService {

    private final StationeryRecordRepository stationeryRecordRepository;

    public StationeryRecordService(StationeryRecordRepository stationeryRecordRepository) {
        this.stationeryRecordRepository = stationeryRecordRepository;
    }

    public List<StationeryRecordDto> findAll() {
        return stationeryRecordRepository.findAll().stream()
            .map(StationeryRecordDto::fromEntity)
            .collect(Collectors.toList());
    }

    public Optional<StationeryRecordDto> findById(Long id) {
        return stationeryRecordRepository.findById(id).map(StationeryRecordDto::fromEntity);
    }

    public Optional<StationeryRecordDto> findByDocumentId(Long documentId) {
        return stationeryRecordRepository.findByDocumentId(documentId).map(StationeryRecordDto::fromEntity);
    }

    public List<StationeryRecordDto> findByEmployeeId(Long employeeId) {
        return stationeryRecordRepository.findByEmployeeId(employeeId).stream()
            .map(StationeryRecordDto::fromEntity)
            .collect(Collectors.toList());
    }

    public StationeryRecordDto create(StationeryRecordDto dto) {
        throw new UnsupportedOperationException("StationeryRecordService.create is not implemented yet");
    }

    public StationeryRecordDto update(Long id, StationeryRecordDto dto) {
        throw new UnsupportedOperationException("StationeryRecordService.update is not implemented yet");
    }

    public void delete(Long id) {
        throw new UnsupportedOperationException("StationeryRecordService.delete is not implemented yet");
    }

    public void deleteByDocumentId(Long documentId) {
        stationeryRecordRepository.deleteByDocumentId(documentId);
    }
}

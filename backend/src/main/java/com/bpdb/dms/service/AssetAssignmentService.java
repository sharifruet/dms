package com.bpdb.dms.service;

import com.bpdb.dms.entity.AssetAssignment;
import com.bpdb.dms.repository.AssetAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AssetAssignmentService {
    @Autowired
    private AssetAssignmentRepository assetAssignmentRepository;

    public Page<AssetAssignment> list(Pageable pageable) {
        return assetAssignmentRepository.findAll(pageable);
    }

    public Optional<AssetAssignment> get(Long id) {
        return assetAssignmentRepository.findById(id);
    }

    public AssetAssignment create(AssetAssignment assignment) {
        return assetAssignmentRepository.save(assignment);
    }

    public AssetAssignment update(Long id, AssetAssignment updated) {
        updated.setId(id);
        return assetAssignmentRepository.save(updated);
    }

    public void delete(Long id) {
        assetAssignmentRepository.deleteById(id);
    }
}

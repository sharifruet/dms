package com.bpdb.dms.service;

import com.bpdb.dms.entity.ProcurementPackage;
import com.bpdb.dms.repository.ProcurementPackageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProcurementPackageService {

    @Autowired
    private ProcurementPackageRepository procurementPackageRepository;

    public Page<ProcurementPackage> list(Pageable pageable) {
        return procurementPackageRepository.findAll(pageable);
    }

    public Optional<ProcurementPackage> get(Long id) {
        return procurementPackageRepository.findById(id);
    }

    public ProcurementPackage create(ProcurementPackage procurementPackage) {
        return procurementPackageRepository.save(procurementPackage);
    }

    public ProcurementPackage update(Long id, ProcurementPackage updated) {
        updated.setId(id);
        return procurementPackageRepository.save(updated);
    }

    public void delete(Long id) {
        procurementPackageRepository.deleteById(id);
    }
}

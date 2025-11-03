package com.bpdb.dms.service;

import com.bpdb.dms.entity.Asset;
import com.bpdb.dms.repository.AssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AssetService {
    @Autowired
    private AssetRepository assetRepository;

    public Page<Asset> list(Pageable pageable) {
        return assetRepository.findAll(pageable);
    }

    public Optional<Asset> get(Long id) {
        return assetRepository.findById(id);
    }

    public Asset create(Asset asset) {
        return assetRepository.save(asset);
    }

    public Asset update(Long id, Asset updated) {
        updated.setId(id);
        return assetRepository.save(updated);
    }

    public void delete(Long id) {
        assetRepository.deleteById(id);
    }
}

package com.bpdb.dms.procurement.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.bpdb.dms.procurement.entity.Contract;
import com.bpdb.dms.procurement.entity.DocumentLink;
import com.bpdb.dms.procurement.entity.ProcurementPackage;
import com.bpdb.dms.procurement.repository.ContractPackageRepository;
import com.bpdb.dms.procurement.repository.ContractRepository;
import com.bpdb.dms.procurement.repository.DocumentLinkRepository;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;
import com.bpdb.dms.service.DocumentIndexingService.SearchResultItem;

/**
 * Puts the procurement context back onto a search hit (REQ-L9).
 *
 * <p>Search returns documents; the lifecycle thinks in packages. Without this a user
 * searching for "GRL-24" gets a list of filenames and has to open each one to find out
 * which package it belongs to — which is precisely the document-centric behaviour the
 * revamp exists to replace.
 *
 * <p>Written as a decorator over whatever the search returned, rather than as a change to
 * the index: the index is Elasticsearch and the linkage is relational, and keeping package
 * numbers correct in two stores is a synchronisation problem nobody needs. Looking them up
 * per result set costs one query per page of hits.
 */
@Service
public class SearchContextService {

    private final DocumentLinkRepository linkRepository;
    private final ProcurementPackageRepository packageRepository;
    private final ContractPackageRepository contractPackageRepository;
    private final ContractRepository contractRepository;
    private final StageDefinitionService definitions;

    public SearchContextService(DocumentLinkRepository linkRepository,
                                ProcurementPackageRepository packageRepository,
                                ContractPackageRepository contractPackageRepository,
                                ContractRepository contractRepository,
                                StageDefinitionService definitions) {
        this.linkRepository = linkRepository;
        this.packageRepository = packageRepository;
        this.contractPackageRepository = contractPackageRepository;
        this.contractRepository = contractRepository;
        this.definitions = definitions;
    }

    /** Fill in package, contract and stage on each hit that has them. */
    public void decorate(List<SearchResultItem> results) {
        if (results == null || results.isEmpty()) {
            return;
        }
        Map<Long, PackageContext> byPackage = new HashMap<>();

        for (SearchResultItem item : results) {
            if (item.getDocumentId() == null) {
                continue;
            }
            Optional<DocumentLink> link = linkRepository.findByDocumentId(item.getDocumentId())
                    .stream().findFirst();
            if (link.isEmpty() || link.get().getPackageId() == null) {
                continue; // not a procurement document, or not linked yet (REQ-L10)
            }
            DocumentLink l = link.get();
            PackageContext context = byPackage.computeIfAbsent(
                    l.getPackageId(), this::contextFor);

            item.setPackageId(l.getPackageId());
            item.setPackageNumber(context.packageNumber);
            item.setContractNumber(context.contractNumber);
            if (l.getStageCode() != null) {
                item.setStageCode(l.getStageCode());
                item.setStageName(definitions.stageName(l.getStageCode()));
            }
        }
    }

    /** The package numbers matching a search term, so a query can be widened to them. */
    public Set<Long> packagesMatching(String term) {
        Set<Long> ids = new HashSet<>();
        if (term == null || term.isBlank()) {
            return ids;
        }
        for (ProcurementPackage pkg : packageRepository.findAll()) {
            if (pkg.getPackageNumber() != null
                    && pkg.getPackageNumber().toLowerCase().contains(term.toLowerCase())) {
                ids.add(pkg.getId());
            }
        }
        return ids;
    }

    /** Documents belonging to the given packages, for widening a search by package number. */
    public List<Long> documentIdsForPackages(Set<Long> packageIds) {
        List<Long> documentIds = new ArrayList<>();
        for (Long packageId : packageIds) {
            for (DocumentLink link : linkRepository.findByPackageId(packageId)) {
                if (link.getDocumentId() != null) {
                    documentIds.add(link.getDocumentId());
                }
            }
        }
        return documentIds;
    }

    private PackageContext contextFor(Long packageId) {
        PackageContext context = new PackageContext();
        packageRepository.findById(packageId)
                .ifPresent(pkg -> context.packageNumber = pkg.getPackageNumber());
        contractPackageRepository.findByPackageId(packageId).stream().findFirst()
                .flatMap(cp -> contractRepository.findById(cp.getContractId()))
                .map(Contract::getContractNumber)
                .ifPresent(number -> context.contractNumber = number);
        return context;
    }

    private static class PackageContext {
        private String packageNumber;
        private String contractNumber;
    }
}

package com.bpdb.dms.repository;

import com.bpdb.dms.entity.SmartFolderDefinition;
import com.bpdb.dms.entity.SmartFolderScope;
import com.bpdb.dms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SmartFolderDefinitionRepository extends JpaRepository<SmartFolderDefinition, Long> {
    List<SmartFolderDefinition> findByOwner(User owner);
    List<SmartFolderDefinition> findByScope(SmartFolderScope scope);
    List<SmartFolderDefinition> findByIsActiveTrue();
}



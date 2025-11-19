package com.bpdb.dms.repository;

import com.bpdb.dms.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Folder entity
 */
@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    
    /**
     * Find folders by parent folder (null for root folders)
     */
    List<Folder> findByParentFolderIsNull();
    
    /**
     * Find folders by parent folder
     */
    List<Folder> findByParentFolder(Folder parentFolder);
    
    /**
     * Find folders by parent folder ID
     */
    List<Folder> findByParentFolderId(Long parentFolderId);
    
    /**
     * Find root folders (no parent)
     */
    List<Folder> findByParentFolderIsNullAndIsActiveTrue();
    
    /**
     * Find folders by department
     */
    List<Folder> findByDepartment(String department);
    
    /**
     * Find active folders
     */
    List<Folder> findByIsActiveTrue();
    
    /**
     * Find folder by path
     */
    Optional<Folder> findByFolderPath(String folderPath);
    
    /**
     * Find folders by name (case-insensitive)
     */
    List<Folder> findByNameIgnoreCase(String name);
    
    /**
     * Find folders by name and parent
     */
    Optional<Folder> findByNameAndParentFolder(String name, Folder parentFolder);
    
    /**
     * Check if folder has subfolders
     */
    @Query("SELECT COUNT(f) > 0 FROM Folder f WHERE f.parentFolder = :folder")
    boolean hasSubFolders(@Param("folder") Folder folder);
    
    /**
     * Get folder tree starting from root
     */
    @Query("SELECT f FROM Folder f WHERE f.parentFolder IS NULL AND f.isActive = true ORDER BY f.name")
    List<Folder> findRootFolders();
    
    /**
     * Get all subfolders recursively (for a given folder)
     */
    @Query("SELECT f FROM Folder f WHERE f.folderPath LIKE :pathPattern AND f.isActive = true")
    List<Folder> findSubFoldersRecursive(@Param("pathPattern") String pathPattern);
}


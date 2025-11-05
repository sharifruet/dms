package com.bpdb.dms.performance;

import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.DocumentType;
import com.bpdb.dms.entity.Role;
import com.bpdb.dms.entity.Role.RoleType;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.RoleRepository;
import com.bpdb.dms.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DocumentPerformanceTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        Role officerRole = new Role();
        officerRole.setName(RoleType.OFFICER);
        officerRole = roleRepository.save(officerRole);
        
        testUser = new User();
        testUser.setUsername("perftest");
        testUser.setEmail("perf@example.com");
        testUser.setPassword("password");
        testUser.setRole(officerRole);
        testUser.setIsActive(true);
        testUser = userRepository.save(testUser);
    }

    @Test
    void bulkDocumentInsert_PerformanceTest() {
        // Given
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Document doc = new Document();
            doc.setFileName("perf-test-" + i + ".pdf");
            doc.setFilePath("/uploads/perf-test-" + i + ".pdf");
            doc.setDocumentType(DocumentType.OTHER);
            doc.setUploadedBy(testUser);
            doc.setCreatedAt(LocalDateTime.now());
            doc.setIsActive(true);
            documents.add(doc);
        }

        // When
        long startTime = System.currentTimeMillis();
        documentRepository.saveAll(documents);
        long endTime = System.currentTimeMillis();

        // Then
        long executionTime = endTime - startTime;
        System.out.println("Bulk insert of 1000 documents took: " + executionTime + "ms");
        
        // Performance assertion: Should complete within 5 seconds
        assertTrue(executionTime < 5000, "Bulk insert took too long: " + executionTime + "ms");
    }

    @Test
    void documentSearch_PerformanceTest() {
        // Given - Create test documents
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Document doc = new Document();
            doc.setFileName("search-test-" + i + ".pdf");
            doc.setFilePath("/uploads/search-test-" + i + ".pdf");
            doc.setDocumentType(DocumentType.OTHER);
            doc.setUploadedBy(testUser);
            doc.setCreatedAt(LocalDateTime.now());
            doc.setIsActive(true);
            documents.add(doc);
        }
        documentRepository.saveAll(documents);

        // When
        long startTime = System.currentTimeMillis();
        Pageable pageable = PageRequest.of(0, 100);
        var results = documentRepository.findByFileNameContaining("search-test", pageable);
        long endTime = System.currentTimeMillis();

        // Then
        long executionTime = endTime - startTime;
        System.out.println("Search for documents took: " + executionTime + "ms");
        
        // Performance assertion: Should complete within 1 second
        assertTrue(executionTime < 1000, "Search took too long: " + executionTime + "ms");
        assertTrue(results.getTotalElements() >= 100, "Search results incomplete");
    }

    @Test
    void documentPagination_PerformanceTest() {
        // Given - Create test documents
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            Document doc = new Document();
            doc.setFileName("page-test-" + i + ".pdf");
            doc.setFilePath("/uploads/page-test-" + i + ".pdf");
            doc.setDocumentType(DocumentType.OTHER);
            doc.setUploadedBy(testUser);
            doc.setCreatedAt(LocalDateTime.now());
            doc.setIsActive(true);
            documents.add(doc);
        }
        documentRepository.saveAll(documents);

        // When
        long startTime = System.currentTimeMillis();
        var page1 = documentRepository.findByIsActiveTrue(
            org.springframework.data.domain.PageRequest.of(0, 50)
        );
        var page2 = documentRepository.findByIsActiveTrue(
            org.springframework.data.domain.PageRequest.of(1, 50)
        );
        long endTime = System.currentTimeMillis();

        // Then
        long executionTime = endTime - startTime;
        System.out.println("Pagination query took: " + executionTime + "ms");
        
        // Performance assertion: Should complete within 500ms
        assertTrue(executionTime < 500, "Pagination took too long: " + executionTime + "ms");
        assertTrue(page1.getContent().size() <= 50, "Page size incorrect");
        assertTrue(page2.getContent().size() <= 50, "Page size incorrect");
    }
}

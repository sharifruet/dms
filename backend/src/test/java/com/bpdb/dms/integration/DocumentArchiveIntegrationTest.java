package com.bpdb.dms.integration;

import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for Document Archive and Restore feature
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class DocumentArchiveIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Document testDocument;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setIsActive(true);
        testUser = userRepository.save(testUser);

        // Create test document
        testDocument = new Document();
        testDocument.setFileName("test.pdf");
        testDocument.setFilePath("/uploads/test.pdf");
        testDocument.setDocumentType("OTHER");
        testDocument.setUploadedBy(testUser);
        testDocument.setIsActive(true);
        testDocument.setIsArchived(false);
        testDocument = documentRepository.save(testDocument);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "OFFICER")
    void archiveDocument_IntegrationTest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/documents/" + testDocument.getId() + "/archive")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isArchived").value(true))
                .andExpect(jsonPath("$.archivedAt").exists());

        // Verify document is archived
        var archivedDocument = documentRepository.findById(testDocument.getId());
        assertTrue(archivedDocument.isPresent());
        assertTrue(archivedDocument.get().getIsArchived());
        assertNotNull(archivedDocument.get().getArchivedAt());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "OFFICER")
    void restoreArchivedDocument_IntegrationTest() throws Exception {
        // Given - Archive document first
        testDocument.setIsArchived(true);
        testDocument.setArchivedAt(LocalDateTime.now());
        documentRepository.save(testDocument);

        // When & Then
        mockMvc.perform(post("/api/documents/" + testDocument.getId() + "/restore-archive")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isArchived").value(false));

        // Verify document is restored
        var restoredDocument = documentRepository.findById(testDocument.getId());
        assertTrue(restoredDocument.isPresent());
        assertFalse(restoredDocument.get().getIsArchived());
        assertNull(restoredDocument.get().getArchivedAt());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "OFFICER")
    void deleteDocument_IntegrationTest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/documents/" + testDocument.getId() + "/delete")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false))
                .andExpect(jsonPath("$.deletedAt").exists());

        // Verify document is soft deleted
        var deletedDocument = documentRepository.findById(testDocument.getId());
        assertTrue(deletedDocument.isPresent());
        assertFalse(deletedDocument.get().getIsActive());
        assertNotNull(deletedDocument.get().getDeletedAt());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "VIEWER")
    void getArchivedDocuments_IntegrationTest() throws Exception {
        // Given - Create archived document
        Document archivedDoc = new Document();
        archivedDoc.setFileName("archived.pdf");
        archivedDoc.setFilePath("/uploads/archived.pdf");
        archivedDoc.setDocumentType("OTHER");
        archivedDoc.setUploadedBy(testUser);
        archivedDoc.setIsActive(true);
        archivedDoc.setIsArchived(true);
        archivedDoc.setArchivedAt(LocalDateTime.now());
        documentRepository.save(archivedDoc);

        // When & Then
        mockMvc.perform(get("/api/documents/archived")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].isArchived").value(true));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "VIEWER")
    void getArchiveStatistics_IntegrationTest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/documents/archive/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivedCount").exists())
                .andExpect(jsonPath("$.deletedCount").exists())
                .andExpect(jsonPath("$.activeCount").exists());
    }
}


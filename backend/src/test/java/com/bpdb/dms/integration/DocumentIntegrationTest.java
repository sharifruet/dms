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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class DocumentIntegrationTest {

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
        testDocument.setDocumentType("PDF");
        testDocument.setUploadedBy(testUser);
        testDocument.setCreatedAt(LocalDateTime.now());
        testDocument.setUpdatedAt(LocalDateTime.now());
        testDocument.setIsActive(true);
        testDocument = documentRepository.save(testDocument);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "OFFICER")
    void uploadDocument_IntegrationTest() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "integration-test.pdf",
            "application/pdf",
            "Integration test PDF content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/documents/upload")
                .file(file)
                .param("documentType", "BILL")
                .param("description", "Integration Test Document")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("integration-test.pdf"))
                .andExpect(jsonPath("$.documentType").value("BILL"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getDocuments_IntegrationTest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/documents")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].fileName").value("test.pdf"));
    }

    @Test
    @WithMockUser(roles = "OFFICER")
    void deleteDocument_IntegrationTest() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/documents/" + testDocument.getId())
                .with(csrf()))
                .andExpect(status().isOk());

        // Verify document is soft deleted
        var deletedDocument = documentRepository.findById(testDocument.getId());
        assert deletedDocument.isPresent();
        assert !deletedDocument.get().getIsActive();
    }
}

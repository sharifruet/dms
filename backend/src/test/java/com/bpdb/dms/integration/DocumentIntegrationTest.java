package com.bpdb.dms.integration;

import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.RoleRepository;
import com.bpdb.dms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DocumentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User testUser;
    private Document testDocument;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setRole(com.bpdb.dms.support.TestRoles.officer(roleRepository));
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
    @WithMockUser(username = "testuser", authorities = {"ROLE_OFFICER", "PERM_DOCUMENT_VIEW", "PERM_DOCUMENT_UPLOAD", "PERM_DOCUMENT_DELETE"})
    void uploadDocument_IntegrationTest() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "integration-test.pdf",
            "application/pdf",
            "Integration test PDF content".getBytes()
        );

        // OTHER, not BILL: Phase 2 made BILL a follow-up document that must land in a
        // folder already carrying a tender workflow. That gate is worth testing, but not
        // here - this test is about the upload endpoint itself, and FileUploadServiceTest
        // already covers the workflow-gated types.
        mockMvc.perform(multipart("/api/documents/upload")
                .file(file)
                .param("documentType", "OTHER")
                .param("description", "Integration Test Document")
                .with(csrf()))
                .andExpect(status().isOk())
                // fileName is the generated storage name (timestamp + hash); the name the
                // user uploaded is preserved separately as originalName
                .andExpect(jsonPath("$.originalName").value("integration-test.pdf"))
                .andExpect(jsonPath("$.documentType").value("OTHER"));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_VIEWER", "PERM_DOCUMENT_VIEW"})
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
    // username matters: the endpoint resolves the principal against the user table, and
    // the default "user" from @WithMockUser was never created by setUp
    @WithMockUser(username = "testuser", authorities = {"ROLE_OFFICER", "PERM_DOCUMENT_VIEW", "PERM_DOCUMENT_UPLOAD", "PERM_DOCUMENT_DELETE"})
    void deleteDocument_IntegrationTest() throws Exception {
        // Deletion is a soft delete and is exposed as POST /{id}/delete, not DELETE /{id}
        // - the record is retained with is_active = false rather than removed
        mockMvc.perform(post("/api/documents/" + testDocument.getId() + "/delete")
                .with(csrf()))
                .andExpect(status().isOk());

        // Verify document is soft deleted
        var deletedDocument = documentRepository.findById(testDocument.getId());
        assert deletedDocument.isPresent();
        assert !deletedDocument.get().getIsActive();
    }
}

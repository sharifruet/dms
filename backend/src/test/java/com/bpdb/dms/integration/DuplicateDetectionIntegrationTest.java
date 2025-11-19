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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for Duplicate Detection feature
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class DuplicateDetectionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private byte[] testFileContent;
    private String testFileHash;

    @BeforeEach
    void setUp() throws Exception {
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setIsActive(true);
        testUser = userRepository.save(testUser);

        // Create test file content and hash
        testFileContent = "Test duplicate detection content".getBytes(StandardCharsets.UTF_8);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(testFileContent);
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        testFileHash = hexString.toString();
    }

    @Test
    @WithMockUser(username = "testuser", roles = "OFFICER")
    void uploadDuplicateFile_ShouldDetectDuplicate() throws Exception {
        // Given - Create an existing document with the same hash
        Document existingDocument = new Document();
        existingDocument.setFileName("existing.pdf");
        existingDocument.setFilePath("/uploads/existing.pdf");
        existingDocument.setDocumentType("OTHER");
        existingDocument.setFileHash(testFileHash);
        existingDocument.setUploadedBy(testUser);
        existingDocument.setIsActive(true);
        existingDocument = documentRepository.save(existingDocument);

        // When - Upload the same file
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "duplicate.pdf",
            "application/pdf",
            testFileContent
        );

        // Then - Should detect duplicate
        mockMvc.perform(multipart("/api/documents/upload")
                .file(file)
                .param("documentType", "OTHER")
                .param("description", "Duplicate test")
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isDuplicate").value(true))
                .andExpect(jsonPath("$.duplicateDocumentId").value(existingDocument.getId()))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("identical content")));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "OFFICER")
    void handleDuplicateUpload_AsVersion() throws Exception {
        // Given - Create an existing document
        Document existingDocument = new Document();
        existingDocument.setFileName("existing.pdf");
        existingDocument.setFilePath("/uploads/existing.pdf");
        existingDocument.setDocumentType("OTHER");
        existingDocument.setFileHash(testFileHash);
        existingDocument.setUploadedBy(testUser);
        existingDocument.setIsActive(true);
        existingDocument = documentRepository.save(existingDocument);

        // When - Upload as version
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "duplicate.pdf",
            "application/pdf",
            testFileContent
        );

        mockMvc.perform(multipart("/api/documents/upload-duplicate")
                .file(file)
                .param("documentType", "OTHER")
                .param("duplicateDocumentId", existingDocument.getId().toString())
                .param("action", "version")
                .param("description", "New version")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "OFFICER")
    void handleDuplicateUpload_Replace() throws Exception {
        // Given - Create an existing document
        Document existingDocument = new Document();
        existingDocument.setFileName("existing.pdf");
        existingDocument.setFilePath("/uploads/existing.pdf");
        existingDocument.setDocumentType("OTHER");
        existingDocument.setFileHash(testFileHash);
        existingDocument.setUploadedBy(testUser);
        existingDocument.setIsActive(true);
        existingDocument = documentRepository.save(existingDocument);

        // When - Replace document
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "replacement.pdf",
            "application/pdf",
            testFileContent
        );

        mockMvc.perform(multipart("/api/documents/upload-duplicate")
                .file(file)
                .param("documentType", "OTHER")
                .param("duplicateDocumentId", existingDocument.getId().toString())
                .param("action", "replace")
                .with(csrf()))
                .andExpect(status().isOk());
    }
}


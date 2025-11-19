package com.bpdb.dms.integration;

import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.DocumentRelationship;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.DocumentRelationshipRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for Document Relationships feature
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class DocumentRelationshipsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentRelationshipRepository relationshipRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Document sourceDocument;
    private Document targetDocument;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setIsActive(true);
        testUser = userRepository.save(testUser);

        // Create source document
        sourceDocument = new Document();
        sourceDocument.setFileName("source.pdf");
        sourceDocument.setFilePath("/uploads/source.pdf");
        sourceDocument.setDocumentType("CONTRACT_AGREEMENT");
        sourceDocument.setUploadedBy(testUser);
        sourceDocument.setIsActive(true);
        sourceDocument = documentRepository.save(sourceDocument);

        // Create target document
        targetDocument = new Document();
        targetDocument.setFileName("target.pdf");
        targetDocument.setFilePath("/uploads/target.pdf");
        targetDocument.setDocumentType("BANK_GUARANTEE_BG");
        targetDocument.setUploadedBy(testUser);
        targetDocument.setIsActive(true);
        targetDocument = documentRepository.save(targetDocument);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "OFFICER")
    void createRelationship_IntegrationTest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/documents/" + sourceDocument.getId() + "/relationships")
                .contentType("application/json")
                .content("""
                    {
                        "targetDocumentId": %d,
                        "relationshipType": "CONTRACT_TO_BG",
                        "description": "Test relationship"
                    }
                    """.formatted(targetDocument.getId()))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relationshipType").value("CONTRACT_TO_BG"))
                .andExpect(jsonPath("$.sourceDocument.id").value(sourceDocument.getId()))
                .andExpect(jsonPath("$.targetDocument.id").value(targetDocument.getId()));

        // Verify relationship was created
        var relationships = relationshipRepository.findAllRelationshipsForDocument(sourceDocument);
        assertTrue(relationships.size() > 0);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "VIEWER")
    void getRelationships_IntegrationTest() throws Exception {
        // Create a relationship first
        DocumentRelationship relationship = new DocumentRelationship();
        relationship.setSourceDocument(sourceDocument);
        relationship.setTargetDocument(targetDocument);
        relationship.setRelationshipType(com.bpdb.dms.entity.DocumentRelationshipType.CONTRACT_TO_BG);
        relationship.setCreatedBy(testUser);
        relationshipRepository.save(relationship);

        // When & Then
        mockMvc.perform(get("/api/documents/" + sourceDocument.getId() + "/relationships"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relationships").isArray())
                .andExpect(jsonPath("$.relationships[0].relationshipType").value("CONTRACT_TO_BG"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "OFFICER")
    void deleteRelationship_IntegrationTest() throws Exception {
        // Create a relationship first
        DocumentRelationship relationship = new DocumentRelationship();
        relationship.setSourceDocument(sourceDocument);
        relationship.setTargetDocument(targetDocument);
        relationship.setRelationshipType(com.bpdb.dms.entity.DocumentRelationshipType.CONTRACT_TO_BG);
        relationship.setCreatedBy(testUser);
        relationship = relationshipRepository.save(relationship);

        // When & Then
        mockMvc.perform(delete("/api/documents/" + sourceDocument.getId() + "/relationships/" + relationship.getId())
                .with(csrf()))
                .andExpect(status().isOk());

        // Verify relationship was deleted
        assertFalse(relationshipRepository.findById(relationship.getId()).isPresent());
    }
}


package com.bpdb.dms.integration;

import com.bpdb.dms.entity.Role;
import com.bpdb.dms.entity.Role.RoleType;
import com.bpdb.dms.repository.RoleRepository;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for DD1-DD4 role upload permissions
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DDRolePermissionsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private com.bpdb.dms.repository.UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Create DD roles if they don't exist
        roleRepository.findByName(RoleType.DD1).orElseGet(() -> {
            Role role = new Role();
            role.setName(RoleType.DD1);
            role.setDisplayName("DD1");
            role.setIsActive(true);
            return roleRepository.save(role);
        });

        roleRepository.findByName(RoleType.DD2).orElseGet(() -> {
            Role role = new Role();
            role.setName(RoleType.DD2);
            role.setDisplayName("DD2");
            role.setIsActive(true);
            return roleRepository.save(role);
        });

        roleRepository.findByName(RoleType.DD3).orElseGet(() -> {
            Role role = new Role();
            role.setName(RoleType.DD3);
            role.setDisplayName("DD3");
            role.setIsActive(true);
            return roleRepository.save(role);
        });

        roleRepository.findByName(RoleType.DD4).orElseGet(() -> {
            Role role = new Role();
            role.setName(RoleType.DD4);
            role.setDisplayName("DD4");
            role.setIsActive(true);
            return roleRepository.save(role);
        });

        // The upload endpoint resolves the authenticated principal against the user table,
        // so @WithMockUser alone is not enough - each dd*user must actually exist.
        ensureUser("dd1user", RoleType.DD1);
        ensureUser("dd2user", RoleType.DD2);
        ensureUser("dd3user", RoleType.DD3);
        ensureUser("dd4user", RoleType.DD4);
    }

    private void ensureUser(String username, RoleType roleType) {
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }
        com.bpdb.dms.entity.User user = new com.bpdb.dms.entity.User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("password");
        user.setIsActive(true);
        user.setRole(com.bpdb.dms.support.TestRoles.ensure(roleRepository, roleType));
        userRepository.save(user);
    }

    @Test
    @WithMockUser(username = "dd1user", authorities = {"ROLE_DD1", "PERM_DOCUMENT_VIEW", "PERM_DOCUMENT_UPLOAD"})
    void dd1User_CanUploadDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "Test content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                .file(file)
                .param("documentType", "OTHER")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "dd2user", authorities = {"ROLE_DD2", "PERM_DOCUMENT_VIEW", "PERM_DOCUMENT_UPLOAD"})
    void dd2User_CanUploadDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "Test content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                .file(file)
                .param("documentType", "OTHER")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "dd3user", authorities = {"ROLE_DD3", "PERM_DOCUMENT_VIEW", "PERM_DOCUMENT_UPLOAD"})
    void dd3User_CanUploadDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "Test content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                .file(file)
                .param("documentType", "OTHER")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "dd4user", authorities = {"ROLE_DD4", "PERM_DOCUMENT_VIEW", "PERM_DOCUMENT_UPLOAD"})
    void dd4User_CanUploadDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "Test content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                .file(file)
                .param("documentType", "OTHER")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "dd1user", authorities = {"ROLE_DD1", "PERM_DOCUMENT_VIEW", "PERM_DOCUMENT_UPLOAD"})
    void dd1User_CanReprocessOCR() throws Exception {
        // This test verifies DD1-DD4 can access OCR reprocessing endpoint
        // The endpoint should be accessible (may return 404 if document doesn't exist, which is acceptable)
        int status = mockMvc.perform(post("/api/documents/1/reprocess-ocr")
                .with(csrf()))
                .andReturn()
                .getResponse()
                .getStatus();
        // Accept either OK (200) or NotFound (404) - both indicate endpoint is accessible
        assertTrue(status == 200 || status == 404, "Expected status 200 or 404, got: " + status);
    }
}


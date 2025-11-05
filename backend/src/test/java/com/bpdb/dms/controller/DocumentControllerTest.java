package com.bpdb.dms.controller;

import com.bpdb.dms.dto.FileUploadResponse;
import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.DocumentType;
import com.bpdb.dms.entity.Role;
import com.bpdb.dms.entity.Role.RoleType;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.FileUploadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileUploadService fileUploadService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private Document testDocument;
    private Role officerRole;

    @BeforeEach
    void setUp() {
        officerRole = new Role();
        officerRole.setId(2L);
        officerRole.setName(RoleType.OFFICER);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(officerRole);

        testDocument = new Document();
        testDocument.setId(1L);
        testDocument.setFileName("test.pdf");
        testDocument.setFilePath("/uploads/test.pdf");
        testDocument.setDocumentType(DocumentType.OTHER);
        testDocument.setUploadedBy(testUser);
        testDocument.setCreatedAt(LocalDateTime.now());
        testDocument.setIsActive(true);
    }

    @Test
    @WithMockUser(roles = "OFFICER")
    void uploadDocument_Success() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "Test PDF content".getBytes()
        );

        FileUploadResponse response = FileUploadResponse.success(
            testDocument.getId(),
            testDocument.getFileName(),
            testDocument.getOriginalName(),
            testDocument.getFileSize(),
            testDocument.getMimeType(),
            testDocument.getDocumentType()
        );
        when(userRepository.findByUsernameWithRole(anyString())).thenReturn(java.util.Optional.of(testUser));
        when(fileUploadService.uploadFile(any(), any(), any(), anyString()))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(multipart("/api/documents/upload")
                .file(file)
                .param("documentType", "OTHER")
                .param("description", "Test Document")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void uploadDocument_Unauthorized() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "Test PDF content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/documents/upload")
                .file(file)
                .param("documentType", "OTHER")
                .param("description", "Test Document")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OFFICER")
    void uploadDocument_InvalidFile() throws Exception {
        // Given
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file",
            "test.exe",
            "application/x-executable",
            "Executable content".getBytes()
        );

        when(userRepository.findByUsernameWithRole(anyString())).thenReturn(java.util.Optional.of(testUser));
        FileUploadResponse errorResponse = FileUploadResponse.error("Invalid file type");
        when(fileUploadService.uploadFile(any(), any(), any(), anyString()))
            .thenReturn(errorResponse);

        // When & Then
        mockMvc.perform(multipart("/api/documents/upload")
                .file(invalidFile)
                .param("documentType", "OTHER")
                .param("description", "Test Document")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}

package com.bpdb.dms.controller;

import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.DocumentType;
import com.bpdb.dms.entity.User;
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
import static org.mockito.ArgumentMatchers.anyLong;
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

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private Document testDocument;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole("OFFICER");

        testDocument = new Document();
        testDocument.setId(1L);
        testDocument.setFileName("test.pdf");
        testDocument.setFilePath("/uploads/test.pdf");
        testDocument.setDocumentType(DocumentType.PDF);
        testDocument.setUploadedBy(testUser);
        testDocument.setUploadedAt(LocalDateTime.now());
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

        when(fileUploadService.uploadFile(any(), anyLong(), any(), any(), any()))
            .thenReturn(testDocument);

        // When & Then
        mockMvc.perform(multipart("/api/documents/upload")
                .file(file)
                .param("userId", "1")
                .param("title", "Test Document")
                .param("documentType", "PDF")
                .param("department", "IT")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("test.pdf"))
                .andExpect(jsonPath("$.documentType").value("PDF"));
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
                .param("userId", "1")
                .param("title", "Test Document")
                .param("documentType", "PDF")
                .param("department", "IT")
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

        // When & Then
        mockMvc.perform(multipart("/api/documents/upload")
                .file(invalidFile)
                .param("userId", "1")
                .param("title", "Test Document")
                .param("documentType", "PDF")
                .param("department", "IT")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}

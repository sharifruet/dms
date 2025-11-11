package com.bpdb.dms.controller;

import com.bpdb.dms.dto.FileUploadResponse;
import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.DocumentCategory;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.DocumentCategoryService;
import com.bpdb.dms.service.FileUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
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
    private DocumentCategoryService documentCategoryService;

    @MockBean
    private DocumentRepository documentRepository;

    @MockBean
    private UserRepository userRepository;

    private User testUser;
    private Document testDocument;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testDocument = new Document();
        testDocument.setId(1L);
        testDocument.setFileName("test.pdf");
        testDocument.setFilePath("/uploads/test.pdf");
        testDocument.setDocumentType("BILL");
        testDocument.setUploadedBy(testUser);
        testDocument.setCreatedAt(LocalDateTime.now());
        testDocument.setUpdatedAt(LocalDateTime.now());
        testDocument.setIsActive(true);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "OFFICER")
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

        when(userRepository.findByUsernameWithRole("testuser")).thenReturn(java.util.Optional.of(testUser));
        when(documentCategoryService.ensureCategoryExists(anyString())).thenReturn(new DocumentCategory("BILL", "Bill", null));
        when(fileUploadService.uploadFile(any(), any(User.class), anyString(), any(), anyMap())).thenReturn(response);

        // When & Then
        mockMvc.perform(multipart("/api/documents/upload")
                .file(file)
                .param("documentType", "BILL")
                .param("description", "Monthly bill")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("test.pdf"))
                .andExpect(jsonPath("$.documentType").value("BILL"));
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
                .param("documentType", "BILL")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "OFFICER")
    void uploadDocument_InvalidFile() throws Exception {
        // Given
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file",
            "test.exe",
            "application/x-executable",
            "Executable content".getBytes()
        );

        FileUploadResponse errorResponse = FileUploadResponse.error("File type not supported");

        when(userRepository.findByUsernameWithRole("testuser")).thenReturn(java.util.Optional.of(testUser));
        when(documentCategoryService.ensureCategoryExists(anyString())).thenReturn(new DocumentCategory("BILL", "Bill", null));
        when(fileUploadService.uploadFile(any(), any(User.class), anyString(), any(), anyMap())).thenReturn(errorResponse);

        // When & Then
        mockMvc.perform(multipart("/api/documents/upload")
                .file(invalidFile)
                .param("documentType", "BILL")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}

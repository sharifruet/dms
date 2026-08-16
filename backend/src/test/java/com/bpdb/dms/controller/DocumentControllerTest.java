package com.bpdb.dms.controller;

import com.bpdb.dms.dto.FileUploadResponse;
import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.DocumentCategory;
import com.bpdb.dms.entity.Role;
import com.bpdb.dms.entity.Role.RoleType;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.AppDocumentEntryRepository;
import com.bpdb.dms.repository.DocumentIndexRepository;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.FolderRepository;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.DatabaseMetadataExtractionService;
import com.bpdb.dms.service.DocumentArchiveService;
import com.bpdb.dms.service.DocumentCategoryService;
import com.bpdb.dms.service.DocumentMetadataService;
import com.bpdb.dms.service.DocumentTypeFieldService;
import com.bpdb.dms.service.FileUploadService;
import com.bpdb.dms.service.StationeryTrackingService;
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

// The real rules are imported deliberately: uploadDocument_Unauthorized asserts a 403,
// and without SecurityConfig the slice falls back to Boot's default "any authenticated
// user" chain, which would let the request through and make the assertion meaningless.
@WebMvcTest(DocumentController.class)
@org.springframework.context.annotation.Import(com.bpdb.dms.security.SecurityConfig.class)
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

    // @WebMvcTest builds no repositories or services, so every collaborator the
    // controller autowires has to be mocked or the context will not start. These are the
    // ones the controller grew after this test was written.
    @MockBean
    private DocumentIndexRepository documentIndexRepository;

    @MockBean
    private DocumentArchiveService documentArchiveService;

    @MockBean
    private StationeryTrackingService stationeryTrackingService;

    @MockBean
    private AppDocumentEntryRepository appDocumentEntryRepository;

    @MockBean
    private DocumentTypeFieldService documentTypeFieldService;

    @MockBean
    private DocumentMetadataService documentMetadataService;

    @MockBean
    private FolderRepository folderRepository;

    @MockBean
    private DatabaseMetadataExtractionService databaseMetadataExtractionService;

    // The security filter chain is part of a @WebMvcTest slice, and the JWT filter pulls
    // in the user-details lookup and token utility, neither of which exists in the slice.
    @MockBean
    private com.bpdb.dms.security.JwtUtil jwtUtil;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

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
    @WithMockUser(username = "testuser", authorities = {"ROLE_OFFICER", "PERM_DOCUMENT_VIEW", "PERM_DOCUMENT_UPLOAD", "PERM_DOCUMENT_DELETE"})
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
        when(fileUploadService.uploadFile(any(), any(User.class), anyString(), any(), anyMap(), any())).thenReturn(response);

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
    @WithMockUser(authorities = {"ROLE_VIEWER", "PERM_DOCUMENT_VIEW"})
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
    @WithMockUser(username = "testuser", authorities = {"ROLE_OFFICER", "PERM_DOCUMENT_VIEW", "PERM_DOCUMENT_UPLOAD", "PERM_DOCUMENT_DELETE"})
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
        when(fileUploadService.uploadFile(any(), any(User.class), anyString(), any(), anyMap(), any())).thenReturn(errorResponse);

        // When & Then
        mockMvc.perform(multipart("/api/documents/upload")
                .file(invalidFile)
                .param("documentType", "BILL")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}

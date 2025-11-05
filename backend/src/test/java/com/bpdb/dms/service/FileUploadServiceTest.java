package com.bpdb.dms.service;

import com.bpdb.dms.dto.FileUploadResponse;
import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.DocumentType;
import com.bpdb.dms.entity.Role;
import com.bpdb.dms.entity.Role.RoleType;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OCRService ocrService;

    @Mock
    private DocumentIndexingService documentIndexingService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private FileUploadService fileUploadService;

    private User testUser;
    private Document testDocument;
    private MultipartFile testFile;

    @BeforeEach
    void setUp() {
        Role officerRole = new Role();
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

        testFile = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "Test PDF content".getBytes()
        );
    }

    @Test
    void uploadFile_Success() {
        // Given
        when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

        // When
        var result = fileUploadService.uploadFile(testFile, testUser, DocumentType.OTHER, "Test Document");

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(documentRepository, times(1)).save(any(Document.class));
    }

    @Test
    void uploadFile_InvalidFileType() {
        // Given
        MultipartFile invalidFile = new MockMultipartFile(
            "file",
            "test.exe",
            "application/x-executable",
            "Executable content".getBytes()
        );

        // When
        var result = fileUploadService.uploadFile(invalidFile, testUser, DocumentType.OTHER, "Test Document");

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    void uploadFile_FileTooLarge() {
        // Given
        byte[] largeContent = new byte[101 * 1024 * 1024]; // 101MB (exceeds default 100MB limit)
        MultipartFile largeFile = new MockMultipartFile(
            "file",
            "large.pdf",
            "application/pdf",
            largeContent
        );

        // When
        var result = fileUploadService.uploadFile(largeFile, testUser, DocumentType.OTHER, "Test Document");

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }
}

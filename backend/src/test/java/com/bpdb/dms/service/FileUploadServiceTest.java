package com.bpdb.dms.service;

import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.DocumentType;
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
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

        // When
        var result = fileUploadService.uploadFile(testFile, 1L, "Test Document", DocumentType.PDF, "IT");

        // Then
        assertNotNull(result);
        assertEquals("test.pdf", result.getFileName());
        verify(documentRepository, times(1)).save(any(Document.class));
        verify(auditService, times(1)).logActivity(anyString(), anyString(), anyString(), any());
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

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            fileUploadService.uploadFile(invalidFile, 1L, "Test Document", DocumentType.PDF, "IT");
        });
    }

    @Test
    void uploadFile_UserNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            fileUploadService.uploadFile(testFile, 1L, "Test Document", DocumentType.PDF, "IT");
        });
    }

    @Test
    void uploadFile_FileTooLarge() {
        // Given
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MultipartFile largeFile = new MockMultipartFile(
            "file",
            "large.pdf",
            "application/pdf",
            largeContent
        );

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            fileUploadService.uploadFile(largeFile, 1L, "Test Document", DocumentType.PDF, "IT");
        });
    }
}

package com.bpdb.dms.service;

import com.bpdb.dms.dto.FileUploadResponse;
import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private OCRService ocrService;

    @Mock
    private DocumentIndexingService documentIndexingService;

    @Mock
    private AuditService auditService;

    @Mock
    private AppDocumentService appDocumentService;

    @Mock
    private DocumentMetadataService documentMetadataService;

    @InjectMocks
    private FileUploadService fileUploadService;

    private User testUser;
    private MultipartFile testPdfFile;
    private MultipartFile testExcelFile;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setDepartment("Finance");

        testPdfFile = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "Test PDF content".getBytes()
        );

        testExcelFile = new MockMultipartFile(
            "file",
            "app-data.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "Excel content".getBytes()
        );
    }

    @Test
    void uploadFile_Success() {
        // Given
        AtomicReference<Document> storedDocument = new AtomicReference<>();
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.setId(1L);
            doc.setUploadedBy(testUser);
            doc.setCreatedAt(LocalDateTime.now());
            doc.setUpdatedAt(LocalDateTime.now());
            storedDocument.set(doc);
            return doc;
        });
        when(documentRepository.findById(anyLong())).thenAnswer(invocation -> Optional.ofNullable(storedDocument.get()));
        when(ocrService.isOcrAvailable()).thenReturn(false);
        when(documentMetadataService.getMetadataMap(any(Document.class))).thenReturn(Map.of());
        when(documentMetadataService.extractMetadataFromText(any(Document.class), anyString())).thenReturn(Map.of());

        // When
        var result = fileUploadService.uploadFile(testPdfFile, testUser, "BILL", "Monthly billing statement", Map.of(), null);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("BILL", result.getDocumentType());
        verify(documentRepository, times(1)).save(any(Document.class));
        verify(documentIndexingService, times(1))
            .indexDocument(any(Document.class), eq(""), eq(Map.of("ocrStatus", "unavailable")), eq(0.0), eq(0.0));
        verify(appDocumentService, never()).processAndStoreEntries(any(), any());
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
        var response = fileUploadService.uploadFile(invalidFile, testUser, "BILL", "Monthly billing statement", Map.of(), null);
        assertFalse(response.isSuccess());
        verify(documentRepository, never()).save(any(Document.class));
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

        // When & Then
        var response = fileUploadService.uploadFile(largeFile, testUser, "BILL", "Large document", Map.of(), null);
        assertFalse(response.isSuccess());
        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void uploadFile_ProcessesAppExcelEntries() {
        // Given
        AtomicReference<Document> storedDocument = new AtomicReference<>();
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.setId(5L);
            doc.setUploadedBy(testUser);
            storedDocument.set(doc);
            return doc;
        });
        when(documentRepository.findById(anyLong())).thenAnswer(invocation -> Optional.ofNullable(storedDocument.get()));
        when(appDocumentService.processAndStoreEntries(any(Document.class), eq(testExcelFile)))
            .thenReturn(Map.of("appStatus", "processed", "appEntryCount", "3"));
        when(ocrService.isOcrAvailable()).thenReturn(false);
        when(documentMetadataService.getMetadataMap(any(Document.class))).thenReturn(Map.of());
        when(documentMetadataService.extractMetadataFromText(any(Document.class), anyString())).thenReturn(Map.of());

        // When
        var result = fileUploadService.uploadFile(testExcelFile, testUser, "CONTRACT", "APP spreadsheet", Map.of(), null);

        // Then
        assertTrue(result.isSuccess());
        verify(appDocumentService, times(1)).processAndStoreEntries(any(Document.class), eq(testExcelFile));
    }
}

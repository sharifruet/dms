package com.bpdb.dms.service;

import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.FolderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
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
    private FolderRepository folderRepository;

    @Mock
    private OCRService ocrService;

    @Mock
    private DocumentIndexingService documentIndexingService;

    @Mock
    private DocumentMetadataService documentMetadataService;

    @InjectMocks
    private FileUploadService fileUploadService;

    /** Any folder id: uploading is no longer gated on the folder carrying a workflow (Q-16). */
    private static final Long FOLDER_ID = 42L;

    @TempDir
    Path tempDir;

    private User testUser;
    private MultipartFile testPdfFile;

    @BeforeEach
    void setUp() {
        // Set up temporary upload directory and max file size
        ReflectionTestUtils.setField(fileUploadService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(fileUploadService, "maxFileSize", 104857600L); // 100MB

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
        // OCR off, so no text is extracted and no metadata is derived from it - stubbing
        // extractMetadataFromText/applyManualMetadata here would go unused
        when(ocrService.isOcrAvailable()).thenReturn(false);
        when(documentMetadataService.getMetadataMap(any(Document.class))).thenReturn(Map.of());

        // When
        var result = fileUploadService.uploadFile(testPdfFile, testUser, "BILL", "Monthly billing statement", Map.of(), FOLDER_ID);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("BILL", result.getDocumentType());
        verify(documentRepository, times(1)).save(any(Document.class));
    }

    /**
     * Records what upload validation actually does with an executable: it accepts it.
     *
     * <p>This test used to assert the opposite and passed, but not for the reason it read as.
     * It uploaded a BILL with a null folder, and the tender-workflow gate rejected that before
     * anything looked at the file at all. Removing the gate with the workflow engine (Q-16)
     * took the cover away: {@code validateFile} checks emptiness and size and nothing else -
     * {@code ALLOWED_MIME_TYPES} is declared and never read, and the code says so deliberately,
     * to let APP attachments and other supporting formats through.
     *
     * <p>So the suite never had a MIME allow-list check; it had a workflow check wearing one's
     * name. Whether uploads should be restricted by type is a policy decision, not a cleanup:
     * asserting the real behaviour here keeps the gap visible instead of re-hiding it.
     */
    @Test
    void uploadFile_AcceptsAnyFileType() {
        // Given
        AtomicReference<Document> storedDocument = new AtomicReference<>();
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.setId(9L);
            doc.setUploadedBy(testUser);
            storedDocument.set(doc);
            return doc;
        });
        when(documentRepository.findById(anyLong())).thenAnswer(invocation -> Optional.ofNullable(storedDocument.get()));
        when(ocrService.isOcrAvailable()).thenReturn(false);
        when(documentMetadataService.getMetadataMap(any(Document.class))).thenReturn(Map.of());

        MultipartFile executable = new MockMultipartFile(
            "file",
            "test.exe",
            "application/x-executable",
            "Executable content".getBytes()
        );

        // When
        var response = fileUploadService.uploadFile(executable, testUser, "BILL", "Monthly billing statement", Map.of(), null);

        // Then
        assertTrue(response.isSuccess(), "upload validation does not filter by MIME type");
        verify(documentRepository, times(1)).save(any(Document.class));
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

    // uploadFile_ProcessesAppExcelEntries is gone with AppDocumentService: APP workbooks are
    // imported through the procurement Stage 1 path now, covered by AppWorkbookParserTest and
    // AppPackageImportServiceTest.
}

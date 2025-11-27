package com.bpdb.dms.service;

import com.bpdb.dms.service.OCRService.OCRResult;
import net.sourceforge.tess4j.TesseractException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for OCRService to verify Tesseract OCR functionality
 */
@SpringBootTest
@ActiveProfiles("test")
class OCRServiceTest {

    @Autowired
    private OCRService ocrService;
    
    private static final String TEST_IMAGE_PATH = "/Users/til/Downloads/test.png";

    @Test
    void testOCRServiceInitialization() {
        // Verify that OCR service is available
        assertTrue(ocrService.isOcrAvailable(), 
            "OCR service should be available after initialization");
    }

    @Test
    void testExtractTextFromTestImage() throws IOException, TesseractException {
        // Check if test image exists
        File testImageFile = new File(TEST_IMAGE_PATH);
        assertTrue(testImageFile.exists(), 
            "Test image file should exist at: " + TEST_IMAGE_PATH);

        // Create MultipartFile from test image
        try (FileInputStream fis = new FileInputStream(testImageFile)) {
            MultipartFile multipartFile = new MockMultipartFile(
                "test.png",
                "test.png",
                "image/png",
                fis
            );

            // Extract text using OCR
            OCRResult result = ocrService.extractText(multipartFile);

            // Assertions
            assertNotNull(result, "OCR result should not be null");
            assertTrue(result.isSuccess(), "OCR extraction should succeed");
            assertNotNull(result.getExtractedText(), "Extracted text should not be null");
            
            System.out.println("===========================================");
            System.out.println("OCR Extraction Results:");
            System.out.println("===========================================");
            System.out.println("File Name: " + result.getFileName());
            System.out.println("Content Type: " + result.getContentType());
            System.out.println("File Size: " + result.getFileSize() + " bytes");
            System.out.println("Confidence: " + result.getConfidence());
            System.out.println("Success: " + result.isSuccess());
            System.out.println("\nExtracted Text:");
            System.out.println("-------------------------------------------");
            System.out.println(result.getExtractedText());
            System.out.println("-------------------------------------------");
            System.out.println("Text Length: " + 
                (result.getExtractedText() != null ? result.getExtractedText().length() : 0) + " characters");
            
            if (result.getMetadata() != null) {
                System.out.println("\nMetadata:");
                result.getMetadata().forEach((key, value) -> 
                    System.out.println("  " + key + ": " + value)
                );
            }
            
            if (result.getDocumentType() != null) {
                System.out.println("\nDocument Type: " + result.getDocumentType());
                System.out.println("Classification Confidence: " + result.getClassificationConfidence());
            }
            System.out.println("===========================================");

            // Verify that some text was extracted (at least a few characters)
            String extractedText = result.getExtractedText();
            assertFalse(extractedText == null || extractedText.trim().isEmpty(), 
                "Extracted text should not be empty. OCR should have extracted some text from the image.");

            // Log if text is very short (might indicate OCR issues)
            if (extractedText.trim().length() < 10) {
                System.out.println("WARNING: Extracted text is very short. " +
                    "This might indicate OCR quality issues or the image contains minimal text.");
            }
        }
    }

    @Test
    void testExtractTextFromNonExistentFile() {
        // Create a mock file that doesn't exist
        MultipartFile multipartFile = new MockMultipartFile(
            "nonexistent.png",
            "nonexistent.png",
            "image/png",
            new byte[0]
        );

        // Should handle gracefully
        assertThrows(Exception.class, () -> {
            ocrService.extractText(multipartFile);
        }, "Should throw exception for invalid image file");
    }

    @Test
    void testOCRWithInvalidImage() {
        // Create an invalid image (empty or corrupted)
        MultipartFile invalidFile = new MockMultipartFile(
            "invalid.png",
            "invalid.png",
            "image/png",
            new byte[] { 0, 1, 2, 3, 4, 5 } // Not a valid PNG
        );

        // Should handle gracefully
        assertThrows(Exception.class, () -> {
            ocrService.extractText(invalidFile);
        }, "Should throw exception for invalid image data");
    }

    @Test
    void testOCRServiceAvailability() {
        // Test that we can check OCR availability
        boolean available = ocrService.isOcrAvailable();
        
        // On macOS with Homebrew Tesseract, it should be available
        assertTrue(available, 
            "OCR service should be available when Tesseract is properly configured");
    }
}


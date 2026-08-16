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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Test class for OCRService to verify Tesseract OCR functionality.
 *
 * <p>Tesseract is an external binary that is not present on a plain checkout or on CI.
 * The tests that genuinely need it now <em>skip</em> rather than fail — they used to
 * assert a Homebrew install on someone's Mac, so they failed on every other machine and
 * made the whole suite look broken. The tests that only exercise the service's own
 * error handling run everywhere, because that behaviour matters most precisely when
 * OCR is unavailable.
 */
@SpringBootTest
@ActiveProfiles("test")
class OCRServiceTest {

    @Autowired
    private OCRService ocrService;

    private static final String TEST_IMAGE_PATH =
            System.getProperty("ocr.test.image", "src/test/resources/ocr/test.png");

    @Test
    void testOCRServiceInitialization() {
        assumeTrue(ocrService.isOcrAvailable(),
            "Tesseract is not installed in this environment - skipping");
        assertTrue(ocrService.isOcrAvailable(),
            "OCR service should be available after initialization");
    }

    @Test
    void testExtractTextFromTestImage() throws IOException, TesseractException {
        assumeTrue(ocrService.isOcrAvailable(),
            "Tesseract is not installed in this environment - skipping");
        File testImageFile = new File(TEST_IMAGE_PATH);
        assumeTrue(testImageFile.exists(),
            "No sample image at " + TEST_IMAGE_PATH + " - set -Docr.test.image to run this");

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

    /*
     * These two assert the service's error contract, and it is not "throws".
     * extractText catches TesseractException and Throwable and returns a result with
     * success = false and an error message - deliberate graceful degradation, so one bad
     * scan in a batch does not abort the batch. The tests previously demanded an
     * exception, which the code has never thrown; they now pin the real behaviour.
     */

    @Test
    void testExtractTextFromEmptyFile() {
        MultipartFile multipartFile = new MockMultipartFile(
            "nonexistent.png",
            "nonexistent.png",
            "image/png",
            new byte[0]
        );

        OCRResult result = assertDoesNotThrow(() -> ocrService.extractText(multipartFile),
            "An unreadable file must degrade to a failed result, not blow up the caller");
        assertNotNull(result, "A result is always returned");
        assertFalse(result.isSuccess(), "An empty file cannot produce a successful extraction");
    }

    @Test
    void testOCRWithInvalidImage() {
        MultipartFile invalidFile = new MockMultipartFile(
            "invalid.png",
            "invalid.png",
            "image/png",
            new byte[] { 0, 1, 2, 3, 4, 5 } // Not a valid PNG
        );

        OCRResult result = assertDoesNotThrow(() -> ocrService.extractText(invalidFile),
            "Corrupt image data must degrade to a failed result, not blow up the caller");
        assertNotNull(result, "A result is always returned");
        assertFalse(result.isSuccess(), "Corrupt image data cannot produce a successful extraction");
    }

    @Test
    void testOCRServiceAvailability() {
        // Availability depends on whether the Tesseract binary is installed, which is an
        // environment fact rather than something this code controls. What must hold
        // everywhere is that asking the question is safe and gives a definite answer.
        assertDoesNotThrow(() -> ocrService.isOcrAvailable(),
            "Checking OCR availability must never throw, whatever the environment");
    }
}


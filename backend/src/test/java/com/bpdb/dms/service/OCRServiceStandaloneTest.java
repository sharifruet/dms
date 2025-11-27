package com.bpdb.dms.service;

import com.bpdb.dms.service.OCRService.OCRResult;
import net.sourceforge.tess4j.TesseractException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Standalone test class for OCRService to verify Tesseract OCR functionality
 * This test doesn't require full Spring Boot context
 */
class OCRServiceStandaloneTest {

    private static final String TEST_IMAGE_PATH = "/Users/til/Downloads/test.png";

    @Test
    void testExtractTextFromTestImage() throws IOException, TesseractException {
        // Check if test image exists
        File testImageFile = new File(TEST_IMAGE_PATH);
        assertTrue(testImageFile.exists(), 
            "Test image file should exist at: " + TEST_IMAGE_PATH);

        // Create OCRService instance and configure it
        OCRService ocrService = new OCRService();
        
        // Set configuration using reflection (mimicking Spring's @Value injection)
        setPrivateField(ocrService, "tesseractDataPath", "/opt/homebrew/share");
        setPrivateField(ocrService, "tesseractLanguage", "eng");
        setPrivateField(ocrService, "tesseractBinary", "/opt/homebrew/bin/tesseract");
        setPrivateField(ocrService, "ocrEnabled", true);
        setPrivateField(ocrService, "processImages", true);
        setPrivateField(ocrService, "pageSegMode", 6);
        
        // Initialize OCR service
        try {
            java.lang.reflect.Method setUpMethod = OCRService.class.getDeclaredMethod("setUp");
            setUpMethod.setAccessible(true);
            setUpMethod.invoke(ocrService);
        } catch (Exception e) {
            fail("Failed to initialize OCR service: " + e.getMessage());
        }

        // Verify OCR is available
        assertTrue(ocrService.isOcrAvailable(), 
            "OCR service should be available after initialization");

        // Create MultipartFile from test image
        try (FileInputStream fis = new FileInputStream(testImageFile)) {
            byte[] imageBytes = fis.readAllBytes();
            MultipartFile multipartFile = new MockMultipartFile(
                "test.png",
                "test.png",
                "image/png",
                imageBytes
            );

            // Extract text using OCR
            OCRResult result = ocrService.extractText(multipartFile);

            // Assertions
            assertNotNull(result, "OCR result should not be null");
            assertTrue(result.isSuccess(), "OCR extraction should succeed");
            assertNotNull(result.getExtractedText(), "Extracted text should not be null");
            
            System.out.println("\n===========================================");
            System.out.println("OCR Extraction Results:");
            System.out.println("===========================================");
            System.out.println("File Name: " + result.getFileName());
            System.out.println("Content Type: " + result.getContentType());
            System.out.println("File Size: " + result.getFileSize() + " bytes");
            System.out.println("Confidence: " + result.getConfidence());
            System.out.println("Success: " + result.isSuccess());
            System.out.println("\nExtracted Text:");
            System.out.println("-------------------------------------------");
            String extractedText = result.getExtractedText();
            System.out.println(extractedText);
            System.out.println("-------------------------------------------");
            System.out.println("Text Length: " + 
                (extractedText != null ? extractedText.length() : 0) + " characters");
            
            if (result.getMetadata() != null && !result.getMetadata().isEmpty()) {
                System.out.println("\nMetadata:");
                result.getMetadata().forEach((key, value) -> 
                    System.out.println("  " + key + ": " + value)
                );
            }
            
            if (result.getDocumentType() != null) {
                System.out.println("\nDocument Type: " + result.getDocumentType());
                System.out.println("Classification Confidence: " + result.getClassificationConfidence());
            }
            System.out.println("===========================================\n");

            // Verify that some text was extracted
            assertFalse(extractedText == null || extractedText.trim().isEmpty(), 
                "Extracted text should not be empty. OCR should have extracted some text from the image.");

            // Log if text is very short (might indicate OCR issues)
            if (extractedText.trim().length() < 10) {
                System.out.println("WARNING: Extracted text is very short (" + 
                    extractedText.trim().length() + " characters). " +
                    "This might indicate OCR quality issues or the image contains minimal text.");
            } else {
                System.out.println("✅ SUCCESS: OCR extracted " + extractedText.trim().length() + 
                    " characters from the image!");
            }
        }
    }

    /**
     * Helper method to set private fields using reflection
     */
    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName + ": " + e.getMessage(), e);
        }
    }
}


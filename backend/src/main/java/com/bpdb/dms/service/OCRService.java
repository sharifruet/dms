package com.bpdb.dms.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import java.io.*;

/**
 * Service for OCR processing and text extraction
 */
@Service
public class OCRService {
    
    private static final Logger logger = LoggerFactory.getLogger(OCRService.class);
    
    @Value("${app.tesseract.data.path:/usr/share/tesseract-ocr/4.00/tessdata}")
    private String tesseractDataPath;
    
    @Value("${app.tesseract.language:eng}")
    private String tesseractLanguage;
    
    @Value("${app.ocr.psm:6}")
    private int pageSegMode;

    @Value("${app.tesseract.binary:tesseract}")
    private String tesseractBinary;
    
    @Value("${app.ocr.enabled:true}")
    private boolean ocrEnabled;
    
    @Value("${app.ocr.process-images:true}")
    private boolean processImages;
    private final ITesseract tesseract;
    private final Tika tika;
    private volatile boolean ocrAvailable;
    
    @Autowired(required = false)
    private DocumentClassificationService documentClassificationService;
    
    public OCRService() {
        this.tesseract = new Tesseract();
        this.tika = new Tika();
        this.ocrAvailable = false;
    }
    
    @PostConstruct
    public void setUp() {
        initializeTesseract();
    }
    
    /**
     * Initialize Tesseract OCR engine
     */
    private void initializeTesseract() {
        if (!ocrEnabled) {
            ocrAvailable = false;
            logger.info("OCR processing disabled via configuration");
            return;
        }
        
        try {
            // Resolve tessdata path if the configured one doesn't exist (common on macOS/Homebrew)
            Path configuredPath = Paths.get(tesseractDataPath);
            // tess4j expects datapath pointing to the PARENT of the 'tessdata' folder
            if (configuredPath.getFileName() != null && configuredPath.getFileName().toString().equalsIgnoreCase("tessdata")) {
                configuredPath = configuredPath.getParent();
                if (configuredPath != null) {
                    tesseractDataPath = configuredPath.toString();
                }
            }
            if (!Files.exists(configuredPath)) {
                String resolvedPath = resolveTessdataPathFallback();
                if (resolvedPath != null && !resolvedPath.isBlank()) {
                    logger.warn("Configured tessdata path not found: {}. Using detected path: {}", tesseractDataPath, resolvedPath);
                    tesseractDataPath = resolvedPath;
                } else {
                    logger.warn("Configured tessdata path not found and no fallback found: {}", tesseractDataPath);
                    ocrAvailable = false;
                    return;
                }
            }
            
            tesseract.setDatapath(tesseractDataPath);
            tesseract.setLanguage(tesseractLanguage);
            // Page segmentation mode: default to 6 (Assume a single uniform block of text)
            tesseract.setPageSegMode(pageSegMode);
            tesseract.setOcrEngineMode(1); // Neural nets LSTM engine only
            // Quick sanity check: verify 'eng.traineddata' can be resolved
            try {
                Path trained = Paths.get(tesseractDataPath, "tessdata", tesseractLanguage + ".traineddata");
                logger.info("Tesseract OCR initialized (datapath: {}, lang: {}, traineddata exists: {})",
                        tesseractDataPath, tesseractLanguage, Files.exists(trained));
            } catch (Exception ig) {
                logger.info("Tesseract OCR initialized (datapath: {}, lang: {})", tesseractDataPath, tesseractLanguage);
            }
            ocrAvailable = true;
        } catch (Exception e) {
            logger.error("Failed to initialize Tesseract OCR: {}", e.getMessage());
            ocrAvailable = false;
        } catch (Error err) {
            logger.error("Critical error while initializing Tesseract OCR: {}", err.getMessage());
            ocrAvailable = false;
        }
    }

    /**
     * Attempt to resolve Tesseract tessdata directory automatically.
     * Order: TESSDATA_PREFIX env -> `tesseract --print-tesseract-data-dir` -> common Homebrew paths.
     */
    private String resolveTessdataPathFallback() {
        try {
            // 1) Environment variable
            String env = System.getenv("TESSDATA_PREFIX");
            if (env != null && !env.isBlank()) {
                Path envPath = Paths.get(env);
                if (Files.exists(envPath)) {
                    return envPath.toString();
                }
            }

            // 2) Ask tesseract binary
            try {
                Process p = new ProcessBuilder("tesseract", "--print-tesseract-data-dir").redirectErrorStream(true).start();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line = br.readLine();
                    if (line != null) {
                        String candidate = line.trim();
                        if (!candidate.isBlank() && Files.exists(Paths.get(candidate))) {
                            return candidate;
                        }
                    }
                }
            } catch (Exception ignored) {
                // Ignore - binary may not be on PATH
            }

            // 3) Common Homebrew paths (Apple Silicon / Intel)
            String[] brewCandidates = new String[] {
                "/opt/homebrew/share/tessdata",
                "/usr/local/share/tessdata"
            };
            for (String candidate : brewCandidates) {
                if (Files.exists(Paths.get(candidate))) {
                    return candidate;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to resolve tessdata path automatically: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Extract text from uploaded file using OCR
     */
    @Async
    public CompletableFuture<OCRResult> extractTextAsync(MultipartFile file, Long documentId) {
        try {
            logger.info("Starting OCR processing for document: {}", documentId);
            
            OCRResult result = extractText(file);
            result.setDocumentId(documentId);
            
            int textLength = result.getExtractedText() != null ? result.getExtractedText().length() : 0;
            logger.info("OCR processing completed for document: {} - Text length: {}", 
                       documentId, textLength);
            
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            logger.error("OCR processing failed for document: {} - Error: {}", documentId, e.getMessage());
            OCRResult errorResult = new OCRResult();
            errorResult.setDocumentId(documentId);
            errorResult.setSuccess(false);
            errorResult.setErrorMessage(e.getMessage());
            return CompletableFuture.completedFuture(errorResult);
        }
    }
    
    /**
     * Extract text from file synchronously
     */
    public OCRResult extractText(MultipartFile file) throws IOException, TesseractException {
        OCRResult result = new OCRResult();
        
        if (!ocrAvailable) {
            result.setSuccess(false);
            result.setErrorMessage("OCR is currently unavailable");
            return result;
        }
        
        // Detect file type
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        
        result.setFileName(fileName);
        result.setContentType(contentType);
        result.setFileSize(file.getSize());
        
        // If OCR is completely disabled, skip all OCR processing
        if (!ocrEnabled) {
            logger.info("OCR is disabled, skipping OCR processing for file: {}", fileName);
            result.setExtractedText("");
            result.setSuccess(true);
            result.setConfidence(0.0);
            return result;
        }
        
        try {
            String extractedText;
            
            if (isImageFile(contentType)) {
                // Process image files with OCR (skip if disabled to prevent crashes)
                if (processImages) {
                    try {
                        extractedText = processImageWithOCR(file);
                    } catch (TesseractException te) {
                        // Tesseract-specific errors (library not available, initialization issues)
                        logger.error("Tesseract OCR error for image file {}: {}", fileName, te.getMessage());
                        result.setErrorMessage("Tesseract OCR is not properly configured. " + 
                            "Please ensure Tesseract is installed on the system. " + 
                            "Details: " + te.getMessage());
                        extractedText = "";
                    } catch (Throwable t) {
                        // Catch native crashes and other unexpected errors
                        logger.error("OCR processing crashed for image file {}: {} - {}", 
                            fileName, t.getClass().getSimpleName(), t.getMessage());
                        result.setErrorMessage("OCR processing failed: " + t.getMessage());
                        extractedText = "";
                    }
                } else {
                    logger.info("OCR for images is disabled, skipping OCR for file: {}", fileName);
                    extractedText = "";
                }
            } else if (isPDFFile(contentType)) {
                // Process PDF files
                extractedText = processPDFWithOCR(file);
            } else if (isOfficeDocument(contentType)) {
                // Process Office documents
                extractedText = processOfficeDocument(file);
            } else {
                // Process as plain text
                extractedText = processPlainText(file);
            }
            
            result.setExtractedText(extractedText);
            result.setConfidence(calculateConfidence(extractedText));
            result.setSuccess(true);
            
            // Extract metadata
            Map<String, String> metadata = extractMetadata(file);
            
            // add contract number extraction
            Map<String, String> contractData = getContractNumber(extractedText);
            metadata.putAll(contractData);
            
            result.setMetadata(metadata);
            
            // Classify document type
            DocumentTypeClassification classification = classifyDocument(extractedText, fileName);
            result.setDocumentType(classification.getDocumentType());
            result.setClassificationConfidence(classification.getConfidence());
            
        } catch (Exception e) {
            logger.error("Error processing file {}: {}", fileName, e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }
    
    public boolean isOcrAvailable() {
        return ocrAvailable;
    }
    
    /**
     * Process image files with OCR
     */
    private String processImageWithOCR(MultipartFile file) throws IOException, TesseractException {
        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            throw new IOException("Unable to read image file");
        }
        
        // Preprocess image for better OCR results
        BufferedImage processedImage = preprocessImage(image);
        
        try {
            return tesseract.doOCR(processedImage);
        } catch (UnsatisfiedLinkError | NoClassDefFoundError ex) {
            // Native library errors - try external fallback
            logger.warn("tess4j native library error: {}. Trying external 'tesseract' fallback...", ex.toString());
            String fb = runExternalTesseract(processedImage);
            if (fb != null && !fb.trim().isEmpty()) {
                logger.info("External tesseract fallback succeeded, extracted {} characters", fb.length());
                return fb;
            }
            throw new TesseractException("tess4j native library failed and external tesseract fallback failed", ex);
        } catch (TesseractException ex) {
            // Tesseract-specific errors - try external fallback
            logger.warn("tess4j OCR error: {}. Trying external 'tesseract' fallback...", ex.toString());
            String fb = runExternalTesseract(processedImage);
            if (fb != null && !fb.trim().isEmpty()) {
                logger.info("External tesseract fallback succeeded, extracted {} characters", fb.length());
                return fb;
            }
            throw ex;
        } catch (Exception ex) {
            // Other exceptions - try external fallback
            logger.warn("Unexpected OCR error: {}. Trying external 'tesseract' fallback...", ex.toString());
            String fb = runExternalTesseract(processedImage);
            if (fb != null && !fb.trim().isEmpty()) {
                logger.info("External tesseract fallback succeeded, extracted {} characters", fb.length());
                return fb;
            }
            throw new TesseractException("OCR processing failed and external tesseract fallback failed", ex);
        }
    }

    /**
     * Fallback OCR using system 'tesseract' CLI by writing a temporary PNG and reading stdout.
     */
    private String runExternalTesseract(BufferedImage image) {
        File tmp = null;
        try {
            tmp = File.createTempFile("ocr_img_", ".png");
            ImageIO.write(image, "png", tmp);

            // TESSDATA_PREFIX should point to the parent folder that contains 'tessdata'
            String tessPrefix = tesseractDataPath;
            if (tessPrefix != null) {
                Path p = Paths.get(tessPrefix);
                if (p.getFileName() != null && "tessdata".equalsIgnoreCase(p.getFileName().toString())) {
                    Path parent = p.getParent();
                    if (parent != null) {
                        tessPrefix = parent.toString();
                    }
                }
            }

            ProcessBuilder pb = new ProcessBuilder(
                    tesseractBinary,
                    tmp.getAbsolutePath(),
                    "stdout",
                    "-l", tesseractLanguage,
                    "--psm", String.valueOf(this.pageSegMode)
            );
            Map<String, String> env = pb.environment();
            if (tessPrefix != null && !tessPrefix.isBlank()) {
                env.put("TESSDATA_PREFIX", tessPrefix);
            }
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            StringBuilder out = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }
            int code = proc.waitFor();
            if (code == 0) {
                return out.toString();
            } else {
                logger.warn("External tesseract exited with code {}. Output: {}", code, out.toString());
            }
        } catch (Exception ex) {
            logger.error("External tesseract fallback error: {}", ex.toString());
        } finally {
            if (tmp != null) {
                try { Files.deleteIfExists(tmp.toPath()); } catch (Exception ignore) {}
            }
        }
        return null;
    }
    
    /**
     * Process PDF files with OCR
     */
	private String processPDFWithOCR(MultipartFile file) throws IOException, TesseractException {

		// 1. Try Tika extraction first
		try {
			String text = tika.parseToString(file.getInputStream());
			if (text != null && !text.trim().isEmpty()) {
				return text;
			}
		} catch (Exception e) {
			logger.warn("Tika failed, falling back to OCR", e);
		}
		try {
			// 2. Load PDF properly (PDFBox 3.x)
			byte[] pdfBytes = file.getBytes();
			PDDocument document = PDDocument.load(pdfBytes);

			PDFRenderer renderer = new PDFRenderer(document);
			StringBuilder sb = new StringBuilder();

			// 3. OCR each page
			for (int i = 0; i < document.getNumberOfPages(); i++) {
				BufferedImage image = renderer.renderImageWithDPI(i, 300);
				sb.append(tesseract.doOCR(image)).append("\n");
			}

			document.close();
			return sb.toString();
		} catch (Error err) {
			logger.error("Native OCR error: {}", err.getMessage());
			throw new TesseractException("Native OCR error: " + err.getMessage());
		}
	}

    
    /**
     * Process Office documents
     */
    private String processOfficeDocument(MultipartFile file) throws IOException, TikaException {
        return tika.parseToString(file.getInputStream());
    }
    
    /**
     * Process plain text files
     */
    private String processPlainText(MultipartFile file) throws IOException {
        return new String(file.getBytes());
    }
    
    /**
     * Preprocess image for better OCR results
     */
    private BufferedImage preprocessImage(BufferedImage originalImage) {
        // Convert to grayscale
        BufferedImage gray = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        java.awt.Graphics2D g = gray.createGraphics();
        g.drawImage(originalImage, 0, 0, null);
        g.dispose();
        
        // Apply simple global thresholding (Otsu-like approximation)
        int w = gray.getWidth();
        int h = gray.getHeight();
        int[] histogram = new int[256];
        int[] pixels = new int[w * h];
        gray.getRaster().getPixels(0, 0, w, h, pixels);
        for (int i = 0; i < pixels.length; i++) {
            int v = pixels[i];
            if (v < 0) v = 0; if (v > 255) v = 255;
            histogram[v]++;
        }
        // Compute threshold (basic Otsu)
        int total = w * h;
        float sum = 0;
        for (int t = 0; t < 256; t++) sum += t * histogram[t];
        float sumB = 0; int wB = 0; int wF; float varMax = 0; int threshold = 127;
        for (int t = 0; t < 256; t++) {
            wB += histogram[t]; if (wB == 0) continue; wF = total - wB; if (wF == 0) break;
            sumB += (float) (t * histogram[t]);
            float mB = sumB / wB; float mF = (sum - sumB) / wF;
            float varBetween = (float) wB * (float) wF * (mB - mF) * (mB - mF);
            if (varBetween > varMax) { varMax = varBetween; threshold = t; }
        }
        // Create binary image
        BufferedImage binary = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        int idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = pixels[idx++];
                int b = v > threshold ? 0xFFFFFF : 0x000000;
                binary.setRGB(x, y, (0xFF << 24) | b);
            }
        }
        return binary;
    }
    
    /**
     * Extract metadata from file
     */
    private Map<String, String> extractMetadata(MultipartFile file) {
        Map<String, String> metadata = new HashMap<>();
        
        try {
            metadata.put("fileName", file.getOriginalFilename());
            metadata.put("contentType", file.getContentType());
            metadata.put("fileSize", String.valueOf(file.getSize()));
            metadata.put("lastModified", String.valueOf(file.getInputStream().available()));
            
            // Extract additional metadata using Tika
            Map<String, String> tikaMetadata = new HashMap<>();
            try {
                tikaMetadata = tika.parseToString(file.getInputStream()).length() > 0 ? 
                    Map.of("hasText", "true") : Map.of("hasText", "false");
            } catch (Exception e) {
                logger.warn("Failed to extract Tika metadata: {}", e.getMessage());
            }
            
            metadata.putAll(tikaMetadata);
            
        } catch (Exception e) {
            logger.error("Error extracting metadata: {}", e.getMessage());
        }
        
        return metadata;
    }
    
    /**
     * Classify document type based on content using DocumentClassificationService
     */
    private DocumentTypeClassification classifyDocument(String text, String fileName) {
        DocumentTypeClassification classification = new DocumentTypeClassification();
        
        // Use the enhanced classification service if available
        if (documentClassificationService != null) {
            try {
                DocumentClassificationService.ClassificationResult result = 
                    documentClassificationService.classify(text, fileName);
                
                classification.setDocumentType(result.getDocumentType().name());
                classification.setConfidence(result.getConfidence());
            } catch (Exception e) {
                logger.warn("Document classification service failed, using fallback: {}", e.getMessage());
                // Fall through to fallback
            }
        }
        
        // Fallback to simple classification if service not available or failed
        if (classification.getDocumentType() == null || classification.getConfidence() < 0.3) {
            String lowerText = text != null ? text.toLowerCase() : "";
            
            if (lowerText.contains("tender") || lowerText.contains("bid") || lowerText.contains("proposal")) {
                classification.setDocumentType("TENDER_DOCUMENT");
                classification.setConfidence(0.6);
            } else if (lowerText.contains("bank guarantee") || lowerText.contains("bg")) {
                classification.setDocumentType("BANK_GUARANTEE_BG");
                classification.setConfidence(0.6);
            } else if (lowerText.contains("contract") || lowerText.contains("agreement")) {
                classification.setDocumentType("CONTRACT_AGREEMENT");
                classification.setConfidence(0.6);
            } else if (lowerText.contains("invoice") || lowerText.contains("bill")) {
                classification.setDocumentType("BILL");
                classification.setConfidence(0.6);
            } else {
                classification.setDocumentType("OTHER");
                classification.setConfidence(0.3);
            }
        }
        
        return classification;
    }
    
    /**
     * Calculate confidence score for extracted text
     */
    private double calculateConfidence(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }
        
        // Simple confidence calculation based on text length and character patterns
        double confidence = 0.5; // Base confidence
        
        // Increase confidence for longer text
        if (text.length() > 100) {
            confidence += 0.2;
        }
        if (text.length() > 500) {
            confidence += 0.1;
        }
        
        // Increase confidence for structured text (contains numbers, dates, etc.)
        if (text.matches(".*\\d+.*")) {
            confidence += 0.1;
        }
        
        return Math.min(confidence, 1.0);
    }
    
    /**
     * Check if file is an image
     */
    private boolean isImageFile(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }
    
    /**
     * Check if file is a PDF
     */
    private boolean isPDFFile(String contentType) {
        return "application/pdf".equals(contentType);
    }
    
    /**
     * Check if file is an Office document
     */
    private boolean isOfficeDocument(String contentType) {
        return contentType != null && (
            contentType.contains("word") ||
            contentType.contains("excel") ||
            contentType.contains("powerpoint") ||
            contentType.contains("officedocument")
        );
    }
    
    /**
     * OCR Result class
     */
    public static class OCRResult {
        private Long documentId;
        private String fileName;
        private String contentType;
        private Long fileSize;
        private String extractedText;
        private double confidence;
        private boolean success;
        private String errorMessage;
        private Map<String, String> metadata;
        private String documentType;
        private double classificationConfidence;
        
        // Getters and setters
        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        public String getExtractedText() { return extractedText; }
        public void setExtractedText(String extractedText) { this.extractedText = extractedText; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }
        public double getClassificationConfidence() { return classificationConfidence; }
        public void setClassificationConfidence(double classificationConfidence) { this.classificationConfidence = classificationConfidence; }
    }
    
    /**
     * Document Type Classification class
     */
    public static class DocumentTypeClassification {
        private String documentType;
        private double confidence;
        
        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }
    
	private Map<String, String> getContractNumber(String extractedText) {

		String[] contractPatterns = { "Contract N[o0]\\.:?\\s*([A-Za-z0-9\\/\\.-]+)" };
		
		String[] datePatterns = { "Date\\s*:?\\s*(\\d{2}/\\d{2}/\\d{4})", "Dated\\s*:?\\s*(\\d{2}/\\d{2}/\\d{4})",
				"Date\\s*:?\\s*(\\d{4}-\\d{2}-\\d{2})", "Dated\\s*:?\\s*(\\d{4}-\\d{2}-\\d{2})" };
		
		Map<String, String> contractData = new HashMap<>();
		
		for (String patternStr : contractPatterns) {
			Pattern pattern = Pattern.compile(patternStr);
			Matcher matcher = pattern.matcher(extractedText);
			if (matcher.find()) {
				contractData.put("contractNumber", matcher.group(1));
				break;
			}
		}
		
		for (String patternStr : datePatterns) {
			Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(extractedText);
			if (matcher.find()) {
				contractData.put("contractDate", matcher.group(1));
				break;
			}
		}

		return contractData;
	}
}

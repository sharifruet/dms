package com.bpdb.dms.controller;

import com.bpdb.dms.service.WatermarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Controller for document watermarking operations
 */
@RestController
@RequestMapping("/api/watermark")
@CrossOrigin(origins = "*")
public class WatermarkController {
    
    @Autowired
    private WatermarkService watermarkService;
    
    /**
     * Apply text watermark to PDF document
     */
    @PostMapping("/pdf/text")
    public ResponseEntity<byte[]> applyTextWatermarkToPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("text") String text,
            @RequestParam(value = "position", defaultValue = "CENTER") String position,
            @RequestParam(value = "fontSize", defaultValue = "48") int fontSize,
            @RequestParam(value = "opacity", defaultValue = "0.5") float opacity,
            @RequestParam(value = "rotation", required = false) Integer rotation,
            Authentication authentication) {
        
        try {
            byte[] pdfBytes = file.getBytes();
            byte[] watermarkedPdf = watermarkService.addTextWatermarkToPdf(
                    pdfBytes, text, position, fontSize, opacity, rotation);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "watermarked_" + file.getOriginalFilename());
            headers.setContentLength(watermarkedPdf.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(watermarkedPdf);
                    
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Apply image watermark to PDF document
     */
    @PostMapping("/pdf/image")
    public ResponseEntity<byte[]> applyImageWatermarkToPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("watermarkImage") MultipartFile watermarkImage,
            @RequestParam(value = "position", defaultValue = "CENTER") String position,
            @RequestParam(value = "opacity", defaultValue = "0.5") float opacity,
            Authentication authentication) {
        
        try {
            byte[] pdfBytes = file.getBytes();
            byte[] imageBytes = watermarkImage.getBytes();
            
            byte[] watermarkedPdf = watermarkService.addImageWatermarkToPdf(
                    pdfBytes, imageBytes, position, opacity);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "watermarked_" + file.getOriginalFilename());
            headers.setContentLength(watermarkedPdf.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(watermarkedPdf);
                    
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Apply text watermark to image
     */
    @PostMapping("/image/text")
    public ResponseEntity<byte[]> applyTextWatermarkToImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("text") String text,
            @RequestParam(value = "position", defaultValue = "CENTER") String position,
            @RequestParam(value = "fontSize", defaultValue = "48") int fontSize,
            @RequestParam(value = "fontColor", defaultValue = "#000000") String fontColor,
            @RequestParam(value = "opacity", defaultValue = "0.5") float opacity,
            Authentication authentication) {
        
        try {
            byte[] imageBytes = file.getBytes();
            byte[] watermarkedImage = watermarkService.addTextWatermarkToImage(
                    imageBytes, text, position, fontSize, fontColor, opacity);
            
            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = "image/png";
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", "watermarked_" + file.getOriginalFilename());
            headers.setContentLength(watermarkedImage.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(watermarkedImage);
                    
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Preview watermark configuration (returns watermark info without applying)
     */
    @PostMapping("/preview")
    public ResponseEntity<Map<String, Object>> previewWatermark(
            @RequestBody Map<String, Object> config) {
        
        try {
            Map<String, Object> preview = Map.of(
                    "success", true,
                    "message", "Watermark preview generated",
                    "config", config
            );
            
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}

